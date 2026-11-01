/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.common.reference.master.data.sdk;
import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.common.reference.master.data.sdk.model.Revision;
import java.time.Instant;
import org.junit.jupiter.api.Test;
class InsuranceReferenceAbsoluteTimeGeneratedContractTest {
    @Test void genuineRevisionSdkPreservesAbsoluteEffectivityType()throws Exception{assertEquals(Instant.class,Revision.class.getMethod("getEffectiveFrom").getReturnType());assertEquals(Instant.class,Revision.class.getMethod("getEffectiveUntil").getReturnType());}
}
