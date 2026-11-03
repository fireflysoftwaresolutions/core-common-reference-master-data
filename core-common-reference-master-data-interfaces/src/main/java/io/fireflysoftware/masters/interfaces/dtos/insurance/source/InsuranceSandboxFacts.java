package io.fireflysoftware.masters.interfaces.dtos.insurance.source;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCommands.PublicationDraft;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;

/** Durable synthetic catalog provenance only; no legal, tax, insurance or financial permission. */
public final class InsuranceSandboxFacts {
    public static final String SCOPE="SANDBOX";
    @Schema(name="InsuranceSandboxFactType")
    public enum Type { SOURCE, RIGHTS, LOCALE }
    @Schema(name="InsuranceSandboxFactState")
    public enum State { ACTIVE, WITHDRAWN }
    private InsuranceSandboxFacts() {}
    @Schema(name="InsuranceSandboxFactReference",additionalProperties=Schema.AdditionalPropertiesValue.FALSE,
            requiredProperties={"type","factId","version","generation","digest"})
    public record Reference(Type type,UUID factId,long version,long generation,String digest) {
        public Reference { Objects.requireNonNull(type);id(factId);if(version<1||generation<0)throw invalid();sha(digest); }
        @Override public String toString(){return "InsuranceSandboxFactReference";}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE,
            requiredProperties={"actionId","intentDigest","publicationId","revision"})
    public record SubmittedPublication(UUID actionId,String intentDigest,UUID publicationId,long revision) {
        public SubmittedPublication {id(actionId);sha(intentDigest);id(publicationId);if(revision<1)throw invalid();}
        @Override public String toString(){return "InsuranceSubmittedPublicationReference";}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE,
            requiredProperties={"actionId","factId","expectedVersion","expectedGeneration","localeId","localeCode","effectiveUntil"})
    public record RegisterLocale(UUID actionId,UUID factId,long expectedVersion,long expectedGeneration,
            UUID localeId,String localeCode,@Schema(ref="#/components/schemas/FireflyInstant") Instant effectiveUntil) {
        public RegisterLocale {base(actionId,factId,expectedVersion,expectedGeneration,effectiveUntil);id(localeId);if(localeCode==null||!localeCode.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,49}"))throw invalid();}
        @Override public String toString(){return "InsuranceSandboxLocaleRegistration";}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE,
            requiredProperties={"actionId","factId","expectedVersion","expectedGeneration","sourceId","insuranceKind","sourceText","effectiveUntil"})
    public record RegisterRights(UUID actionId,UUID factId,long expectedVersion,long expectedGeneration,UUID sourceId,
            InsuranceReferenceContracts.Kind insuranceKind,String sourceText,Reference locale,@Schema(ref="#/components/schemas/FireflyInstant") Instant effectiveUntil) {
        public RegisterRights {base(actionId,factId,expectedVersion,expectedGeneration,effectiveUntil);id(sourceId);Objects.requireNonNull(insuranceKind);text(sourceText);InsuranceSandboxFacts.locale(insuranceKind,locale);}
        @Override public String toString(){return "InsuranceSandboxRightsRegistration";}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE,
            requiredProperties={"actionId","factId","expectedVersion","expectedGeneration","submitted","draft","sourceText","rights","effectiveUntil"})
    public record RegisterSource(UUID actionId,UUID factId,long expectedVersion,long expectedGeneration,
            SubmittedPublication submitted,PublicationDraft draft,String sourceText,Reference rights,Reference locale,@Schema(ref="#/components/schemas/FireflyInstant") Instant effectiveUntil) {
        public RegisterSource {base(actionId,factId,expectedVersion,expectedGeneration,effectiveUntil);Objects.requireNonNull(submitted);Objects.requireNonNull(draft);text(sourceText);if(rights==null||rights.type()!=Type.RIGHTS)throw invalid();InsuranceSandboxFacts.locale(draft.kind(),locale);}
        @Override public String toString(){return "InsuranceSandboxSourceRegistration";}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE,
            requiredProperties={"actionId","type","factId","expectedVersion","expectedGeneration","reasonCode"})
    public record Withdraw(UUID actionId,Type type,UUID factId,long expectedVersion,long expectedGeneration,
            @Schema(minLength=1,maxLength=64,pattern="^[A-Z][A-Z0-9_]{0,63}(?![\\s\\S])") String reasonCode) {
        public Withdraw {id(actionId);id(factId);Objects.requireNonNull(type);if(expectedVersion<1||expectedGeneration<0||reasonCode==null||!reasonCode.matches("[A-Z][A-Z0-9_]{0,63}"))throw invalid();}
        @Override public String toString(){return "InsuranceSandboxFactWithdrawal";}
    }
    /** Historical registration data; not current source evidence or publication approval. */
    @Schema(name="InsuranceSandboxFactReceipt",additionalProperties=Schema.AdditionalPropertiesValue.FALSE,requiredProperties={"actionId","reference","state"})
    public record Receipt(UUID actionId,Reference reference,State state) {
        public Receipt {id(actionId);Objects.requireNonNull(reference);Objects.requireNonNull(state);}
        @Override public String toString(){return "InsuranceSandboxFactReceipt";}
    }
    public static String sourceUri(UUID factId){id(factId);return "urn:firefly:sandbox:source:"+factId;}
    public static String rightsUri(Reference rights){if(rights==null||rights.type()!=Type.RIGHTS)throw invalid();return "urn:firefly:sandbox:rights:"+rights.factId()+":v"+rights.version();}
    private static void locale(InsuranceReferenceContracts.Kind kind,Reference locale){if(kind==InsuranceReferenceContracts.Kind.PREMIUM_TAX?locale!=null:locale==null||locale.type()!=Type.LOCALE)throw invalid();}
    private static void base(UUID action,UUID fact,long version,long generation,Instant until){id(action);id(fact);if(version<0||version==Long.MAX_VALUE||generation<0||generation>=Long.MAX_VALUE-1||version==0&&generation!=0)throw invalid();InsuranceReferenceContracts.requireTimestamp(until);}
    private static void id(UUID value){if(value==null||value.equals(new UUID(0,0)))throw invalid();}
    private static void sha(String value){if(value==null||!value.matches("[0-9a-f]{64}"))throw invalid();}
    private static void text(String value){
        if(value==null||value.isBlank()||value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>131072)throw invalid();
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);
            if(Character.isHighSurrogate(c)){if(i+1>=value.length()||!Character.isLowSurrogate(value.charAt(++i)))throw invalid();}
            else if(Character.isLowSurrogate(c)||c==0)throw invalid();
        }
    }
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INSURANCE_SANDBOX_FACT_SHAPE");}
}
