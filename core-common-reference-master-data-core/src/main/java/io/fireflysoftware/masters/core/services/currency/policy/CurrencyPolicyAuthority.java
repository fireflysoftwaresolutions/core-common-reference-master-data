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

package io.fireflysoftware.masters.core.services.currency.policy;

import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.spi.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Instant;
import java.util.*;

@Component
public class CurrencyPolicyAuthority {
    public static final String STRICT_POLICY = "firefly.security.resource-server.strict-policy";
    private final org.springframework.core.env.Environment environment;
    private final ObjectProvider<SecurityContextPort> contexts;
    private final ObjectProvider<PolicyDecisionPort> policies;

    public CurrencyPolicyAuthority(
            ObjectProvider<SecurityContextPort> contexts,
            ObjectProvider<PolicyDecisionPort> policies,
            org.springframework.core.env.Environment environment) {
        this.environment = environment;
        this.contexts = contexts;
        this.policies = policies;
    }

    public Mono<SecurityPrincipal> authorize(
            String operation, String currencyId, Map<String, Object> safeContext) {
        return Mono.defer(
                        () -> {
                            if (!Boolean.TRUE.equals(
                                    environment.getProperty(STRICT_POLICY, Boolean.class)))
                                return Mono.error(forbidden());
                            CurrencyPolicyRules.uuid(currencyId);
                            if (!Set.of("source.read", "publish", "read", "retire", "current")
                                    .contains(operation)) return Mono.error(forbidden());
                            var context = contexts.getIfAvailable();
                            var policy = policies.getIfAvailable();
                            if (context == null || policy == null) return Mono.error(forbidden());
                            var values = new HashMap<String, Object>(safeContext);
                            values.put("scope", "GLOBAL_MASTER");
                            values.put("currencyId", currencyId);
                            return context.currentPrincipal()
                                    .flatMap(
                                            p -> {
                                                requireCurrent(p);
                                                return policy.authorize(
                                                                p,
                                                                "masters.currency-policy."
                                                                        + operation,
                                                                "currency/" + currencyId,
                                                                Map.copyOf(values))
                                                        .filter(
                                                                d ->
                                                                        d.granted()
                                                                                && d.obligations()
                                                                                        .isEmpty())
                                                        .map(
                                                                d -> {
                                                                    requireCurrent(p);
                                                                    return p;
                                                                });
                                            })
                                    .switchIfEmpty(Mono.error(forbidden()));
                        })
                .onErrorMap(e -> forbidden());
    }

    public static void requireCurrent(SecurityPrincipal p) {
        try {
            if (p == null
                    || p.subject() == null
                    || p.subject().isBlank()
                    || p.subject().length() > 255
                    || p.issuer() == null
                    || p.issuer().length() > 500) throw forbidden();
            CurrencyPolicyRules.text(p.subject(), 255);
            CurrencyPolicyRules.text(p.issuer(), 500);
            var issuer = URI.create(p.issuer());
            if (!issuer.isAbsolute()
                    || !Set.of("https", "http").contains(issuer.getScheme())
                    || issuer.getHost() == null) throw forbidden();
            if (!(p.claims().get("exp") instanceof Instant exp)
                    || !exp.isAfter(Instant.now())
                    || exp.toEpochMilli() > CurrencyPolicyRules.MAX_TIME) throw forbidden();
        } catch (RuntimeException e) {
            throw forbidden();
        }
    }

    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(
                HttpStatus.FORBIDDEN, "Current trusted currency policy permission required");
    }
}
