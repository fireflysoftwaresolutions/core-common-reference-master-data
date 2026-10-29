/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;
import java.time.Instant;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.Target;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.DocumentLayoutDefinition;
import reactor.core.publisher.Mono;

public interface DocumentReferenceCurrentReader {
    record Snapshot(Target target,DocumentLayoutDefinition definition,String canonicalJson,Instant recordedAt,Instant until,R2dbcOperationBudget budget){@Override public String toString(){return "DocumentReferenceNativeSnapshot";}}
    Mono<Snapshot> read(Target target,SecurityPrincipal actor,DocumentReferenceCurrentService.OriginalBudget original);
}
