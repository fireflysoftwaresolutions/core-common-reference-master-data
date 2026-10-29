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

import io.fireflysoftware.masters.models.repositories.currency.policy.CurrencyPolicyTechnicalRepository;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import java.util.*;

@Component
public class CurrencyPolicyDatabaseContext {
    private final R2dbcEntityTemplate operations;
    private final CurrencyPolicyTechnicalRepository technical;
    public CurrencyPolicyDatabaseContext(R2dbcEntityTemplate operations) {
        this.operations=Objects.requireNonNull(operations);
        this.technical=new R2dbcRepositoryFactory(operations).getRepository(CurrencyPolicyTechnicalRepository.class);
    }
    public boolean usesOperations(R2dbcEntityTemplate value) { return operations==value; }
    public Mono<Void> establish(SecurityPrincipal principal,UUID currency,String purpose) {
        return Mono.defer(()->{
            CurrencyPolicyAuthority.requireCurrent(principal);
            if(currency==null||!Set.of("READ","PUBLISH","RETIRE").contains(purpose))
                return Mono.error(new IllegalStateException("Server currency policy context required"));
            return org.springframework.transaction.reactive.TransactionSynchronizationManager.forCurrentTransaction()
                    .filter(org.springframework.transaction.reactive.TransactionSynchronizationManager::isActualTransactionActive)
                    .switchIfEmpty(Mono.error(new IllegalStateException("Native currency transaction required")))
                    .then(technical.runtimeAllowed().filter(Boolean.TRUE::equals)
                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                    "Nonprivileged scoped currency policy runtime required"))))
                    .then(technical.establish(currency.toString(),purpose)).then();
        });
    }
}
