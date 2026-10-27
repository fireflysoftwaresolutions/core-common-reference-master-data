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

import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface CurrencyExecutionPolicyService {
    record PublicationOutcome(CurrencyPolicyView publication, boolean replayed) {}
    Mono<CurrencyPolicySourceObservation> observe(UUID currency);
    Mono<PublicationOutcome> publish(CurrencyPolicyPublishRequest request);
    Mono<CurrencyPolicyView> get(UUID policy, UUID currency);
    Mono<CurrencyPolicyView> byAction(UUID action, UUID currency);
    Mono<CurrencyPolicyView> retire(UUID policy, CurrencyPolicyRetireRequest request);
}
