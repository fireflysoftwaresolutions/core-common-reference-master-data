/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.common.reference.master.data.sdk;

import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.fireflysoftware.common.reference.master.data.sdk.invoker.ApiClient;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Runs after the original Generator; it does not supply or modify generated models. */
class InsuranceReferenceSdkWireTest {
    @Test
    void genuineMapperAndModelsRoundTripAllThreeDraftKindsThroughTheActualOwnerDecoder() throws Exception {
        var mapper = ApiClient.createDefaultMapper(null);
        var requestClass = Class.forName("io.fireflysoftware.common.reference.master.data.sdk.model.SubmitPublication");
        assertEquals(ApiClient.class.getProtectionDomain().getCodeSource().getLocation(),
                requestClass.getProtectionDomain().getCodeSource().getLocation());
        var basedir = java.nio.file.Path.of(java.util.Objects.requireNonNull(
                System.getProperty("basedir"), "Maven SDK module basedir is required"));
        assertEquals("core-common-reference-master-data-sdk", basedir.getFileName().toString());
        var webClasses = basedir.resolve("../core-common-reference-master-data-web/target/classes").normalize();
        assertTrue(java.nio.file.Files.isRegularFile(webClasses.resolve(
                "io/fireflysoftware/masters/web/controllers/insurance/reference/InsuranceReferenceRequestDecoder.class")));
        try (var webLoader = new java.net.URLClassLoader(new java.net.URL[] { webClasses.toUri().toURL() },
                ApiClient.class.getClassLoader())) {
            var decoder = Class.forName("io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceReferenceRequestDecoder",
                    true, webLoader);
            assertEquals(webClasses.toUri().toURL(), decoder.getProtectionDomain().getCodeSource().getLocation());
            assertSame(ApiClient.class, webLoader.loadClass(ApiClient.class.getName()));
            var decode = decoder.getDeclaredMethod("publication", byte[].class);
            decode.setAccessible(true);
            for (var kind : java.util.List.of("PREMIUM_TAX", "COVERAGE_WORDING", "EXCLUSION_WORDING")) {
                var raw = draft(kind);
                var sdk = mapper.readValue(raw, requestClass);
                var actualDraft = requestClass.getMethod("getDraft").invoke(sdk);
                var definitionType = actualDraft.getClass().getMethod("getDefinition").getReturnType();
                assertTrue(definitionType.isInterface());
                assertEquals(JsonTypeInfo.Id.DEDUCTION, definitionType.getAnnotation(JsonTypeInfo.class).use());
                assertEquals(Instant.class, actualDraft.getClass().getMethod("getEffectiveFrom").getReturnType());
                byte[] wire = mapper.writeValueAsBytes(sdk);
                var time = mapper.readTree(wire).get("draft").get("effectiveFrom");
                assertTrue(time.isTextual());
                assertEquals(Instant.parse("2026-10-27T00:00:00.123456Z"), Instant.parse(time.textValue()));
                assertEquals(decode.invoke(null, (Object) raw.getBytes(StandardCharsets.UTF_8)),
                        decode.invoke(null, (Object) wire));
            }
        }
    }

    private static String draft(String kind) {
        String applicability = "{\"jurisdictions\":[\"US\"],\"productProfiles\":[\"TERM_PC_INSURANCE_V1\"],\"events\":[\"NEW_BUSINESS\"]}";
        String body = kind.equals("PREMIUM_TAX")
                ? "{\"basis\":\"NET_PREMIUM_EXCLUSIVE\",\"ratePartsPerMillion\":1250,\"roundingMode\":\"HALF_UP\",\"postedScale\":2,"
                        + "\"currencyPolicy\":{\"policyId\":\"10000000-0000-0000-0000-000000000003\",\"currencyId\":\"10000000-0000-0000-0000-000000000004\","
                        + "\"isoCode\":\"USD\",\"version\":1,\"digest\":\"" + "a".repeat(64) + "\",\"encodingVersion\":\"MASTERS_CURRENCY_POLICY_V1\"},\"applicability\":" + applicability + "}"
                : "{\"localeId\":\"10000000-0000-0000-0000-000000000005\",\"localeCode\":\"en-US\",\"mediaType\":\"text/plain\","
                        + "\"content\":\"Sandbox café 😀\\nOnly\",\"applicability\":" + applicability + "}";
        return "{\"actionId\":\"10000000-0000-0000-0000-000000000001\",\"draft\":{\"recordId\":\"10000000-0000-0000-0000-000000000002\","
                + "\"kind\":\"" + kind + "\",\"code\":\"SANDBOX_REFERENCE\",\"effectiveFrom\":\"2026-10-27T00:00:00.123456Z\",\"effectiveUntil\":null,"
                + "\"definition\":" + body + ",\"provenance\":{\"origin\":\"SANDBOX\",\"sourceReference\":\"sandbox-source\",\"versionText\":\"1\","
                + "\"sourceDigest\":\"" + "a".repeat(64) + "\",\"rightsReference\":\"sandbox-only\"},\"expected\":{\"publicationId\":null,\"revision\":0,\"generation\":0}}}";
    }
}
