package io.fireflysoftware.masters.core.services.insurance.source;

import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import reactor.core.publisher.Mono;

/** Current-authorized durable sandbox administration; never publication approval. */
public interface InsuranceSandboxFactRegistrationService {
    Mono<Receipt> registerLocale(RegisterLocale command);
    Mono<Receipt> registerRights(RegisterRights command);
    Mono<Receipt> registerSource(RegisterSource command);
    Mono<Receipt> withdraw(Withdraw command);
}
