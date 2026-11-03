/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.config.insurance.reference;

import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferencePublicationService;
import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceSourceVerifier;
import io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferencePublicationServiceImpl;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactoryOptions;
import java.time.Duration;
import java.util.UUID;
import org.fireflyframework.security.rs.ResourceServerProperties;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.fireflyframework.security.spi.SecurityContextPort;
import org.fireflyframework.security.webflux.authentication.FireflyAuthenticationToken;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "firefly.masters.insurance-reference.commands", name = "enabled", havingValue = "true")
public class InsuranceReferenceCommandConfiguration {
    @Bean
    InsuranceReferenceSettings insuranceReferenceSettings(Environment environment) {
        return Binder.get(environment).bind("firefly.masters.insurance-reference", InsuranceReferenceSettings.class)
                .orElseThrow(InsuranceReferenceCommandConfiguration::unavailable);
    }

    @Bean
    InsuranceReferenceSecurity insuranceReferenceSecurity(InsuranceReferenceSettings settings, ResourceServerProperties properties,
            SecurityContextPort contexts, PolicyDecisionPort decisions, Environment environment) {
        if (!environment.getProperty("firefly.security.resource-server.enabled", Boolean.class, true)
                || !properties.isStrictPolicy() || !settings.issuers().contains(properties.getIssuer())
                || properties.getAudiences() == null || properties.getAudiences().isEmpty()
                || properties.getAudiences().stream().anyMatch(value -> value == null || value.isBlank() || value.contains("${"))
                || !properties.isTenantRequired() || properties.getTenantClaimPaths() == null || properties.getTenantClaimPaths().isEmpty()
                || properties.getTenantClaimPaths().stream().anyMatch(value -> value == null || value.isBlank()
                        || java.util.Arrays.stream(value.split("\\.", -1)).anyMatch(String::isBlank))
                || !properties.getPermitMatchers().isEmpty()) throw unavailable();
        return new InsuranceReferenceSecurity(verifiedContext(contexts), decisions);
    }

    @Bean(destroyMethod = "close")
    InsuranceReferenceResources insuranceReferenceResources(InsuranceReferenceSettings settings, InsuranceReferenceSecurity security) {
        var writer = pool(settings.writer());
        ConnectionPool reader = null;
        try {
            reader = pool(settings.reader());
            return new InsuranceReferenceResources(writer, reader);
        } catch (RuntimeException failure) {
            try { writer.dispose(); } finally { if (reader != null) reader.dispose(); }
            throw unavailable();
        }
    }

    @Bean
    InsuranceReferencePublicationService insuranceReferencePublicationService(InsuranceReferenceSettings settings,
            InsuranceReferenceSecurity security, InsuranceReferenceResources resources, ObjectProvider<InsuranceReferenceSourceVerifier> sources) {
        return new InsuranceReferencePublicationServiceImpl(true, security.contexts(), security.decisions(), settings.issuers(),
                resources.persistence, sources.getIfAvailable(InsuranceReferenceSourceVerifier::unavailable),
                resources.clock, resources.nanos, UUID::randomUUID);
    }

    private static ConnectionPool pool(InsuranceReferenceSettings.Connection settings) {
        var options = ConnectionFactoryOptions.parse(settings.url()).mutate()
                .option(ConnectionFactoryOptions.USER, settings.username()).option(ConnectionFactoryOptions.PASSWORD, settings.password()).build();
        var factory = ConnectionFactories.get(options);
        return new ConnectionPool(ConnectionPoolConfiguration.builder(factory).initialSize(0).maxSize(4)
                .maxAcquireTime(Duration.ofSeconds(2)).build());
    }

    static SecurityContextPort verifiedContext(SecurityContextPort delegate) {
        return () -> Mono.defer(() -> delegate.currentPrincipal().zipWith(ReactiveSecurityContextHolder.getContext())
                .flatMap(tuple -> {
                    var principal = tuple.getT1();
                    var authentication = tuple.getT2().getAuthentication();
                    if (!(authentication instanceof FireflyAuthenticationToken token) || !authentication.isAuthenticated()
                            || token.securityPrincipal() != principal || principal.tenantId() == null || principal.tenantId().isBlank())
                        return Mono.error(unavailable());
                    return Mono.just(principal);
                }).switchIfEmpty(Mono.error(unavailable())));
    }

    record InsuranceReferenceSecurity(SecurityContextPort contexts, PolicyDecisionPort decisions) {}

    private static IllegalStateException unavailable() {
        return new IllegalStateException("INSURANCE_REFERENCE_AUTHENTICATED_CONFIGURATION_REQUIRED");
    }
}
