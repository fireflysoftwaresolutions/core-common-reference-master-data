package io.fireflysoftware.qualification.security;

import java.util.Set;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.webflux.context.ReactorSecurityContextAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

class NativeFixtureSecurityTest {
    @Configuration(proxyBeanMethods = false)
    @EnableWebFlux
    @EnableWebFluxSecurity
    static class Fixture {
        @Bean
        SecurityWebFilterChain fixtureChain(ServerHttpSecurity http) {
            return NativeFixtureSecurityImpl.chain(http,
                    authorization -> "Bearer synthetic-native-fixture".equals(authorization)
                            ? SecurityPrincipal.builder().subject("fixture-subject")
                                    .issuer("https://fixture.invalid").scopes(Set.of("fixture-scope")).build()
                            : null,
                    Set.of("/api/v1/native-fixture"));
        }

        @Bean
        RouterFunction<ServerResponse> fixtureRoutes() {
            return RouterFunctions.route()
                    .GET("/v3/api-docs", request -> ServerResponse.ok().bodyValue("metadata"))
                    .GET("/api/v1/native-fixture", request -> new ReactorSecurityContextAdapter()
                            .currentPrincipal().flatMap(principal -> ServerResponse.ok().bodyValue(
                                    principal.issuer() + "|" + principal.subject() + "|"
                                            + principal.hasScope("fixture-scope"))))
                    .POST("/api/v1/native-fixture", request -> ServerResponse.ok().bodyValue("accepted"))
                    .GET("/api/v1/undeclared", request -> ServerResponse.ok().bodyValue("must not reach"))
                    .build();
        }
    }

    @Test
    void metadataIsPublicButBusinessRequiresNativeAuthentication() {
        try (var context = new AnnotationConfigApplicationContext(Fixture.class)) {
            var client = WebTestClient.bindToApplicationContext(context).build();
            client.get().uri("/v3/api-docs").exchange().expectStatus().isOk();
            client.get().uri("/api/v1/native-fixture").exchange().expectStatus().isUnauthorized();
        }
    }

    @Test
    void exactOpaqueCredentialReachesThePublishedNativePrincipalAdapter() {
        try (var context = new AnnotationConfigApplicationContext(Fixture.class)) {
            var client = WebTestClient.bindToApplicationContext(context).build();
            client.get().uri("/api/v1/native-fixture")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer synthetic-native-fixture")
                    .exchange().expectStatus().isOk().expectBody(String.class)
                    .isEqualTo("https://fixture.invalid|fixture-subject|true");
            client.post().uri("/api/v1/native-fixture")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer synthetic-native-fixture")
                    .exchange().expectStatus().isOk();
        }
    }

    @Test
    void invalidCredentialsAndUndeclaredRoutesRemainClosed() {
        try (var context = new AnnotationConfigApplicationContext(Fixture.class)) {
            var client = WebTestClient.bindToApplicationContext(context).build();
            client.get().uri("/api/v1/native-fixture")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer unknown-fixture")
                    .exchange().expectStatus().isUnauthorized();
            client.get().uri("/api/v1/native-fixture")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer synthetic-native-fixture", "Bearer unknown-fixture")
                    .exchange().expectStatus().isUnauthorized();
            client.get().uri("/api/v1/undeclared")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer synthetic-native-fixture")
                    .exchange().expectStatus().isForbidden();
        }
    }
}
