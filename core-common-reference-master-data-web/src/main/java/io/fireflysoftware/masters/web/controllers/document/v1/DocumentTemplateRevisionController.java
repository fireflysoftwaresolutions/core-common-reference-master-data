package io.fireflysoftware.masters.web.controllers.document.v1;

import io.fireflysoftware.masters.core.services.document.v1.DocumentTemplateRevisionService;
import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplatePublicationRequest;
import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplateRevisionDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/document-template-revisions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Document Template Revisions", description = "Immutable localized publications and exact revision resolution")
public class DocumentTemplateRevisionController {
    private final DocumentTemplateRevisionService service;

    public DocumentTemplateRevisionController(DocumentTemplateRevisionService service) {
        this.service = service;
    }

    @Operation(operationId = "publishDocumentTemplateRevision", summary = "Publish the exact localized draft or replay the same source snapshot",
            parameters = @io.swagger.v3.oas.annotations.Parameter(name = "X-Idempotency-Key",
                    in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER,
                    description = "Accepted for SDK compatibility and ignored. Retained publication and current authorization govern replay.",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")))
    @PostMapping(value = "/templates/{templateId}/publish", consumes = MediaType.APPLICATION_JSON_VALUE)
    @DisableIdempotency
    public Mono<DocumentTemplateRevisionDTO> publishDocumentTemplateRevision(@PathVariable UUID templateId,
            @Valid @RequestBody DocumentTemplatePublicationRequest request) {
        return service.publish(templateId, request);
    }

    @Operation(operationId = "getDocumentTemplateRevision", summary = "Read an authorized historical publication, including retired revisions")
    @GetMapping("/{revisionId}")
    public Mono<DocumentTemplateRevisionDTO> getDocumentTemplateRevision(@PathVariable UUID revisionId) {
        return service.get(revisionId);
    }

    @Operation(operationId = "resolveDocumentTemplateRevision", summary = "Resolve an exact active publication without version or locale fallback")
    @GetMapping("/resolve")
    public Mono<DocumentTemplateRevisionDTO> resolveDocumentTemplateRevision(@RequestParam String templateCode,
            @RequestParam String version, @RequestParam String localeCode) {
        return service.resolve(templateCode, version, localeCode);
    }

    @Operation(operationId = "retireDocumentTemplateRevision", summary = "Prevent new issuance while preserving immutable historical readback",
            parameters = @io.swagger.v3.oas.annotations.Parameter(name = "X-Idempotency-Key",
                    in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER,
                    description = "Accepted for SDK compatibility and ignored. Retained retirement and current authorization govern replay.",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string")))
    @PostMapping("/{revisionId}/retire")
    @DisableIdempotency
    public Mono<DocumentTemplateRevisionDTO> retireDocumentTemplateRevision(@PathVariable UUID revisionId) {
        return service.retire(revisionId);
    }
}
