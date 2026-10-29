package io.fireflysoftware.masters.sdk;

import org.fireflyframework.security.api.domain.Decision;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.fireflyframework.security.spi.SecurityContextPort;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Mono;
import java.util.Set;

/** Test-only loader.path entry point; production jar has no synthetic trust provider. */
public final class DocumentTemplateRuntimeBootstrap {
    private DocumentTemplateRuntimeBootstrap() { }
    public static void main(String[] args) throws ClassNotFoundException {
        new SpringApplicationBuilder(Class.forName("io.fireflysoftware.masters.web.MasterDataApplication"), SyntheticTrustedAdministration.class)
                .web(WebApplicationType.REACTIVE).run(args);
    }
    /** Explicit synthetic authenticator and action-specific policy, never a production authentication bypass. */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class SyntheticTrustedAdministration {
        @Bean org.fireflyframework.web.idempotency.scope.IdempotencyScopeResolver syntheticTrustedCacheScope(SecurityContextPort context) {
            return exchange -> context.currentPrincipal().map(principal ->
                    "GLOBAL_MASTER:" + principal.issuer() + ":" + principal.subject() + ":" +
                            principal.scopes().stream().sorted().collect(java.util.stream.Collectors.joining(",")));
        }
        @Bean SecurityContextPort syntheticSecurityContext() {
            return () -> Mono.deferContextual(context -> Mono.justOrEmpty(context.<SecurityPrincipal>getOrEmpty(SecurityPrincipal.class)));
        }
        @Bean PolicyDecisionPort syntheticPolicy() {
            return (principal, action, resource, context) -> Mono.just(switch (action) {
                case "masters.document-template.read" -> principal.hasScope("templates.read") ? Decision.permit() : Decision.deny("Fixture read denied");
                case "masters.document-template.resolve" -> principal.hasScope("templates.resolve") ? Decision.permit() : Decision.deny("Fixture resolve denied");
                case "masters.document-template.publish", "masters.document-template.retire", "masters.document-template.draft.write" ->
                        principal.hasScope("templates.admin") && !Boolean.TRUE.equals(principal.claim("fixtureAdministrationRevoked", Boolean.class))
                                ? Decision.permit() : Decision.deny("Fixture administration denied");
                default -> Decision.deny("Unknown fixture action");
            });
        }
        @Bean @org.springframework.core.annotation.Order(-90) WebFilter syntheticAuthentication() {
            return (exchange, chain) -> {
                String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
                SecurityPrincipal principal = switch (authorization == null ? "" : authorization) {
                    case "Bearer synthetic-admin" -> SecurityPrincipal.builder().subject("synthetic-document-admin")
                            .issuer("synthetic-test-fixture").scopes(Set.of("templates.read", "templates.resolve", "templates.admin")).build();
                    case "Bearer synthetic-admin-current-policy-denied" -> SecurityPrincipal.builder().subject("synthetic-document-admin")
                            .issuer("synthetic-test-fixture").scopes(Set.of("templates.read", "templates.resolve", "templates.admin"))
                            .claims(java.util.Map.of("fixtureAdministrationRevoked", true)).build();
                    case "Bearer synthetic-reader" -> SecurityPrincipal.builder().subject("synthetic-document-reader")
                            .issuer("synthetic-test-fixture").scopes(Set.of("templates.read")).build();
                    case "Bearer synthetic-generator" -> SecurityPrincipal.builder().subject("synthetic-document-generator")
                            .issuer("synthetic-test-fixture").scopes(Set.of("templates.resolve")).build();
                    default -> null;
                };
                return principal == null ? chain.filter(exchange) : chain.filter(exchange).contextWrite(context -> context.put(SecurityPrincipal.class, principal));
            };
        }
    }
}
