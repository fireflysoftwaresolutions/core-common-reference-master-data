/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation.impl;
import java.time.*;
import java.util.*;
import java.util.function.LongSupplier;
import io.fireflysoftware.masters.core.services.document.observation.*;
import io.fireflysoftware.masters.core.services.document.layout.v1.*;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import reactor.core.publisher.Mono;

/** Same original budget before/after trusted policy and the native owned read graph. */
public final class DocumentReferenceCurrentServiceImpl implements DocumentReferenceCurrentService {
    private static final Duration LIMIT=Duration.ofSeconds(5);
    private final DocumentReferenceCurrentReader reader;
    private final DocumentReferenceCurrentAuthorization authority;
    private final DocumentLayoutProfileRegistry profiles;
    private final Clock clock;
    private final LongSupplier nanos;
    public DocumentReferenceCurrentServiceImpl(DocumentReferenceCurrentReader reader,DocumentReferenceCurrentAuthorization authority,DocumentLayoutProfileRegistry profiles,Clock clock,LongSupplier nanos){this.reader=Objects.requireNonNull(reader);this.authority=Objects.requireNonNull(authority);this.profiles=Objects.requireNonNull(profiles);this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);}
    @Override public Mono<Observation> observeCurrent(Target target){return Mono.defer(()->{long before=nanos.getAsLong();Instant start=clock.instant();Instant until=start.plus(LIMIT);return observeCurrent(target,new OriginalBudget(R2dbcOperationBudget.observed(start,until,before,LIMIT,clock,nanos),until));});}
    @Override public Mono<Observation> observeCurrent(Target target,OriginalBudget original){return Mono.defer(()->{
        Objects.requireNonNull(target);Objects.requireNonNull(original);original.budget().remaining();long before=nanos.getAsLong();Instant start=clock.instant();Instant until=min(original.until(),start.plus(LIMIT));
        var initial=original.budget().intersect(R2dbcOperationBudget.observed(start,until,before,LIMIT,clock,nanos));
        return authority.authorize(target).switchIfEmpty(Mono.error(failure(DocumentReferenceCurrentFailure.Kind.FORBIDDEN))).timeout(initial.remaining()).flatMap(actor->{
            Instant cap=min(until,trusted(actor,target));var authenticated=initial.intersect(R2dbcOperationBudget.observed(start,cap,before,LIMIT,clock,nanos));authenticated.remaining();
            return reader.read(target,actor,new OriginalBudget(authenticated,cap)).switchIfEmpty(Mono.error(failure(DocumentReferenceCurrentFailure.Kind.NOT_CURRENT))).timeout(authenticated.remaining()).flatMap(snapshot->{
                checked(target,snapshot,cap);var retained=authenticated.intersect(snapshot.budget());Instant absolute=min(cap,snapshot.until());
                retained=retained.intersect(R2dbcOperationBudget.observed(start,absolute,before,LIMIT,clock,nanos));var bounded=retained;
                return authority.authorize(target).switchIfEmpty(Mono.error(failure(DocumentReferenceCurrentFailure.Kind.FORBIDDEN))).timeout(bounded.remaining()).map(current->{
                    trusted(current,target);if(!actor.equals(current))throw failure(DocumentReferenceCurrentFailure.Kind.FORBIDDEN);checked(target,snapshot,cap);Instant now=clock.instant();Instant delivered=min(absolute,now.plus(bounded.remaining()));bounded.remaining();if(!now.isBefore(delivered)||!snapshot.recordedAt().isBefore(delivered))throw unavailable();
                    return new Observation(new Current(target,snapshot.definition(),snapshot.recordedAt().toEpochMilli(),delivered.toEpochMilli()),bounded,absolute);
                });
            });
        }).timeout(initial.remaining());
    }).onErrorMap(error->error instanceof DocumentReferenceCurrentFailure?error:unavailable());}
    private Instant trusted(SecurityPrincipal actor,Target target){if(actor==null||actor.issuer()==null||actor.subject()==null||actor.subject().isBlank()||!target.product().tenantId().toString().equals(actor.tenantId())||!(actor.claims().get("exp")instanceof Instant expiry)||!expiry.isAfter(clock.instant()))throw failure(DocumentReferenceCurrentFailure.Kind.FORBIDDEN);return expiry;}
    private void checked(Target target,DocumentReferenceCurrentReader.Snapshot snapshot,Instant cap){
        if(snapshot==null||!target.equals(snapshot.target())||snapshot.definition()==null||snapshot.recordedAt()==null||snapshot.until()==null||snapshot.budget()==null||snapshot.recordedAt().isAfter(clock.instant())||!snapshot.until().isAfter(clock.instant())||snapshot.until().isAfter(cap)||Duration.between(snapshot.recordedAt(),snapshot.until()).compareTo(LIMIT)>0)throw unavailable();
        var definition=snapshot.definition();var canonical=DocumentLayoutRules.canonical(definition);var source=definition.sourceReference();
        if(!canonical.json().equals(snapshot.canonicalJson())||!canonical.digest().equals(target.layout().digest())||!source.revisionId().equals(target.template().id().toString())||!source.version().equals(target.template().versionText())||!source.sourceDigest().equals(target.template().digest())||!source.localeCode().equals(target.locale())||!definition.profileMetadata().equals(profiles.requireRecognized(definition.profileMetadata().reference())))throw unavailable();
    }
    private static Instant min(Instant...values){return Arrays.stream(values).min(Instant::compareTo).orElseThrow();}
    private static DocumentReferenceCurrentFailure failure(DocumentReferenceCurrentFailure.Kind kind){return new DocumentReferenceCurrentFailure(kind);}
    private static DocumentReferenceCurrentFailure unavailable(){return failure(DocumentReferenceCurrentFailure.Kind.UNAVAILABLE);}
}
