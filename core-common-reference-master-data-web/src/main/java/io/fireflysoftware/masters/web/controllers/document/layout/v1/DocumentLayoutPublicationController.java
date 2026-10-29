/*
 * Copyright 2026 Firefly Software Foundation
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

package io.fireflysoftware.masters.web.controllers.document.layout.v1;

import io.fireflysoftware.masters.core.services.document.v1.DocumentLayoutPublicationService;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping(
        value = "/api/v1/document-layout-publications",
        produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
        name = "Document Layout Publications",
        description =
                "Immutable master source and layout metadata authority; no renderer or output"
                        + " assets")
public class DocumentLayoutPublicationController {
    private final DocumentLayoutPublicationService service;

    public DocumentLayoutPublicationController(DocumentLayoutPublicationService service) {
        this.service = service;
    }

    @DisableIdempotency
    @GetMapping("/templates/{templateId}/source-observation")
    @Operation(
            operationId = "observeDocumentLayoutSource",
            summary =
                    "Observe the current exact localized draft before requesting a new layout"
                            + " publication")
    public Mono<DocumentLayoutSourceObservation> observeDocumentLayoutSource(
            @PathVariable UUID templateId, @RequestParam String localeCode) {
        return service.observe(templateId, localeCode);
    }

    @PostMapping(value = "/templates/{templateId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @DisableIdempotency
    @Operation(
            operationId = "publishDocumentLayoutRevision",
            summary =
                    "Atomically publish a genuinely new localized revision and its immutable layout"
                            + " association")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "New atomic publication",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        DocumentLayoutPublicationDTO.class))),
        @ApiResponse(
                responseCode = "200",
                description = "Accepted native action replay with current lifecycle",
                content =
                        @Content(
                                schema =
                                        @Schema(
                                                implementation =
                                                        DocumentLayoutPublicationDTO.class)))
    })
    public Mono<ResponseEntity<DocumentLayoutPublicationDTO>> publishDocumentLayoutRevision(
            @PathVariable UUID templateId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    DocumentLayoutPublishRequest
                                                                            .class)))
                    @RequestBody
                    String raw) {
        return Mono.defer(
                        () ->
                                service.publish(
                                        templateId, DocumentLayoutRequestDecoder.publish(raw)))
                .map(
                        outcome ->
                                ResponseEntity.status(
                                                outcome.replayed()
                                                        ? HttpStatus.OK
                                                        : HttpStatus.CREATED)
                                        .body(outcome.publication()));
    }

    @DisableIdempotency
    @GetMapping("/{layoutRevisionId}")
    @Operation(
            operationId = "getDocumentLayoutRevision",
            summary =
                    "Read an authorized retained layout association, including retired"
                            + " publications")
    public Mono<DocumentLayoutPublicationDTO> getDocumentLayoutRevision(
            @PathVariable UUID layoutRevisionId) {
        return service.get(layoutRevisionId);
    }

    @DisableIdempotency
    @GetMapping("/resolve")
    @Operation(
            operationId = "resolveDocumentLayoutRevision",
            summary = "Resolve the exact currently usable layout and recognized metadata profile")
    public Mono<DocumentLayoutPublicationDTO> resolveDocumentLayoutRevision(
            @RequestParam String templateCode,
            @RequestParam String version,
            @RequestParam String localeCode,
            @RequestParam String outputProfileCode,
            @RequestParam String outputProfileVersion,
            @RequestParam String outputProfileManifestDigest) {
        return service.resolve(
                templateCode,
                version,
                localeCode,
                new DocumentLayoutOutputProfileReference(
                        outputProfileCode,
                        outputProfileVersion,
                        MediaType.TEXT_HTML_VALUE,
                        outputProfileManifestDigest));
    }

    @PostMapping(value = "/{layoutRevisionId}/retire", consumes = MediaType.APPLICATION_JSON_VALUE)
    @DisableIdempotency
    @Operation(
            operationId = "retireDocumentLayoutRevision",
            summary =
                    "Retire an association using an immutable action and original expected state"
                            + " version zero")
    public Mono<DocumentLayoutPublicationDTO> retireDocumentLayoutRevision(
            @PathVariable UUID layoutRevisionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            required = true,
                            content =
                                    @Content(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    DocumentLayoutRetireRequest
                                                                            .class)))
                    @RequestBody
                    String raw) {
        return Mono.defer(
                () -> service.retire(layoutRevisionId, DocumentLayoutRequestDecoder.retire(raw)));
    }
}
