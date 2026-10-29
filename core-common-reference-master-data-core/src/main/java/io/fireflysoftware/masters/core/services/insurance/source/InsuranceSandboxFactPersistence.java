package io.fireflysoftware.masters.core.services.insurance.source;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;
import org.fireflyframework.core.transaction.*;
import org.fireflyframework.core.transaction.impl.DefaultBoundedRootTransaction;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceOutcome;
import io.fireflysoftware.masters.models.repositories.insurance.source.*;
import io.fireflysoftware.masters.models.repositories.insurance.source.impl.InsuranceSandboxFactRepositoryImpl;
import reactor.core.publisher.Mono;

/** Explicit source-role resources. A database clock/role observation is not a caller/PDP grant. */
public final class InsuranceSandboxFactPersistence {
    public static final Duration LIMIT=Duration.ofSeconds(5);
    public static final class Session {
        private final BoundedRootTransaction transactions;
        private final InsuranceSandboxFactRepository repository;
        private final String role;
        public Session(R2dbcEntityTemplate operations,R2dbcTransactionManager manager,boolean writer){
            var client=operations.getDatabaseClient();
            if(!(client.getConnectionFactory() instanceof OwnedLeaseConnectionFactory factory))throw new IllegalArgumentException("Qualified source resource graph required");
            transactions=new DefaultBoundedRootTransaction(BoundedR2dbcResources.verify(factory,client,operations,manager),new CleanupPolicy(Duration.ofSeconds(2),Duration.ofSeconds(2)));
            repository=new InsuranceSandboxFactRepositoryImpl(operations);role=writer?"masters_ins_ref_source_registrar":"masters_ins_ref_source_reader";
        }
    }
    public final class Work {
        private final InsuranceSandboxFactRepository repository;
        private final RootTransactionScope scope;
        private final Instant firstNativeNow,observed;
        private final long before;
        private R2dbcOperationBudget budget;
        private Instant until;
        private Work(Session session,RootTransactionScope scope,R2dbcOperationBudget original,Instant originalUntil,Instant nativeNow,Instant observed,long before){
            this.repository=session.repository;this.scope=scope;this.firstNativeNow=nativeNow;this.observed=observed;this.before=before;
            this.until=minimum(originalUntil,nativeNow.plus(LIMIT),observed.plusMillis(4999));
            this.budget=original.intersect(R2dbcOperationBudget.observed(observed,until,before,LIMIT,clock,nanos));scope.narrow(budget);
        }
        public InsuranceSandboxFactRepository repository(){return repository;}
        public Instant nativeNow(){return firstNativeNow;}
        public R2dbcOperationBudget budget(){return budget;}
        public Instant until(){return until;}
        public void narrow(Instant...caps){budget.remaining();Instant next=minimum(until,minimum(caps));budget=budget.intersect(R2dbcOperationBudget.observed(observed,next,before,LIMIT,clock,nanos));until=next;scope.narrow(budget);budget.remaining();}
        @Override public String toString(){return "InsuranceSandboxProtectedWork";}
    }
    public record Completed<T>(T value,R2dbcOperationBudget budget,Instant until,Instant observedAt,TransactionFacts facts) {
        @Override public String toString(){return "InsuranceSandboxCompletedWork";}
    }
    private final Session writer,reader;
    private final Clock clock;
    private final LongSupplier nanos;
    public InsuranceSandboxFactPersistence(Session writer,Session reader,Clock clock,LongSupplier nanos){this.writer=Objects.requireNonNull(writer);this.reader=Objects.requireNonNull(reader);this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);}
    public boolean usesTimeSources(Clock clock,LongSupplier nanos){return this.clock==clock&&this.nanos==nanos;}
    public <T> Mono<Completed<T>> run(UUID actionId,boolean write,R2dbcOperationBudget original,Instant originalUntil,Function<Work,Mono<T>> callback){
        return Mono.defer(()->{
            original.remaining();Session session=write?writer:reader;var definition=new DefaultTransactionDefinition();definition.setReadOnly(!write);
            definition.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
            var retained=new AtomicReference<Work>();var finalObserved=new AtomicReference<Instant>();var failure=new AtomicReference<Throwable>();var workCompleted=new AtomicBoolean();
            return session.transactions.execute(original,definition,scope->{
                long before=nanos.getAsLong();Instant observed=clock.instant();
                return session.repository.runtimeAllowed(session.role).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(InsuranceSandboxSourceRules.unavailable()))
                    .then(session.repository.now()).flatMap(nativeNow->{
                        var work=new Work(session,scope,original,originalUntil,nativeNow,observed,before);retained.set(work);
                        return Mono.defer(()->callback.apply(work)).switchIfEmpty(Mono.error(InsuranceSandboxSourceRules.unavailable()))
                            .flatMap(value->session.repository.now().flatMap(last->{
                                work.budget().remaining();if(last.isBefore(nativeNow)||!last.isBefore(work.until()))throw InsuranceSandboxSourceRules.unavailable();
                                finalObserved.set(last);workCompleted.set(true);return Mono.just(value);
                            }));
                    }).doOnError(failure::set);
            }).map(done->{try{var work=retained.get();work.budget().remaining();return new Completed<>(done.value(),work.budget(),work.until(),finalObserved.get(),done.transactionFacts());}
                catch(RuntimeException expired){var f=done.transactionFacts();throw new InsuranceReferenceOutcome(actionId,f.commitAcknowledged(),f.boundaryCompleted(),f.rollbackAcknowledged());}})
              .onErrorMap(RootTransactionException.class,error->{var f=error.transactionFacts();if(!workCompleted.get()&&failure.get()!=null&&f.rollbackAcknowledged()&&error.cleanupFact()==CleanupFact.COMPLETE&&!f.commitAcknowledged()&&!f.boundaryCompleted())return failure.get();return new InsuranceReferenceOutcome(actionId,f.commitAcknowledged(),f.boundaryCompleted(),f.rollbackAcknowledged());});
        });
    }
    private static Instant minimum(Instant...values){return Arrays.stream(values).filter(Objects::nonNull).min(Instant::compareTo).orElseThrow();}
}
