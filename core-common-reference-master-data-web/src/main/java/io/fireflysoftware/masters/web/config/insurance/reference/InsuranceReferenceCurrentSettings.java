package io.fireflysoftware.masters.web.config.insurance.reference;

import java.net.URI;
import java.util.*;

/** CURRENT binds only its reader credentials and trust, never command/writer settings. */
record InsuranceReferenceCurrentSettings(Set<String> issuers,InsuranceReferenceSettings.Connection reader) {
    InsuranceReferenceCurrentSettings {
        issuers=Set.copyOf(issuers);Objects.requireNonNull(reader);
        if(issuers.isEmpty()||issuers.size()>16)throw new IllegalArgumentException("INSURANCE_REFERENCE_CURRENT_CONFIGURATION_REQUIRED");
        for(String issuer:issuers){var uri=URI.create(issuer);
            if(issuer.length()>500||!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getRawUserInfo()!=null||uri.getRawFragment()!=null||uri.getRawQuery()!=null)
                throw new IllegalArgumentException("INSURANCE_REFERENCE_CURRENT_CONFIGURATION_REQUIRED");}
    }
    @Override public String toString(){return "InsuranceReferenceCurrentSettings";}
}
