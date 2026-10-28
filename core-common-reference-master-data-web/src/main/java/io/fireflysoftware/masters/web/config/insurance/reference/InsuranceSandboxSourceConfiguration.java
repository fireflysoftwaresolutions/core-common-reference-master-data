package io.fireflysoftware.masters.web.config.insurance.reference;

import java.time.Duration;
import java.util.UUID;
import io.r2dbc.pool.*;
import io.r2dbc.spi.*;
import io.fireflysoftware.masters.core.services.insurance.source.*;
import io.fireflysoftware.masters.core.services.insurance.source.impl.*;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.core.services.currency.policy.*;
import io.fireflysoftware.masters.core.services.currency.observation.impl.CurrencyPolicyCurrentObservationServiceImpl;
import org.fireflyframework.security.rs.ResourceServerProperties;
import org.fireflyframework.security.spi.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(prefix="firefly.masters.insurance-reference.sandbox-source",name="enabled",havingValue="true")
public class InsuranceSandboxSourceConfiguration {
    @Bean InsuranceSandboxSourceSettings insuranceSandboxSourceSettings(Environment environment,InsuranceReferenceSettings publication){
        var settings=Binder.get(environment).bind("firefly.masters.insurance-reference.sandbox-source",InsuranceSandboxSourceSettings.class)
            .orElseThrow(InsuranceSandboxSourceConfiguration::unavailable);
        if(!settings.writer().url().equals(publication.writer().url())||!settings.issuers().equals(publication.issuers()))throw unavailable();
        return settings;
    }
    @Bean InsuranceReferenceCurrentConfiguration.CurrentSecurity insuranceSandboxSourceSecurity(
            InsuranceSandboxSourceSettings settings,ResourceServerProperties properties,SecurityContextPort contexts,PolicyDecisionPort decisions,Environment environment){
        return new InsuranceReferenceCurrentConfiguration().insuranceReferenceCurrentSecurity(
            new InsuranceReferenceCurrentSettings(settings.issuers(),settings.reader()),properties,contexts,decisions,environment);
    }
    @Bean(destroyMethod="close") InsuranceSandboxSourceResources insuranceSandboxSourceResources(InsuranceSandboxSourceSettings settings,
            @org.springframework.beans.factory.annotation.Qualifier("insuranceSandboxSourceSecurity") InsuranceReferenceCurrentConfiguration.CurrentSecurity security){
        var writer=pool(settings.writer());ConnectionPool reader=null;
        try{reader=pool(settings.reader());return new InsuranceSandboxSourceResources(writer,reader);}
        catch(RuntimeException failure){try{writer.dispose();}finally{if(reader!=null)reader.dispose();}throw unavailable();}
    }
    @Bean InsuranceSandboxFactAuthority insuranceSandboxFactAuthority(InsuranceSandboxSourceSettings settings,
            @org.springframework.beans.factory.annotation.Qualifier("insuranceSandboxSourceSecurity") InsuranceReferenceCurrentConfiguration.CurrentSecurity security,
            InsuranceSandboxSourceResources resources){return new InsuranceSandboxFactAuthority(security.contexts(),security.decisions(),settings.issuers(),resources.clock,resources.nanos);}
    @Bean InsuranceSandboxFactReader insuranceSandboxFactReader(InsuranceSandboxFactAuthority authority,InsuranceSandboxSourceResources resources){
        return new InsuranceSandboxFactReaderImpl(authority,resources.persistence);
    }
    @Bean InsuranceSandboxFactRegistrationService insuranceSandboxFactRegistrationService(InsuranceSandboxFactAuthority authority,
            InsuranceSandboxSourceResources resources,InsuranceReferencePublicationService publications){
        return new InsuranceSandboxFactRegistrationServiceImpl(authority,resources.persistence,publications,UUID::randomUUID);
    }
    @Bean @ConditionalOnMissingBean(InsuranceReferenceSourceVerifier.class)
    InsuranceReferenceSourceVerifier durableInsuranceSandboxSourceVerifier(InsuranceSandboxFactReader reader,
            InsuranceSandboxSourceResources resources,@org.springframework.beans.factory.annotation.Qualifier("currencyPolicyCurrentObservationFactory") org.springframework.beans.factory.ObjectProvider<io.fireflysoftware.masters.core.services.currency.observation.CurrencyPolicyCurrentObservationService.Factory> currencies){
        return new DurableInsuranceSandboxSourceVerifier(reader,currencies.getIfAvailable(io.fireflysoftware.masters.core.services.currency.observation.CurrencyPolicyCurrentObservationService::unavailableFactory).create(resources.clock,resources.nanos),resources.clock,resources.nanos);
    }
    private static ConnectionPool pool(InsuranceReferenceSettings.Connection settings){
        var options=ConnectionFactoryOptions.parse(settings.url()).mutate().option(ConnectionFactoryOptions.USER,settings.username())
            .option(ConnectionFactoryOptions.PASSWORD,settings.password()).build();
        return new ConnectionPool(ConnectionPoolConfiguration.builder(ConnectionFactories.get(options)).initialSize(0).maxSize(4).maxAcquireTime(Duration.ofSeconds(2)).build());
    }
    private static IllegalStateException unavailable(){return new IllegalStateException("INSURANCE_SANDBOX_SOURCE_CONFIGURATION_REQUIRED");}
}
