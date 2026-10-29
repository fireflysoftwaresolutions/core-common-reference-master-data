package io.fireflysoftware.masters.core.services.insurance.reference;

/** Static public classification; underlying storage/provider details are never exposed. */
public final class InsuranceReferenceCurrentFailure extends IllegalStateException {
    public enum Kind { FORBIDDEN, NOT_CURRENT, UNAVAILABLE }
    private final Kind kind;
    public InsuranceReferenceCurrentFailure(Kind kind){super("INSURANCE_REFERENCE_CURRENT_"+kind.name());this.kind=kind;}
    public Kind kind(){return kind;}
}
