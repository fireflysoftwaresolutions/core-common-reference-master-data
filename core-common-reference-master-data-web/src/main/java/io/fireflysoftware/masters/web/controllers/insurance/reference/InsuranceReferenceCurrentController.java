package io.fireflysoftware.masters.web.controllers.insurance.reference;

import java.util.*;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(prefix="firefly.masters.insurance-reference.current",name="enabled",havingValue="true")
@RequestMapping(value="/api/v1/insurance-references",produces=MediaType.APPLICATION_JSON_VALUE)
public final class InsuranceReferenceCurrentController {
    private final InsuranceReferenceCurrentObservationService service;
    public InsuranceReferenceCurrentController(InsuranceReferenceCurrentObservationService service){this.service=Objects.requireNonNull(service);}

    @GetMapping("/{kind}/{recordId}/publications/{publicationId}/revisions/{revision}/current")
    @Operation(operationId="observeCurrentInsuranceReference",summary="Observe one exact currently eligible catalog reference")
    @ApiResponse(responseCode="200",description="Finite exact current catalog observation, not financial or legal authority",content=@Content(schema=@Schema(implementation=Current.class)))
    public Mono<ResponseEntity<?>> current(@Parameter(schema=@Schema(implementation=Kind.class)) @PathVariable String kind,@PathVariable String recordId,@PathVariable String publicationId,
            @Parameter(schema=@Schema(type="integer",format="int64",minimum="1")) @PathVariable String revision,
            @Parameter(schema=@Schema(minLength=64,maxLength=64,pattern="^[0-9a-f]{64}(?![\\s\\S])")) @RequestParam String definitionDigest,
            @Parameter(schema=@Schema(type="integer",format="int64",minimum="0")) @RequestParam String generation,
            @Parameter(hidden=true) ServerWebExchange exchange) {
        return Mono.<ResponseEntity<?>>defer(()->{
            Target target=decode(kind,recordId,publicationId,revision,definitionDigest,generation,exchange);
            return service.observe(target).switchIfEmpty(Mono.error(new InsuranceReferenceCurrentFailure(InsuranceReferenceCurrentFailure.Kind.UNAVAILABLE)))
                .map(value->{value.budget().remaining();return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body((Object)value.value());});
        }).onErrorResume(error->{
            HttpStatus status=HttpStatus.SERVICE_UNAVAILABLE;String code="INSURANCE_REFERENCE_CURRENT_UNAVAILABLE";
            if(error instanceof IllegalArgumentException){status=HttpStatus.BAD_REQUEST;code="INSURANCE_REFERENCE_CURRENT_TARGET_INVALID";}
            else if(error instanceof InsuranceReferenceCurrentFailure failure){
                status=switch(failure.kind()){case FORBIDDEN->HttpStatus.FORBIDDEN;case NOT_CURRENT->HttpStatus.CONFLICT;case UNAVAILABLE->HttpStatus.SERVICE_UNAVAILABLE;};
                code=failure.getMessage();
            }
            return Mono.<ResponseEntity<?>>just(ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new Error(code)));
        });
    }
    static Target decode(String kind,String record,String publication,String revision,String digest,String generation,ServerWebExchange exchange) {
        if(!exchange.getRequest().getQueryParams().keySet().equals(Set.of("definitionDigest","generation"))
                || exchange.getRequest().getQueryParams().values().stream().anyMatch(values->values.size()!=1)
                || revision==null || !revision.matches("[1-9][0-9]{0,18}") || generation==null || !generation.matches("0|[1-9][0-9]{0,18}"))throw invalid();
        return new Target(Kind.valueOf(kind),uuid(record),uuid(publication),Long.parseLong(revision),digest,Long.parseLong(generation));
    }
    private static UUID uuid(String value){UUID id=UUID.fromString(value);if(!id.toString().equals(value))throw invalid();return id;}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INSURANCE_REFERENCE_CURRENT_TARGET_INVALID");}
    public record Error(String code) {}
}
