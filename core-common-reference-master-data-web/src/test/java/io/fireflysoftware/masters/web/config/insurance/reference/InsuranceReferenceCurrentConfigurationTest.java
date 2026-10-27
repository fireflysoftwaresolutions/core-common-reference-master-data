package io.fireflysoftware.masters.web.config.insurance.reference;

import org.junit.jupiter.api.Test;
import org.fireflyframework.security.rs.ResourceServerAutoConfiguration;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.fireflyframework.security.api.domain.Decision;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.context.annotation.*;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class InsuranceReferenceCurrentConfigurationTest {
    ReactiveWebApplicationContextRunner runner(){return new ReactiveWebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ResourceServerAutoConfiguration.class,InsuranceReferenceCurrentPolicyAutoConfiguration.class))
        .withUserConfiguration(InsuranceReferenceCurrentConfiguration.class);}
    @Configuration(proxyBeanMethods=false) static class Operator {
        @Bean PolicyDecisionPort policy(){return (p,a,r,c)->Mono.just(Decision.deny("controlled source-only"));}
    }
    @Test void disabledCurrentDoesNotBindSettingsPoolNativeOrProviderResources(){runner().withUserConfiguration(Operator.class).run(context->{assertNull(context.getStartupFailure());assertFalse(context.containsBean("insuranceReferenceCurrentSettings"));assertFalse(context.containsBean("insuranceReferenceCurrentResources"));assertTrue(context.getBeansOfType(io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceCurrentObservationService.class).isEmpty());});}
    @Test void enabledCurrentWithNativeFactoryStillRequiresRealCurrencyOwner(){runner().withUserConfiguration(Operator.class).withPropertyValues(
        "firefly.masters.insurance-reference.current.enabled=true","firefly.masters.insurance-reference.current.issuers[0]=https://issuer.example.test",
        "firefly.masters.insurance-reference.current.reader.url=r2dbc:postgresql://127.0.0.1:55499/catalog?sslMode=disable",
        "firefly.masters.insurance-reference.current.reader.username=metadata_reader","firefly.masters.insurance-reference.current.reader.password=source-context-only",
        "firefly.security.resource-server.issuer=https://issuer.example.test","firefly.security.resource-server.audiences[0]=masters",
        "firefly.security.resource-server.strict-policy=true","firefly.security.resource-server.tenant-required=true","firefly.security.resource-server.tenant-claim-paths[0]=tenant_id"
        ).run(context->{Throwable failure=context.getStartupFailure();assertNotNull(failure);while(failure.getCause()!=null)failure=failure.getCause();assertInstanceOf(org.springframework.beans.factory.NoSuchBeanDefinitionException.class,failure);assertTrue(failure.getMessage().contains("CurrencyExecutionPolicyService"));});}
    @Test void readerSettingsNeverRequireOrBindWriterCredentials(){var settings=new InsuranceReferenceCurrentSettings(java.util.Set.of("https://issuer.example.test"),new InsuranceReferenceSettings.Connection("r2dbc:postgresql://127.0.0.1:55499/catalog?sslMode=disable","reader","source-only"));assertEquals("InsuranceReferenceCurrentSettings",settings.toString());assertEquals(2,settings.getClass().getRecordComponents().length);}
    @Test void realReaderOnlyGraphCanBeConstructedWithoutConnectionOrPositiveNativeProvider(){
        var options=io.r2dbc.spi.ConnectionFactoryOptions.parse("r2dbc:postgresql://127.0.0.1:55499/catalog?sslMode=disable").mutate().option(io.r2dbc.spi.ConnectionFactoryOptions.USER,"source_reader").option(io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD,"source-context-only").build();
        var pool=new io.r2dbc.pool.ConnectionPool(io.r2dbc.pool.ConnectionPoolConfiguration.builder(io.r2dbc.spi.ConnectionFactories.get(options)).initialSize(0).maxSize(1).build());
        try(var resources=new InsuranceReferenceCurrentResources(pool,operations->io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceCurrentNativeContext.unavailable(operations.getDatabaseClient()))){
            assertSame(resources.factory,resources.client.getConnectionFactory());assertSame(resources.factory,resources.manager.getConnectionFactory());
            assertSame(resources.client,resources.operations.getDatabaseClient());assertEquals(0,pool.getMetrics().orElseThrow().allocatedSize());
            assertInstanceOf(io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferenceCurrentReaderImpl.class,resources.reader);
        }
    }

}
