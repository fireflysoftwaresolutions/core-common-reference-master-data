package io.fireflysoftware.masters.core.services.currency.observation;

import java.time.Instant;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyView;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import reactor.core.publisher.Mono;

public interface CurrencyPolicyCurrentReader {
    record Snapshot(CurrencyPolicyView publication,CurrencyPolicyCurrentNativeContext.Stamp stamp,
            R2dbcOperationBudget budget,Instant absoluteUntil){
        @Override public String toString(){return "CurrencyPolicyCurrentSnapshot";}
    }
    Mono<Snapshot> read(Target target,SecurityPrincipal actor,CurrencyPolicyCurrentObservationService.OriginalBudget original);
}
