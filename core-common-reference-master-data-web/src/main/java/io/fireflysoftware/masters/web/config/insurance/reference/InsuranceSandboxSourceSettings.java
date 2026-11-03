package io.fireflysoftware.masters.web.config.insurance.reference;

import java.util.Set;

/** Selected-branch credentials for the separately administered sandbox catalog. */
record InsuranceSandboxSourceSettings(Set<String> issuers,InsuranceReferenceSettings.Connection writer,InsuranceReferenceSettings.Connection reader) {
    InsuranceSandboxSourceSettings {
        var validated=new InsuranceReferenceSettings(issuers,writer,reader);issuers=validated.issuers();
        if(!"masters_ins_ref_source_registrar".equals(writer.username())||!"masters_ins_ref_source_reader".equals(reader.username()))
            throw new IllegalArgumentException("INSURANCE_SANDBOX_SOURCE_CONFIGURATION_REQUIRED");
    }
    @Override public String toString(){return "InsuranceSandboxSourceSettings";}
}
