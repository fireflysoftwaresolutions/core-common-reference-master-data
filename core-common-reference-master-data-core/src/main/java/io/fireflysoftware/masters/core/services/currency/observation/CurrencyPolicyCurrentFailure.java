package io.fireflysoftware.masters.core.services.currency.observation;

public final class CurrencyPolicyCurrentFailure extends RuntimeException {
    public enum Kind { FORBIDDEN,NOT_CURRENT,UNAVAILABLE }
    private final Kind kind;
    public CurrencyPolicyCurrentFailure(Kind kind){super(switch(kind){
        case FORBIDDEN->"CURRENCY_POLICY_CURRENT_FORBIDDEN";
        case NOT_CURRENT->"CURRENCY_POLICY_NOT_CURRENT";
        case UNAVAILABLE->"CURRENCY_POLICY_CURRENT_UNAVAILABLE";});this.kind=kind;}
    public Kind kind(){return kind;}
}
