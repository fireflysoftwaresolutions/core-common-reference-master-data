package io.fireflysoftware.masters.core.services.insurance.source;

import java.time.Instant;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Revision;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.Reference;
import reactor.core.publisher.Mono;

/** Real persisted exact-fact reader; construction or a SANDBOX label is not positive evidence. */
public interface InsuranceSandboxFactReader {
    record Observation(Reference source,Reference rights,Reference locale,String definitionDigest,
            Instant observedAt,Instant effectiveUntil,org.fireflyframework.core.transaction.R2dbcOperationBudget budget,org.fireflyframework.security.api.domain.SecurityPrincipal actor) {
        @Override public String toString(){return "InsuranceSandboxSourceObservation";}
    }
    Mono<Observation> observe(Revision expected);
}
