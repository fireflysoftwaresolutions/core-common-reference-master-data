package io.fireflysoftware.masters.core.services.document.v1;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.DecimalNode;

import java.util.TreeMap;

/** MASTERS_JSON_V1 is a masters digest contract, not RFC 8785 canonicalization. */
public final class DocumentTemplateCanonicalJson {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN).build();

    private DocumentTemplateCanonicalJson() { }

    public static String canonicalize(String json) {
        try { return MAPPER.writeValueAsString(sorted(MAPPER.readTree(json))); }
        catch (JsonProcessingException error) { throw new IllegalArgumentException("Document template JSON is invalid", error); }
    }

    private static JsonNode sorted(JsonNode node) {
        if (node.isObject()) {
            var values = new TreeMap<String, JsonNode>();
            node.fields().forEachRemaining(entry -> values.put(entry.getKey(), sorted(entry.getValue())));
            var result = MAPPER.createObjectNode();
            values.forEach(result::set);
            return result;
        }
        if (node.isArray()) {
            var result = MAPPER.createArrayNode();
            node.forEach(value -> result.add(sorted(value)));
            return result;
        }
        if (node.isNumber()) {
            var value = node.decimalValue().stripTrailingZeros();
            long integerDigits = Math.max(1L, (long) value.precision() - value.scale());
            long fractionDigits = Math.max(0L, value.scale());
            long plainLength = integerDigits + (fractionDigits == 0 ? 0 : fractionDigits + 1) + (value.signum() < 0 ? 1 : 0);
            if (plainLength > 128) throw new IllegalArgumentException("Document template canonical numbers must fit 128 characters");
            if (node.isFloatingPointNumber()) return DecimalNode.valueOf(value);
        }
        return node;
    }
}
