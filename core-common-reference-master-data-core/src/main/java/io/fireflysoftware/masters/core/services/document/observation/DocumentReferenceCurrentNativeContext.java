/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;
import java.time.Instant;
import java.util.UUID;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.Target;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;

public interface DocumentReferenceCurrentNativeContext {
    @FunctionalInterface interface Factory{DocumentReferenceCurrentNativeContext create(R2dbcEntityTemplate operations);}
    record Request(UUID nonce,Target target,String issuer,String subject,String tenant,Instant until){}
    record Stamp(UUID nonce,Target target,long dbNowMillis,Instant recordedAt,Instant until){}
    DatabaseClient databaseClient();Mono<Stamp> begin(Request request);Mono<Boolean> complete(Request request);
}
