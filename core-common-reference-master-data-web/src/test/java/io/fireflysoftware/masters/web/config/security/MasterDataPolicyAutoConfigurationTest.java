/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.config.security;

import static org.junit.jupiter.api.Assertions.*;

import io.fireflysoftware.masters.web.config.insurance.reference.InsuranceReferenceCurrentPolicyAutoConfiguration;
import io.fireflysoftware.masters.web.config.insurance.reference.InsuranceReferencePolicyAutoConfiguration;
import io.fireflysoftware.masters.web.config.insurance.reference.InsuranceSandboxSourcePolicyAutoConfiguration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.fireflyframework.security.adapter.opa.OpaPolicyDecisionAdapter;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.reactive.function.client.WebClient;

class MasterDataPolicyAutoConfigurationTest {
    private static final String CONFIGURATION =
            "io.fireflysoftware.masters.web.config.security.MasterDataPolicyAutoConfiguration";
    private static final String PREFIX = "firefly.masters.policy.";
    private static final String LEGACY = "firefly.masters.insurance-reference.policy.";

    private Class<?> neutralConfiguration() {
        try { return Class.forName(CONFIGURATION); }
        catch (ClassNotFoundException missing) {
            throw new AssertionError("Independent Masters policy configuration is required", missing);
        }
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                neutralConfiguration(), InsuranceReferencePolicyAutoConfiguration.class,
                InsuranceReferenceCurrentPolicyAutoConfiguration.class,
                InsuranceSandboxSourcePolicyAutoConfiguration.class));
    }

    private Map<String, String> configured() {
        var properties = new LinkedHashMap<String, String>();
        properties.put(PREFIX + "enabled", "true");
        properties.put(PREFIX + "backend", "opa");
        properties.put(PREFIX + "base-uri", "http://127.0.0.1:8181");
        properties.put(PREFIX + "decision-path", "firefly/masters/allow");
        return properties;
    }

    private String[] values(Map<String, String> properties) {
        return properties.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .toArray(String[]::new);
    }

    private void genuineNeutralAdapter(org.springframework.context.ApplicationContext context) {
        assertEquals(1, context.getBeansOfType(PolicyDecisionPort.class).size());
        assertInstanceOf(OpaPolicyDecisionAdapter.class, context.getBean("masterDataPolicyDecisionPort"));
        assertFalse(context.containsBean("insuranceReferencePolicyDecisionPort"));
    }

    @Test void originalDiscoveryIncludesTheIndependentNeutralConfigurationOnce() {
        var candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader()).getCandidates();
        assertEquals(1, candidates.stream().filter(CONFIGURATION::equals).count());
    }

    @Test void defaultOffDoesNotSupplyAPolicyOrRequireBackendProperties() {
        runner().withPropertyValues(PREFIX + "backend=invalid").run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(PolicyDecisionPort.class).isEmpty());
        });
    }

    @ParameterizedTest @ValueSource(strings = {"https://policy.example.invalid", "https://policy.example.invalid/",
            "http://127.0.0.1:8181", "http://[::1]:8181"})
    void explicitNeutralPolicyWorksWithoutAnyInsuranceCapabilityOrSettings(String uri) {
        var properties = configured();
        properties.put(PREFIX + "base-uri", uri);
        runner().withPropertyValues(values(properties)).run(context -> {
            assertNull(context.getStartupFailure());
            genuineNeutralAdapter(context);
        });
    }

    @ParameterizedTest @ValueSource(strings = {"commands", "current", "sandbox-source"})
    void insuranceCapabilitiesShareTheNeutralAdapterWithoutLegacyPolicySettings(String capability) {
        var properties = configured();
        properties.put("firefly.masters.insurance-reference." + capability + ".enabled", "true");
        runner().withPropertyValues(values(properties)).run(context -> {
            assertNull(context.getStartupFailure());
            genuineNeutralAdapter(context);
        });
    }

    @Test void allInsuranceCapabilitiesCanShareTheNeutralAdapterTogether() {
        var properties = configured();
        for (var capability : List.of("commands", "current", "sandbox-source"))
            properties.put("firefly.masters.insurance-reference." + capability + ".enabled", "true");
        runner().withPropertyValues(values(properties)).run(context -> {
            assertNull(context.getStartupFailure());
            genuineNeutralAdapter(context);
        });
    }

    @Test void identicalExplicitLegacySettingsUseOneSharedAdapter() {
        var properties = configured();
        properties.put("firefly.masters.insurance-reference.commands.enabled", "true");
        for (var key : List.of("backend", "base-uri", "decision-path"))
            properties.put(LEGACY + key, properties.get(PREFIX + key));
        runner().withPropertyValues(values(properties)).run(context -> {
            assertNull(context.getStartupFailure());
            genuineNeutralAdapter(context);
        });
    }

    @ParameterizedTest @ValueSource(strings = {"backend", "base-uri", "decision-path"})
    void individualMatchingLegacySettingsDoNotRequireTheMissingLegacySettings(String setting) {
        var properties = configured();
        properties.put(LEGACY + setting, properties.get(PREFIX + setting));
        runner().withPropertyValues(values(properties)).run(context -> {
            assertNull(context.getStartupFailure());
            genuineNeutralAdapter(context);
        });
    }

    @ParameterizedTest @ValueSource(strings = {"backend=embedded", "base-uri=https://other.example.invalid", "decision-path=other/allow"})
    void explicitlyConflictingLegacySettingsRefuseStartup(String setting) {
        var properties = configured();
        var split = setting.split("=", 2);
        properties.put(LEGACY + split[0], split[1]);
        runner().withPropertyValues(values(properties)).run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(context.getStartupFailure().toString().contains("masterDataPolicyDecisionPort"));
        });
    }

    @Test void legacyOnlyConfigurationRetainsItsExistingAdapterAndSettings() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(
                InsuranceReferencePolicyAutoConfiguration.class))
                .withPropertyValues("firefly.masters.insurance-reference.commands.enabled=true",
                        LEGACY + "backend=opa", LEGACY + "base-uri=http://127.0.0.1:8181",
                        LEGACY + "decision-path=firefly/masters/allow")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(PolicyDecisionPort.class).size());
                    assertInstanceOf(OpaPolicyDecisionAdapter.class,
                            context.getBean("insuranceReferencePolicyDecisionPort"));
                });
    }

    @Test void suppliedPolicyBeanRemainsAuthoritativeWithoutNeutralValidationOrReplacement() {
        PolicyDecisionPort supplied = new OpaPolicyDecisionAdapter(
                WebClient.builder().baseUrl("http://127.0.0.1:8181").build(), "owned/custom/allow");
        runner().withBean(PolicyDecisionPort.class, () -> supplied)
                .withPropertyValues(PREFIX + "enabled=true", PREFIX + "backend=invalid",
                        "firefly.masters.insurance-reference.commands.enabled=true", LEGACY + "backend=invalid")
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertEquals(1, context.getBeansOfType(PolicyDecisionPort.class).size());
                    assertSame(supplied, context.getBean(PolicyDecisionPort.class));
                    assertFalse(context.containsBean("masterDataPolicyDecisionPort"));
                    assertFalse(context.containsBean("insuranceReferencePolicyDecisionPort"));
                });
    }

    @ParameterizedTest @ValueSource(strings = {
            "missing:backend", "missing:base-uri", "missing:decision-path", "backend=embedded",
            "base-uri=http://remote.example.invalid", "base-uri=/relative", "base-uri=file:///tmp/policy",
            "base-uri=https://user@example.invalid", "base-uri=https://example.invalid/path",
            "base-uri=https://example.invalid?query=1", "base-uri=https://example.invalid#fragment",
            "base-uri=https://example.invalid:0", "base-uri=https://example.invalid:65536",
            "decision-path=", "decision-path=/leading", "decision-path=trailing/",
            "decision-path=double//segment", "decision-path=../allow"})
    void invalidOrMissingExplicitNeutralSettingsFailClosed(String setting) {
        var properties = configured();
        if (setting.startsWith("missing:")) properties.remove(PREFIX + setting.substring("missing:".length()));
        else {
            var split = setting.split("=", 2);
            properties.put(PREFIX + split[0], split[1]);
        }
        runner().withPropertyValues(values(properties)).run(context -> assertNotNull(context.getStartupFailure()));
    }
}
