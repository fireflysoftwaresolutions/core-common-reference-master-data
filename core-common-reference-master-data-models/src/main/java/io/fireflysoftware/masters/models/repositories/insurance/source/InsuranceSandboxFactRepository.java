package io.fireflysoftware.masters.models.repositories.insurance.source;

import java.time.Instant;
import java.util.UUID;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import io.fireflysoftware.masters.models.entities.insurance.source.InsuranceSandboxFactRows.*;
import reactor.core.publisher.Mono;

/** Explicit same-template mapped persistence; no automatic privileged source registration. */
public interface InsuranceSandboxFactRepository {
    record Head(UUID versionId,long version,long generation,String digest,String lifecycle) {}
    Mono<Boolean> runtimeAllowed(String expectedRole);
    Mono<Instant> now();
    Mono<Void> gate(String key);
    Mono<Head> head(Type type,UUID factId);
    Mono<LocaleSource> localeSource(UUID localeId);
    Mono<LocaleVersion> locale(UUID factId,long version);
    Mono<RightsVersion> rights(UUID factId,long version);
    Mono<SourceVersion> source(UUID factId,long version);
    Mono<Action> action(UUID actionId);
    Mono<Void> insertLocale(LocaleVersion row);
    Mono<Void> insertRights(RightsVersion row);
    Mono<Void> insertSource(SourceVersion row);
    Mono<Void> insertHead(Type type,UUID factId,Head head);
    Mono<Void> compareAndSetHead(Type type,UUID factId,Head expected,Head next);
    Mono<Void> insertAction(Action action);
}
