package io.fireflysoftware.masters.web.config.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import org.fireflyframework.security.rs.ResourceServerAutoConfiguration;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.fireflyframework.security.spi.SecurityContextPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.web.server.SecurityWebFilterChain;

class InsuranceReferenceProductionConfigurationTest {
    private ReactiveWebApplicationContextRunner runner() {
        return new ReactiveWebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ResourceServerAutoConfiguration.class,
                        required("io.fireflysoftware.masters.web.config.insurance.reference.InsuranceReferencePolicyAutoConfiguration")))
                .withUserConfiguration(required("io.fireflysoftware.masters.web.config.insurance.reference.InsuranceReferenceCommandConfiguration"), ProductionPropertiesScan.class)
                .withPropertyValues("firefly.security.resource-server.strict-policy=true");
    }

    private static Class<?> required(String name) {
        try { return Class.forName(name); }
        catch (ClassNotFoundException absent) { throw new AssertionError("Actual owner production configuration missing", absent); }
    }

    @Test void disabledFeatureLeavesRealGlobalJwtChainButNoFeatureSettingsPoolsOrService() {
        runner().run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals(1, context.getBeansOfType(SecurityWebFilterChain.class).size());
            assertEquals("org.fireflyframework.security.webflux.context.ReactorSecurityContextAdapter",
                    context.getBean(SecurityContextPort.class).getClass().getName());
            assertFalse(context.containsBean("insuranceReferenceResources"));
            assertFalse(context.containsBean("insuranceReferenceSettings"));
            assertFalse(context.containsBean("insuranceReferencePolicyDecisionPort"));
            assertTrue(context.getBeansOfType(io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferencePublicationService.class).isEmpty());
            assertFalse(context.getBean(PolicyDecisionPort.class).authorize(null, "GET", "/api/v1/currencies", java.util.Map.of()).block().granted());
        });
    }

    @Test void enabledMissingTrustOrStorageFailsClosedBeforeAnyPool() {
        runner().withPropertyValues("firefly.masters.insurance-reference.commands.enabled=true")
                .run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test void operatorPolicyAndChainRemainTheActualBeansWithoutBindingUnusedOpaProperties() {
        runner().withUserConfiguration(OperatorSecurity.class).run(context -> {
            assertNull(context.getStartupFailure());
            assertSame(context.getBean("operatorPolicy"), context.getBean(PolicyDecisionPort.class));
            assertSame(context.getBean("operatorChain"), context.getBean(SecurityWebFilterChain.class));
            assertFalse(context.containsBean("fireflySecurityWebFilterChain"));
            assertFalse(context.containsBean("insuranceReferencePolicyDecisionPort"));
        });
    }

    @Test void configuredFeatureBuildsRealServiceAndRestrictedResourceGraphsWithUnavailableSource() {
        runner().withPropertyValues(profile()).withUserConfiguration(OperatorPolicy.class).run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals("io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferencePublicationServiceImpl",
                    context.getBean(io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferencePublicationService.class).getClass().getName());
            assertTrue(context.getBeansOfType(io.r2dbc.spi.ConnectionFactory.class).isEmpty());
            assertTrue(context.getBeansOfType(org.springframework.data.r2dbc.core.R2dbcEntityTemplate.class).isEmpty());
            assertTrue(context.getBeansOfType(org.springframework.r2dbc.connection.R2dbcTransactionManager.class).isEmpty());
            var resources = context.getBean(InsuranceReferenceResources.class);
            assertTrue(resources.persistence.usesTimeSources(resources.clock, resources.nanos));
            assertSame(context.getBean("operatorPolicy"), context.getBean(PolicyDecisionPort.class));
            assertFalse(context.containsBean("insuranceReferencePolicyDecisionPort"));
            assertTrue(context.getBeansOfType(io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceSourceVerifier.class).isEmpty());
            assertEquals(0, resources.commands.pool.getMetrics().orElseThrow().acquiredSize());
            assertEquals(0, resources.reads.pool.getMetrics().orElseThrow().allocatedSize());
        });
    }

    @Test void defaultOpaBindsOnlyWhenSelectedAndAnOperatorPortIsNotPresent() {
        runner().withPropertyValues(profile()).withPropertyValues(
                "firefly.masters.insurance-reference.policy.backend=opa",
                "firefly.masters.insurance-reference.policy.base-uri=https://policy.example.test",
                "firefly.masters.insurance-reference.policy.decision-path=masters/allow").run(context -> {
            assertNull(context.getStartupFailure());
            assertEquals("org.fireflyframework.security.adapter.opa.OpaPolicyDecisionAdapter", context.getBean(PolicyDecisionPort.class).getClass().getName());
        });
        runner().withPropertyValues(profile()).withUserConfiguration(OperatorPolicy.class).withPropertyValues(
                "firefly.masters.insurance-reference.policy.backend=unknown",
                "firefly.masters.insurance-reference.policy.base-uri=not-a-uri").run(context -> assertNull(context.getStartupFailure()));
    }

    @Test void enabledFeatureRefusesOptionalTenantAndNonStrictPolicyWithoutChangingThoseSettings() {
        for (var override : java.util.List.of("firefly.security.resource-server.tenant-required=false", "firefly.security.resource-server.strict-policy=false")) {
            runner().withPropertyValues(profile()).withPropertyValues(override).withUserConfiguration(OperatorPolicy.class)
                    .run(context -> assertNotNull(context.getStartupFailure()));
        }
    }

    @Test void actualSharedChainMatchesLegacyAndInsuranceRoutesWithoutPrefixSelection() {
        runner().run(context -> {
            assertNull(context.getStartupFailure());
            var chain = context.getBean(SecurityWebFilterChain.class);
            for (var path : java.util.List.of("/api/v1/currencies", "/api/v1/insurance-references/intents/execute")) {
                assertTrue(chain.matches(org.springframework.mock.web.server.MockServerWebExchange.from(
                        org.springframework.mock.http.server.reactive.MockServerHttpRequest.get(path))).block());
            }
        });
    }

    @Test void actualProductionPackageAndPropertiesScansKeepDisabledFeatureDormant() {
        var main = io.fireflysoftware.masters.web.MasterDataApplication.class;
        var scan = main.getAnnotation(org.springframework.boot.autoconfigure.SpringBootApplication.class);
        assertTrue(java.util.Arrays.asList(scan.scanBasePackages()).contains("io.fireflysoftware.masters"));
        assertNotNull(main.getAnnotation(ConfigurationPropertiesScan.class));
        runner().withUserConfiguration(ActualFeaturePackageScan.class).run(context -> {
            assertNull(context.getStartupFailure());
            assertFalse(context.containsBean("insuranceReferenceResources"));
            assertFalse(context.containsBean("insuranceReferenceSettings"));
            assertFalse(context.containsBean("insuranceReferenceController"));
            assertEquals(1, context.getBeansOfType(SecurityWebFilterChain.class).size());
        });
    }

    @Test void originalImportMetadataAndManagedPomDeclareTheProductionSecurityGraph() throws Exception {
        var imports = org.springframework.boot.context.annotation.ImportCandidates.load(
                org.springframework.boot.autoconfigure.AutoConfiguration.class, getClass().getClassLoader()).getCandidates();
        assertTrue(imports.contains(InsuranceReferencePolicyAutoConfiguration.class.getName()));
        assertTrue(imports.contains(ResourceServerAutoConfiguration.class.getName()));
        var root = java.nio.file.Path.of(System.getProperty("source.root", System.getProperty("user.dir")));
        var pom = root.resolve("core-common-reference-master-data-web/pom.xml");
        if (!java.nio.file.Files.isRegularFile(pom)) pom = root.resolve("pom.xml");
        var xml = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
        for (var artifact : java.util.List.of("fireflyframework-security-resource-server", "fireflyframework-security-adapter-opa")) {
            var found = false;
            var nodes = xml.getElementsByTagName("dependency");
            for (int index = 0; index < nodes.getLength(); index++) {
                var dependency = (org.w3c.dom.Element) nodes.item(index);
                if (!artifact.equals(dependency.getElementsByTagName("artifactId").item(0).getTextContent())) continue;
                assertEquals(0, dependency.getElementsByTagName("version").getLength());
                assertEquals(0, dependency.getElementsByTagName("scope").getLength());
                found = true;
            }
            assertTrue(found, artifact);
        }
    }

    private static String[] profile() {
        var values = new java.util.ArrayList<String>();
        values.add("firefly.masters.insurance-reference.commands.enabled=true");
        values.add("firefly.masters.insurance-reference.issuers[0]=https://issuer.example.test");
        values.add("firefly.security.resource-server.issuer=https://issuer.example.test");
        values.add("firefly.security.resource-server.jwk-set-uri=https://issuer.example.test/jwks");
        values.add("firefly.security.resource-server.audiences[0]=masters");
        values.add("firefly.security.resource-server.tenant-required=true");
        values.add("firefly.security.resource-server.tenant-claim-paths[0]=tenant_id");
        for (var role : java.util.List.of("writer", "reader")) {
            var prefix = "firefly.masters.insurance-reference." + role + ".";
            values.add(prefix + "url=r2dbc:postgresql://127.0.0.1:55499/catalog?sslMode=disable");
            values.add(prefix + "username=metadata_" + role);
            values.add(prefix + "password=" + java.util.UUID.randomUUID());
        }
        return values.toArray(String[]::new);
    }

    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan(basePackageClasses = InsuranceReferenceSettings.class)
    static class ProductionPropertiesScan {}

    @Configuration(proxyBeanMethods = false)
    @ConfigurationPropertiesScan(basePackageClasses = InsuranceReferenceSettings.class)
    @ComponentScan(basePackages = {"io.fireflysoftware.masters.web.config.insurance.reference", "io.fireflysoftware.masters.web.controllers.insurance.reference"},
            excludeFilters = {
                    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter.class),
                    @ComponentScan.Filter(type = FilterType.CUSTOM, classes = org.springframework.boot.context.TypeExcludeFilter.class),
                    @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*Test(?:\\$.*)?")})
    static class ActualFeaturePackageScan {}

    @Configuration(proxyBeanMethods = false)
    static class OperatorSecurity {
        @Bean PolicyDecisionPort operatorPolicy() {
            return (principal, action, resource, context) -> reactor.core.publisher.Mono.just(org.fireflyframework.security.api.domain.Decision.deny("source-context control"));
        }
        @Bean SecurityWebFilterChain operatorChain() {
            return org.springframework.security.config.web.server.ServerHttpSecurity.http()
                    .authorizeExchange(exchanges -> exchanges.anyExchange().denyAll()).build();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class OperatorPolicy {
        @Bean PolicyDecisionPort operatorPolicy() {
            return (principal, action, resource, context) -> reactor.core.publisher.Mono.just(org.fireflyframework.security.api.domain.Decision.deny("source-context control"));
        }
    }
}
