package io.fireflysoftware.masters.core.services.insurance.reference;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.fireflysoftware.masters.core.services.document.v1.DocumentTemplateCanonicalJson;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCommands.Head;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Revision;

/** Immutable server-issued proposal data; it is not a published revision or an approval. */
public final class InsuranceReferenceIntentCodec {
    private static final ObjectMapper JSON=new ObjectMapper(JsonFactory.builder().streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(32).maxStringLength(131072).maxNumberLength(20).build()).build()).enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    private InsuranceReferenceIntentCodec() {}
    public record Intent(String operation,Revision definition,Head expected) {
        public Intent{if(!Set.of("PUBLISH","WITHDRAW").contains(operation)||definition==null||expected==null)throw invalid();}
        @Override public String toString(){return "InsuranceReferenceGovernedIntent";}
    }
    public static String encode(Intent value) {
        try {var n=JSON.createObjectNode();n.put("encodingVersion","MASTERS_INSURANCE_REFERENCE_INTENT_V1");n.put("operation",value.operation());n.set("definition",JSON.readTree(InsuranceReferenceCodec.encode(value.definition())));var e=n.putObject("expected");if(value.expected().publicationId()==null)e.putNull("publicationId");else e.put("publicationId",value.expected().publicationId().toString());e.put("revision",value.expected().revision());e.put("generation",value.expected().generation());return DocumentTemplateCanonicalJson.canonicalize(n.toString());}catch(Exception error){throw invalid();}
    }
    public static Intent decode(String canonical) {
        try {if(canonical==null||canonical.length()>150000||canonical.getBytes(StandardCharsets.UTF_8).length>150000)throw invalid();JsonNode n=JSON.readTree(canonical);fields(n,Set.of("encodingVersion","operation","definition","expected"));if(!"MASTERS_INSURANCE_REFERENCE_INTENT_V1".equals(n.get("encodingVersion").textValue()))throw invalid();JsonNode e=n.get("expected");fields(e,Set.of("publicationId","revision","generation"));UUID publication=e.get("publicationId").isNull()?null:UUID.fromString(e.get("publicationId").textValue());if(publication!=null&&!publication.toString().equals(e.get("publicationId").textValue())||!e.get("revision").isIntegralNumber()||!e.get("revision").canConvertToLong()||!e.get("generation").isIntegralNumber()||!e.get("generation").canConvertToLong())throw invalid();var value=new Intent(n.get("operation").textValue(),InsuranceReferenceCodec.decode(n.get("definition").toString()),new Head(publication,e.get("revision").longValue(),e.get("generation").longValue()));if(!encode(value).equals(canonical))throw invalid();return value;}catch(Exception error){throw invalid();}
    }
    public static String digest(String canonical) {try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(("masters-insurance-governed-intent-v1\n"+canonical).getBytes(StandardCharsets.UTF_8)));}catch(Exception error){throw invalid();}}
    private static void fields(JsonNode n,Set<String> wanted){if(!(n instanceof ObjectNode))throw invalid();var names=new HashSet<String>();n.fieldNames().forEachRemaining(names::add);if(!names.equals(wanted))throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INSURANCE_REFERENCE_STORED_INTENT_INVALID");}
}
