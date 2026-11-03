package io.fireflysoftware.masters.core.services.insurance.reference.impl;

import java.time.*;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.concurrent.atomic.AtomicReference;
import org.fireflyframework.core.transaction.*;
import org.fireflyframework.core.transaction.impl.DefaultBoundedRootTransaction;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.Target;
import io.fireflysoftware.masters.models.repositories.insurance.reference.InsuranceReferenceRepository;
import io.fireflysoftware.masters.models.repositories.insurance.reference.impl.InsuranceReferenceRepositoryImpl;
import reactor.core.publisher.Mono;

/** Real mapped reads only. A separately reviewed CURRENT native adapter is mandatory. */
public final class InsuranceReferenceCurrentReaderImpl implements InsuranceReferenceCurrentReader {
    private final InsuranceReferenceRepository repository;
    private final InsuranceReferenceCurrentNativeContext context;
    private final BoundedRootTransaction transactions;
    private final org.springframework.r2dbc.core.DatabaseClient client;
    private final Clock clock;
    private final LongSupplier nanos;
    private record NativeWindow(R2dbcOperationBudget budget,Instant until) {}

    public InsuranceReferenceCurrentReaderImpl(R2dbcEntityTemplate operations,R2dbcTransactionManager manager,
            InsuranceReferenceCurrentNativeContext context,Clock clock,LongSupplier nanos) {
        this.context=Objects.requireNonNull(context);this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);
        client=operations.getDatabaseClient();
        if (!(client.getConnectionFactory() instanceof OwnedLeaseConnectionFactory factory) || context.databaseClient()!=client)
            throw new IllegalArgumentException("One exact CURRENT reader graph required");
        transactions=new DefaultBoundedRootTransaction(BoundedR2dbcResources.verify(factory,client,operations,manager),
                new CleanupPolicy(Duration.ofSeconds(2),Duration.ofSeconds(2)));
        repository=new InsuranceReferenceRepositoryImpl(operations);
    }
    @Override public Mono<Snapshot> read(Target target,SecurityPrincipal actor,R2dbcOperationBudget original,Instant originalUntil) {
        return Mono.defer(()->{
            original.remaining();Objects.requireNonNull(originalUntil);requireGraph();
            var definition=new DefaultTransactionDefinition();
            definition.setReadOnly(false);
            definition.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
            var failure=new AtomicReference<Throwable>();
            var retainedBudget=new AtomicReference<R2dbcOperationBudget>(original);
            var request=new InsuranceReferenceCurrentNativeContext.Request(UUID.randomUUID(),target,actor.issuer(),actor.subject(),originalUntil);
            return transactions.execute(original,definition,scope->{
                requireGraph();long before=nanos.getAsLong();Instant observed=clock.instant();
                return context.begin(request).switchIfEmpty(Mono.error(unavailable())).flatMap(stamp->{
                    requireGraph();var nativeWindow=nativeBudget(request,stamp,observed,before);var bounded=original.intersect(nativeWindow.budget());
                    scope.narrow(bounded);retainedBudget.set(bounded);
                    Instant until=Collections.min(List.of(originalUntil,nativeWindow.until()));
                    return repository.identity(target.recordId(),target.kind()).switchIfEmpty(Mono.error(notCurrent())).flatMap(identity->
                        repository.revision(target.recordId(),target.kind(),target.publicationId(),target.revision()).switchIfEmpty(Mono.error(notCurrent())).flatMap(revision->
                        repository.control(target.recordId(),target.kind()).switchIfEmpty(Mono.error(notCurrent())).flatMap(control->{
                            var snapshot=new Snapshot(identity,revision,control,stamp,bounded,until);
                            InsuranceReferenceCurrentValidator.checked(target,snapshot);
                            return context.complete(request).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(unavailable()))
                                .then(Mono.fromSupplier(()->{requireGraph();bounded.remaining();return snapshot;}));
                        })));
                }).doOnError(failure::set);
            }).map(done->{requireGraph();done.value().budget().remaining();return done.value();})
              .onErrorMap(RootTransactionException.class,error->{
                  var facts=error.transactionFacts();
                  if(failure.get() instanceof InsuranceReferenceCurrentFailure known
                          && known.kind()==InsuranceReferenceCurrentFailure.Kind.NOT_CURRENT
                          && facts.rollbackAcknowledged() && error.cleanupFact()==CleanupFact.COMPLETE
                          && !facts.commitAcknowledged() && !facts.boundaryCompleted()) {
                      try {requireGraph();retainedBudget.get().remaining();return known;}
                      catch(RuntimeException expiredOrChanged){return error;}
                  }
                  return error;
              });
        });
    }
    private NativeWindow nativeBudget(InsuranceReferenceCurrentNativeContext.Request request,
            InsuranceReferenceCurrentNativeContext.Stamp stamp,Instant observed,long before) {
        if (stamp==null || !request.nonce().equals(stamp.nonce()) || !request.target().equals(stamp.target())) throw unavailable();
        io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.requireTimestamp(stamp.recordedAt());
        io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.requireTimestamp(stamp.effectiveUntil());
        long millis=Math.subtractExact(Math.subtractExact(stamp.effectiveUntil().toEpochMilli(),stamp.dbNowMillis()),1L);
        if (stamp.recordedAt().toEpochMilli()!=stamp.dbNowMillis() || !stamp.effectiveUntil().isAfter(stamp.recordedAt())
                || stamp.effectiveUntil().isAfter(request.authorityUntil()) || millis<=0 || millis>29999) throw unavailable();
        Instant until=Collections.min(List.of(request.authorityUntil(),stamp.effectiveUntil(),observed.plusMillis(millis)));
        return new NativeWindow(R2dbcOperationBudget.observed(observed,until,before,Duration.ofSeconds(30),clock,nanos),until);
    }
    private void requireGraph(){if(context.databaseClient()!=client)throw unavailable();}
    private static IllegalStateException unavailable(){return new IllegalStateException("INSURANCE_REFERENCE_CURRENT_NATIVE_UNAVAILABLE");}
    private static InsuranceReferenceCurrentFailure notCurrent(){return new InsuranceReferenceCurrentFailure(InsuranceReferenceCurrentFailure.Kind.NOT_CURRENT);}
}
