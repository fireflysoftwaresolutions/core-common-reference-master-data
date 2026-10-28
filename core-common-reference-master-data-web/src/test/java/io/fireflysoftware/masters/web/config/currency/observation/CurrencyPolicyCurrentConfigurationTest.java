package io.fireflysoftware.masters.web.config.currency.observation;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import io.fireflysoftware.masters.core.services.currency.observation.CurrencyPolicyCurrentObservationService;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.webflux.authentication.FireflyAuthenticationToken;
import org.fireflyframework.security.webflux.context.ReactorSecurityContextAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.core.context.*;
import reactor.core.publisher.Mono;

/** Source bean/context identity only; constructed tokens are not JWT authentication acceptance. */
class CurrencyPolicyCurrentConfigurationTest {
    @Test void defaultOffDoesNotBindCreatePoolsFactoriesOrGlobalGraphs(){
        try(var context=new AnnotationConfigApplicationContext()){
            context.register(CurrencyPolicyCurrentConfiguration.class,io.fireflysoftware.masters.web.controllers.currency.policy.CurrencyPolicyCurrentController.class);context.refresh();
            assertEquals(0,context.getBeansOfType(CurrencyPolicyCurrentResources.class).size());assertEquals(0,context.getBeansOfType(CurrencyPolicyCurrentObservationService.Factory.class).size());
            assertEquals(0,context.getBeansOfType(io.r2dbc.spi.ConnectionFactory.class).size());assertEquals(0,context.getBeansOfType(org.springframework.data.r2dbc.core.R2dbcEntityTemplate.class).size());
            assertEquals(0,context.getBeansOfType(io.fireflysoftware.masters.web.controllers.currency.policy.CurrencyPolicyCurrentController.class).size());
        }
    }
    @Test void actualDefaultFlywayStillSelectsOnlyOriginal36Resources()throws Exception {
        var yaml=new org.springframework.beans.factory.config.YamlPropertiesFactoryBean();
        yaml.setResources(new org.springframework.core.io.ClassPathResource("application.yaml"));
        var actual=yaml.getObject();assertEquals("true",actual.getProperty("spring.flyway.enabled"));assertEquals("classpath:db/migration",actual.getProperty("spring.flyway.locations"));
        var resources=new org.springframework.core.io.support.PathMatchingResourcePatternResolver();
        var names=java.util.Arrays.stream(resources.getResources("classpath*:db/migration/*.sql")).map(org.springframework.core.io.Resource::getFilename).toList();
        assertEquals(36,names.size());assertEquals(36,new java.util.HashSet<>(names).size());assertFalse(names.stream().anyMatch(n->n.startsWith("V37")));
        var optional=resources.getResources("classpath*:db/optional/currency-current/V37__Currency_Policy_Finite_Current.sql");
        assertEquals(1,optional.length);assertTrue(optional[0].isReadable());
    }
    @Test void enabledMissingSettingsRefusesRatherThanUsingLegacyMainCredentials(){
        try(var context=new AnnotationConfigApplicationContext()){
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("explicit-source-control",Map.of("firefly.masters.currency-policy.current.enabled","true")));
            context.register(CurrencyPolicyCurrentConfiguration.class);assertThrows(RuntimeException.class,context::refresh);
        }
    }
    @Test void exactFrameworkAuthenticationPrincipalIsRetained(){
        var actor=SecurityPrincipal.builder().subject("source-only").issuer("https://issuer.example.test").tenantId("platform").build();
        var authentication=new SecurityContextImpl(new FireflyAuthenticationToken(actor));
        assertSame(actor,CurrencyPolicyCurrentConfiguration.verifiedContext(new ReactorSecurityContextAdapter()).currentPrincipal().contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(authentication))).block());
    }
    @Test void equalButDifferentPrincipalAndMissingTenantAreNeverRepaired(){
        var actor=SecurityPrincipal.builder().subject("source-only").issuer("https://issuer.example.test").tenantId("platform").build();var authentication=new SecurityContextImpl(new FireflyAuthenticationToken(actor));var copy=actor.toBuilder().build();
        assertThrows(RuntimeException.class,()->CurrencyPolicyCurrentConfiguration.verifiedContext(()->Mono.just(copy)).currentPrincipal().contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(authentication))).block());
        var absent=actor.toBuilder().tenantId(null).claims(Map.of("tenant_id","must-not-repair")).build();var missing=new SecurityContextImpl(new FireflyAuthenticationToken(absent));
        assertThrows(RuntimeException.class,()->CurrencyPolicyCurrentConfiguration.verifiedContext(()->Mono.just(absent)).currentPrincipal().contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(missing))).block());assertNull(absent.tenantId());
    }
}
