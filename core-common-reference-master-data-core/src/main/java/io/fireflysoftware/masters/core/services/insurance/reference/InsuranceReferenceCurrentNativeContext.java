package io.fireflysoftware.masters.core.services.insurance.reference;

import java.time.Instant;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.Target;
import reactor.core.publisher.Mono;

/** Technical CURRENT read purpose. No command/recovery context or fabricated action is accepted. */
public interface InsuranceReferenceCurrentNativeContext {
    record Request(UUID nonce,Target target,String issuer,String subject,Instant authorityUntil) {
        @Override public String toString(){return "InsuranceReferenceCurrentNativeRequest";}
    }
    record Stamp(UUID nonce,Target target,long dbNowMillis,Instant recordedAt,Instant effectiveUntil) {
        @Override public String toString(){return "InsuranceReferenceCurrentNativeStamp";}
    }
    @FunctionalInterface
    interface Factory { InsuranceReferenceCurrentNativeContext create(org.springframework.data.r2dbc.core.R2dbcEntityTemplate operations); }
    DatabaseClient databaseClient();
    Mono<Stamp> begin(Request request);
    Mono<Boolean> complete(Request request);
    static InsuranceReferenceCurrentNativeContext unavailable(DatabaseClient client) {
        return new InsuranceReferenceCurrentNativeContext() {
            public DatabaseClient databaseClient(){return client;}
            public Mono<Stamp> begin(Request request){return Mono.error(new IllegalStateException("INSURANCE_REFERENCE_CURRENT_NATIVE_UNAVAILABLE"));}
            public Mono<Boolean> complete(Request request){return Mono.error(new IllegalStateException("INSURANCE_REFERENCE_CURRENT_NATIVE_UNAVAILABLE"));}
        };
    }
}
