/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation.impl;
import java.time.*;
import java.util.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fireflysoftware.masters.core.services.document.observation.*;
import io.fireflysoftware.masters.core.services.document.v1.DocumentTemplateCanonicalJson;
import io.fireflysoftware.masters.models.repositories.document.observation.DocumentReferenceCurrentContextRepository;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;

public final class DocumentReferenceCurrentNativeContextImpl implements DocumentReferenceCurrentNativeContext {
    private static final JsonMapper JSON=JsonMapper.builder().build();private final DatabaseClient client;private final DocumentReferenceCurrentContextRepository repository;
    public DocumentReferenceCurrentNativeContextImpl(R2dbcEntityTemplate operations){client=Objects.requireNonNull(operations).getDatabaseClient();repository=new R2dbcRepositoryFactory(operations).getRepository(DocumentReferenceCurrentContextRepository.class);}
    @Override public DatabaseClient databaseClient(){return client;}
    public static String canonical(Object target){try{return DocumentTemplateCanonicalJson.canonicalize(JSON.writeValueAsString(target));}catch(Exception error){throw unavailable();}}
    @Override public Mono<Stamp> begin(Request request){return Mono.defer(()->{validate(request);String expected=canonical(request.target());return repository.runtimeAllowed().filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(unavailable())).then(repository.begin(request.nonce(),expected,request.issuer(),request.subject(),request.tenant(),OffsetDateTime.ofInstant(request.until(),ZoneOffset.UTC))).switchIfEmpty(Mono.error(new DocumentReferenceCurrentFailure(DocumentReferenceCurrentFailure.Kind.NOT_CURRENT))).map(v->{if(!request.nonce().equals(v.contextNonce())||!expected.equals(v.targetCanonical())||v.dbNowMillis()==null||v.recordedAt()==null||v.effectiveUntil()==null)throw unavailable();Instant at=v.recordedAt().toInstant(),until=v.effectiveUntil().toInstant();if(at.toEpochMilli()!=v.dbNowMillis()||!until.isAfter(at)||until.isAfter(request.until())||Duration.between(at,until).compareTo(Duration.ofSeconds(5))>0)throw unavailable();return new Stamp(v.contextNonce(),request.target(),v.dbNowMillis(),at,until);});}).onErrorMap(e->e instanceof DocumentReferenceCurrentFailure?e:unavailable());}
    @Override public Mono<Boolean> complete(Request request){return Mono.defer(()->{validate(request);return repository.complete(request.nonce()).filter(Boolean.TRUE::equals).switchIfEmpty(Mono.error(unavailable()));}).onErrorMap(e->unavailable());}
    private static void validate(Request r){if(r==null||r.nonce()==null||r.nonce().equals(new UUID(0,0))||r.target()==null||r.until()==null||!r.target().product().tenantId().toString().equals(r.tenant()))throw unavailable();for(String v:new String[]{r.issuer(),r.subject(),r.tenant()})if(v==null||v.isBlank()||v.length()>500||v.indexOf(0)>=0)throw unavailable();if(r.subject().length()>255||r.tenant().length()>255)throw unavailable();}
    private static DocumentReferenceCurrentFailure unavailable(){return new DocumentReferenceCurrentFailure(DocumentReferenceCurrentFailure.Kind.UNAVAILABLE);}
}
