package io.fireflysoftware.masters.web.config.insurance.reference;

import java.time.Duration;
import io.r2dbc.pool.*;
import io.r2dbc.spi.*;
import io.fireflysoftware.masters.core.services.currency.observation.impl.CurrencyPolicyCurrentObservationServiceImpl;
import io.fireflysoftware.masters.core.services.currency.policy.*;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferenceCurrentObservationServiceImpl;
import org.fireflyframework.security.rs.ResourceServerProperties;
import org.fireflyframework.security.spi.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(prefix="firefly.masters.insurance-reference.current",name="enabled",havingValue="true")
public class InsuranceReferenceCurrentConfiguration {
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean(InsuranceReferenceCurrentNativeContext.Factory.class)
    InsuranceReferenceCurrentNativeContext.Factory insuranceReferenceCurrentNativeFactory() {
        return io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferenceCurrentNativeContextImpl::new;
    }

    @Bean InsuranceReferenceCurrentSettings insuranceReferenceCurrentSettings(Environment environment) {
        return Binder.get(environment).bind("firefly.masters.insurance-reference.current",InsuranceReferenceCurrentSettings.class).orElseThrow(InsuranceReferenceCurrentConfiguration::unavailable);
    }
    @Bean CurrentSecurity insuranceReferenceCurrentSecurity(
            InsuranceReferenceCurrentSettings settings,ResourceServerProperties properties,SecurityContextPort contexts,PolicyDecisionPort policy,Environment environment) {
        if(!environment.getProperty("firefly.security.resource-server.enabled",Boolean.class,true)||!properties.isStrictPolicy()
                ||!settings.issuers().contains(properties.getIssuer())||properties.getAudiences()==null||properties.getAudiences().isEmpty()
                ||properties.getAudiences().stream().anyMatch(v->v==null||v.isBlank()||v.contains("${"))
                ||!properties.isTenantRequired()||properties.getTenantClaimPaths()==null||properties.getTenantClaimPaths().isEmpty()
                ||properties.getTenantClaimPaths().stream().anyMatch(v->v==null||v.isBlank()||java.util.Arrays.stream(v.split("\\.",-1)).anyMatch(String::isBlank))
                ||!properties.getPermitMatchers().isEmpty())throw unavailable();
        return new CurrentSecurity(InsuranceReferenceCommandConfiguration.verifiedContext(contexts),policy);
    }
    @Bean(destroyMethod="close") InsuranceReferenceCurrentResources insuranceReferenceCurrentResources(
            InsuranceReferenceCurrentSettings settings,
            @org.springframework.beans.factory.annotation.Qualifier("insuranceReferenceCurrentSecurity") CurrentSecurity security,
            InsuranceReferenceCurrentNativeContext.Factory nativeContexts) {
        var settingsReader=settings.reader();var options=ConnectionFactoryOptions.parse(settingsReader.url()).mutate()
            .option(ConnectionFactoryOptions.USER,settingsReader.username()).option(ConnectionFactoryOptions.PASSWORD,settingsReader.password()).build();
        var pool=new ConnectionPool(ConnectionPoolConfiguration.builder(ConnectionFactories.get(options)).initialSize(0).maxSize(4).maxAcquireTime(Duration.ofSeconds(2)).build());
        try{return new InsuranceReferenceCurrentResources(pool,nativeContexts);}catch(RuntimeException failure){pool.dispose();throw unavailable();}
    }
    @Bean InsuranceReferenceCurrentObservationService insuranceReferenceCurrentObservationService(InsuranceReferenceCurrentSettings settings,
            @org.springframework.beans.factory.annotation.Qualifier("insuranceReferenceCurrentSecurity") CurrentSecurity security,
            InsuranceReferenceCurrentResources resources,ObjectProvider<InsuranceReferenceSourceVerifier> source,
            @org.springframework.beans.factory.annotation.Qualifier("currencyPolicyCurrentObservationFactory") ObjectProvider<io.fireflysoftware.masters.core.services.currency.observation.CurrencyPolicyCurrentObservationService.Factory> currencies) {
        return new InsuranceReferenceCurrentObservationServiceImpl(security.contexts(),security.decisions(),settings.issuers(),resources.reader,
            source.getIfAvailable(InsuranceReferenceSourceVerifier::unavailable),
            currencies.getIfAvailable(io.fireflysoftware.masters.core.services.currency.observation.CurrencyPolicyCurrentObservationService::unavailableFactory).create(resources.clock,resources.nanos),resources.clock,resources.nanos);
    }
    record CurrentSecurity(SecurityContextPort contexts,PolicyDecisionPort decisions) {}
    private static IllegalStateException unavailable(){return new IllegalStateException("INSURANCE_REFERENCE_CURRENT_CONFIGURATION_REQUIRED");}
}
