package io.fireflysoftware.masters.core.services.insurance.reference;
import java.time.Instant;
import reactor.core.publisher.Mono;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyReference;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Revision;
/** Required real current-owner dependency; a reference DTO or constructor does not fulfill it. */
public interface InsuranceReferenceSourceVerifier {
    record Evidence(String definitionDigest,CurrencyPolicyReference currencyPolicy,Integer postedScale,Instant observedAt,Instant effectiveUntil) { @Override public String toString(){return "InsuranceReferenceSourceEvidence";} }
    Mono<Evidence> verify(Revision proposed);
    static InsuranceReferenceSourceVerifier unavailable(){return proposed->Mono.error(new IllegalStateException("INSURANCE_REFERENCE_SOURCE_OWNER_UNAVAILABLE"));}
}
