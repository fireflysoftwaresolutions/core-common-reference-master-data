package io.fireflysoftware.masters.web.config.currency.observation;

import java.time.Duration;
import io.r2dbc.pool.*;
import io.r2dbc.spi.*;
import io.fireflysoftware.masters.core.services.currency.observation.*;
import io.fireflysoftware.masters.core.services.currency.observation.impl.*;
import io.fireflysoftware.masters.core.services.currency.policy.CurrencyPolicyAuthority;
import org.fireflyframework.security.rs.*;
import org.fireflyframework.security.webflux.authentication.FireflyAuthenticationToken;
import org.fireflyframework.security.spi.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

@Configuration(proxyBeanMethods=false)
@ConditionalOnProperty(prefix="firefly.masters.currency-policy.current",name="enabled",havingValue="true")
public class CurrencyPolicyCurrentConfiguration {
    @Bean @ConditionalOnMissingBean(CurrencyPolicyCurrentNativeContext.Factory.class)
    CurrencyPolicyCurrentNativeContext.Factory currencyPolicyCurrentNativeFactory(){return CurrencyPolicyCurrentNativeContextImpl::new;}
    @Bean CurrencyPolicyCurrentSettings currencyPolicyCurrentSettings(Environment environment){
        return Binder.get(environment).bind("firefly.masters.currency-policy.current",CurrencyPolicyCurrentSettings.class).orElseThrow(CurrencyPolicyCurrentConfiguration::unavailable);
    }
    @Bean(destroyMethod="close") CurrencyPolicyCurrentResources currencyPolicyCurrentResources(CurrencyPolicyCurrentSettings settings,CurrencyPolicyCurrentNativeContext.Factory contexts,
            ResourceServerProperties security,SecurityContextPort callers,PolicyDecisionPort policy,Environment environment){
        if(!environment.getProperty("firefly.security.resource-server.enabled",Boolean.class,true)||!security.isStrictPolicy()||!settings.issuers().contains(security.getIssuer())
                ||security.getAudiences()==null||security.getAudiences().isEmpty()||security.getAudiences().stream().anyMatch(v->v==null||v.isBlank()||v.contains("${"))
                ||!security.isTenantRequired()||security.getTenantClaimPaths()==null||security.getTenantClaimPaths().isEmpty()
                ||security.getTenantClaimPaths().stream().anyMatch(v->v==null||v.isBlank()||java.util.Arrays.stream(v.split("\\.",-1)).anyMatch(String::isBlank))
                ||!security.getPermitMatchers().isEmpty())throw unavailable();
        var reader=settings.reader();var options=ConnectionFactoryOptions.parse(reader.url()).mutate().option(ConnectionFactoryOptions.USER,reader.username()).option(ConnectionFactoryOptions.PASSWORD,reader.password()).build();
        var pool=new ConnectionPool(ConnectionPoolConfiguration.builder(ConnectionFactories.get(options)).initialSize(0).maxSize(4).maxAcquireTime(Duration.ofSeconds(2)).build());
        try{return new CurrencyPolicyCurrentResources(pool,contexts);}catch(RuntimeException failure){pool.dispose();throw unavailable();}
    }
    @Bean @ConditionalOnMissingBean(name="currencyPolicyCurrentObservationFactory")
    CurrencyPolicyCurrentObservationService.Factory currencyPolicyCurrentObservationFactory(CurrencyPolicyCurrentResources resources,SecurityContextPort callers,ObjectProvider<PolicyDecisionPort> policies,Environment environment){
        SecurityContextPort verified=verifiedContext(callers);
        ObjectProvider<SecurityContextPort> contexts=new ObjectProvider<>(){@Override public SecurityContextPort getObject(){return verified;}};
        var authority=new CurrencyPolicyAuthority(contexts,policies,environment);
        return (clock,nanos)->new NativeCurrencyPolicyCurrentObservationServiceImpl(resources.reader(clock,nanos),authority,clock,nanos);
    }
    @Bean("currencyPolicyNativeCurrentObservationService")
    CurrencyPolicyCurrentObservationService currencyPolicyNativeCurrentObservationService(CurrencyPolicyCurrentResources resources,
            @org.springframework.beans.factory.annotation.Qualifier("currencyPolicyCurrentObservationFactory") CurrencyPolicyCurrentObservationService.Factory factory){return factory.create(resources.clock,resources.nanos);}
    static SecurityContextPort verifiedContext(SecurityContextPort delegate){
        return ()->Mono.defer(()->delegate.currentPrincipal().zipWith(ReactiveSecurityContextHolder.getContext()).flatMap(tuple->{
            var principal=tuple.getT1();var authentication=tuple.getT2().getAuthentication();
            if(!(authentication instanceof FireflyAuthenticationToken token)||!authentication.isAuthenticated()||token.securityPrincipal()!=principal||principal.tenantId()==null||principal.tenantId().isBlank())return Mono.error(unavailable());
            return Mono.just(principal);
        }).switchIfEmpty(Mono.error(unavailable())));
    }
    private static IllegalStateException unavailable(){return new IllegalStateException("CURRENCY_POLICY_CURRENT_CONFIGURATION_REQUIRED");}
}
