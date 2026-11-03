/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.controllers.insurance.reference;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyReference;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCommands.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.*;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

final class InsuranceReferenceRequestDecoder {
    static final int MAX_BYTES = 512 * 1024;
    private static final ObjectMapper JSON = JsonMapper.builder(
                    JsonFactory.builder()
                            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                            .streamReadConstraints(StreamReadConstraints.builder()
                                    .maxNestingDepth(32)
                                    .maxStringLength(65536)
                                    .maxNumberLength(20)
                                    .build())
                            .build())
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private InsuranceReferenceRequestDecoder() {}

    static SubmitPublication publication(byte[] raw) {
        return decode(raw, root -> {
            closed(root, Set.of("actionId", "draft"), Set.of());
            var draft = root.get("draft");
            closed(draft, Set.of("recordId", "kind", "code", "effectiveFrom", "definition", "provenance", "expected"), Set.of("effectiveUntil"));
            var kind = enumeration(draft.get("kind"), Kind.class);
            return new SubmitPublication(id(root.get("actionId")), new PublicationDraft(
                    id(draft.get("recordId")), kind, text(draft.get("code")),
                    timestamp(draft.get("effectiveFrom")), nullableTimestamp(draft.get("effectiveUntil")),
                    body(draft.get("definition"), kind), provenance(draft.get("provenance")), head(draft.get("expected"))));
        });
    }

    static SubmitWithdrawal withdrawal(byte[] raw) {
        return decode(raw, root -> {
            closed(root, Set.of("actionId", "recordId", "kind", "expected"), Set.of());
            return new SubmitWithdrawal(id(root.get("actionId")), id(root.get("recordId")),
                    enumeration(root.get("kind"), Kind.class), head(root.get("expected")));
        });
    }

    static IntentAction intentAction(byte[] raw) {
        return decode(raw, root -> {
            closed(root, Set.of("actionId", "recordId", "kind", "intentId", "intentDigest", "expectedIntentGeneration"), Set.of());
            return new IntentAction(id(root.get("actionId")), id(root.get("recordId")),
                    enumeration(root.get("kind"), Kind.class), id(root.get("intentId")),
                    text(root.get("intentDigest")), integer(root.get("expectedIntentGeneration")));
        });
    }

    static Recovery recovery(byte[] raw) {
        return decode(raw, root -> {
            closed(root, Set.of("actionId", "recordId", "kind", "intentDigest"), Set.of());
            return new Recovery(id(root.get("actionId")), id(root.get("recordId")),
                    enumeration(root.get("kind"), Kind.class), text(root.get("intentDigest")));
        });
    }

    private static Body body(JsonNode node, Kind kind) {
        if (kind == Kind.PREMIUM_TAX) {
            closed(node, Set.of("basis", "ratePartsPerMillion", "roundingMode", "postedScale", "currencyPolicy", "applicability"), Set.of());
            return new PremiumTax(enumeration(node.get("basis"), TaxBasis.class), smallInteger(node.get("ratePartsPerMillion")),
                    enumeration(node.get("roundingMode"), Rounding.class), smallInteger(node.get("postedScale")),
                    currency(node.get("currencyPolicy")), applicability(node.get("applicability")));
        }
        closed(node, Set.of("localeId", "localeCode", "mediaType", "content", "applicability"), Set.of());
        return new Wording(id(node.get("localeId")), text(node.get("localeCode")), text(node.get("mediaType")),
                text(node.get("content")), applicability(node.get("applicability")));
    }

    private static CurrencyPolicyReference currency(JsonNode node) {
        closed(node, Set.of("policyId", "currencyId", "isoCode", "version", "digest", "encodingVersion"), Set.of());
        return new CurrencyPolicyReference(id(node.get("policyId")).toString(), id(node.get("currencyId")).toString(),
                text(node.get("isoCode")), integer(node.get("version")), text(node.get("digest")), text(node.get("encodingVersion")));
    }

    private static Applicability applicability(JsonNode node) {
        closed(node, Set.of("jurisdictions", "productProfiles", "events"), Set.of());
        return new Applicability(array(node.get("jurisdictions"), InsuranceReferenceRequestDecoder::text),
                array(node.get("productProfiles"), InsuranceReferenceRequestDecoder::text),
                array(node.get("events"), value -> enumeration(value, Event.class)));
    }

    private static Provenance provenance(JsonNode node) {
        closed(node, Set.of("origin", "sourceReference", "versionText", "sourceDigest", "rightsReference"), Set.of());
        return new Provenance(enumeration(node.get("origin"), Origin.class), text(node.get("sourceReference")),
                text(node.get("versionText")), text(node.get("sourceDigest")), text(node.get("rightsReference")));
    }

    private static Head head(JsonNode node) {
        closed(node, Set.of("revision", "generation"), Set.of("publicationId"));
        var publication = node.get("publicationId");
        return new Head(publication == null || publication.isNull() ? null : id(publication),
                integer(node.get("revision")), integer(node.get("generation")));
    }

    private static <T> T decode(byte[] raw, Function<JsonNode, T> decoder) {
        try {
            if (raw == null || raw.length > MAX_BYTES) throw invalid();
            var utf8 = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.apply(JSON.readTree(utf8.decode(ByteBuffer.wrap(raw)).toString()));
        } catch (Exception ignored) {
            throw invalid();
        }
    }

    private static void closed(JsonNode node, Set<String> required, Set<String> optional) {
        if (node == null || !node.isObject()) throw invalid();
        required.forEach(key -> { if (!node.has(key)) throw invalid(); });
        node.fieldNames().forEachRemaining(key -> { if (!required.contains(key) && !optional.contains(key)) throw invalid(); });
    }

    private static String text(JsonNode node) {
        if (node == null || !node.isTextual()) throw invalid();
        return node.textValue();
    }

    private static UUID id(JsonNode node) {
        var value = text(node);
        var result = UUID.fromString(value);
        if (!result.toString().equals(value) || result.equals(new UUID(0, 0))) throw invalid();
        return result;
    }

    private static long integer(JsonNode node) {
        if (node == null || !node.isIntegralNumber() || !node.canConvertToLong()) throw invalid();
        return node.longValue();
    }

    private static int smallInteger(JsonNode node) {
        return Math.toIntExact(integer(node));
    }

    private static Instant timestamp(JsonNode node) {
        return Instant.parse(text(node));
    }

    private static Instant nullableTimestamp(JsonNode node) {
        return node == null || node.isNull() ? null : timestamp(node);
    }

    private static <T extends Enum<T>> T enumeration(JsonNode node, Class<T> type) {
        return Enum.valueOf(type, text(node));
    }

    private static <T> List<T> array(JsonNode node, Function<JsonNode, T> decoder) {
        if (node == null || !node.isArray() || node.size() > 64) throw invalid();
        var result = new ArrayList<T>();
        node.forEach(value -> result.add(decoder.apply(value)));
        return result;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("INSURANCE_REFERENCE_REQUEST_INVALID");
    }
}
