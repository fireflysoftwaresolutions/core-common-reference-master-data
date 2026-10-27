/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.currency.observation;

import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyReference;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyView;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import reactor.core.publisher.Mono;

/** Exact representation-policy observation; not money, tax applicability or financial authority. */
public interface CurrencyPolicyCurrentObservationService {
    record Observation(CurrencyPolicyView policy, R2dbcOperationBudget budget) {
        @Override public String toString() { return "CurrencyPolicyCurrentObservation"; }
    }

    Mono<Observation> observe(CurrencyPolicyReference expected, int postedScale,
            R2dbcOperationBudget originalBudget);
}
