package io.fireflysoftware.masters.core.services.currency.observation;

import java.time.Instant;
import java.util.UUID;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;

/** Protected technical scope/time only; current shared principal and PDP remain application obligations. */
public interface CurrencyPolicyCurrentNativeContext {
    record Request(UUID nonce,Target target,String issuer,String subject,String tenant,Instant authorityUntil){
        @Override public String toString(){return "CurrencyPolicyCurrentNativeRequest";}
    }
    record Stamp(UUID nonce,Target target,long dbNowMillis,Instant recordedAt,Instant effectiveUntil){
        @Override public String toString(){return "CurrencyPolicyCurrentNativeStamp";}
    }
    @FunctionalInterface interface Factory{CurrencyPolicyCurrentNativeContext create(R2dbcEntityTemplate operations);}
    DatabaseClient databaseClient();
    Mono<Stamp> begin(Request request);
    Mono<Boolean> complete(Request request);
}
