/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.openapi.capture;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;

class OriginalOpenApiCaptureConfigurationTest {
    @TempDir Path directory;

    private AnnotationConfigApplicationContext context(boolean profile, Map<String,Object> properties) {
        return context(profile, properties, new StandardEnvironment());
    }

    private AnnotationConfigApplicationContext context(boolean profile, Map<String,Object> properties,
            ConfigurableEnvironment environment) {
        var context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        if (profile) context.getEnvironment().setActiveProfiles("openapi-gen");
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("capture-test", properties));
        context.register(OriginalOpenApiCaptureConfiguration.class);
        return context;
    }

    @Test
    void normalOriginalDiscoveryRequiresNoCapturePropertiesAndCreatesNoWriter() {
        try (var context = context(true, Map.of())) {
            context.refresh();
            assertTrue(context.getBeansOfType(OriginalOpenApiCaptureFilter.class).isEmpty());
            assertFalse(Files.exists(directory.resolve("receipt.json")));
        }
    }

    @Test
    void explicitCaptureCreatesTheFilterOnlyInTheOriginalDiscoveryProfile() {
        Map<String,Object> properties = Map.of(
                "firefly.openapi.capture.enabled", "true",
                "firefly.openapi.capture.directory", directory.toString(),
                "firefly.openapi.capture.source-sha", "a4e25d3e46f1880ab7909bb78ee57b24c1f91200",
                "firefly.openapi.capture.invocation", "isolated-original-producer-build-0001");
        try (var context = context(true, properties)) {
            context.refresh();
            assertEquals(1, context.getBeansOfType(OriginalOpenApiCaptureFilter.class).size());
            assertFalse(Files.exists(directory.resolve("receipt.json")));
        }
        try (var context = context(false, properties)) {
            context.refresh();
            assertTrue(context.getBeansOfType(OriginalOpenApiCaptureFilter.class).isEmpty());
        }
    }

    @Test
    void partialCaptureConfigurationCannotStartAWriter() {
        try (var context = context(true, Map.of("firefly.openapi.capture.enabled", "true"))) {
            assertThrows(RuntimeException.class, context::refresh);
            assertFalse(Files.exists(directory.resolve("receipt.json")));
        }
    }

    @Test
    void ambientFullCapturePropertiesCannotCompleteAPartialTestConfiguration() {
        Map<String,Object> ambient = Map.of(
                "firefly.openapi.capture.enabled", "true",
                "firefly.openapi.capture.directory", directory.toString(),
                "firefly.openapi.capture.source-sha", "a4e25d3e46f1880ab7909bb78ee57b24c1f91200",
                "firefly.openapi.capture.invocation", "isolated-ambient-capture-context-0001");
        var originalProperties = new java.util.HashMap<String, String>();
        ambient.keySet().forEach(name -> originalProperties.put(name, System.getProperty(name)));
        for (var name : java.util.List.of(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME,
                StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME)) {
            var environment = new StandardEnvironment();
            environment.getPropertySources().replace(name, new MapPropertySource(name, ambient));
            try (var context = context(true, Map.of("firefly.openapi.capture.enabled", "true"), environment)) {
                assertThrows(RuntimeException.class, context::refresh, name);
                assertFalse(Files.exists(directory.resolve("receipt.json")));
            }
        }
        originalProperties.forEach((name, value) -> assertEquals(value, System.getProperty(name), name));
    }
}
