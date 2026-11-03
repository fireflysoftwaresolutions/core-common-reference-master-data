package io.fireflysoftware.masters.core.services.document.v1;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTemplateCanonicalJsonTest {
    @Test void exactFinancialNumbersRetainDigitsAndNormalizeEquivalentRepresentations() {
        assertThat(DocumentTemplateCanonicalJson.canonicalize("{\"n\":9007199254740993,\"amount\":0.123456789012345678,\"z\":1.2300}"))
                .isEqualTo("{\"amount\":0.123456789012345678,\"n\":9007199254740993,\"z\":1.23}");
    }

    @Test void smallExponentTokensCannotExpandIntoUnboundedPlainNumbers() {
        for (String json : java.util.List.of("{\"n\":1e100000}", "{\"n\":1e-100000}", "{\"n\":1e128}")) {
            assertThatThrownBy(() -> DocumentTemplateCanonicalJson.canonicalize(json)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(DocumentTemplateCanonicalJson.canonicalize("{\"n\":1e127}"))
                .isEqualTo("{\"n\":1" + "0".repeat(127) + "}");
    }
}
