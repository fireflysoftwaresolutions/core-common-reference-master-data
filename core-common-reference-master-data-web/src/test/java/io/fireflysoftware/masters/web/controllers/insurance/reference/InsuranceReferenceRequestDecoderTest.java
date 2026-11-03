package io.fireflysoftware.masters.web.controllers.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.Test;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.*;

class InsuranceReferenceRequestDecoderTest {
    static final String ACTION="10000000-0000-0000-0000-000000000001",RECORD="10000000-0000-0000-0000-000000000002",INTENT="10000000-0000-0000-0000-000000000003",PUBLICATION="10000000-0000-0000-0000-000000000004";
    static final String DIGEST="a".repeat(64);
    static final ObjectMapper JSON=new ObjectMapper();
    static byte[] bytes(String value){return value.getBytes(StandardCharsets.UTF_8);}
    static String tax(){return """
        {"actionId":"%s","draft":{"recordId":"%s","kind":"PREMIUM_TAX","code":"SANDBOX_TAX","effectiveFrom":"2026-10-27T00:00:00Z","effectiveUntil":null,
        "definition":{"basis":"NET_PREMIUM_EXCLUSIVE","ratePartsPerMillion":1250,"roundingMode":"HALF_UP","postedScale":2,
        "currencyPolicy":{"policyId":"%s","currencyId":"%s","isoCode":"USD","version":1,"digest":"%s","encodingVersion":"MASTERS_CURRENCY_POLICY_V1"},
        "applicability":{"jurisdictions":["US"],"productProfiles":["TERM_PC_INSURANCE_V1"],"events":["NEW_BUSINESS"]}},
        "provenance":{"origin":"SANDBOX","sourceReference":"sandbox-source","versionText":"1","sourceDigest":"%s","rightsReference":"sandbox-only"},
        "expected":{"publicationId":null,"revision":0,"generation":0}}}
        """.formatted(ACTION,RECORD,PUBLICATION,INTENT,DIGEST,DIGEST);}
    static String action(){return "{\"actionId\":\""+ACTION+"\",\"recordId\":\""+RECORD+"\",\"kind\":\"PREMIUM_TAX\",\"intentId\":\""+INTENT+"\",\"intentDigest\":\""+DIGEST+"\",\"expectedIntentGeneration\":1}";}
    @Test void actualTaxDraftIsDataNotAServerAllocatedRevision(){
        var value=InsuranceReferenceRequestDecoder.publication(bytes(tax()));
        assertEquals(UUID.fromString(ACTION),value.actionId());assertEquals(Kind.PREMIUM_TAX,value.draft().kind());
        assertEquals(1250,((PremiumTax)value.draft().definition()).ratePartsPerMillion());assertNull(value.draft().expected().publicationId());
    }
    @Test void nullableWireFieldsMayBeOmittedByTheNormalSerializer()throws Exception{
        var root=JSON.readTree(tax());((com.fasterxml.jackson.databind.node.ObjectNode)root.get("draft")).remove("effectiveUntil");((com.fasterxml.jackson.databind.node.ObjectNode)root.get("draft").get("expected")).remove("publicationId");
        assertEquals(InsuranceReferenceRequestDecoder.publication(bytes(tax())),InsuranceReferenceRequestDecoder.publication(JSON.writeValueAsBytes(root)));
    }
    @Test void bothWordingKindsDecodeTheirActualShape()throws Exception{
        for(String kind:new String[]{"COVERAGE_WORDING","EXCLUSION_WORDING"}){
            var root=JSON.readTree(tax());var draft=(com.fasterxml.jackson.databind.node.ObjectNode)root.get("draft");draft.put("kind",kind);
            var body=JSON.createObjectNode();body.put("localeId",INTENT);body.put("localeCode","en-US");body.put("mediaType","text/plain");body.put("content","Sandbox wording\nOnly");body.set("applicability",draft.get("definition").get("applicability"));draft.set("definition",body);
            assertInstanceOf(Wording.class,InsuranceReferenceRequestDecoder.publication(JSON.writeValueAsBytes(root)).draft().definition());
        }
    }
    @Test void allOtherExistingCommandsKeepExactSelectors(){
        var a=InsuranceReferenceRequestDecoder.intentAction(bytes(action()));assertEquals(UUID.fromString(INTENT),a.intentId());assertEquals(1,a.expectedIntentGeneration());
        var recover=action().replace("\"intentId\":\""+INTENT+"\",","").replace(",\"expectedIntentGeneration\":1","");assertEquals(DIGEST,InsuranceReferenceRequestDecoder.recovery(bytes(recover)).intentDigest());
        var withdrawal="{\"actionId\":\""+ACTION+"\",\"recordId\":\""+RECORD+"\",\"kind\":\"PREMIUM_TAX\",\"expected\":{\"publicationId\":\""+PUBLICATION+"\",\"revision\":1,\"generation\":0}}";
        assertEquals(UUID.fromString(PUBLICATION),InsuranceReferenceRequestDecoder.withdrawal(bytes(withdrawal)).expected().publicationId());
    }
    @Test void authorityFieldsDuplicatesAndTrailingTokensAreRejected(){
        for(String raw:new String[]{action().replace("{","{\"actor\":\"staff\","),action()+" {}",action().replace("{","{\"actionId\":\""+ACTION+"\",")})
            invalid(()->InsuranceReferenceRequestDecoder.intentAction(bytes(raw)));
    }
    @Test void exactJsonIntegersNeverCoerceStringsDecimalsOrOverflow(){
        for(String number:new String[]{"\"1\"","1.0","1e0","9223372036854775808","true","null"})invalid(()->InsuranceReferenceRequestDecoder.intentAction(bytes(action().replace("\"expectedIntentGeneration\":1","\"expectedIntentGeneration\":"+number))));
    }
    @Test void malformedCanonicalIdsDigestsAndKindsAreRejected(){
        for(String raw:new String[]{action().replace(ACTION,"1-1-1-1-1"),action().replace(ACTION,"00000000-0000-0000-0000-000000000000"),action().replace(DIGEST,"A".repeat(64)),action().replace("PREMIUM_TAX","premium_tax")})invalid(()->InsuranceReferenceRequestDecoder.intentAction(bytes(raw)));
    }
    @Test void coreDefinitionRulesAndTimePrecisionRemainAuthoritative(){
        for(String raw:new String[]{tax().replace("\"postedScale\":2","\"postedScale\":11"),tax().replace("\"ratePartsPerMillion\":1250","\"ratePartsPerMillion\":1000001"),tax().replace("2026-10-27T00:00:00Z","2026-10-27T00:00:00.0000001Z"),tax().replace("\"kind\":\"PREMIUM_TAX\"","\"kind\":\"COVERAGE_WORDING\"")})invalid(()->InsuranceReferenceRequestDecoder.publication(bytes(raw)));
    }
    @Test void malformedUtf8AndRealByteLimitAreBounded(){
        invalid(()->InsuranceReferenceRequestDecoder.publication(new byte[]{(byte)0xc3,(byte)0x28}));
        invalid(()->InsuranceReferenceRequestDecoder.publication(new byte[512*1024+1]));
    }
    private static void invalid(org.junit.jupiter.api.function.Executable action){var error=assertThrows(IllegalArgumentException.class,action);assertEquals("INSURANCE_REFERENCE_REQUEST_INVALID",error.getMessage());assertNull(error.getCause());}
}
