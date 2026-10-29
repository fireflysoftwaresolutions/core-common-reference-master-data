/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.openapi.capture;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
@Profile("openapi-gen")
@ConditionalOnProperty(prefix = "firefly.openapi.capture", name = "enabled", havingValue = "true")
public class OriginalOpenApiCaptureConfiguration {
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    OriginalOpenApiCaptureFilter originalOpenApiCaptureFilter(
            @Value("${firefly.openapi.capture.directory}") String directory,
            @Value("${firefly.openapi.capture.source-sha}") String sourceSha,
            @Value("${firefly.openapi.capture.invocation}") String invocation) {
        return new OriginalOpenApiCaptureFilter(Path.of(directory), sourceSha, invocation);
    }
}
