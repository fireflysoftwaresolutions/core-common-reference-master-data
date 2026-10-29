/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.masters.core.services.document.v1;

import org.fireflyframework.core.queries.PaginationRequest;
import org.fireflyframework.core.queries.PaginationResponse;
import org.fireflyframework.core.queries.PaginationUtils;
import io.fireflysoftware.masters.core.mappers.document.v1.DocumentTemplateCatalogMapper;
import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplateCatalogDTO;
import io.fireflysoftware.masters.models.entities.document.v1.DocumentTemplateCatalog;
import io.fireflysoftware.masters.models.repositories.document.v1.DocumentTemplateCatalogRepository;
import org.fireflyframework.validators.schema.OfflineJsonSchemaValidator;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of the DocumentTemplateCatalogService interface.
 */
@Service
@Transactional
public class DocumentTemplateCatalogServiceImpl implements DocumentTemplateCatalogService {

    @Autowired
    private DocumentTemplateCatalogRepository repository;

    @Autowired
    private DocumentTemplateCatalogMapper mapper;

    @Autowired
    private DocumentTemplateAccessGuard access;

    private final OfflineJsonSchemaValidator schemas = new OfflineJsonSchemaValidator();

    @Override
    public Mono<PaginationResponse<DocumentTemplateCatalogDTO>> listDocumentTemplates(PaginationRequest paginationRequest) {
        return access.mono("read", "document-template-catalog", () -> {
            return PaginationUtils.paginateQuery(
                    paginationRequest,
                    mapper::toDTO,
                    pageable -> repository.findAllBy(pageable),
                    () -> repository.count()
            );
        });
    }

    @Override
    public Mono<PaginationResponse<DocumentTemplateCatalogDTO>> listDocumentTemplatesByCategory(String category, PaginationRequest paginationRequest) {
        return access.mono("read", "document-template-catalog", () -> {
            return PaginationUtils.paginateQuery(
                    paginationRequest,
                    mapper::toDTO,
                    pageable -> repository.findByCategory(category, pageable),
                    () -> repository.countByCategory(category)
            );
        });
    }

    @Override
    public Mono<PaginationResponse<DocumentTemplateCatalogDTO>> listDocumentTemplatesByTypeId(UUID typeId, PaginationRequest paginationRequest) {
        return access.mono("read", "document-template-catalog", () -> {
            return PaginationUtils.paginateQuery(
                    paginationRequest,
                    mapper::toDTO,
                    pageable -> repository.findByTypeId(typeId, pageable),
                    () -> repository.countByTypeId(typeId)
            );
        });
    }

    @Override
    public Mono<DocumentTemplateCatalogDTO> createDocumentTemplate(DocumentTemplateCatalogDTO documentTemplateDTO) {
        return access.mono("draft.write", "document-template-catalog", () -> {
            validateDraftSchema(documentTemplateDTO);
            // Set audit fields
            LocalDateTime now = LocalDateTime.now();
            documentTemplateDTO.setDateCreated(now);
            documentTemplateDTO.setDateUpdated(now);

            return Mono.just(documentTemplateDTO)
                    .map(mapper::toEntity)
                    .flatMap(repository::save)
                    .map(mapper::toDTO)
                    .onErrorResume(e -> Mono.error(new RuntimeException("Error creating document template: " + e.getMessage(), e)));
        });
    }

    @Override
    public Mono<DocumentTemplateCatalogDTO> getDocumentTemplate(UUID templateId) {
        return access.mono("read", "document-template-catalog", () -> {
            return repository.findById(templateId)
                    .map(mapper::toDTO)
                    .switchIfEmpty(Mono.error(new RuntimeException("Document template not found with ID: " + templateId)));
        });
    }

    @Override
    public Mono<DocumentTemplateCatalogDTO> getDocumentTemplateByCode(String templateCode) {
        return access.mono("read", "document-template-catalog", () -> {
            return repository.findByTemplateCode(templateCode)
                    .map(mapper::toDTO)
                    .switchIfEmpty(Mono.error(new RuntimeException("Document template not found with code: " + templateCode)));
        });
    }

    @Override
    public Mono<DocumentTemplateCatalogDTO> updateDocumentTemplate(UUID templateId, DocumentTemplateCatalogDTO documentTemplateDTO) {
        return access.mono("draft.write", "document-template-catalog", () -> {
            validateDraftSchema(documentTemplateDTO);
            return repository.findById(templateId)
                    .switchIfEmpty(Mono.error(new RuntimeException("Document template not found with ID: " + templateId)))
                    .flatMap(existingEntity -> {
                        DocumentTemplateCatalog updatedEntity = mapper.toEntity(documentTemplateDTO);
                        updatedEntity.setTemplateId(templateId);
                        updatedEntity.setDateCreated(existingEntity.getDateCreated());
                        updatedEntity.setDateUpdated(LocalDateTime.now());
                        return repository.save(updatedEntity);
                    })
                    .map(mapper::toDTO)
                    .onErrorResume(e -> Mono.error(new RuntimeException("Error updating document template: " + e.getMessage(), e)));
        });
    }

    @Override
    public Mono<Void> deleteDocumentTemplate(UUID templateId) {
        return access.mono("draft.write", "document-template-catalog", () -> {
            return repository.findById(templateId)
                    .switchIfEmpty(Mono.error(new RuntimeException("Document template not found with ID: " + templateId)))
                    .flatMap(repository::delete)
                    .onErrorResume(e -> Mono.error(new RuntimeException("Error deleting document template: " + e.getMessage(), e)));
        });
    }
    private void validateDraftSchema(DocumentTemplateCatalogDTO draft) {
        if (draft.getInputSchemaJson() != null && draft.getInputSchema() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Supply one schema representation: inputSchemaJson or inputSchema");
        }
        if (draft.getInputSchemaJson() != null) {
            var validation = schemas.validateSchema(draft.getInputSchemaJson());
            if (!validation.valid()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    validation.issues().getFirst().code() + ": Draft input schema is invalid or unsupported");
            String canonical;
            try {
                canonical = DocumentTemplateCanonicalJson.canonicalize(draft.getInputSchemaJson());
            } catch (IllegalArgumentException error) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft input schema exceeds supported canonical JSON limits");
            }
            if (canonical.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 65_536) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Canonical draft schema exceeds the byte limit");
            }
            draft.setInputSchemaJson(canonical);
        }
    }

}
