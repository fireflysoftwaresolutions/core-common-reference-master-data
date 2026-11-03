/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.common.reference.master.data.sdk;

import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.common.reference.master.data.sdk.model.DocumentReferenceCurrentTarget;
import org.junit.jupiter.api.Test;

/** No generated Java or YAML is supplied by this test; the original producer must emit both uses. */
class DocumentReferenceQuotePurposeGeneratedContractTest {
    @Test void originalGeneratedSdkPreservesDepositAndAddsOnlyExactQuotePurpose(){var deposit=DocumentReferenceCurrentTarget.PurposeEnum.valueOf("DEPOSIT_ACCOUNT_ENROLLMENT");var quote=DocumentReferenceCurrentTarget.PurposeEnum.valueOf("INSURANCE_TERM_PC_QUOTE");assertNotEquals(deposit,quote);assertEquals(java.util.Set.of("DEPOSIT_ACCOUNT_ENROLLMENT","INSURANCE_TERM_PC_QUOTE","CARD_ENROLLMENT"),java.util.Arrays.stream(DocumentReferenceCurrentTarget.PurposeEnum.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet()));}
    @Test void actualGeneratedCardPurposeSurvivesItsJsonWire()throws Exception{
        var mapper=io.fireflysoftware.common.reference.master.data.sdk.invoker.ApiClient.createDefaultMapper(null);
        var purpose=DocumentReferenceCurrentTarget.PurposeEnum.valueOf("CARD_ENROLLMENT");
        var wire=mapper.writeValueAsString(purpose);assertEquals("\"CARD_ENROLLMENT\"",wire);
        assertEquals(purpose,mapper.readValue(wire,DocumentReferenceCurrentTarget.PurposeEnum.class));
    }
    @Test void sdkCannotSubstitutePolicyIssuanceSigningOrProductPurposeNames(){for(var value:java.util.List.of("INSURANCE_POLICY_ISSUANCE","INSURANCE_CONTRACT_SIGNATURE","TERM_PC_INSURANCE_QUOTE"))assertThrows(IllegalArgumentException.class,()->DocumentReferenceCurrentTarget.PurposeEnum.valueOf(value));}
}
