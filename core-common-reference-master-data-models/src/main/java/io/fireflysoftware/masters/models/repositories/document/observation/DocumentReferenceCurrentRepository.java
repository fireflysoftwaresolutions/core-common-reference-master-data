/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.models.repositories.document.observation;
import java.util.UUID;
import org.springframework.data.repository.*;
import io.fireflysoftware.masters.models.entities.document.observation.DocumentReferenceCurrentRow;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface DocumentReferenceCurrentRepository extends Repository<DocumentReferenceCurrentRow,UUID>{Mono<DocumentReferenceCurrentRow> findByLayoutRevisionIdAndSourceRevisionId(UUID layoutRevisionId,UUID sourceRevisionId);}
