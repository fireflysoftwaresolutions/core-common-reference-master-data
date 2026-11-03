package io.fireflysoftware.masters.core.services.insurance.reference;
import java.time.Instant;
import java.util.UUID;
import reactor.core.publisher.Mono;
import org.springframework.r2dbc.core.DatabaseClient;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;
/** Technical native port only. Registration and installed V32/role qualification are explicit prerequisites. */
public interface InsuranceReferenceNativeContext {
    record Request(UUID nonce,UUID recordId,Kind kind,UUID actionId,UUID intentId,String mode,String issuer,String subject,Instant authorityUntil) { @Override public String toString(){return "InsuranceReferenceNativeRequest";} }
    record Stamp(UUID nonce,UUID recordId,Kind kind,long dbNowMillis,Instant recordedAt,Instant effectiveUntil) { @Override public String toString(){return "InsuranceReferenceNativeStamp";} }
    DatabaseClient databaseClient();
    Mono<Stamp> begin(Request request);
    Mono<Boolean> complete(Request request);
    static InsuranceReferenceNativeContext unavailable(DatabaseClient client) {
        return new InsuranceReferenceNativeContext(){public DatabaseClient databaseClient(){return client;}public Mono<Stamp> begin(Request request){return Mono.error(new Unavailable());}public Mono<Boolean> complete(Request request){return Mono.error(new Unavailable());}};
    }
    final class Unavailable extends IllegalStateException {public Unavailable(){super("INSURANCE_REFERENCE_NATIVE_CONTEXT_UNAVAILABLE");}}
}
