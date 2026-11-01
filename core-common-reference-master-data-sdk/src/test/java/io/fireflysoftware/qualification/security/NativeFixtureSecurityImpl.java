package io.fireflysoftware.qualification.security;

import java.util.Set;
import java.util.function.Function;
import org.fireflyframework.security.api.domain.Decision;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.webflux.authentication.FireflyAuthenticationToken;
import org.fireflyframework.security.webflux.authz.PolicyAuthorizationManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.web.server.BearerTokenServerAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.AuthenticationWebFilter;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import reactor.core.publisher.Mono;

/** Explicit private opaque-credential deployment; never packaged in the production executable. */
public final class NativeFixtureSecurityImpl {
    private NativeFixtureSecurityImpl() {}

    public static SecurityWebFilterChain chain(
            ServerHttpSecurity http,
            Function<String, SecurityPrincipal> principalForAuthorization,
            Set<String> resourceRoots) {
        ReactiveAuthenticationManager manager = authentication -> {
            var principal = principalForAuthorization.apply((String) authentication.getCredentials());
            return principal == null
                    ? Mono.error(new BadCredentialsException("Unknown private fixture credential"))
                    : Mono.just(new FireflyAuthenticationToken(principal));
        };
        var entryPoint = new BearerTokenServerAuthenticationEntryPoint();
        var authentication = new AuthenticationWebFilter(manager);
        authentication.setSecurityContextRepository(NoOpServerSecurityContextRepository.getInstance());
        authentication.setAuthenticationFailureHandler(
                new ServerAuthenticationEntryPointFailureHandler(entryPoint));
        authentication.setServerAuthenticationConverter(exchange -> {
            var values = exchange.getRequest().getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION);
            if (values.size() > 1)
                return Mono.error(new BadCredentialsException("One private bearer credential required"));
            var value = values.isEmpty() ? null : values.getFirst();
            if (value == null) return Mono.empty();
            if (!value.startsWith("Bearer "))
                return Mono.error(new BadCredentialsException("Private bearer credential required"));
            return Mono.just(new PreAuthenticatedAuthenticationToken("private-fixture", value));
        });
        var urlPolicy = new PolicyAuthorizationManager((principal, action, resource, context) -> {
            boolean declared = Set.of("GET", "POST", "PUT", "DELETE").contains(action)
                    && resourceRoots.stream().anyMatch(root -> resource.equals(root)
                            || resource.startsWith(root + "/"));
            return Mono.just(declared ? Decision.permit() : Decision.deny("Undeclared fixture route"));
        });
        return http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .exceptionHandling(errors -> errors.authenticationEntryPoint(entryPoint))
                .addFilterAt(authentication, SecurityWebFiltersOrder.AUTHENTICATION)
                .authorizeExchange(routes -> routes
                        .pathMatchers(HttpMethod.GET, "/v3/api-docs").permitAll()
                        .anyExchange().access((candidate, context) -> urlPolicy.authorize(
                                candidate.filter(value -> value instanceof FireflyAuthenticationToken), context)))
                .build();
    }
}
