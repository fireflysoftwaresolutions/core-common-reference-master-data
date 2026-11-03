/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation.impl;
import java.time.*;
import java.util.*;
import java.util.function.LongSupplier;
import java.util.concurrent.atomic.AtomicReference;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fireflysoftware.masters.core.services.document.observation.*;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.Target;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.DocumentLayoutDefinition;
import io.fireflysoftware.masters.models.repositories.document.observation.DocumentReferenceCurrentRepository;
import org.fireflyframework.core.transaction.*;
import org.fireflyframework.core.transaction.impl.DefaultBoundedRootTransaction;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import reactor.core.publisher.Mono;

public final class DocumentReferenceCurrentReaderImpl implements DocumentReferenceCurrentReader {
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private final DocumentReferenceCurrentRepository repository;private final DocumentReferenceCurrentNativeContext context;private final BoundedRootTransaction transactions;private final DatabaseClient client;private final Clock clock;private final LongSupplier nanos;
    public DocumentReferenceCurrentReaderImpl(R2dbcEntityTemplate operations,R2dbcTransactionManager manager,DocumentReferenceCurrentNativeContext context,Clock clock,LongSupplier nanos){this.context=Objects.requireNonNull(context);this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);client=operations.getDatabaseClient();if(!(client.getConnectionFactory()instanceof OwnedLeaseConnectionFactory factory)||context.databaseClient()!=client)throw new IllegalArgumentException("One exact document CURRENT reader graph required");transactions=new DefaultBoundedRootTransaction(BoundedR2dbcResources.verify(factory,client,operations,manager),new CleanupPolicy(Duration.ofSeconds(2),Duration.ofSeconds(2)));repository=new R2dbcRepositoryFactory(operations).getRepository(DocumentReferenceCurrentRepository.class);}
    @Override public Mono<Snapshot> read(Target target,SecurityPrincipal actor,DocumentReferenceCurrentService.OriginalBudget original){return Mono.defer(()->{
        graph();original.budget().remaining();var definition=new DefaultTransactionDefinition();definition.setReadOnly(false);definition.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ);var failure=new AtomicReference<Throwable>();var retained=new AtomicReference<R2dbcOperationBudget>(original.budget());var request=new DocumentReferenceCurrentNativeContext.Request(UUID.randomUUID(),target,actor.issuer(),actor.subject(),actor.tenantId(),original.until());
        return transactions.execute(original.budget(),definition,scope->{graph();long before=nanos.getAsLong();Instant wall=clock.instant();return context.begin(request).switchIfEmpty(Mono.error(unavailable())).flatMap(stamp->{
            var window=DocumentReferenceCurrentWindow.admit(original,stamp,request.nonce(),target,wall,before,clock,nanos);var until=window.until();var budget=window.budget();scope.narrow(budget);retained.set(budget);
            return repository.findByLayoutRevisionIdAndSourceRevisionId(target.layout().id(),target.template().id()).switchIfEmpty(Mono.error(notCurrent())).flatMap(row->{
                if(!row.version().equals(target.template().versionText())||!row.localeCode().equals(target.locale())||!row.documentType().equals(target.documentType())||!row.definitionDigest().equals(target.layout().digest())||!row.sourceDigest().equals(target.template().digest()))throw notCurrent();DocumentLayoutDefinition layout;try{layout=JSON.readValue(row.canonicalJson(),DocumentLayoutDefinition.class);}catch(Exception error){throw unavailable();}var snapshot=new Snapshot(target,layout,row.canonicalJson(),stamp.recordedAt(),until,budget);return context.complete(request).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(unavailable())).then(Mono.fromSupplier(()->{graph();budget.remaining();return snapshot;}));
            });
        }).doOnError(failure::set);}).map(done->{graph();done.value().budget().remaining();return done.value();}).onErrorMap(RootTransactionException.class,error->{var facts=error.transactionFacts();if(failure.get()instanceof DocumentReferenceCurrentFailure known&&known.kind()==DocumentReferenceCurrentFailure.Kind.NOT_CURRENT&&facts.rollbackAcknowledged()&&error.cleanupFact()==CleanupFact.COMPLETE&&!facts.commitAcknowledged()&&!facts.boundaryCompleted()){try{graph();retained.get().remaining();return known;}catch(RuntimeException exhausted){return error;}}return error;});
    });}
    private void graph(){if(context.databaseClient()!=client)throw unavailable();}
    private static DocumentReferenceCurrentFailure notCurrent(){return new DocumentReferenceCurrentFailure(DocumentReferenceCurrentFailure.Kind.NOT_CURRENT);}
    private static DocumentReferenceCurrentFailure unavailable(){return new DocumentReferenceCurrentFailure(DocumentReferenceCurrentFailure.Kind.UNAVAILABLE);}
}
