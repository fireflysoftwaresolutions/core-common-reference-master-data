package io.fireflysoftware.masters.core.services.insurance.reference;

import com.fasterxml.jackson.databind.node.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class InsuranceReferenceCodecTest {
    static final com.fasterxml.jackson.databind.ObjectMapper JSON=new com.fasterxml.jackson.databind.ObjectMapper();
    static Object call(String method,Class<?> arg,Object value){
        try{return Class.forName("io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceCodec").getMethod(method,arg).invoke(null,value);}
        catch(InvocationTargetException error){if(error.getCause() instanceof RuntimeException cause)throw cause;throw new AssertionError(error.getCause());}
        catch(ReflectiveOperationException error){throw new AssertionError("Typed governed insurance reference codec missing",error);}
    }
    static Object decode(String raw){return call("decode",String.class,raw);}
    static String encode(Object value){return (String)call("encode",value.getClass(),value);}
    static String digest(Object value){return (String)call("digest",value.getClass(),value);}
    static ObjectNode fixture(String kind){
        var n=JSON.createObjectNode();n.put("encodingVersion","MASTERS_INSURANCE_REFERENCE_V1");n.put("scope","GLOBAL_MASTER");n.put("kind",kind);n.put("recordId","11111111-2222-3333-4444-555555555555");n.put("publicationId","22222222-2222-3333-4444-555555555555");n.put("revision",1);n.put("code","SANDBOX_REFERENCE");n.put("effectiveFrom","2026-11-01T00:00:00.000000Z");n.put("effectiveUntil","2027-11-01T00:00:00.000000Z");
        var provenance=n.putObject("provenance");provenance.put("origin","SANDBOX");provenance.put("sourceReference","urn:firefly:sandbox:reference");provenance.put("versionText","draft-1");provenance.put("sourceDigest","a".repeat(64));provenance.put("rightsReference","sandbox-owned");
        var body=n.putObject("definition");
        if(kind.equals("PREMIUM_TAX")){
            body.put("basis","NET_PREMIUM_EXCLUSIVE");body.put("ratePartsPerMillion",12500);body.put("roundingMode","HALF_UP");body.put("postedScale",2);
            var currency=body.putObject("currencyPolicy");currency.put("policyId","33333333-2222-3333-4444-555555555555");currency.put("currencyId","44444444-2222-3333-4444-555555555555");currency.put("isoCode","USD");currency.put("version",1);currency.put("digest","b".repeat(64));currency.put("encodingVersion","MASTERS_CURRENCY_POLICY_V1");
        }else{body.put("localeId","55555555-2222-3333-4444-555555555555");body.put("localeCode","en-US");body.put("mediaType","text/plain");body.put("content","Synthetic wording only.\r\nSecond paragraph — no legal representation.");}
        var scope=body.putObject("applicability");scope.putArray("jurisdictions").add("ZZ");scope.putArray("productProfiles").add("TERM_PC_INSURANCE_V1");scope.putArray("events").add("NEW_BUSINESS");return n;
    }
    @ParameterizedTest @ValueSource(strings={"PREMIUM_TAX","COVERAGE_WORDING","EXCLUSION_WORDING"})
    void exactKindRoundTripsCanonicalBytesAndDigest(String kind){
        var value=decode(fixture(kind).toString());String canonical=encode(value);assertEquals(canonical,encode(decode(canonical)));byte[] bytes=(byte[])call("bytes",value.getClass(),value);assertArrayEquals(canonical.getBytes(StandardCharsets.UTF_8),bytes);assertEquals(canonical,encode(call("decodeBytes",byte[].class,bytes)));assertTrue(digest(value).matches("[0-9a-f]{64}"));
    }
    @ParameterizedTest @CsvSource({"PREMIUM_TAX,e6ea756fd9c7c0494c62c9048a1dfe4ce93b60b514b7ff29a65dd57a737128fa","COVERAGE_WORDING,58b2992b81413780f53990d9322408f51504c7e51bafc890d72420346b312ba9","EXCLUSION_WORDING,291888f9c421ddecd5cfddff8d390722a918f983390cb9712e109071467e7ec6"})
    void fullCanonicalDigestMatchesIndependentPythonVector(String kind,String expected){assertEquals(expected,digest(decode(fixture(kind).toString())));}
    @ParameterizedTest @ValueSource(ints={0,1000000})
    void closedRateProfileAcceptsItsExplicitBoundariesAsDataOnly(int ppm){var n=fixture("PREMIUM_TAX");((ObjectNode)n.get("definition")).put("ratePartsPerMillion",ppm);assertTrue(encode(decode(n.toString())).contains("\"ratePartsPerMillion\":"+ppm));}
    @Test void wordingDigestPreservesExactBytesRatherThanNormalizingContent(){
        var revision=(io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Revision)decode(fixture("COVERAGE_WORDING").toString());
        var wording=(io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Wording)revision.definition();assertEquals("371752aa59a40219ef3260b30e6fe7047963b7e9dcf599ec66a9aee1d8455dd4",InsuranceReferenceCodec.contentDigest(wording));
        var changed=new io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Wording(wording.localeId(),wording.localeCode(),wording.mediaType(),wording.content().replace("\r\n","\n"),wording.applicability());assertNotEquals(InsuranceReferenceCodec.contentDigest(wording),InsuranceReferenceCodec.contentDigest(changed));
    }
    @Test void definitionCollectionsAndReturnedBytesCannotMutateTheOriginal(){
        var revision=(io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Revision)decode(fixture("COVERAGE_WORDING").toString());
        var wording=(io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Wording)revision.definition();assertThrows(UnsupportedOperationException.class,()->wording.applicability().jurisdictions().clear());String canonical=encode(revision);byte[] bytes=(byte[])call("bytes",revision.getClass(),revision);bytes[0]=0;assertEquals(canonical,encode(revision));
    }
    @Test void existingDocumentCanonicalizationRemainsItsOriginalOwnerContract(){assertEquals("{\"a\":1,\"b\":2}",io.fireflysoftware.masters.core.services.document.v1.DocumentTemplateCanonicalJson.canonicalize("{\"b\":2,\"a\":1}"));}
    @ParameterizedTest @ValueSource(strings={"recordId","publicationId","revision","kind","effectiveFrom","provenance"})
    void immutablePublicationIdentitiesAndDataBindDigest(String field){var a=fixture("COVERAGE_WORDING");var b=a.deepCopy();switch(field){case "recordId","publicationId"->b.put(field,"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");case "revision"->b.put(field,2);case "kind"->b.put(field,"EXCLUSION_WORDING");case "effectiveFrom"->b.put(field,"2026-11-02T00:00:00.000000Z");default->((ObjectNode)b.get(field)).put("versionText","draft-2");}assertNotEquals(digest(decode(a.toString())),digest(decode(b.toString())));}
    @Test void exactWordingContentLocaleAndOpaqueSourceVersionArePreserved(){var body=fixture("COVERAGE_WORDING");String canonical=encode(decode(body.toString()));assertTrue(canonical.contains("\\r\\n"));assertTrue(canonical.contains("en-US"));((ObjectNode)body.get("definition")).put("localeCode","en_US");assertNotEquals(digest(decode(fixture("COVERAGE_WORDING").toString())),digest(decode(body.toString())));((ObjectNode)body.get("provenance")).put("versionText","1");assertTrue(encode(decode(body.toString())).contains("\"versionText\":\"1\""));}
    @Test void taxRateIsExactIntegerPpmWithoutAnyLegalOrApplicantApproval(){String canonical=encode(decode(fixture("PREMIUM_TAX").toString()));assertTrue(canonical.contains("\"ratePartsPerMillion\":12500"));assertTrue(canonical.contains("SANDBOX"));assertFalse(canonical.contains("approved"));assertFalse(canonical.contains("applicant"));}
    @ParameterizedTest @ValueSource(strings={"scope","tenantId","approved","actor"})
    void rejectsAuthorityAndTenantClaimsInDefinition(String field){var n=fixture("PREMIUM_TAX");n.put(field,"caller");assertThrows(IllegalArgumentException.class,()->decode(n.toString()));}
    @ParameterizedTest @ValueSource(strings={"PREMIUM_TAX_V2","GENERIC","ACCOUNT_TARIFF"})
    void rejectsUnsupportedKinds(String kind){var n=fixture("PREMIUM_TAX");n.put("kind",kind);assertThrows(IllegalArgumentException.class,()->decode(n.toString()));}
    @ParameterizedTest @ValueSource(longs={-1,0})
    void rejectsNonpositiveOwnerRevision(long revision){var n=fixture("PREMIUM_TAX");n.put("revision",revision);assertThrows(IllegalArgumentException.class,()->decode(n.toString()));}
    @ParameterizedTest @ValueSource(strings={"00000000-0000-0000-0000-000000000000","1-1-1-1-1","AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE"})
    void rejectsNoncanonicalOrNilPublicationId(String value){var n=fixture("PREMIUM_TAX");n.put("publicationId",value);assertThrows(IllegalArgumentException.class,()->decode(n.toString()));}
    @ParameterizedTest @ValueSource(ints={-1,1000001})
    void rejectsOutOfProfileRates(int value){var n=fixture("PREMIUM_TAX");((ObjectNode)n.get("definition")).put("ratePartsPerMillion",value);assertThrows(IllegalArgumentException.class,()->decode(n.toString()));}
    @Test void rejectsDecimalRateAndStringRevisionInsteadOfCoercing(){var n=fixture("PREMIUM_TAX");((ObjectNode)n.get("definition")).put("ratePartsPerMillion",1.25);assertThrows(IllegalArgumentException.class,()->decode(n.toString()));var b=fixture("PREMIUM_TAX");b.put("revision","1");assertThrows(IllegalArgumentException.class,()->decode(b.toString()));}
    @ParameterizedTest @ValueSource(strings={"RENEWAL","ENDORSEMENT","CANCELLATION_REFUND"})
    void unsupportedTaxEventsCannotBeDeclaredExecutableByTheFirstQuoteProfile(String event){var n=fixture("PREMIUM_TAX");((ObjectNode)n.get("definition").get("applicability")).putArray("events").add(event);assertThrows(IllegalArgumentException.class,()->decode(n.toString()));}
    @Test void rejectsUnknownTaxBasisOrCurrencyEncoding(){var n=fixture("PREMIUM_TAX");((ObjectNode)n.get("definition")).put("basis","TAX_ON_TAX");assertThrows(IllegalArgumentException.class,()->decode(n.toString()));var b=fixture("PREMIUM_TAX");((ObjectNode)b.get("definition").get("currencyPolicy")).put("encodingVersion","UNKNOWN");assertThrows(IllegalArgumentException.class,()->decode(b.toString()));}
    @Test void rejectsEmptyDuplicateApplicabilityAndUnboundedContent(){var n=fixture("PREMIUM_TAX");((ArrayNode)n.get("definition").get("applicability").get("events")).add("NEW_BUSINESS");assertThrows(IllegalArgumentException.class,()->decode(n.toString()));var b=fixture("COVERAGE_WORDING");((ObjectNode)b.get("definition")).put("content","x".repeat(65537));assertThrows(IllegalArgumentException.class,()->decode(b.toString()));}
    @ParameterizedTest @ValueSource(strings={"{} {}","{\"kind\":\"PREMIUM_TAX\",\"kind\":\"COVERAGE_WORDING\"}","[]"})
    void rejectsTrailingDuplicateAndWrongRoot(String value){assertThrows(IllegalArgumentException.class,()->decode(value));}
    @Test void rejectsMalformedUtf8AndNoncanonicalByteEncoding(){assertThrows(IllegalArgumentException.class,()->call("decodeBytes",byte[].class,new byte[]{(byte)0xc3,0x28}));var value=decode(fixture("PREMIUM_TAX").toString());assertThrows(IllegalArgumentException.class,()->call("decodeBytes",byte[].class,(encode(value)+" ").getBytes(StandardCharsets.UTF_8)));}
    @Test void rejectsNoncanonicalTimestampPrecisionAndInvalidInterval(){var n=fixture("PREMIUM_TAX");n.put("effectiveFrom","2026-11-01T00:00:00Z");assertThrows(IllegalArgumentException.class,()->decode(n.toString()));var b=fixture("PREMIUM_TAX");b.put("effectiveUntil","2026-11-01T00:00:00.000000Z");assertThrows(IllegalArgumentException.class,()->decode(b.toString()));}
}
