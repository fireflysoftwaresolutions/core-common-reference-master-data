package io.fireflysoftware.masters.core.services.insurance.reference;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.fireflyframework.core.transaction.*;
import org.fireflyframework.core.transaction.impl.DefaultBoundedRootTransaction;
import io.fireflysoftware.masters.models.repositories.insurance.reference.InsuranceReferenceRepository;
import io.fireflysoftware.masters.models.repositories.insurance.reference.impl.InsuranceReferenceRepositoryImpl;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;
import reactor.core.publisher.Mono;

/** Opt-in aggregate adapter. Production resource registration remains absent. */
public final class InsuranceReferencePersistence {
    public enum Purpose { READ, WRITE }
    public static final class Session {
        private final InsuranceReferenceRepository repository;
        private final InsuranceReferenceNativeContext context;
        private final BoundedRootTransaction transactions;
        private final org.springframework.r2dbc.core.DatabaseClient client;
        public Session(R2dbcEntityTemplate template,R2dbcTransactionManager manager,InsuranceReferenceNativeContext context) {
            Objects.requireNonNull(context);var client=template.getDatabaseClient();
            if(!(client.getConnectionFactory() instanceof OwnedLeaseConnectionFactory factory)||context.databaseClient()!=client)
                throw new IllegalArgumentException("One qualified insurance reference resource graph required");
            var resources=BoundedR2dbcResources.verify(factory,client,template,manager);
            this.repository=new InsuranceReferenceRepositoryImpl(template);this.context=context;this.client=client;
            transactions=new DefaultBoundedRootTransaction(resources,new CleanupPolicy(Duration.ofSeconds(2),Duration.ofSeconds(2)));
        }
        private void requireCurrent(){if(context.databaseClient()!=client)throw unavailable();}
        public InsuranceReferenceRepository repository(){return repository;}
    }
    /** Original and every observed cap are retained; a subsequent context cannot renew them. */
    public static final class Window {
        private final List<R2dbcOperationBudget> limits;
        private Window(List<R2dbcOperationBudget> limits){this.limits=List.copyOf(limits);}
        public Duration remaining(){return limits.stream().map(R2dbcOperationBudget::remaining).min(Duration::compareTo).orElseThrow();}
        private R2dbcOperationBudget constrain(R2dbcOperationBudget original){for(var limit:limits)original=original.intersect(limit);return original;}
        public void narrow(RootTransactionScope scope){for(var limit:limits)scope.narrow(limit);}
        @Override public String toString(){return "InsuranceReferenceNativeWindow";}
    }
    public record Work(Session session,InsuranceReferenceNativeContext.Stamp stamp,Window window) { @Override public String toString(){return "InsuranceReferenceProtectedWork";} }
    public record Completed<T>(T value,Window window,TransactionFacts facts) { @Override public String toString(){return "InsuranceReferenceCompletedWork";} }
    private final Session commands,reads;
    private final Clock clock;
    private final LongSupplier nanos;
    public InsuranceReferencePersistence(Session commands,Session reads,Clock clock,LongSupplier nanos){this.commands=Objects.requireNonNull(commands);this.reads=Objects.requireNonNull(reads);this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);}

    public Window observeSource(Window inherited,InsuranceReferenceSourceVerifier.Evidence evidence,long before,Instant observed) {
        inherited.remaining();InsuranceReferenceContracts.requireTimestamp(evidence.observedAt());InsuranceReferenceContracts.requireTimestamp(evidence.effectiveUntil());
        long millis=Math.subtractExact(Math.subtractExact(evidence.effectiveUntil().toEpochMilli(),evidence.observedAt().toEpochMilli()),1L);
        if(millis<=0||millis>29999||evidence.observedAt().isAfter(clock.instant()))throw unavailable();
        Instant until=Collections.min(List.of(evidence.effectiveUntil(),observed.plusMillis(millis)));
        var cap=R2dbcOperationBudget.observed(observed,until,before,Duration.ofSeconds(30),clock,nanos);
        var limits=new ArrayList<>(inherited.limits);limits.add(cap);var value=new Window(limits);value.remaining();return value;
    }
    public boolean usesTimeSources(Clock clock,LongSupplier nanos){return this.clock==clock&&this.nanos==nanos;}

    /** Compatibility selector for business session; it never selects physical transaction access mode. */
    public <T> Mono<Completed<T>> execute(R2dbcOperationBudget original,InsuranceReferenceNativeContext.Request request,
            boolean businessReadOnly,Window inherited,Function<Work,Mono<T>> work) {
        return execute(original,request,businessReadOnly?Purpose.READ:Purpose.WRITE,inherited,work);
    }
    public <T> Mono<Completed<T>> execute(R2dbcOperationBudget original,InsuranceReferenceNativeContext.Request request,
            Purpose purpose,Window inherited,Function<Work,Mono<T>> work) {
        return Mono.defer(()->{
            original.remaining();if(inherited!=null)inherited.remaining();
            Objects.requireNonNull(purpose);Session session=purpose==Purpose.READ?reads:commands;session.requireCurrent();var definition=new DefaultTransactionDefinition();
            // The restricted reader remains business SELECT-only; persistent private context needs physical writes.
            definition.setReadOnly(false);
            definition.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
            var window=new AtomicReference<Window>();var failure=new AtomicReference<Throwable>();var workCompleted=new AtomicBoolean();
            var acquisitionBudget=inherited==null?original:inherited.constrain(original);
            return session.transactions.execute(acquisitionBudget,definition,scope->{
                if(inherited!=null)inherited.narrow(scope);
                session.requireCurrent();long before=nanos.getAsLong();Instant observed=clock.instant();
                return Mono.defer(()->session.context.begin(request)).switchIfEmpty(Mono.error(unavailable())).flatMap(stamp->{
                    session.requireCurrent();validate(request,stamp);
                    long nativeMillis=Math.subtractExact(Math.subtractExact(stamp.effectiveUntil().toEpochMilli(),stamp.dbNowMillis()),1L);
                    if(nativeMillis<=0||nativeMillis>29999)throw unavailable();
                    Instant until=Collections.min(List.of(request.authorityUntil(),stamp.effectiveUntil(),observed.plusMillis(nativeMillis)));
                    var cap=R2dbcOperationBudget.observed(observed,until,before,Duration.ofSeconds(30),clock,nanos);
                    var limits=new ArrayList<R2dbcOperationBudget>();limits.add(original);if(inherited!=null)limits.addAll(inherited.limits);limits.add(cap);
                    var retained=new Window(limits);retained.remaining();retained.narrow(scope);window.set(retained);
                    return Mono.defer(()->work.apply(new Work(session,stamp,retained))).switchIfEmpty(Mono.error(unavailable()))
                        .flatMap(value->{session.requireCurrent();return session.context.complete(request).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(unavailable()))
                            .then(Mono.fromSupplier(()->{session.requireCurrent();retained.remaining();workCompleted.set(true);return value;}));});
                }).doOnError(failure::set);
            }).map(done->{try{session.requireCurrent();window.get().remaining();}catch(RuntimeException expired){var facts=done.transactionFacts();throw new InsuranceReferenceOutcome(request.actionId(),facts.commitAcknowledged(),facts.boundaryCompleted(),facts.rollbackAcknowledged());}return new Completed<>(done.value(),window.get(),done.transactionFacts());})
              .onErrorMap(RootTransactionException.class,error->{
                  var facts=error.transactionFacts();var originalFailure=failure.get();
                  if(!workCompleted.get()&&originalFailure!=null&&facts.rollbackAcknowledged()&&error.cleanupFact()==CleanupFact.COMPLETE&&!facts.commitAcknowledged()&&!facts.boundaryCompleted())return originalFailure;
                  return new InsuranceReferenceOutcome(request.actionId(),facts.commitAcknowledged(),facts.boundaryCompleted(),facts.rollbackAcknowledged());
              });
        });
    }
    private static void validate(InsuranceReferenceNativeContext.Request request,InsuranceReferenceNativeContext.Stamp stamp) {
        if(stamp==null||!Objects.equals(request.nonce(),stamp.nonce())||!Objects.equals(request.recordId(),stamp.recordId())||request.kind()!=stamp.kind())throw unavailable();
        InsuranceReferenceContracts.requireTimestamp(stamp.recordedAt());InsuranceReferenceContracts.requireTimestamp(stamp.effectiveUntil());
        if(stamp.dbNowMillis()!=stamp.recordedAt().toEpochMilli()||!stamp.effectiveUntil().isAfter(stamp.recordedAt())||Duration.between(stamp.recordedAt(),stamp.effectiveUntil()).compareTo(Duration.ofSeconds(30))>0)throw unavailable();
    }
    private static IllegalStateException unavailable(){return new IllegalStateException("INSURANCE_REFERENCE_NATIVE_WITNESS_UNAVAILABLE");}
}
