/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.interfaces.dtos.insurance.reference;

import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyReference;
import java.time.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.*;

/** Global catalog data only; construction does not prove publication, current authority or admission. */
public final class InsuranceReferenceContracts {
    public static final String ENCODING="MASTERS_INSURANCE_REFERENCE_V1";
    public static final String SCOPE="GLOBAL_MASTER";
    private static final Instant MIN=Instant.parse("1900-01-01T00:00:00Z"),MAX=Instant.parse("9999-12-31T23:59:59.999999Z");
    private InsuranceReferenceContracts(){}
    public enum Kind { PREMIUM_TAX, COVERAGE_WORDING, EXCLUSION_WORDING }
    public enum Origin { SANDBOX, CATALOG_SOURCE }
    public enum TaxBasis { NET_PREMIUM_EXCLUSIVE }
    public enum Rounding { HALF_UP, HALF_EVEN, DOWN, UP }
    public enum Event { NEW_BUSINESS, RENEWAL, ENDORSEMENT, CANCELLATION_REFUND }
    public enum State { ACTIVE, WITHDRAWN }
    @Schema(oneOf={PremiumTax.class,Wording.class})
    public sealed interface Body permits PremiumTax,Wording {}
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE, requiredProperties={"origin","sourceReference","versionText","sourceDigest","rightsReference"})
    public record Provenance(Origin origin,String sourceReference,String versionText,String sourceDigest,String rightsReference){
        public Provenance{Objects.requireNonNull(origin);text(sourceReference,500,false);text(versionText,128,false);digest(sourceDigest);text(rightsReference,500,false);}
        @Override public String toString(){return "InsuranceReferenceProvenance";}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE, requiredProperties={"jurisdictions","productProfiles","events"})
    public record Applicability(List<String> jurisdictions,List<String> productProfiles,List<Event> events){
        public Applicability{
            jurisdictions=ordered(jurisdictions);for(String code:jurisdictions)require(code.matches("[A-Z]{2}(?:-[A-Z0-9]{1,8})?"));
            productProfiles=ordered(productProfiles);require(productProfiles.equals(List.of("TERM_PC_INSURANCE_V1")));
            Objects.requireNonNull(events);require(!events.isEmpty()&&events.size()<=Event.values().length&&events.stream().allMatch(Objects::nonNull)&&new HashSet<>(events).size()==events.size());events=events.stream().sorted(Comparator.comparing(Enum::name)).toList();
        }
        @Override public String toString(){return "InsuranceReferenceApplicability";}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE, requiredProperties={"basis","ratePartsPerMillion","roundingMode","postedScale","currencyPolicy","applicability"})
    public record PremiumTax(TaxBasis basis,int ratePartsPerMillion,Rounding roundingMode,int postedScale,
            CurrencyPolicyReference currencyPolicy,Applicability applicability) implements Body {
        public PremiumTax{
            Objects.requireNonNull(basis);Objects.requireNonNull(roundingMode);Objects.requireNonNull(applicability);
            require(ratePartsPerMillion>=0&&ratePartsPerMillion<=1000000&&postedScale>=0&&postedScale<=10);
            require(applicability.events().equals(List.of(Event.NEW_BUSINESS)));currency(currencyPolicy);
        }
        @Override public String toString(){return "InsurancePremiumTaxDefinition";}
    }
    @Schema(additionalProperties=Schema.AdditionalPropertiesValue.FALSE, requiredProperties={"localeId","localeCode","mediaType","content","applicability"})
    public record Wording(UUID localeId,String localeCode,String mediaType,String content,Applicability applicability) implements Body {
        public Wording{uuid(localeId);require(localeCode!=null&&localeCode.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,49}"));require("text/plain".equals(mediaType));text(content,65536,true);Objects.requireNonNull(applicability);}
        @Override public String toString(){return "InsuranceWordingDefinition";}
    }
    public record Revision(Kind kind,UUID recordId,UUID publicationId,long revision,String code,Instant effectiveFrom,
            Instant effectiveUntil,Body definition,Provenance provenance){
        public Revision{
            Objects.requireNonNull(kind);uuid(recordId);uuid(publicationId);require(revision>0);require(code!=null&&code.matches("[A-Z][A-Z0-9_]{0,63}"));requireTimestamp(effectiveFrom);if(effectiveUntil!=null){requireTimestamp(effectiveUntil);require(effectiveUntil.isAfter(effectiveFrom));}Objects.requireNonNull(provenance);
            require(kind==Kind.PREMIUM_TAX?definition instanceof PremiumTax:definition instanceof Wording);
        }
        @Override public String toString(){return "InsuranceReferenceRevision";}
    }
    public record Control(UUID recordId,Kind kind,UUID publicationId,long revision,long generation,State state){
        public Control{uuid(recordId);uuid(publicationId);Objects.requireNonNull(kind);Objects.requireNonNull(state);require(revision>0&&generation>=0);}
        @Override public String toString(){return "InsuranceReferenceControl";}
    }
    /** Observation shape only. Actual current stored records, principal, policy and native timing are service obligations. */
    public record Observation(Revision definition,Control control,@Schema(ref="#/components/schemas/FireflyInstant") Instant observedAt,@Schema(ref="#/components/schemas/FireflyInstant") Instant effectiveUntil){
        public Observation{
            Objects.requireNonNull(definition);Objects.requireNonNull(control);requireTimestamp(observedAt);requireTimestamp(effectiveUntil);
            require(control.state()==State.ACTIVE&&control.recordId().equals(definition.recordId())&&control.kind()==definition.kind()&&control.publicationId().equals(definition.publicationId())&&control.revision()==definition.revision());
            require(!observedAt.isBefore(definition.effectiveFrom())&&(definition.effectiveUntil()==null||observedAt.isBefore(definition.effectiveUntil())));
            require(effectiveUntil.isAfter(observedAt)&&Duration.between(observedAt,effectiveUntil).compareTo(Duration.ofSeconds(30))<=0&&(definition.effectiveUntil()==null||!effectiveUntil.isAfter(definition.effectiveUntil())));
        }
        @Override public String toString(){return "InsuranceReferenceObservation";}
    }
    private static List<String> ordered(List<String> values){Objects.requireNonNull(values);require(!values.isEmpty()&&values.size()<=64&&values.stream().allMatch(Objects::nonNull)&&new HashSet<>(values).size()==values.size());return values.stream().sorted().toList();}
    private static void currency(CurrencyPolicyReference value){Objects.requireNonNull(value);parseUuid(value.policyId());parseUuid(value.currencyId());require(value.isoCode()!=null&&value.isoCode().matches("[A-Z]{3}")&&value.version()!=null&&value.version()>0&&"MASTERS_CURRENCY_POLICY_V1".equals(value.encodingVersion()));digest(value.digest());}
    private static void parseUuid(String value){try{var id=UUID.fromString(value);require(id.toString().equals(value));uuid(id);}catch(RuntimeException e){throw invalid();}}
    private static void uuid(UUID value){require(value!=null&&!value.equals(new UUID(0,0)));}
    private static void digest(String value){require(value!=null&&value.matches("[0-9a-f]{64}"));}
    public static void requireTimestamp(Instant value){require(value!=null&&value.getNano()%1000==0&&!value.isBefore(MIN)&&!value.isAfter(MAX));}
    private static void text(String value,int bytes,boolean multiline){
        require(value!=null&&!value.isBlank()&&value.length()<=bytes);
        for(int i=0;i<value.length();){char c=value.charAt(i);if(Character.isSurrogate(c))require(Character.isHighSurrogate(c)&&i+1<value.length()&&Character.isLowSurrogate(value.charAt(i+1)));int point=value.codePointAt(i);require(point!=0x7f&&(point>=0x20||(multiline&&(point=='\r'||point=='\n'||point=='\t'))));if(!multiline)require(point!=0x85&&point!=0x2028&&point!=0x2029);i+=Character.charCount(point);}
        require(value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=bytes);
    }
    private static void require(boolean ok){if(!ok)throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INSURANCE_REFERENCE_SHAPE");}
}
