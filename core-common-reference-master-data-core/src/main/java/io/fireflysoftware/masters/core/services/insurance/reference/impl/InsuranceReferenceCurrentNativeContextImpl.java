package io.fireflysoftware.masters.core.services.insurance.reference.impl;

import java.time.*;
import java.util.*;
import reactor.core.publisher.Mono;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceCurrentNativeContext;
import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceCurrentFailure;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.Target;
import io.fireflysoftware.masters.models.repositories.insurance.reference.InsuranceReferenceCurrentContextRepository;

/** Unregistered technical adapter. Source presence is not installed V34 or runtime-role acceptance. */
public final class InsuranceReferenceCurrentNativeContextImpl implements InsuranceReferenceCurrentNativeContext {
    private final DatabaseClient client;
    private final InsuranceReferenceCurrentContextRepository repository;
    public InsuranceReferenceCurrentNativeContextImpl(R2dbcEntityTemplate operations) {
        Objects.requireNonNull(operations);client=operations.getDatabaseClient();
        repository=new R2dbcRepositoryFactory(operations).getRepository(InsuranceReferenceCurrentContextRepository.class);
    }
    @Override public DatabaseClient databaseClient(){return client;}
    @Override public Mono<Stamp> begin(Request request) {
        return Mono.defer(()->{
            validate(request);var target=request.target();
            return repository.begin(request.nonce(),target.recordId(),target.kind(),target.publicationId(),target.revision(),
                    target.definitionDigest(),target.generation(),request.issuer(),request.subject(),OffsetDateTime.ofInstant(request.authorityUntil(),ZoneOffset.UTC))
                .switchIfEmpty(Mono.error(new InsuranceReferenceCurrentFailure(InsuranceReferenceCurrentFailure.Kind.NOT_CURRENT))).map(value->{
                    var observedTarget=new Target(value.kind(),value.recordId(),value.publicationId(),value.revision(),value.definitionDigest(),value.generation());
                    if(!request.nonce().equals(value.nonce())||!target.equals(observedTarget))throw unavailable();
                    var recorded=value.recordedAt().toInstant();var until=value.effectiveUntil().toInstant();
                    InsuranceReferenceContracts.requireTimestamp(recorded);InsuranceReferenceContracts.requireTimestamp(until);
                    if(value.dbNowMillis()!=recorded.toEpochMilli()||!until.isAfter(recorded)||until.isAfter(request.authorityUntil())
                            ||Duration.between(recorded,until).compareTo(Duration.ofSeconds(30))>0)throw unavailable();
                    return new Stamp(value.nonce(),observedTarget,value.dbNowMillis(),recorded,until);
                });
        }).onErrorMap(error->error instanceof InsuranceReferenceCurrentFailure?error:unavailable());
    }
    @Override public Mono<Boolean> complete(Request request) {
        return Mono.defer(()->{validate(request);return repository.complete(request.nonce()).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(unavailable()));})
            .onErrorMap(error->unavailable());
    }
    private static void validate(Request request) {
        if(request==null||request.nonce()==null||request.nonce().equals(new UUID(0,0))||request.target()==null
                ||request.issuer()==null||request.issuer().isBlank()||request.issuer().length()>500
                ||request.subject()==null||request.subject().isBlank()||request.subject().length()>255)throw unavailable();
        InsuranceReferenceContracts.requireTimestamp(request.authorityUntil());
    }
    private static IllegalStateException unavailable(){return new IllegalStateException("INSURANCE_REFERENCE_CURRENT_NATIVE_UNAVAILABLE");}
}
