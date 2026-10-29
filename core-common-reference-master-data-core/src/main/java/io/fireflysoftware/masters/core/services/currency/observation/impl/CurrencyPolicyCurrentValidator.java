package io.fireflysoftware.masters.core.services.currency.observation.impl;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fireflysoftware.masters.core.services.currency.policy.CurrencyPolicyRules;
import io.fireflysoftware.masters.core.services.document.v1.DocumentTemplateCanonicalJson;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
/** The existing complete immutable definition/source validator, shared without a new encoding. */
final class CurrencyPolicyCurrentValidator {
    private static final Duration LIMIT=Duration.ofSeconds(5);
    private static final Set<String> SOURCE_FIELDS=Set.of("currencyId","isoCode","currencyName","symbol","decimalPrecision","isMajor","status","dateCreated","dateUpdated");
    private static final JsonMapper JSON=JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    static void checked(CurrencyPolicyReference expected, int scale,
            CurrencyPolicyView value, Instant now) {
        try {
            if (value == null || !expected.equals(value.reference())
                    || !Objects.equals(scale, value.postedScale())
                    || !"PUBLISHED".equals(value.lifecycle()) || !Long.valueOf(0).equals(value.stateVersion())
                    || !Boolean.TRUE.equals(value.effectiveNow()) || value.observedAtEpochMillis() == null
                    || value.observedAtEpochMillis() > now.toEpochMilli()
                    || value.observedAtEpochMillis() < now.minus(LIMIT).toEpochMilli()
                    || value.effectiveFromEpochMillis() == null
                    || value.effectiveFromEpochMillis() > value.observedAtEpochMillis()
                    || value.effectiveToEpochMillis() != null
                        && (value.effectiveToEpochMillis() <= now.toEpochMilli()
                            || value.effectiveToEpochMillis() <= value.observedAtEpochMillis())
                    || value.publishedAtEpochMillis() == null
                    || value.publishedAtEpochMillis() > value.observedAtEpochMillis()) throw unavailable();
            CurrencyPolicyRules.text(value.sourceEncoding(), 1048576);
            CurrencyPolicyRules.digest(value.sourceDigest());
            if (!value.sourceDigest().equals(digest(value.sourceEncoding()))) throw unavailable();
            String prefix = CurrencyPolicyRules.SOURCE_ENCODING + "\n";
            if (!value.sourceEncoding().startsWith(prefix)) throw unavailable();
            String raw = value.sourceEncoding().substring(prefix.length());
            JsonNode source = JSON.readTree(raw);
            if (!source.isObject() || source.size() != SOURCE_FIELDS.size()
                    || !DocumentTemplateCanonicalJson.canonicalize(raw).equals(raw)) throw unavailable();
            for (String field : SOURCE_FIELDS) if (!source.has(field)) throw unavailable();
            if (!source.path("currencyId").isTextual()
                    || !expected.currencyId().equals(source.path("currencyId").textValue())
                    || !source.path("isoCode").isTextual()
                    || !expected.isoCode().equals(source.path("isoCode").textValue())
                    || !source.path("status").isTextual()
                    || !"ACTIVE".equals(source.path("status").textValue())
                    || !source.path("decimalPrecision").isIntegralNumber()
                    || !source.path("decimalPrecision").canConvertToInt()
                    || source.path("decimalPrecision").intValue() != scale) throw unavailable();
            var request = new CurrencyPolicyPublishRequest(value.publicationActionId(),
                    expected.currencyId(), expected.isoCode(), value.sourceDigest(),
                    expected.version() - 1, scale, value.effectiveFromEpochMillis(),
                    value.effectiveToEpochMillis(), value.provenanceReference());
            var canonical = CurrencyPolicyRules.definition(request,
                    new CurrencyPolicyRules.Canonical(value.sourceEncoding(), value.sourceDigest()),
                    expected.version());
            if (!canonical.encoding().equals(value.canonicalEncoding())
                    || !canonical.digest().equals(expected.digest())) throw unavailable();
        } catch (Exception error) {
            throw unavailable();
        }
    }

    private static String digest(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static IllegalStateException unavailable() {
        return new IllegalStateException("CURRENCY_POLICY_CURRENT_SOURCE_UNAVAILABLE");
    }
}
