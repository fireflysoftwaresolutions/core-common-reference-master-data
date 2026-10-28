package io.fireflysoftware.masters.core.services.currency.observation.impl;

import java.time.*;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.concurrent.atomic.AtomicReference;
import io.fireflysoftware.masters.core.services.currency.observation.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target;
import io.fireflysoftware.masters.models.repositories.currency.policy.CurrencyPolicyObservationRepository;
import org.fireflyframework.core.transaction.*;
import org.fireflyframework.core.transaction.impl.DefaultBoundedRootTransaction;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Mono;

/** SELECT-only business snapshot on one managed graph; technical native admission uses physical RW. */
public final class CurrencyPolicyCurrentReaderImpl implements CurrencyPolicyCurrentReader {
    private final CurrencyPolicyObservationRepository repository;
    private final CurrencyPolicyCurrentNativeContext context;
    private final BoundedRootTransaction transactions;
    private final DatabaseClient client;
    private final Clock clock;
    private final LongSupplier nanos;
    public CurrencyPolicyCurrentReaderImpl(R2dbcEntityTemplate operations,R2dbcTransactionManager manager,CurrencyPolicyCurrentNativeContext context,Clock clock,LongSupplier nanos){
        this.context=Objects.requireNonNull(context);this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);client=operations.getDatabaseClient();
        if(!(client.getConnectionFactory()instanceof OwnedLeaseConnectionFactory factory)||context.databaseClient()!=client)throw new IllegalArgumentException("One exact currency CURRENT reader graph required");
        transactions=new DefaultBoundedRootTransaction(BoundedR2dbcResources.verify(factory,client,operations,manager),new CleanupPolicy(Duration.ofSeconds(2),Duration.ofSeconds(2)));
        repository=new R2dbcRepositoryFactory(operations).getRepository(CurrencyPolicyObservationRepository.class);
    }
    @Override public Mono<Snapshot> read(Target target,SecurityPrincipal actor,CurrencyPolicyCurrentObservationService.OriginalBudget original){return Mono.defer(()->{
        requireGraph();original.budget().remaining();var definition=new DefaultTransactionDefinition();definition.setReadOnly(false);definition.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var failure=new AtomicReference<Throwable>();var retained=new AtomicReference<R2dbcOperationBudget>(original.budget());
        var request=new CurrencyPolicyCurrentNativeContext.Request(UUID.randomUUID(),target,actor.issuer(),actor.subject(),actor.tenantId(),original.until());
        return transactions.execute(original.budget(),definition,scope->{
            requireGraph();long before=nanos.getAsLong();Instant observed=clock.instant();
            return context.begin(request).switchIfEmpty(Mono.error(unavailable())).flatMap(stamp->{
                var window=CurrencyPolicyCurrentWindow.admit(original,stamp,request.nonce(),target,observed,before,clock,nanos);scope.narrow(window.budget());retained.set(window.budget());
                var ref=target.reference();
                return repository.findByPolicyIdAndCurrencyId(UUID.fromString(ref.policyId()),UUID.fromString(ref.currencyId())).switchIfEmpty(Mono.error(notCurrent())).flatMap(row->{
                    var view=new CurrencyPolicyView(new CurrencyPolicyReference(row.policyId().toString(),row.currencyId().toString(),row.isoCode(),row.version(),row.definitionDigest(),row.encodingVersion()),
                            row.canonicalEncoding(),row.sourceEncoding(),row.sourceDigest(),row.postedScale(),row.effectiveFrom(),row.effectiveTo(),row.provenanceReference(),row.publicationActionId().toString(),row.publishedAt(),row.lifecycle(),row.stateVersion(),row.observedAt(),
                            row.lifecycle().equals("PUBLISHED")&&row.observedAt()>=row.effectiveFrom()&&(row.effectiveTo()==null||row.observedAt()<row.effectiveTo()));
                    CurrencyPolicyCurrentValidator.checked(ref,target.postedScale(),view,clock.instant());
                    var snapshot=new Snapshot(view,stamp,window.budget(),window.until());
                    return context.complete(request).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(unavailable())).then(Mono.fromSupplier(()->{requireGraph();window.budget().remaining();return snapshot;}));
                });
            }).doOnError(failure::set);
        }).map(done->{requireGraph();done.value().budget().remaining();return done.value();}).onErrorMap(RootTransactionException.class,error->{
            var facts=error.transactionFacts();
            if(failure.get()instanceof CurrencyPolicyCurrentFailure known&&known.kind()==CurrencyPolicyCurrentFailure.Kind.NOT_CURRENT&&facts.rollbackAcknowledged()&&error.cleanupFact()==CleanupFact.COMPLETE&&!facts.commitAcknowledged()&&!facts.boundaryCompleted()){
                try{requireGraph();retained.get().remaining();return known;}catch(RuntimeException expiredOrChanged){return error;}
            }
            return error;
        });
    });}
    private void requireGraph(){if(context.databaseClient()!=client)throw unavailable();}
    private static CurrencyPolicyCurrentFailure notCurrent(){return new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.NOT_CURRENT);}
    private static CurrencyPolicyCurrentFailure unavailable(){return new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE);}
}
