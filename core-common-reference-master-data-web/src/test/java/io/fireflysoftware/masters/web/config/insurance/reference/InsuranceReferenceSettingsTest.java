package io.fireflysoftware.masters.web.config.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class InsuranceReferenceSettingsTest {
    @Test void standardBinderKeepsExplicitTwoCredentialGraphsAndHidesConfigurationValues() {
        var environment = environment(Map.of());
        var settings = Binder.get(environment).bind("firefly.masters.insurance-reference", InsuranceReferenceSettings.class).orElseThrow(IllegalStateException::new);
        assertEquals(java.util.Set.of("https://issuer.example.test"), settings.issuers());
        assertEquals(settings.writer().url(), settings.reader().url());
        assertNotEquals(settings.writer().username(), settings.reader().username());
        assertEquals("InsuranceReferenceSettings", settings.toString());
        assertEquals("InsuranceReferenceConnectionSettings", settings.writer().toString());
    }

    @Test void mismatchedDatabasesInlineCredentialsAndMissingTrustAreRefusedWithoutInputDetails() {
        for (var override : java.util.List.of(
                Map.of("firefly.masters.insurance-reference.reader.url", "r2dbc:postgresql://127.0.0.1:55499/other"),
                Map.of("firefly.masters.insurance-reference.writer.url", "r2dbc:postgresql://inline:secret@127.0.0.1:55499/catalog"),
                Map.of("firefly.masters.insurance-reference.reader.username", "metadata_writer"),
                Map.of("firefly.masters.insurance-reference.issuers[0]", ""))) {
            assertThrows(RuntimeException.class, () -> Binder.get(environment(override))
                    .bind("firefly.masters.insurance-reference", InsuranceReferenceSettings.class).orElseThrow(IllegalStateException::new));
        }
    }

    private static StandardEnvironment environment(Map<String, String> override) {
        var values = new java.util.HashMap<String, Object>();
        var prefix = "firefly.masters.insurance-reference.";
        values.put(prefix + "issuers[0]", "https://issuer.example.test");
        for (var name : java.util.List.of("writer", "reader")) {
            values.put(prefix + name + ".url", "r2dbc:postgresql://127.0.0.1:55499/catalog?sslMode=disable");
            values.put(prefix + name + ".username", "metadata_" + name);
            values.put(prefix + name + ".password", "source-context-only");
        }
        values.putAll(override);
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("source-control", values));
        return environment;
    }
}
