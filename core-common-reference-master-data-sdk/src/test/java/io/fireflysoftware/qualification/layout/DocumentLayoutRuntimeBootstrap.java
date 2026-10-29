/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.qualification.layout;

import io.fireflysoftware.masters.core.services.document.layout.v1.*;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;

import org.fireflyframework.security.api.domain.*;
import org.fireflyframework.security.spi.*;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.*;
import org.springframework.web.server.WebFilter;

import reactor.core.publisher.Mono;

import java.util.*;

/** Explicit private integration deployment; never packaged in the production executable. */
public final class DocumentLayoutRuntimeBootstrap {
    private DocumentLayoutRuntimeBootstrap() {}

    public static void main(String[] args) throws Exception {
        new SpringApplicationBuilder(
                        Class.forName("io.fireflysoftware.masters.web.MasterDataApplication"),
                        Fixture.class)
                .web(WebApplicationType.REACTIVE)
                .run(args);
    }

    @Configuration(proxyBeanMethods = false)
    static class Fixture {
        private static String credential() {
            String value = System.getenv("DOCUMENT_LAYOUT_FIXTURE_CREDENTIAL");
            if (value == null || value.length() < 32)
                throw new IllegalStateException("Private fixture credential is required");
            return value;
        }

        @Bean
        CacheControl layoutFixtureCacheControl() {
            return new CacheControl();
        }

        @Bean
        SecurityContextPort layoutFixtureContext() {
            return () ->
                    Mono.deferContextual(
                            c ->
                                    Mono.justOrEmpty(
                                            c.<SecurityPrincipal>getOrEmpty(
                                                    SecurityPrincipal.class)));
        }

        @Bean
        org.fireflyframework.web.idempotency.scope.IdempotencyScopeResolver layoutFixtureCacheScope(
                SecurityContextPort context) {
            return exchange ->
                    context.currentPrincipal()
                            .map(
                                    p ->
                                            "GLOBAL_MASTER:"
                                                    + p.issuer()
                                                    + ":"
                                                    + p.subject()
                                                    + ":"
                                                    + String.join(
                                                            ",",
                                                            p.scopes().stream().sorted().toList()));
        }

        @Bean
        PolicyDecisionPort layoutFixturePolicy() {
            return (p, a, r, c) -> {
                if (!"GLOBAL_MASTER".equals(c.get("scope")))
                    return Mono.just(Decision.deny("Fixture context required"));
                boolean declared =
                        Set.of(
                                        "masters.document-template.layout-observe",
                                        "masters.document-template.layout-publish",
                                        "masters.document-template.layout-read",
                                        "masters.document-template.layout-resolve",
                                        "masters.document-template.layout-retire",
                                        "masters.document-template.publish",
                                        "masters.document-template.read",
                                        "masters.document-template.resolve",
                                        "masters.document-template.retire",
                                        "masters.document-template.draft.write")
                                .contains(a);
                if (!declared
                        || !p.hasScope("layout.fixture")
                        || Boolean.TRUE.equals(p.claim("fixtureCurrentDenied", Boolean.class)))
                    return Mono.just(Decision.deny("Fixture current policy denied"));
                if (!a.startsWith("masters.document-template.layout-"))
                    return Mono.just(Decision.permit());
                return org.springframework.transaction.reactive.TransactionSynchronizationManager
                        .forCurrentTransaction()
                        .map(tx -> Decision.deny("Fixture policy cannot run in SQL transaction"))
                        .onErrorResume(
                                org.springframework.transaction.NoTransactionException.class,
                                e -> Mono.just(Decision.permit()));
            };
        }

        @Bean
        @org.springframework.core.annotation.Order(-90)
        WebFilter layoutFixtureAuthentication() {
            String secret = credential();
            return (exchange, chain) -> {
                String authorization = exchange.getRequest().getHeaders().getFirst("Authorization");
                boolean normal = ("Bearer " + secret).equals(authorization),
                        denied = ("Bearer " + secret + "-denied").equals(authorization);
                if (!normal && !denied) return chain.filter(exchange);
                var principal =
                        SecurityPrincipal.builder()
                                .subject("synthetic-layout-admin")
                                .issuer("synthetic-layout-fixture")
                                .scopes(Set.of("layout.fixture"))
                                .claims(Map.of("fixtureCurrentDenied", denied))
                                .build();
                return chain.filter(exchange)
                        .contextWrite(c -> c.put(SecurityPrincipal.class, principal));
            };
        }
    }

    @io.swagger.v3.oas.annotations.Hidden
    @org.springframework.web.bind.annotation.RestController
    static class CacheControl {
        private final java.util.concurrent.atomic.AtomicInteger invocations =
                new java.util.concurrent.atomic.AtomicInteger();

        @org.springframework.web.bind.annotation.PostMapping(
                value = "/synthetic/layout-cache-control",
                consumes = "application/json",
                produces = "application/json")
        public Mono<Map<String, Integer>> count(
                @org.springframework.web.bind.annotation.RequestBody String body) {
            return Mono.fromSupplier(() -> Map.of("invocations", invocations.incrementAndGet()));
        }
    }
}
