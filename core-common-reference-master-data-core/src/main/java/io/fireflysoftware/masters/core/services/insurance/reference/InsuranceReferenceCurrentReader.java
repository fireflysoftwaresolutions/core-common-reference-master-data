package io.fireflysoftware.masters.core.services.insurance.reference;

import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.Target;
import io.fireflysoftware.masters.models.entities.insurance.reference.InsuranceReferenceRows.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import reactor.core.publisher.Mono;
import java.time.Instant;

/** Technical mapped snapshot port; calling it does not replace verified caller/PDP authorization. */
public interface InsuranceReferenceCurrentReader {
    record Snapshot(Identity identity,Revision revision,ReferenceControl control,
            InsuranceReferenceCurrentNativeContext.Stamp stamp,R2dbcOperationBudget budget,Instant absoluteUntil) {
        @Override public String toString(){return "InsuranceReferenceCurrentSnapshot";}
    }
    Mono<Snapshot> read(Target target,SecurityPrincipal actor,R2dbcOperationBudget originalBudget,Instant originalUntil);
}
