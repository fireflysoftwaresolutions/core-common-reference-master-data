package io.fireflysoftware.masters.interfaces.dtos.insurance.reference;
import java.time.Instant;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.*;
/** Catalog commands contain selectors and data, never an actor, approval flag or authority receipt. */
public final class InsuranceReferenceCommands {
    private InsuranceReferenceCommands() {}
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE, requiredProperties={"revision","generation"})
    public record Head(UUID publicationId,long revision,long generation) {
        public Head {if(revision<0||generation<0||(publicationId==null)!=(revision==0)||publicationId==null&&generation!=0)throw invalid();if(publicationId!=null)id(publicationId);}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE, requiredProperties={"recordId","kind","code","effectiveFrom","definition","provenance","expected"})
    public record PublicationDraft(UUID recordId,Kind kind,String code,@Schema(ref="#/components/schemas/FireflyInstant") Instant effectiveFrom,@Schema(ref="#/components/schemas/FireflyInstant") Instant effectiveUntil,Body definition,Provenance provenance,Head expected) {
        public PublicationDraft {id(recordId);Objects.requireNonNull(kind);Objects.requireNonNull(expected);if(code==null||!code.matches("[A-Z][A-Z0-9_]{0,63}"))throw invalid();InsuranceReferenceContracts.requireTimestamp(effectiveFrom);if(effectiveUntil!=null){InsuranceReferenceContracts.requireTimestamp(effectiveUntil);if(!effectiveUntil.isAfter(effectiveFrom))throw invalid();}Objects.requireNonNull(provenance);if(kind==Kind.PREMIUM_TAX?!(definition instanceof PremiumTax):!(definition instanceof Wording))throw invalid();}
        @Override public String toString(){return "InsuranceReferencePublicationDraft";}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE, requiredProperties={"actionId","draft"})
    public record SubmitPublication(UUID actionId,PublicationDraft draft) {public SubmitPublication{id(actionId);Objects.requireNonNull(draft);}@Override public String toString(){return "InsuranceReferenceSubmitPublication";}}
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE, requiredProperties={"actionId","recordId","kind","expected"})
    public record SubmitWithdrawal(UUID actionId,UUID recordId,Kind kind,Head expected) {public SubmitWithdrawal{id(actionId);id(recordId);Objects.requireNonNull(kind);Objects.requireNonNull(expected);if(expected.publicationId()==null)throw invalid();}}
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE, requiredProperties={"actionId","recordId","kind","intentId","intentDigest","expectedIntentGeneration"})
    public record IntentAction(UUID actionId,UUID recordId,Kind kind,UUID intentId,String intentDigest,@Schema(minimum="0") long expectedIntentGeneration) {public IntentAction{id(actionId);id(recordId);Objects.requireNonNull(kind);id(intentId);digest(intentDigest);if(expectedIntentGeneration<0)throw invalid();}@Override public String toString(){return "InsuranceReferenceIntentAction";}}
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE, requiredProperties={"actionId","recordId","kind","intentDigest"})
    public record Recovery(UUID actionId,UUID recordId,Kind kind,String intentDigest) {public Recovery{id(actionId);id(recordId);Objects.requireNonNull(kind);digest(intentDigest);}}
    /** Historical catalog action data; it is not a current publication observation or a new grant. */
    @Schema(accessMode=Schema.AccessMode.READ_ONLY, description="Historical action receipt; not a current publication observation")
    public record Receipt(UUID actionId,UUID recordId,Kind kind,UUID intentId,String intentDigest,UUID publicationId,long revision,long intentGeneration,String operation) {
        public Receipt {id(actionId);id(recordId);Objects.requireNonNull(kind);id(intentId);digest(intentDigest);if(publicationId!=null)id(publicationId);if(revision<0||intentGeneration<0||operation==null)throw invalid();}
        @Override public String toString(){return "InsuranceReferenceActionReceipt";}
    }
    private static void id(UUID id){if(id==null||id.equals(new UUID(0,0)))throw invalid();}
    private static void digest(String digest){if(digest==null||!digest.matches("[0-9a-f]{64}"))throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INSURANCE_REFERENCE_COMMAND_SHAPE");}
}
