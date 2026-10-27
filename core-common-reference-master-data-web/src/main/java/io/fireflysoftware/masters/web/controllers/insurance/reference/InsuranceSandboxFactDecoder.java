package io.fireflysoftware.masters.web.controllers.insurance.reference;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;

/** Closed source-administration wire contract; it accepts no principal, grant or authorization data. */
final class InsuranceSandboxFactDecoder {
    static final int MAX_BYTES=512*1024;
    private static final ObjectMapper JSON=JsonMapper.builder(JsonFactory.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
        .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(32).maxStringLength(131072).maxNumberLength(20).build()).build())
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private InsuranceSandboxFactDecoder(){}
    static RegisterLocale locale(byte[] raw){return decode(raw,node->{
        closed(node,Set.of("actionId","factId","expectedVersion","expectedGeneration","localeId","localeCode","effectiveUntil"),Set.of());
        return new RegisterLocale(id(node.get("actionId")),id(node.get("factId")),number(node.get("expectedVersion")),number(node.get("expectedGeneration")),id(node.get("localeId")),text(node.get("localeCode")),time(node.get("effectiveUntil")));
    });}
    static RegisterRights rights(byte[] raw){return decode(raw,node->{
        closed(node,Set.of("actionId","factId","expectedVersion","expectedGeneration","sourceId","insuranceKind","sourceText","effectiveUntil"),Set.of("locale"));
        return new RegisterRights(id(node.get("actionId")),id(node.get("factId")),number(node.get("expectedVersion")),number(node.get("expectedGeneration")),id(node.get("sourceId")),Kind.valueOf(text(node.get("insuranceKind"))),text(node.get("sourceText")),nullableRef(node.get("locale")),time(node.get("effectiveUntil")));
    });}
    static RegisterSource source(byte[] raw){return decode(raw,node->{
        closed(node,Set.of("actionId","factId","expectedVersion","expectedGeneration","submitted","draft","sourceText","rights","effectiveUntil"),Set.of("locale"));
        var submitted=node.get("submitted");closed(submitted,Set.of("actionId","intentDigest","publicationId","revision"),Set.of());
        var receipt=new SubmittedPublication(id(submitted.get("actionId")),text(submitted.get("intentDigest")),id(submitted.get("publicationId")),number(submitted.get("revision")));
        var publication=JSON.createObjectNode();publication.put("actionId",receipt.actionId().toString());publication.set("draft",node.get("draft"));
        final byte[] original;try{original=JSON.writeValueAsBytes(publication);}catch(Exception invalid){throw invalid();}
        var draft=InsuranceReferenceRequestDecoder.publication(original).draft();
        return new RegisterSource(id(node.get("actionId")),id(node.get("factId")),number(node.get("expectedVersion")),number(node.get("expectedGeneration")),receipt,draft,text(node.get("sourceText")),ref(node.get("rights")),nullableRef(node.get("locale")),time(node.get("effectiveUntil")));
    });}
    static Withdraw withdraw(byte[] raw){return decode(raw,node->{
        closed(node,Set.of("actionId","type","factId","expectedVersion","expectedGeneration","reasonCode"),Set.of());
        return new Withdraw(id(node.get("actionId")),Type.valueOf(text(node.get("type"))),id(node.get("factId")),number(node.get("expectedVersion")),number(node.get("expectedGeneration")),text(node.get("reasonCode")));
    });}
    private static Reference nullableRef(JsonNode node){return node==null||node.isNull()?null:ref(node);}
    private static Reference ref(JsonNode node){closed(node,Set.of("type","factId","version","generation","digest"),Set.of());return new Reference(Type.valueOf(text(node.get("type"))),id(node.get("factId")),number(node.get("version")),number(node.get("generation")),text(node.get("digest")));}
    private static <T>T decode(byte[] raw,Function<JsonNode,T> parse){try{
        if(raw==null||raw.length>MAX_BYTES)throw invalid();
        var utf8=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        return parse.apply(JSON.readTree(utf8.decode(ByteBuffer.wrap(raw)).toString()));
    }catch(Exception failure){throw invalid();}}
    private static void closed(JsonNode node,Set<String> required,Set<String> optional){if(node==null||!node.isObject())throw invalid();required.forEach(key->{if(!node.has(key))throw invalid();});node.fieldNames().forEachRemaining(key->{if(!required.contains(key)&&!optional.contains(key))throw invalid();});}
    private static String text(JsonNode node){if(node==null||!node.isTextual())throw invalid();return node.textValue();}
    private static UUID id(JsonNode node){var text=text(node);UUID value=UUID.fromString(text);if(!value.toString().equals(text)||value.equals(new UUID(0,0)))throw invalid();return value;}
    private static long number(JsonNode node){if(node==null||!node.isIntegralNumber()||!node.canConvertToLong())throw invalid();return node.longValue();}
    private static Instant time(JsonNode node){return Instant.parse(text(node));}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("INSURANCE_SANDBOX_FACT_REQUEST_INVALID");}
}
