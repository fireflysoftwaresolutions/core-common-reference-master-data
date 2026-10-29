package io.fireflysoftware.masters.web.controllers.insurance.reference;

import java.util.Objects;
import java.util.function.Function;
import io.fireflysoftware.masters.core.services.insurance.source.*;
import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceOutcome;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(prefix="firefly.masters.insurance-reference.sandbox-source",name="enabled",havingValue="true")
@RequestMapping(value="/api/v1/insurance-references/sandbox-facts",produces=MediaType.APPLICATION_JSON_VALUE)
public final class InsuranceSandboxFactController {
    private final InsuranceSandboxFactRegistrationService service;
    public InsuranceSandboxFactController(InsuranceSandboxFactRegistrationService service){this.service=Objects.requireNonNull(service);}
    @DisableIdempotency
    @PostMapping(value="/locales",consumes=MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId="registerInsuranceSandboxLocale",summary="Current-authorized durable sandbox catalog administration")
    @RequestBody(required=true,content=@Content(schema=@Schema(implementation=RegisterLocale.class)))
    @ApiResponse(responseCode="200",description="Historical sandbox fact receipt, not current eligibility or legal authority",content=@Content(schema=@Schema(implementation=Receipt.class)))
    public Mono<ResponseEntity<?>> registerLocale(@Parameter(hidden=true) ServerWebExchange exchange){return command(exchange,InsuranceSandboxFactDecoder::locale,service::registerLocale);}
    @DisableIdempotency
    @PostMapping(value="/rights",consumes=MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId="registerInsuranceSandboxRights",summary="Current-authorized durable sandbox catalog administration")
    @RequestBody(required=true,content=@Content(schema=@Schema(implementation=RegisterRights.class)))
    @ApiResponse(responseCode="200",description="Historical sandbox fact receipt, not current eligibility or legal authority",content=@Content(schema=@Schema(implementation=Receipt.class)))
    public Mono<ResponseEntity<?>> registerRights(@Parameter(hidden=true) ServerWebExchange exchange){return command(exchange,InsuranceSandboxFactDecoder::rights,service::registerRights);}
    @DisableIdempotency
    @PostMapping(value="/sources",consumes=MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId="registerInsuranceSandboxSource",summary="Current-authorized durable sandbox catalog administration")
    @RequestBody(required=true,content=@Content(schema=@Schema(implementation=RegisterSource.class)))
    @ApiResponse(responseCode="200",description="Historical sandbox fact receipt, not current eligibility or legal authority",content=@Content(schema=@Schema(implementation=Receipt.class)))
    public Mono<ResponseEntity<?>> registerSource(@Parameter(hidden=true) ServerWebExchange exchange){return command(exchange,InsuranceSandboxFactDecoder::source,service::registerSource);}
    @DisableIdempotency
    @PostMapping(value="/withdraw",consumes=MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId="withdrawInsuranceSandboxFact",summary="Current-authorized durable sandbox catalog administration")
    @RequestBody(required=true,content=@Content(schema=@Schema(implementation=Withdraw.class)))
    @ApiResponse(responseCode="200",description="Historical sandbox fact receipt, not current eligibility or legal authority",content=@Content(schema=@Schema(implementation=Receipt.class)))
    public Mono<ResponseEntity<?>> withdraw(@Parameter(hidden=true) ServerWebExchange exchange){return command(exchange,InsuranceSandboxFactDecoder::withdraw,service::withdraw);}
    public record Error(String code) {}

    private <T> Mono<ResponseEntity<?>> command(ServerWebExchange exchange, Function<byte[], T> decoder,
            Function<T, Mono<Receipt>> operation) {
        return DataBufferUtils.join(exchange.getRequest().getBody(), InsuranceSandboxFactDecoder.MAX_BYTES)
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
                        return Mono.just(response(HttpStatus.BAD_REQUEST, new Error("INSURANCE_SANDBOX_FACT_REQUEST_INVALID")));
                    }
                    return Mono.defer(() -> operation.apply(request))
                            .switchIfEmpty(Mono.error(new IllegalStateException("INSURANCE_SANDBOX_FACT_CURRENT_OPERATION_UNAVAILABLE")))
                            .<ResponseEntity<?>>map(receipt -> response(HttpStatus.OK, receipt))
                            .onErrorResume(InsuranceReferenceOutcome.class, failure -> Mono.just(response(
                                    HttpStatus.SERVICE_UNAVAILABLE, new Error("INSURANCE_SANDBOX_FACT_PROTECTED_RESULT_UNAVAILABLE"))))
                            .onErrorResume(InsuranceSandboxFactFailure.class,failure->Mono.just(response(switch(failure.kind()){
                                case FORBIDDEN->HttpStatus.FORBIDDEN;case CONFLICT->HttpStatus.CONFLICT;case UNAVAILABLE->HttpStatus.SERVICE_UNAVAILABLE;
                            },new Error(failure.getMessage()))))
                            .onErrorResume(failure->Mono.just(response(HttpStatus.SERVICE_UNAVAILABLE,new Error("INSURANCE_SANDBOX_FACT_UNAVAILABLE"))));
                })
                .switchIfEmpty(Mono.fromSupplier(() -> response(HttpStatus.BAD_REQUEST, new Error("INSURANCE_SANDBOX_FACT_REQUEST_INVALID"))))
                .onErrorResume(DataBufferLimitException.class, failure -> Mono.just(response(
                        HttpStatus.PAYLOAD_TOO_LARGE, new Error("INSURANCE_SANDBOX_FACT_REQUEST_TOO_LARGE"))))
                .onErrorResume(failure -> Mono.just(response(HttpStatus.BAD_REQUEST, new Error("INSURANCE_SANDBOX_FACT_REQUEST_INVALID"))));
    }

    private static ResponseEntity<?> response(HttpStatus status, Object body) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(body);
    }
}
