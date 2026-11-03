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

import static org.assertj.core.api.Assertions.*;

import org.fireflyframework.security.api.domain.*;
import org.fireflyframework.security.spi.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

class CurrencyPolicyAuthorityTest {
    static SecurityPrincipal principal(Object expiry) {
        return SecurityPrincipal.builder()
                .subject("synthetic-author")
                .issuer("https://synthetic.invalid")
                .claims(expiry == null ? Map.of() : Map.of("exp", expiry))
                .build();
    }

    static CurrencyPolicyAuthority authority(SecurityPrincipal p, PolicyDecisionPort pdp) {
        var f = new StaticListableBeanFactory();
        if (p != null) f.addBean("context", (SecurityContextPort) () -> Mono.just(p));
        if (pdp != null) f.addBean("policy", pdp);
        return new CurrencyPolicyAuthority(
                f.getBeanProvider(SecurityContextPort.class),
                f.getBeanProvider(PolicyDecisionPort.class),
                new org.springframework.mock.env.MockEnvironment()
                        .withProperty(CurrencyPolicyAuthority.STRICT_POLICY, "true"));
    }

    static void denied(CurrencyPolicyAuthority a) {
        assertThatThrownBy(
                        () ->
                                a.authorize(
                                                "publish",
                                                "12345678-1234-4234-8234-123456789abc",
                                                Map.of())
                                        .block())
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(403));
    }

    @Test
    void absentExpiredAndUnprovedNumericExpiryFailClosed() {
        for (Object exp :
                new Object[] {null, Instant.now().minusSeconds(1), Instant.MAX, 1L, "tomorrow"})
            denied(authority(principal(exp), (p, a, r, c) -> Mono.just(Decision.permit())));
        denied(authority(null, null));
        denied(authority(principal(Instant.now().plusSeconds(60)), null));
    }

    @Test
    void exactActionCurrencyAndGlobalScopeReachCurrentPolicyEveryTime() {
        var revoked = new AtomicBoolean();
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        var a =
                authority(
                        principal(Instant.now().plusSeconds(60)),
                        (p, action, r, c) -> {
                            calls.incrementAndGet();
                            assertThat(action).isEqualTo("masters.currency-policy.publish");
                            assertThat(r)
                                    .isEqualTo("currency/12345678-1234-4234-8234-123456789abc");
                            assertThat(c).containsEntry("scope", "GLOBAL_MASTER");
                            return Mono.just(
                                    revoked.get()
                                            ? Decision.deny("synthetic revoked")
                                            : Decision.permit());
                        });
        assertThat(a.authorize("publish", "12345678-1234-4234-8234-123456789abc", Map.of()).block())
                .isNotNull();
        revoked.set(true);
        denied(a);
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void emptyOrUnavailablePolicyDoesNotBecomePermission() {
        denied(authority(principal(Instant.now().plusSeconds(60)), (p, a, r, c) -> Mono.empty()));
        denied(
                authority(
                        principal(Instant.now().plusSeconds(60)),
                        (p, a, r, c) -> Mono.error(new IllegalStateException("private error"))));
    }

    @Test
    void missingOrDisabledStrictModeCannotUseAPermittingProjectPolicy() {
        for (String configured : new String[] {null, "false"}) {
            var env = new org.springframework.mock.env.MockEnvironment();
            if (configured != null)
                env.withProperty(CurrencyPolicyAuthority.STRICT_POLICY, configured);
            var beans = new StaticListableBeanFactory();
            beans.addBean(
                    "context",
                    (SecurityContextPort)
                            () -> Mono.just(principal(Instant.now().plusSeconds(60))));
            var called = new AtomicBoolean();
            beans.addBean(
                    "policy",
                    (PolicyDecisionPort)
                            (p, a, r, c) -> {
                                called.set(true);
                                return Mono.just(Decision.permit());
                            });
            denied(
                    new CurrencyPolicyAuthority(
                            beans.getBeanProvider(SecurityContextPort.class),
                            beans.getBeanProvider(PolicyDecisionPort.class),
                            env));
            assertThat(called).isFalse();
        }
    }

    @Test
    void unmetObligationsAndIndeterminatePolicyAreDenied() {
        denied(
                authority(
                        principal(Instant.now().plusSeconds(60)),
                        (p, a, r, c) ->
                                Mono.just(
                                        Decision.permit(
                                                List.of(
                                                        Obligation.of(
                                                                "mask",
                                                                Map.of("field", "source")))))));
        denied(
                authority(
                        principal(Instant.now().plusSeconds(60)),
                        (p, a, r, c) ->
                                Mono.just(Decision.indeterminate("Unavailable authority"))));
    }

    @Test
    void trustedPrincipalTextMustRemainExactScalarUnicode() {
        for (String subject :
                new String[] {"synthetic-\uD800", "synthetic-\uDC00", "synthetic-\u0000"}) {
            var p =
                    SecurityPrincipal.builder()
                            .subject(subject)
                            .issuer("https://synthetic.invalid")
                            .claims(Map.of("exp", Instant.now().plusSeconds(60)))
                            .build();
            denied(
                    authority(
                            p, (actor, action, resource, context) -> Mono.just(Decision.permit())));
        }
        var valid =
                SecurityPrincipal.builder()
                        .subject("synthetic-Ω-\uD83D\uDE42")
                        .issuer("https://synthetic.invalid")
                        .claims(Map.of("exp", Instant.now().plusSeconds(60)))
                        .build();
        assertThat(
                        authority(valid, (p, a, r, c) -> Mono.just(Decision.permit()))
                                .authorize("read", "12345678-1234-4234-8234-123456789abc", Map.of())
                                .block())
                .isEqualTo(valid);
    }
}
