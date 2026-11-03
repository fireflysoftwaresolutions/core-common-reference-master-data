/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.controllers.insurance.reference;

import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceOutcome;
import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferencePublicationService;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCommands.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import java.util.function.Function;
import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(prefix = "firefly.masters.insurance-reference.commands", name = "enabled", havingValue = "true")
@RequestMapping(value = "/api/v1/insurance-references", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Insurance Reference Commands", description = "Current-authorized catalog commands and historical action receipts; receipts are not current publication evidence")
public final class InsuranceReferenceController {
    private final InsuranceReferencePublicationService service;

    public InsuranceReferenceController(InsuranceReferencePublicationService service) {
        this.service = Objects.requireNonNull(service);
    }

    @DisableIdempotency
    @PostMapping(value = "/publication-intents", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "submitInsuranceReferencePublication", summary = "Submit catalog data for independent approval")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SubmitPublication.class)))
    @ApiResponse(responseCode = "200", description = "Historical action receipt", content = @Content(schema = @Schema(implementation = Receipt.class)))
    public Mono<ResponseEntity<?>> submitPublication(@Parameter(hidden = true) ServerWebExchange exchange) {
        return command(exchange, InsuranceReferenceRequestDecoder::publication, service::submitPublication);
    }

    @DisableIdempotency
    @PostMapping(value = "/withdrawal-intents", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "submitInsuranceReferenceWithdrawal", summary = "Submit an exact current catalog publication for withdrawal approval")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = SubmitWithdrawal.class)))
    @ApiResponse(responseCode = "200", description = "Historical action receipt", content = @Content(schema = @Schema(implementation = Receipt.class)))
    public Mono<ResponseEntity<?>> submitWithdrawal(@Parameter(hidden = true) ServerWebExchange exchange) {
        return command(exchange, InsuranceReferenceRequestDecoder::withdrawal, service::submitWithdrawal);
    }

    @DisableIdempotency
    @PostMapping(value = "/actions/recover", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "recoverInsuranceReferenceAction", summary = "Recover an exact retained receipt under fresh current permission")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = Recovery.class)))
    @ApiResponse(responseCode = "200", description = "Historical action receipt without a current eligibility claim", content = @Content(schema = @Schema(implementation = Receipt.class)))
    public Mono<ResponseEntity<?>> recover(@Parameter(hidden = true) ServerWebExchange exchange) {
        return command(exchange, InsuranceReferenceRequestDecoder::recovery, service::recover);
    }

    @DisableIdempotency
    @PostMapping(value = "/intents/approve", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "approveInsuranceReferenceIntent", summary = "Approve an exact retained intent as an independent current caller")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = IntentAction.class)))
    @ApiResponse(responseCode = "200", description = "Historical action receipt", content = @Content(schema = @Schema(implementation = Receipt.class)))
    public Mono<ResponseEntity<?>> approve(@Parameter(hidden = true) ServerWebExchange exchange) {
        return command(exchange, InsuranceReferenceRequestDecoder::intentAction, service::approve);
    }

    @DisableIdempotency
    @PostMapping(value = "/intents/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "executeInsuranceReferenceIntent", summary = "Execute an exact approved intent after current source and native checks")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = IntentAction.class)))
    @ApiResponse(responseCode = "200", description = "Historical action receipt", content = @Content(schema = @Schema(implementation = Receipt.class)))
    public Mono<ResponseEntity<?>> execute(@Parameter(hidden = true) ServerWebExchange exchange) {
        return command(exchange, InsuranceReferenceRequestDecoder::intentAction, service::execute);
    }

    @DisableIdempotency
    @PostMapping(value = "/intents/withdraw", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "withdrawInsuranceReferenceIntent", summary = "Withdraw an exact retained intent under current permission")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = IntentAction.class)))
    @ApiResponse(responseCode = "200", description = "Historical action receipt", content = @Content(schema = @Schema(implementation = Receipt.class)))
    public Mono<ResponseEntity<?>> withdrawIntent(@Parameter(hidden = true) ServerWebExchange exchange) {
        return command(exchange, InsuranceReferenceRequestDecoder::intentAction, service::withdrawIntent);
    }

    public record Error(String code) {}

    private <T> Mono<ResponseEntity<?>> command(ServerWebExchange exchange, Function<byte[], T> decoder,
            Function<T, Mono<Receipt>> operation) {
        return DataBufferUtils.join(exchange.getRequest().getBody(), InsuranceReferenceRequestDecoder.MAX_BYTES)
                .map(buffer -> {
                    try {
                        var bytes = new byte[buffer.readableByteCount()];
                        buffer.read(bytes);
                        return bytes;
                    } finally {
                        DataBufferUtils.release(buffer);
                    }
                })
                .<ResponseEntity<?>>flatMap(bytes -> {
                    final T request;
                    try {
                        request = decoder.apply(bytes);
                    } catch (IllegalArgumentException invalid) {
                        return Mono.just(response(HttpStatus.BAD_REQUEST, new Error("INSURANCE_REFERENCE_REQUEST_INVALID")));
                    }
                    return Mono.defer(() -> operation.apply(request))
                            .switchIfEmpty(Mono.error(new IllegalStateException("INSURANCE_REFERENCE_CURRENT_OPERATION_UNAVAILABLE")))
                            .<ResponseEntity<?>>map(receipt -> response(HttpStatus.OK, receipt))
                            .onErrorResume(InsuranceReferenceOutcome.class, failure -> Mono.just(response(
                                    HttpStatus.SERVICE_UNAVAILABLE, new Error("INSURANCE_REFERENCE_PROTECTED_RESULT_UNAVAILABLE"))))
                            .onErrorResume(failure -> Mono.just(response(
                                    HttpStatus.FORBIDDEN, new Error("INSURANCE_REFERENCE_CURRENT_OPERATION_UNAVAILABLE"))));
                })
                .switchIfEmpty(Mono.fromSupplier(() -> response(HttpStatus.BAD_REQUEST, new Error("INSURANCE_REFERENCE_REQUEST_INVALID"))))
                .onErrorResume(DataBufferLimitException.class, failure -> Mono.just(response(
                        HttpStatus.PAYLOAD_TOO_LARGE, new Error("INSURANCE_REFERENCE_REQUEST_TOO_LARGE"))))
                .onErrorResume(failure -> Mono.just(response(HttpStatus.BAD_REQUEST, new Error("INSURANCE_REFERENCE_REQUEST_INVALID"))));
    }

    private static ResponseEntity<?> response(HttpStatus status, Object body) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }
}
