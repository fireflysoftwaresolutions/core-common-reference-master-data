package io.fireflysoftware.masters.core.services.insurance.reference;

import static io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.*;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class InsuranceReferencePublicCodecTest {
    private static JsonNode vectors()throws Exception {
        try(var resource=InsuranceReferencePublicCodecTest.class.getResourceAsStream("/insurance-reference-codec-v1-vectors.json")){
            assertNotNull(resource);return new ObjectMapper().readTree(resource).get("cases");
        }
    }
    @Test void allThreeKindsKeepQualifiedBytesDigestAndCompatibilityFacade()throws Exception {
        for(Kind kind:Kind.values()){
            var fixed=vectors().get(kind.name());String canonical=fixed.get("canonical").textValue();byte[] raw=canonical.getBytes(StandardCharsets.UTF_8);
            var value=io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.decode(canonical);
            assertEquals(kind,value.kind());assertEquals(1,value.revision());assertEquals("SANDBOX_REFERENCE",value.code());
            assertEquals(fixed.get("utf8Bytes").intValue(),raw.length);
            assertArrayEquals(raw,io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.bytes(value));
            assertEquals(fixed.get("digest").textValue(),io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.digest(value));
            assertEquals(value,io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.decodeBytes(raw));
            assertEquals(canonical,InsuranceReferenceCodec.encode(value));assertArrayEquals(raw,InsuranceReferenceCodec.bytes(value));
            assertEquals(value,InsuranceReferenceCodec.decode(canonical));assertEquals(value,InsuranceReferenceCodec.decodeBytes(raw));
            assertEquals(fixed.get("digest").textValue(),InsuranceReferenceCodec.digest(value));
            if(value.definition()instanceof Wording wording){
                assertEquals(fixed.get("contentDigest").textValue(),io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.contentDigest(wording));
                assertEquals(fixed.get("contentDigest").textValue(),InsuranceReferenceCodec.contentDigest(wording));
            }else assertInstanceOf(PremiumTax.class,value.definition());
        }
    }
    @Test void canonicalBytesStayStrictAndStringDecodeKeepsLegacyWhitespaceTolerance()throws Exception {
        String canonical=vectors().get("COVERAGE_WORDING").get("canonical").textValue();
        var value=io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.decode(" \n"+canonical);
        assertEquals(Kind.COVERAGE_WORDING,value.kind());
        for(byte[] invalid:new byte[][]{(" \n"+canonical).getBytes(StandardCharsets.UTF_8),new byte[]{(byte)0xc3,0x28},canonical.replace("\"revision\":1","\"revision\":1.0").getBytes(StandardCharsets.UTF_8)}){
            assertThrows(IllegalArgumentException.class,()->io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.decodeBytes(invalid));
            assertThrows(IllegalArgumentException.class,()->InsuranceReferenceCodec.decodeBytes(invalid));
        }
    }
    @Test void unknownFieldsDuplicateFieldsAndDifferentImmutableFactsNeverRetainDigest()throws Exception {
        String canonical=vectors().get("EXCLUSION_WORDING").get("canonical").textValue();
        for(String invalid:new String[]{canonical.substring(0,canonical.length()-1)+",\"unknown\":1}",canonical.replace("\"revision\":1","\"revision\":1,\"revision\":1")}){
            assertThrows(IllegalArgumentException.class,()->io.fireflysoftware.masters.interfaces.protocol.insurance.reference.InsuranceReferenceCodec.decode(invalid));
            assertThrows(IllegalArgumentException.class,()->InsuranceReferenceCodec.decode(invalid));
        }
        var original=InsuranceReferenceCodec.decode(canonical);var changed=new Revision(original.kind(),original.recordId(),original.publicationId(),original.revision(),"CHANGED_CODE",original.effectiveFrom(),original.effectiveUntil(),original.definition(),original.provenance());
        assertNotEquals(InsuranceReferenceCodec.digest(original),InsuranceReferenceCodec.digest(changed));
    }
}
