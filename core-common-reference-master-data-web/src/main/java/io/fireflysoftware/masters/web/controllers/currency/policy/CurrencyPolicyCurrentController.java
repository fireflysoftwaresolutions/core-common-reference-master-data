package io.fireflysoftware.masters.web.controllers.currency.policy;

import java.util.*;
import io.fireflysoftware.masters.core.services.currency.observation.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@ConditionalOnProperty(prefix="firefly.masters.currency-policy.current",name="enabled",havingValue="true")
@RequestMapping(value="/api/v1/currency-execution-policies",produces=MediaType.APPLICATION_JSON_VALUE)
public final class CurrencyPolicyCurrentController {
    private final CurrencyPolicyCurrentObservationService service;
    public CurrencyPolicyCurrentController(@Qualifier("currencyPolicyNativeCurrentObservationService") CurrencyPolicyCurrentObservationService service){this.service=Objects.requireNonNull(service);}
    @DisableIdempotency @GetMapping("/{policyId}/current")
    @Operation(operationId="observeCurrentCurrencyExecutionPolicy",summary="Observe an exact finite currently usable representation; not financial authority")
    @ApiResponse(responseCode="200",content=@Content(schema=@Schema(implementation=Current.class)))
    public Mono<ResponseEntity<?>> current(@PathVariable String policyId,@RequestParam String currencyId,@RequestParam String isoCode,
            @Parameter(schema=@Schema(type="integer",format="int64",minimum="1")) @RequestParam String version,
            @Parameter(schema=@Schema(minLength=64,maxLength=64,pattern="^[0-9a-f]{64}(?![\\s\\S])")) @RequestParam String digest,
            @RequestParam String encodingVersion,@Parameter(schema=@Schema(type="integer",minimum="0",maximum="10")) @RequestParam String postedScale,
            @Parameter(hidden=true) ServerWebExchange exchange){
        return Mono.<ResponseEntity<?>>defer(()->{
            if(!exchange.getRequest().getQueryParams().keySet().equals(Set.of("currencyId","isoCode","version","digest","encodingVersion","postedScale"))
                    ||exchange.getRequest().getQueryParams().values().stream().anyMatch(v->v.size()!=1)||version==null||!version.matches("[1-9][0-9]{0,18}")
                    ||postedScale==null||!postedScale.matches("0|[1-9]|10"))throw new IllegalArgumentException();
            var target=new Target(new CurrencyPolicyReference(policyId,currencyId,isoCode,Long.parseLong(version),digest,encodingVersion),Integer.parseInt(postedScale));
            return service.observeCurrent(target).switchIfEmpty(Mono.error(new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE)))
                    .map(value->{value.budget().remaining();return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body((Object)value.value());});
        }).onErrorResume(error->{
            HttpStatus status=HttpStatus.SERVICE_UNAVAILABLE;String code="CURRENCY_POLICY_CURRENT_UNAVAILABLE";
            if(error instanceof IllegalArgumentException){status=HttpStatus.BAD_REQUEST;code="CURRENCY_POLICY_CURRENT_TARGET_INVALID";}
            else if(error instanceof CurrencyPolicyCurrentFailure failure){status=switch(failure.kind()){case FORBIDDEN->HttpStatus.FORBIDDEN;case NOT_CURRENT->HttpStatus.CONFLICT;case UNAVAILABLE->HttpStatus.SERVICE_UNAVAILABLE;};code=failure.getMessage();}
            return Mono.just(ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new Error(code)));
        });
    }
    public record Error(String code){}
}
