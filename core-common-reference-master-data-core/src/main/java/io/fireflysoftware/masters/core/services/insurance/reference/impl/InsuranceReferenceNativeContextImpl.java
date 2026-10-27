package io.fireflysoftware.masters.core.services.insurance.reference.impl;

import java.time.Duration;
import java.util.*;
import reactor.core.publisher.Mono;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceNativeContext;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;
import io.fireflysoftware.masters.models.repositories.insurance.reference.InsuranceReferenceContextRepository;

/** Explicit opt-in technical adapter. A native observation is not current principal/PDP/source authority. */
public final class InsuranceReferenceNativeContextImpl implements InsuranceReferenceNativeContext {
    private static final Set<String> MODES=Set.of("SUBMIT_PUBLICATION","SUBMIT_WITHDRAWAL","APPROVE","PREPARE_EXECUTION","EXECUTE","WITHDRAW_INTENT","RECOVER");
    private final InsuranceReferenceContextRepository repository;
    private final DatabaseClient client;
    public InsuranceReferenceNativeContextImpl(R2dbcEntityTemplate operations){Objects.requireNonNull(operations);client=operations.getDatabaseClient();repository=new R2dbcRepositoryFactory(operations).getRepository(InsuranceReferenceContextRepository.class);}
    @Override public DatabaseClient databaseClient(){return client;}
    @Override public Mono<Stamp> begin(Request request) {
        return Mono.defer(()->{
            validate(request);
            return repository.begin(request.nonce(),request.recordId(),request.kind(),request.actionId(),request.intentId(),request.mode(),request.issuer(),request.subject(),java.time.OffsetDateTime.ofInstant(request.authorityUntil(),java.time.ZoneOffset.UTC)).switchIfEmpty(Mono.error(new Unavailable())).map(value->{
                if(!Objects.equals(request.nonce(),value.nonce())||!Objects.equals(request.recordId(),value.recordId())||request.kind()!=value.kind())throw new Unavailable();
                InsuranceReferenceContracts.requireTimestamp(value.recordedAt().toInstant());InsuranceReferenceContracts.requireTimestamp(value.effectiveUntil().toInstant());
                if(value.dbNowMillis()!=value.recordedAt().toInstant().toEpochMilli()||!value.effectiveUntil().toInstant().isAfter(value.recordedAt().toInstant())
                        ||value.effectiveUntil().toInstant().isAfter(request.authorityUntil())||Duration.between(value.recordedAt().toInstant(),value.effectiveUntil().toInstant()).compareTo(Duration.ofSeconds(30))>0)throw new Unavailable();
                return new Stamp(value.nonce(),value.recordId(),value.kind(),value.dbNowMillis(),value.recordedAt().toInstant(),value.effectiveUntil().toInstant());
            });
        }).onErrorMap(error->new Unavailable());
    }
    @Override public Mono<Boolean> complete(Request request) {
        return Mono.defer(()->{validate(request);return repository.complete(request.nonce()).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(new Unavailable()));})
            .onErrorMap(error->new Unavailable());
    }
    private static void validate(Request value) {
        if(value==null||value.nonce()==null||value.recordId()==null||value.actionId()==null||value.kind()==null
                ||value.nonce().equals(new UUID(0,0))||value.recordId().equals(new UUID(0,0))||value.actionId().equals(new UUID(0,0))
                ||!MODES.contains(value.mode())||value.issuer()==null||value.issuer().isBlank()||value.issuer().length()>500
                ||value.subject()==null||value.subject().isBlank()||value.subject().length()>255)throw new Unavailable();
        boolean needsIntent=Set.of("APPROVE","PREPARE_EXECUTION","EXECUTE","WITHDRAW_INTENT").contains(value.mode());
        if(needsIntent!=(value.intentId()!=null)||new UUID(0,0).equals(value.intentId()))throw new Unavailable();
        InsuranceReferenceContracts.requireTimestamp(value.authorityUntil());
    }
}
