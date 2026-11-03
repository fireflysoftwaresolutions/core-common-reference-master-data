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

package io.fireflysoftware.masters.models.repositories.currency.policy;

import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.v1.CurrencyDTO;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface CurrencyPolicyRepository {
    public record Action(
            UUID actionId,
            UUID currencyId,
            UUID policyId,
            String kind,
            String canonical,
            String digest) {}

    public record Binding(UUID currencyId, String isoCode, long headVersion) {}

    Mono<Long> now();
    Mono<Void> actionGate(UUID action);
    Mono<Void> gates(UUID currency, String iso);
    Mono<CurrencyDTO> currency(UUID currency, boolean lock);
    Mono<Binding> binding(UUID currency);
    Mono<Void> ensureBinding(UUID currency, String iso);
    Mono<Long> advance(UUID currency, long expected);
    Mono<Action> action(UUID action, UUID currency);
    Mono<Long> claim(Action action);
    Mono<CurrencyPolicyView> find(UUID policy, UUID currency);
    Mono<Void> insert(UUID policy, CurrencyPolicyPublishRequest request, long version,
            String encoding, String digest, String source, String sourceDigest,
            long now, String actor, String issuer);
    Mono<Long> retire(UUID policy, UUID currency, UUID action, long now, String actor, String reason);
    Mono<Void> event(UUID policy, UUID currency, UUID action, String kind, long now,
            String actor, String issuer, String payload);
    boolean usesOperations(org.springframework.data.r2dbc.core.R2dbcEntityTemplate operations);
}
