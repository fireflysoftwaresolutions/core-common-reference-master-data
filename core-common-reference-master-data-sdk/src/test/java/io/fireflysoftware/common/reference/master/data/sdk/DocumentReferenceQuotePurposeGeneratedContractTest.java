/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.common.reference.master.data.sdk;

import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.common.reference.master.data.sdk.model.DocumentReferenceCurrentTarget;
import org.junit.jupiter.api.Test;

/** No generated Java or YAML is supplied by this test; the original producer must emit both uses. */
class DocumentReferenceQuotePurposeGeneratedContractTest {
    @Test void originalGeneratedSdkPreservesDepositAndAddsOnlyExactQuotePurpose(){var deposit=DocumentReferenceCurrentTarget.PurposeEnum.valueOf("DEPOSIT_ACCOUNT_ENROLLMENT");var quote=DocumentReferenceCurrentTarget.PurposeEnum.valueOf("INSURANCE_TERM_PC_QUOTE");assertNotEquals(deposit,quote);assertEquals(2,DocumentReferenceCurrentTarget.PurposeEnum.values().length);}
    @Test void sdkCannotSubstitutePolicyIssuanceSigningOrProductPurposeNames(){for(var value:java.util.List.of("INSURANCE_POLICY_ISSUANCE","INSURANCE_CONTRACT_SIGNATURE","TERM_PC_INSURANCE_QUOTE"))assertThrows(IllegalArgumentException.class,()->DocumentReferenceCurrentTarget.PurposeEnum.valueOf(value));}
}
