/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.common.reference.master.data.sdk;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PackagedOpenApiCompatibilityTest {
    @Test
    void compatibilityResourceMirrorsTheProducerSpecificationExactly() throws Exception {
        try (var original = getClass().getResourceAsStream("/openapi.yml");
                var compatibility = getClass().getResourceAsStream("/api-spec/openapi.yml")) {
            assertNotNull(original);
            assertNotNull(compatibility);
            assertArrayEquals(original.readAllBytes(), compatibility.readAllBytes());
        }
    }
}
