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

package io.fireflysoftware.qualification.currency;

import org.fireflyframework.security.api.domain.*;
import org.fireflyframework.security.spi.*;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.*;
import org.springframework.web.server.WebFilter;

import reactor.core.publisher.Mono;

import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Private explicit opaque-credential authentication/PDP deployment; never packaged in production.
 */
public final class CurrencyPolicyRuntimeBootstrap {
    private CurrencyPolicyRuntimeBootstrap() {}

    public static void main(String[] args) throws Exception {
        var sources = new ArrayList<Class<?>>();
        sources.add(Class.forName("io.fireflysoftware.masters.web.MasterDataApplication"));
        sources.add(Fixture.class);
        if ("true".equals(System.getenv("CURRENCY_FIXTURE_FRAMEWORK_FALLBACK"))) {
            sources.add(
                    Class.forName(
                            "io.fireflysoftware.qualification.currency.CurrencyPolicyFrameworkFallbackConfiguration"));
        }
        new SpringApplicationBuilder(sources.toArray(Class<?>[]::new))
                .web(WebApplicationType.REACTIVE)
                .run(args);
    }

    @Configuration(proxyBeanMethods = false)
    static class Fixture {
        static String required(String key) {
            String v = System.getenv(key);
            if (v == null || v.isBlank())
                throw new IllegalStateException("Private currency fixture configuration required");
            return v;
        }

        @Bean
        SecurityContextPort currencyFixtureContext() {
            return () ->
                    Mono.deferContextual(
                            c ->
                                    Mono.justOrEmpty(
                                            c.<SecurityPrincipal>getOrEmpty(
                                                    SecurityPrincipal.class)));
        }

        @Bean
        org.fireflyframework.web.idempotency.scope.IdempotencyScopeResolver
                currencyFixtureCacheScope(SecurityContextPort context) {
            return e ->
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
        @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
                name = "fixture.currency.policy.enabled",
                matchIfMissing = true)
        PolicyDecisionPort currencyFixturePolicy() {
            var resource = "currency/" + required("CURRENCY_FIXTURE_RESOURCE");
            var policyFile = Path.of(required("CURRENCY_FIXTURE_POLICY_FILE"));
            return (p, action, r, c) ->
                    Mono.defer(
                            () -> {
                                try {
                                    if (!resource.equals(r)
                                            || !"GLOBAL_MASTER".equals(c.get("scope"))
                                            || !p.hasScope("currency.policy.fixture")
                                            || !Set.of(
                                                            "masters.currency-policy.source.read",
                                                            "masters.currency-policy.read",
                                                            "masters.currency-policy.publish",
                                                            "masters.currency-policy.retire")
                                                    .contains(action)
                                            || !Files.readString(policyFile).equals("PERMIT"))
                                        return Mono.just(
                                                Decision.deny(
                                                        "Exact private current currency policy"
                                                                + " denied"));
                                    if ((action.endsWith(".publish") || action.endsWith(".retire"))
                                            && (!(c.get("intentDigest") instanceof String d)
                                                    || !d.matches("[0-9a-f]{64}")))
                                        return Mono.just(
                                                Decision.deny("Exact immutable intent required"));
                                    return org.springframework.transaction.reactive
                                            .TransactionSynchronizationManager
                                            .forCurrentTransaction()
                                            .map(
                                                    tx ->
                                                            Decision.deny(
                                                                    "PDP cannot run inside owner"
                                                                            + " SQL transaction"))
                                            .onErrorResume(
                                                    org.springframework.transaction
                                                            .NoTransactionException.class,
                                                    e -> Mono.just(Decision.permit()));
                                } catch (Exception e) {
                                    return Mono.just(Decision.deny("Private policy unavailable"));
                                }
                            });
        }

        @Bean
        @org.springframework.core.annotation.Order(-90)
        WebFilter currencyFixtureAuthentication() {
            String token = required("CURRENCY_FIXTURE_CREDENTIAL");
            if (token.length() < 32)
                throw new IllegalStateException("Private high-entropy credential required");
            Instant expiry = Instant.parse(required("CURRENCY_FIXTURE_EXPIRY"));
            return (exchange, chain) -> {
                String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
                boolean normal = ("Bearer " + token).equals(auth),
                        expired = ("Bearer " + token + "-expired").equals(auth),
                        missing = ("Bearer " + token + "-missing-exp").equals(auth);
                if (!normal && !expired && !missing) return chain.filter(exchange);
                var principal =
                        SecurityPrincipal.builder()
                                .subject("synthetic-currency-author")
                                .issuer("https://synthetic-currency.invalid")
                                .scopes(Set.of("currency.policy.fixture"))
                                .claims(
                                        missing
                                                ? Map.of()
                                                : Map.of("exp", expired ? Instant.EPOCH : expiry))
                                .build();
                return chain.filter(exchange)
                        .contextWrite(c -> c.put(SecurityPrincipal.class, principal));
            };
        }
    }
}
