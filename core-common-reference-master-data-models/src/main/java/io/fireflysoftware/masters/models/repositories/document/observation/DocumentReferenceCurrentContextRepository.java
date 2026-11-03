/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.models.repositories.document.observation;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.r2dbc.repository.Query;
import io.fireflysoftware.masters.models.entities.document.observation.DocumentReferenceCurrentRow;
import reactor.core.publisher.Mono;

/** Fixed technical native scope; business projection is a separate mapped Spring Data repository. */
@NoRepositoryBean
public interface DocumentReferenceCurrentContextRepository extends Repository<DocumentReferenceCurrentRow,UUID>{
    @Query("SELECT public.document_reference_current_runtime_allowed() AS allowed") Mono<Boolean> runtimeAllowed();
    @Query("SELECT * FROM public.document_reference_current_begin(:nonce,:target,:issuer,:subject,:tenant,:until)") Mono<Stamp> begin(@Param("nonce")UUID nonce,@Param("target")String target,@Param("issuer")String issuer,@Param("subject")String subject,@Param("tenant")String tenant,@Param("until")OffsetDateTime until);
    @Query("SELECT public.document_reference_current_complete(:nonce) AS completed") Mono<Boolean> complete(@Param("nonce")UUID nonce);
    record Stamp(UUID contextNonce,String targetCanonical,Long dbNowMillis,OffsetDateTime recordedAt,OffsetDateTime effectiveUntil){}
}
