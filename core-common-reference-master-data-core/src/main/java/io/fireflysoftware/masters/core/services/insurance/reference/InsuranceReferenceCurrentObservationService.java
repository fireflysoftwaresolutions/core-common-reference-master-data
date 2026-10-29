package io.fireflysoftware.masters.core.services.insurance.reference;

import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import reactor.core.publisher.Mono;
import java.time.Instant;

/** A current catalog observation is distinct from a historical action receipt. */
public interface InsuranceReferenceCurrentObservationService {
    /** Both values must be retained from the same original caller boundary, never reconstructed. */
    record OriginalBudget(R2dbcOperationBudget budget,Instant until) {
        public OriginalBudget { java.util.Objects.requireNonNull(budget);java.util.Objects.requireNonNull(until); }
        @Override public String toString(){return "InsuranceReferenceOriginalBudget";}
    }
    record Observation(Current value,R2dbcOperationBudget budget) {
        @Override public String toString(){return "InsuranceReferenceBoundedCurrentObservation";}
    }
    Mono<Observation> observe(Target target);
    Mono<Observation> observe(Target target,OriginalBudget original);
}
