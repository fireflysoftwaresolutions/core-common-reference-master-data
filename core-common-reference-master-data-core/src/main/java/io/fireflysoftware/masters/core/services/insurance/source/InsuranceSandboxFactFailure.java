package io.fireflysoftware.masters.core.services.insurance.source;

public final class InsuranceSandboxFactFailure extends IllegalStateException {
    public enum Kind { FORBIDDEN, CONFLICT, UNAVAILABLE }
    private final Kind kind;
    public InsuranceSandboxFactFailure(Kind kind){super("INSURANCE_SANDBOX_FACT_"+kind.name());this.kind=kind;}
    public Kind kind(){return kind;}
}
