package io.fireflysoftware.masters.core.services.insurance.reference;
import java.util.UUID;
/** Payload-free technical facts; neither rollback nor no-effect is inferred from a missing response. */
public final class InsuranceReferenceOutcome extends IllegalStateException {
    private final UUID actionId;
    private final boolean commitAcknowledged,boundaryCompleted,rollbackAcknowledged;
    public InsuranceReferenceOutcome(UUID actionId,boolean commitAcknowledged,boolean boundaryCompleted,boolean rollbackAcknowledged){super("INSURANCE_REFERENCE_PROTECTED_RESULT_UNAVAILABLE");this.actionId=actionId;this.commitAcknowledged=commitAcknowledged;this.boundaryCompleted=boundaryCompleted;this.rollbackAcknowledged=rollbackAcknowledged;}
    public UUID actionId(){return actionId;}
    public boolean commitAcknowledged(){return commitAcknowledged;}
    public boolean boundaryCompleted(){return boundaryCompleted;}
    public boolean rollbackAcknowledged(){return rollbackAcknowledged;}
}
