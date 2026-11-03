/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.interfaces.protocol.insurance.reference;

import static io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.*;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyReference;
import java.nio.*;
import java.nio.charset.*;
import java.security.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/** Strict new reference encoding; no permission or current-owner evidence is inferred. */
public final class InsuranceReferenceCodec {
    private static final int MAX_BYTES=131072;
    private static final DateTimeFormatter TIME=new DateTimeFormatterBuilder().appendInstant(6).toFormatter();
    private static final ObjectMapper JSON=JsonMapper.builder(JsonFactory.builder().streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(24).maxStringLength(65536).maxNumberLength(20).build()).build())
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION).enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private InsuranceReferenceCodec(){}
    public static Revision decode(String raw){
        try{
            require(raw!=null&&raw.length()<=MAX_BYTES&&raw.getBytes(StandardCharsets.UTF_8).length<=MAX_BYTES);var n=JSON.readTree(raw);
            closed(n,"encodingVersion","scope","kind","recordId","publicationId","revision","code","effectiveFrom","effectiveUntil","definition","provenance");
            require(ENCODING.equals(text(n,"encodingVersion"))&&SCOPE.equals(text(n,"scope")));var kind=Kind.valueOf(text(n,"kind"));var body=n.get("definition");Body definition;
            if(kind==Kind.PREMIUM_TAX){closed(body,"basis","ratePartsPerMillion","roundingMode","postedScale","currencyPolicy","applicability");var c=body.get("currencyPolicy");closed(c,"policyId","currencyId","isoCode","version","digest","encodingVersion");definition=new PremiumTax(TaxBasis.valueOf(text(body,"basis")),integer(body,"ratePartsPerMillion"),Rounding.valueOf(text(body,"roundingMode")),integer(body,"postedScale"),new CurrencyPolicyReference(text(c,"policyId"),text(c,"currencyId"),text(c,"isoCode"),number(c,"version"),text(c,"digest"),text(c,"encodingVersion")),applicability(body.get("applicability")));}
            else{closed(body,"localeId","localeCode","mediaType","content","applicability");definition=new Wording(id(body,"localeId"),text(body,"localeCode"),text(body,"mediaType"),text(body,"content"),applicability(body.get("applicability")));}
            var p=n.get("provenance");closed(p,"origin","sourceReference","versionText","sourceDigest","rightsReference");var provenance=new Provenance(Origin.valueOf(text(p,"origin")),text(p,"sourceReference"),text(p,"versionText"),text(p,"sourceDigest"),text(p,"rightsReference"));
            return new Revision(kind,id(n,"recordId"),id(n,"publicationId"),number(n,"revision"),text(n,"code"),time(text(n,"effectiveFrom")),n.get("effectiveUntil").isNull()?null:time(text(n,"effectiveUntil")),definition,provenance);
        }catch(Exception error){throw invalid();}
    }
    public static String encode(Revision value){
        try{Objects.requireNonNull(value);var root=JSON.createObjectNode();root.put("encodingVersion",ENCODING);root.put("scope",SCOPE);root.put("kind",value.kind().name());root.put("recordId",value.recordId().toString());root.put("publicationId",value.publicationId().toString());root.put("revision",value.revision());root.put("code",value.code());root.put("effectiveFrom",TIME.format(value.effectiveFrom()));if(value.effectiveUntil()==null)root.putNull("effectiveUntil");else root.put("effectiveUntil",TIME.format(value.effectiveUntil()));root.set("provenance",JSON.valueToTree(value.provenance()));root.set("definition",JSON.valueToTree(value.definition()));String result=InsuranceReferenceCanonicalJson.canonicalize(JSON.writeValueAsString(root));require(result.getBytes(StandardCharsets.UTF_8).length<=MAX_BYTES);return result;}
        catch(Exception error){throw invalid();}
    }
    public static byte[] bytes(Revision value){return encode(value).getBytes(StandardCharsets.UTF_8);}
    public static String digest(Revision value){return hash("masters-insurance-reference-v1",encode(value));}
    public static String contentDigest(Wording value){Objects.requireNonNull(value);return hash("masters-insurance-wording-content-v1",value.content());}
    public static Revision decodeBytes(byte[] value){
        try{require(value!=null&&value.length<=MAX_BYTES);String raw=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(value)).toString();var definition=decode(raw);require(Arrays.equals(value,bytes(definition)));return definition;}catch(Exception error){throw invalid();}
    }
    private static Applicability applicability(JsonNode n){closed(n,"jurisdictions","productProfiles","events");return new Applicability(strings(n.get("jurisdictions")),strings(n.get("productProfiles")),strings(n.get("events")).stream().map(Event::valueOf).toList());}
    private static List<String> strings(JsonNode n){require(n!=null&&n.isArray()&&n.size()<=64);var values=new ArrayList<String>();for(var value:n){require(value.isTextual());values.add(value.textValue());}return values;}
    private static String text(JsonNode n,String key){var v=n.get(key);require(v!=null&&v.isTextual());return v.textValue();}
    private static long number(JsonNode n,String key){var v=n.get(key);require(v!=null&&v.isIntegralNumber()&&v.canConvertToLong());return v.longValue();}
    private static int integer(JsonNode n,String key){long value=number(n,key);require(value>=Integer.MIN_VALUE&&value<=Integer.MAX_VALUE);return (int)value;}
    private static UUID id(JsonNode n,String key){String raw=text(n,key);var value=UUID.fromString(raw);require(value.toString().equals(raw));return value;}
    private static Instant time(String raw){var value=Instant.parse(raw);require(TIME.format(value).equals(raw));return value;}
    private static void closed(JsonNode n,String... keys){require(n!=null&&n.isObject());var fields=new HashSet<String>();n.fieldNames().forEachRemaining(fields::add);require(fields.equals(Set.of(keys)));}
    private static String hash(String namespace,String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((namespace+"\n"+value).getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException error){throw new IllegalStateException("SHA-256 unavailable",error);}}
    private static void require(boolean ok){if(!ok)throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INSURANCE_REFERENCE_SHAPE");}
}
