package io.fireflysoftware.masters.core.services.insurance.reference;
import reactor.core.publisher.Mono;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCommands.*;
public interface InsuranceReferencePublicationService {
    Mono<Receipt> submitPublication(SubmitPublication command);
    Mono<Receipt> submitWithdrawal(SubmitWithdrawal command);
    Mono<Receipt> recover(Recovery command);
    Mono<Receipt> approve(IntentAction command);
    Mono<Receipt> execute(IntentAction command);
    Mono<Receipt> withdrawIntent(IntentAction command);
}
