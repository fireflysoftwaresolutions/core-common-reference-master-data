package io.fireflysoftware.masters.interfaces.dtos.currency.policy;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/** Exact representation data; neither a financial permit nor a current grant. */
public final class CurrencyPolicyCurrentContracts {
    private CurrencyPolicyCurrentContracts() {}
    @Schema(name="CurrencyPolicyCurrentTarget",additionalProperties=Schema.AdditionalPropertiesValue.FALSE,requiredProperties={"reference","postedScale"})
    public record Target(CurrencyPolicyReference reference,int postedScale) {
        public Target {
            Objects.requireNonNull(reference);
            uuid(reference.policyId());uuid(reference.currencyId());
            if(reference.isoCode()==null||!reference.isoCode().matches("[A-Z]{3}")||reference.version()==null||reference.version()<1
                    ||reference.digest()==null||!reference.digest().matches("[0-9a-f]{64}")
                    ||!"MASTERS_CURRENCY_POLICY_V1".equals(reference.encodingVersion())||postedScale<0||postedScale>10)throw invalid();
        }
    }
    @Schema(name="CurrencyPolicyCurrent",additionalProperties=Schema.AdditionalPropertiesValue.FALSE,requiredProperties={"target","publication","recordedAt","effectiveUntil"})
    public record Current(Target target,CurrencyPolicyView publication,
            @Schema(ref="#/components/schemas/FireflyInstant") Instant recordedAt,
            @Schema(ref="#/components/schemas/FireflyInstant") Instant effectiveUntil) {
        public Current {
            Objects.requireNonNull(target);Objects.requireNonNull(publication);Objects.requireNonNull(recordedAt);Objects.requireNonNull(effectiveUntil);
            if(!target.reference().equals(publication.reference())||!Objects.equals(target.postedScale(),publication.postedScale())
                    ||!effectiveUntil.isAfter(recordedAt))throw invalid();
        }
    }
    private static void uuid(String value){
        if(value==null)throw invalid();UUID id=UUID.fromString(value);
        if(!id.toString().equals(value)||id.equals(new UUID(0,0)))throw invalid();
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("CURRENCY_POLICY_CURRENT_TARGET_INVALID");}
}
