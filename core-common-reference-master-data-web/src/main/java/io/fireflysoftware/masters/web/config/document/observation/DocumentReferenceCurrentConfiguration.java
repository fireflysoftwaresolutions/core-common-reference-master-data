/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.config.document.observation;
import java.time.Duration;
import io.r2dbc.pool.*;
import io.r2dbc.spi.*;
import io.fireflysoftware.masters.core.services.document.observation.*;
import io.fireflysoftware.masters.core.services.document.observation.impl.*;
import io.fireflysoftware.masters.core.services.document.layout.v1.DocumentLayoutProfileRegistry;
import org.fireflyframework.security.rs.*;
import org.fireflyframework.security.webflux.authentication.FireflyAuthenticationToken;
import org.fireflyframework.security.spi.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(prefix="firefly.masters.document-reference.current",name="enabled",havingValue="true")
public class DocumentReferenceCurrentConfiguration {
    @Bean @ConditionalOnMissingBean(DocumentReferenceCurrentNativeContext.Factory.class) DocumentReferenceCurrentNativeContext.Factory documentReferenceCurrentNativeFactory(){return DocumentReferenceCurrentNativeContextImpl::new;}
    @Bean DocumentReferenceCurrentSettings documentReferenceCurrentSettings(Environment environment){return Binder.get(environment).bind("firefly.masters.document-reference.current",DocumentReferenceCurrentSettings.class).orElseThrow(DocumentReferenceCurrentConfiguration::unavailable);}
    @Bean(destroyMethod="close") DocumentReferenceCurrentResources documentReferenceCurrentResources(DocumentReferenceCurrentSettings settings,DocumentReferenceCurrentNativeContext.Factory contexts,ResourceServerProperties security,SecurityContextPort callers,PolicyDecisionPort policy,Environment environment){
        if(!environment.getProperty("firefly.security.resource-server.enabled",Boolean.class,true)||!security.isStrictPolicy()||!settings.issuers().contains(security.getIssuer())||security.getAudiences()==null||security.getAudiences().isEmpty()||security.getAudiences().stream().anyMatch(v->v==null||v.isBlank()||v.contains("${"))||!security.isTenantRequired()||security.getTenantClaimPaths()==null||security.getTenantClaimPaths().isEmpty()||security.getTenantClaimPaths().stream().anyMatch(v->v==null||v.isBlank()||java.util.Arrays.stream(v.split("\\.",-1)).anyMatch(String::isBlank))||!security.getPermitMatchers().isEmpty())throw unavailable();
        var reader=settings.reader();var options=ConnectionFactoryOptions.parse(reader.url()).mutate().option(ConnectionFactoryOptions.USER,reader.username()).option(ConnectionFactoryOptions.PASSWORD,reader.password()).build();var pool=new ConnectionPool(ConnectionPoolConfiguration.builder(ConnectionFactories.get(options)).initialSize(0).maxSize(4).maxAcquireTime(Duration.ofSeconds(2)).build());try{return new DocumentReferenceCurrentResources(pool,contexts);}catch(RuntimeException failure){pool.dispose();throw unavailable();}
    }
    @Bean("documentReferenceNativeCurrentService") DocumentReferenceCurrentService documentReferenceNativeCurrentService(DocumentReferenceCurrentResources resources,DocumentReferenceCurrentSettings settings,SecurityContextPort callers,PolicyDecisionPort policies,DocumentLayoutProfileRegistry profiles){var authorization=new DocumentReferenceCurrentAuthorizationImpl(verifiedContext(callers),policies,settings.issuers(),resources.clock);return new DocumentReferenceCurrentServiceImpl(resources.reader(resources.clock,resources.nanos),authorization,profiles,resources.clock,resources.nanos);}
    static SecurityContextPort verifiedContext(SecurityContextPort delegate){return ()->Mono.defer(()->delegate.currentPrincipal().zipWith(ReactiveSecurityContextHolder.getContext()).flatMap(tuple->{var principal=tuple.getT1();var authentication=tuple.getT2().getAuthentication();if(!(authentication instanceof FireflyAuthenticationToken token)||!authentication.isAuthenticated()||token.securityPrincipal()!=principal||principal.tenantId()==null||principal.tenantId().isBlank())return Mono.error(unavailable());return Mono.just(principal);}).switchIfEmpty(Mono.error(unavailable())));}
    private static IllegalStateException unavailable(){return new IllegalStateException("DOCUMENT_REFERENCE_CURRENT_CONFIGURATION_REQUIRED");}
}
