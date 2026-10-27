/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.config.insurance.reference;

import java.net.URI;
import java.util.Set;
import org.fireflyframework.security.adapter.opa.OpaPolicyDecisionAdapter;
import org.fireflyframework.security.rs.ResourceServerAutoConfiguration;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration(before = ResourceServerAutoConfiguration.class)
@ConditionalOnProperty(prefix = "firefly.masters.insurance-reference.commands", name = "enabled", havingValue = "true")
@Import(InsuranceReferencePolicyAutoConfiguration.DefaultPolicyBackend.class)
public class InsuranceReferencePolicyAutoConfiguration {
    @ConditionalOnMissingBean(PolicyDecisionPort.class)
    static class DefaultPolicyBackend {
        @Bean
        PolicyDecisionPort insuranceReferencePolicyDecisionPort(Environment environment) {
            if (!"opa".equals(environment.getProperty("firefly.masters.insurance-reference.policy.backend")))
                throw new IllegalStateException("INSURANCE_REFERENCE_CURRENT_POLICY_REQUIRED");
            var properties = Binder.get(environment)
                    .bind("firefly.masters.insurance-reference.policy", PolicyProperties.class)
                    .orElseThrow(() -> new IllegalStateException("INSURANCE_REFERENCE_CURRENT_POLICY_REQUIRED"));
            return new OpaPolicyDecisionAdapter(WebClient.builder().baseUrl(properties.baseUri().toString()).build(), properties.decisionPath());
        }
    }

    record PolicyProperties(URI baseUri, String decisionPath) {
        PolicyProperties {
            if (baseUri == null || baseUri.toString().length() > 2048 || baseUri.getHost() == null
                    || !("https".equals(baseUri.getScheme()) || "http".equals(baseUri.getScheme())
                            && Set.of("127.0.0.1", "::1", "[::1]").contains(baseUri.getHost()))
                    || baseUri.getUserInfo() != null || baseUri.getRawQuery() != null || baseUri.getRawFragment() != null
                    || !(baseUri.getPath().isEmpty() || baseUri.getPath().equals("/"))
                    || decisionPath == null || decisionPath.length() > 512
                    || !decisionPath.matches("[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*"))
                throw new IllegalArgumentException("INSURANCE_REFERENCE_CURRENT_POLICY_REQUIRED");
        }
    }
}
