package io.fireflysoftware.masters.models.repositories.currency.policy;

import io.fireflysoftware.masters.models.repositories.currency.policy.impl.CurrencyPolicyRepositoryImpl;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.persistence.autoconfigure.PersistenceExceptionTranslationAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyPolicyRepositoryProxyContextTest {
    @Configuration(proxyBeanMethods=false)
    @Import({PersistenceExceptionTranslationAutoConfiguration.class,CurrencyPolicyRepositoryImpl.class})
    static class ProductionRepositoryGraph {
        @Bean AtomicInteger connections(){return new AtomicInteger();}
        @Bean ConnectionFactory connectionFactory(AtomicInteger connections){
            return new ConnectionFactory(){
                public ConnectionFactoryMetadata getMetadata(){return ()->"PostgreSQL";}
                public Publisher<? extends Connection> create(){
                    connections.incrementAndGet();return Mono.error(new AssertionError("Source context must never open a connection"));
                }
            };
        }
        @Bean R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory factory){return new R2dbcEntityTemplate(factory);}
    }
    @Test void productionDefaultClassProxyKeepsNamedRepositoryAndExactOperations()throws Exception {
        try(var context=new AnnotationConfigApplicationContext()){
            context.register(ProductionRepositoryGraph.class);context.refresh();
            var repository=context.getBean("currencyPolicyRepository",CurrencyPolicyRepository.class);
            assertTrue(AopUtils.isCglibProxy(repository));
            assertSame(repository,context.getBean(CurrencyPolicyRepositoryImpl.class));
            assertTrue(repository.usesOperations(context.getBean(R2dbcEntityTemplate.class)));
            assertEquals(0,context.getBean(AtomicInteger.class).get());
            assertOrigin();
        }
    }
    @Test void explicitInterfaceProxyControlKeepsSamePublicContractWithoutCoreOrConnection()throws Exception {
        try(var context=new AnnotationConfigApplicationContext()){
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("explicit-interface-control",java.util.Map.of("spring.aop.proxy-target-class",false)));
            context.register(ProductionRepositoryGraph.class);context.refresh();
            var repository=context.getBean("currencyPolicyRepository",CurrencyPolicyRepository.class);
            assertTrue(AopUtils.isJdkDynamicProxy(repository));
            assertTrue(repository.usesOperations(context.getBean(R2dbcEntityTemplate.class)));
            assertEquals(0,context.getBean(AtomicInteger.class).get());
            assertOrigin();
        }
    }
    private static void assertOrigin()throws Exception {
        String expected=System.getProperty("expected.repository.origin");
        if(expected!=null)assertEquals(URI.create(expected),CurrencyPolicyRepositoryImpl.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    }
}
