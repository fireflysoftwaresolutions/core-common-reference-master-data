package io.fireflysoftware.masters.web.config.insurance.reference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import io.fireflysoftware.masters.core.services.insurance.source.InsuranceSandboxFactRegistrationService;
import static org.junit.jupiter.api.Assertions.*;

class InsuranceSandboxSourceConfigurationTest {
    Class<?> configuration() throws Exception {return Class.forName("io.fireflysoftware.masters.web.config.insurance.reference.InsuranceSandboxSourceConfiguration");}
    @Test void disabledSourceHasNoSettingsPoolsOrAdministration() throws Exception {
        new ReactiveWebApplicationContextRunner().withUserConfiguration(configuration()).run(context->{
            assertNull(context.getStartupFailure());assertFalse(context.containsBean("insuranceSandboxSourceSettings"));
            assertFalse(context.containsBean("insuranceSandboxSourceResources"));assertTrue(context.getBeansOfType(InsuranceSandboxFactRegistrationService.class).isEmpty());
        });
    }
    @Test void sourceHasIndependentExplicitOptInAndMissingSettingsRefuses() throws Exception {
        var type=configuration();var flag=type.getAnnotation(ConditionalOnProperty.class);
        assertEquals("firefly.masters.insurance-reference.sandbox-source",flag.prefix());assertFalse(flag.matchIfMissing());
        new ReactiveWebApplicationContextRunner().withUserConfiguration(type).withPropertyValues("firefly.masters.insurance-reference.sandbox-source.enabled=true").run(context->assertNotNull(context.getStartupFailure()));
    }
    @Test void sourceSettingsRefuseUnexpectedRolesAndOtherDatabases(){
        var url="r2dbc:postgresql://127.0.0.1:55499/catalog?sslMode=disable";
        var writer=new InsuranceReferenceSettings.Connection(url,"masters_ins_ref_source_registrar","source-only");
        var reader=new InsuranceReferenceSettings.Connection(url,"masters_ins_ref_source_reader","source-only");
        var valid=new InsuranceSandboxSourceSettings(java.util.Set.of("https://issuer.example.test"),writer,reader);assertEquals("InsuranceSandboxSourceSettings",valid.toString());
        assertThrows(IllegalArgumentException.class,()->new InsuranceSandboxSourceSettings(valid.issuers(),new InsuranceReferenceSettings.Connection(url,"masters_ins_ref_writer","source-only"),reader));
        assertThrows(IllegalArgumentException.class,()->new InsuranceSandboxSourceSettings(valid.issuers(),writer,new InsuranceReferenceSettings.Connection(url.replace("/catalog?","/other?"),reader.username(),"source-only")));
    }
    @Test void actualSourceGraphsUseTheirOwnSameBoundFactoryWithoutConnecting(){
        var options=io.r2dbc.spi.ConnectionFactoryOptions.parse("r2dbc:postgresql://127.0.0.1:55499/catalog?sslMode=disable").mutate().option(io.r2dbc.spi.ConnectionFactoryOptions.USER,"source_context").option(io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD,"source-only").build();
        java.util.function.Supplier<io.r2dbc.pool.ConnectionPool> pool=()->new io.r2dbc.pool.ConnectionPool(io.r2dbc.pool.ConnectionPoolConfiguration.builder(io.r2dbc.spi.ConnectionFactories.get(options)).initialSize(0).maxSize(1).build());
        try(var resources=new InsuranceSandboxSourceResources(pool.get(),pool.get())){
            assertNotSame(resources.writer.factory,resources.reader.factory);
            for(var graph:java.util.List.of(resources.writer,resources.reader)){
                assertSame(graph.factory,graph.client.getConnectionFactory());assertSame(graph.factory,graph.manager.getConnectionFactory());assertSame(graph.client,graph.operations.getDatabaseClient());
                assertEquals(0,graph.pool.getMetrics().orElseThrow().allocatedSize());
            }
            assertTrue(resources.persistence.usesTimeSources(resources.clock,resources.nanos));
        }
    }
}
