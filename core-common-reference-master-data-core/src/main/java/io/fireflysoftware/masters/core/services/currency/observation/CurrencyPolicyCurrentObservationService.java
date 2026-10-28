/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.currency.observation;

import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyReference;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyView;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.*;
import java.time.Instant;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import reactor.core.publisher.Mono;

/** Exact representation-policy observation; not money, tax applicability or financial authority. */
public interface CurrencyPolicyCurrentObservationService {
    @FunctionalInterface
    interface Factory {
        CurrencyPolicyCurrentObservationService create(java.time.Clock clock,java.util.function.LongSupplier nanos);
    }
    /** Both values are retained from the same trusted original caller boundary. */
    record OriginalBudget(R2dbcOperationBudget budget,Instant until) {
        public OriginalBudget{java.util.Objects.requireNonNull(budget);java.util.Objects.requireNonNull(until);}
        @Override public String toString(){return "CurrencyPolicyOriginalBudget";}
    }
    record CurrentObservation(Current value,R2dbcOperationBudget budget,Instant absoluteUntil) {
        @Override public String toString(){return "CurrencyPolicyFiniteCurrentObservation";}
    }
    record Observation(CurrencyPolicyView policy, R2dbcOperationBudget budget) {
        @Override public String toString() { return "CurrencyPolicyCurrentObservation"; }
    }

    Mono<Observation> observe(CurrencyPolicyReference expected, int postedScale,
            R2dbcOperationBudget originalBudget);
    default Mono<CurrentObservation> observeCurrent(Target expected){return Mono.error(new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE));}
    default Mono<CurrentObservation> observeCurrent(Target expected,OriginalBudget original){return Mono.error(new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE));}
    static CurrencyPolicyCurrentObservationService unavailable(){return (expected,scale,budget)->Mono.error(new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE));}
    static Factory unavailableFactory(){return (clock,nanos)->unavailable();}
}
