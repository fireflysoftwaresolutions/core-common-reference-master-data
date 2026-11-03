package io.fireflysoftware.masters.interfaces.dtos.insurance.reference;

import java.util.Objects;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;

/** Exact catalog reference only; construction does not establish current eligibility. */
public final class InsuranceReferenceCurrentContracts {
    private InsuranceReferenceCurrentContracts() {}
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE,
            requiredProperties={"kind","recordId","publicationId","revision","definitionDigest","generation"})
    public record Target(InsuranceReferenceContracts.Kind kind, UUID recordId, UUID publicationId,
            @Schema(minimum="1") long revision,
            @Schema(minLength=64,maxLength=64,pattern="^[0-9a-f]{64}(?![\\s\\S])") String definitionDigest,
            @Schema(minimum="0") long generation) {
        public Target {
            Objects.requireNonNull(kind);
            if (recordId==null || publicationId==null || recordId.equals(new UUID(0,0))
                    || publicationId.equals(new UUID(0,0)) || revision<1 || generation<0
                    || definitionDigest==null || !definitionDigest.matches("[0-9a-f]{64}"))
                throw new IllegalArgumentException("INSURANCE_REFERENCE_CURRENT_TARGET_INVALID");
        }
        @Override public String toString(){return "InsuranceReferenceCurrentTarget";}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE,requiredProperties={"reference","publication"})
    public record Current(Target reference, InsuranceReferenceContracts.Observation publication) {
        public Current { Objects.requireNonNull(reference);Objects.requireNonNull(publication); }
        @Override public String toString(){return "InsuranceReferenceCurrentObservation";}
    }
}
