/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.config.security;

import io.fireflysoftware.masters.web.config.insurance.reference.InsuranceReferenceCurrentPolicyAutoConfiguration;
import io.fireflysoftware.masters.web.config.insurance.reference.InsuranceReferencePolicyAutoConfiguration;
import io.fireflysoftware.masters.web.config.insurance.reference.InsuranceSandboxSourcePolicyAutoConfiguration;
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

/** Supplied policy beans take precedence; otherwise one neutral OPA adapter serves every capability. */
@AutoConfiguration(before = {ResourceServerAutoConfiguration.class,
        InsuranceReferencePolicyAutoConfiguration.class, InsuranceReferenceCurrentPolicyAutoConfiguration.class,
        InsuranceSandboxSourcePolicyAutoConfiguration.class})
@ConditionalOnProperty(prefix = "firefly.masters.policy", name = "enabled", havingValue = "true")
@Import(MasterDataPolicyAutoConfiguration.DefaultPolicyBackend.class)
public class MasterDataPolicyAutoConfiguration {
    @ConditionalOnMissingBean(PolicyDecisionPort.class)
    static class DefaultPolicyBackend {
        @Bean
        PolicyDecisionPort masterDataPolicyDecisionPort(Environment environment) {
            var binder = Binder.get(environment);
            var settings = binder.bind("firefly.masters.policy", PolicySettings.class)
                    .orElseThrow(() -> new IllegalStateException("MASTERS_POLICY_CONFIGURATION_REQUIRED"));
            binder.bind("firefly.masters.insurance-reference.policy", LegacyPolicySettings.class)
                    .ifBound(legacy -> {
                        if (legacy.backend() != null && !legacy.backend().equals(settings.backend())
                                || legacy.baseUri() != null && !legacy.baseUri().equals(settings.baseUri())
                                || legacy.decisionPath() != null && !legacy.decisionPath().equals(settings.decisionPath()))
                            throw new IllegalStateException("MASTERS_POLICY_CONFIGURATION_CONFLICT");
                    });
            return new OpaPolicyDecisionAdapter(
                    WebClient.builder().baseUrl(settings.baseUri().toString()).build(), settings.decisionPath());
        }
    }

    record LegacyPolicySettings(String backend, URI baseUri, String decisionPath) {}

    record PolicySettings(String backend, URI baseUri, String decisionPath) {
        PolicySettings {
            if (!"opa".equals(backend) || baseUri == null || baseUri.toString().length() > 2048
                    || baseUri.getHost() == null
                    || !("https".equals(baseUri.getScheme()) || "http".equals(baseUri.getScheme())
                            && Set.of("127.0.0.1", "::1", "[::1]").contains(baseUri.getHost()))
                    || baseUri.getPort() == 0 || baseUri.getPort() > 65535
                    || baseUri.getUserInfo() != null || baseUri.getRawQuery() != null || baseUri.getRawFragment() != null
                    || !(baseUri.getPath().isEmpty() || baseUri.getPath().equals("/"))
                    || decisionPath == null || decisionPath.length() > 512
                    || !decisionPath.matches("[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*"))
                throw new IllegalArgumentException("MASTERS_POLICY_CONFIGURATION_REQUIRED");
        }
    }
}
