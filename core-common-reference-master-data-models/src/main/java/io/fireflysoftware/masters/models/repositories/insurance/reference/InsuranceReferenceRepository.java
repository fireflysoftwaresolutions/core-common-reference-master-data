package io.fireflysoftware.masters.models.repositories.insurance.reference;

import java.util.UUID;
import reactor.core.publisher.Mono;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;
import io.fireflysoftware.masters.models.entities.insurance.reference.InsuranceReferenceRows.*;

/** Narrow mapped persistence; callers must establish the qualified native transaction and authority. */
public interface InsuranceReferenceRepository {
    Mono<Identity> identity(UUID recordId,Kind kind);
    Mono<Revision> revision(UUID recordId,Kind kind,UUID publicationId,long revision);
    Mono<ReferenceControl> control(UUID recordId,Kind kind);
    Mono<MakerIntent> intent(UUID recordId,Kind kind,UUID intentId);
    Mono<IntentControl> intentControl(UUID recordId,Kind kind,UUID intentId);
    Mono<CheckerApproval> approval(UUID recordId,Kind kind,UUID intentId,UUID approvalId,String intentDigest);
    Mono<Action> action(UUID recordId,Kind kind,UUID actionId);
    Mono<Audit> audit(UUID recordId,Kind kind,UUID actionId);
    Mono<Outbox> outbox(UUID recordId,Kind kind,UUID actionId);
    Mono<Long> compareAndSetControl(ReferenceControl expected,ReferenceControl replacement);
    Mono<Long> compareAndSetIntent(IntentControl expected,IntentControl replacement);
    Mono<Identity> appendIdentity(Identity row);
    Mono<Revision> appendRevision(Revision row);
    Mono<ReferenceControl> appendReferenceControl(ReferenceControl row);
    Mono<MakerIntent> appendMakerIntent(MakerIntent row);
    Mono<IntentControl> appendIntentControl(IntentControl row);
    Mono<CheckerApproval> appendCheckerApproval(CheckerApproval row);
    Mono<Action> appendAction(Action row);
    Mono<Audit> appendAudit(Audit row);
    Mono<Outbox> appendOutbox(Outbox row);
}
