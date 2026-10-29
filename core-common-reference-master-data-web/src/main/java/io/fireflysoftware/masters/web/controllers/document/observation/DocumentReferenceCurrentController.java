/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.controllers.document.observation;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fireflysoftware.masters.core.services.document.observation.*;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(prefix="firefly.masters.document-reference.current",name="enabled",havingValue="true")
@RequestMapping(value="/api/v1/document-reference-current-observations",produces=MediaType.APPLICATION_JSON_VALUE)
public final class DocumentReferenceCurrentController {
    private static final ObjectMapper JSON=JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(8).maxStringLength(8192).maxNumberLength(32).build()).build()).enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,DeserializationFeature.FAIL_ON_TRAILING_TOKENS).disable(MapperFeature.ALLOW_COERCION_OF_SCALARS).build();
    static{var text=JSON.coercionConfigFor(com.fasterxml.jackson.databind.type.LogicalType.Textual);for(var shape:java.util.List.of(com.fasterxml.jackson.databind.cfg.CoercionInputShape.Integer,com.fasterxml.jackson.databind.cfg.CoercionInputShape.Float,com.fasterxml.jackson.databind.cfg.CoercionInputShape.Boolean))text.setCoercion(shape,com.fasterxml.jackson.databind.cfg.CoercionAction.Fail);}
    private final DocumentReferenceCurrentService service;
    public DocumentReferenceCurrentController(@Qualifier("documentReferenceNativeCurrentService")DocumentReferenceCurrentService service){this.service=java.util.Objects.requireNonNull(service);}
    @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE) @DisableIdempotency
    @Operation(operationId="observeCurrentDocumentReferences",summary="Observe exact finite source/layout references for caller product intent; no product or asset admission")
    @ApiResponse(responseCode="200",content=@Content(schema=@Schema(implementation=Current.class)))
    public Mono<ResponseEntity<?>> current(@io.swagger.v3.oas.annotations.parameters.RequestBody(required=true,content=@Content(schema=@Schema(implementation=Target.class))) @RequestBody String raw){return Mono.<ResponseEntity<?>>defer(()->{
        Target target;try{if(raw==null||raw.getBytes(StandardCharsets.UTF_8).length>8192)throw new IllegalArgumentException();var tree=JSON.readTree(raw);if(tree==null||!tree.isObject())throw new IllegalArgumentException();for(String key:java.util.List.of("tenantId","rootId","productId","revisionId","publicationId"))canonicalUuid(tree.path("product").path(key));canonicalUuid(tree.path("template").path("id"));canonicalUuid(tree.path("layout").path("id"));target=JSON.treeToValue(tree,Target.class);}catch(Exception invalid){throw new IllegalArgumentException("DOCUMENT_REFERENCE_CURRENT_TARGET_INVALID");}
        return service.observeCurrent(target).switchIfEmpty(Mono.error(new DocumentReferenceCurrentFailure(DocumentReferenceCurrentFailure.Kind.UNAVAILABLE))).map(value->{value.budget().remaining();return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body((Object)value.value());});
    }).onErrorResume(error->{HttpStatus status=HttpStatus.SERVICE_UNAVAILABLE;String code="DOCUMENT_REFERENCE_CURRENT_UNAVAILABLE";if(error instanceof IllegalArgumentException){status=HttpStatus.BAD_REQUEST;code="DOCUMENT_REFERENCE_CURRENT_TARGET_INVALID";}else if(error instanceof DocumentReferenceCurrentFailure failure){status=switch(failure.kind()){case FORBIDDEN->HttpStatus.FORBIDDEN;case NOT_CURRENT->HttpStatus.CONFLICT;case UNAVAILABLE->HttpStatus.SERVICE_UNAVAILABLE;};code=failure.getMessage();}return Mono.just(ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new Error(code)));});}
    private static void canonicalUuid(JsonNode value){if(!value.isTextual()||!value.textValue().matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))throw new IllegalArgumentException();}
    public record Error(String code){}
}
