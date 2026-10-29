/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;

import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.masters.core.services.document.observation.impl.DocumentReferenceCurrentWindow;

import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.Target;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.junit.jupiter.api.Test;

class DocumentReferenceFiniteWindowTest {
    static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    static final UUID NONCE=UUID.fromString("10000000-0000-4000-8000-000000000001");
    static final Target TARGET=new DocumentReferenceCurrentServiceTest().target();
    final Clock clock=Clock.fixed(NOW,ZoneOffset.UTC);
    final AtomicLong nanos=new AtomicLong();
    final java.util.function.LongSupplier ticker=nanos::get;
    DocumentReferenceCurrentService.OriginalBudget original(){
        return new DocumentReferenceCurrentService.OriginalBudget(
                R2dbcOperationBudget.observed(NOW,NOW.plusSeconds(2),0,Duration.ofSeconds(2),clock,ticker),NOW.plusSeconds(2));
    }
    DocumentReferenceCurrentNativeContext.Stamp stamp(long millis){
        return new DocumentReferenceCurrentNativeContext.Stamp(NONCE,TARGET,NOW.toEpochMilli(),NOW,NOW.plusMillis(millis));
    }
    @Test void actualNativeCutoffIsRetainedWithFloorQuantumCharged(){
        var value=DocumentReferenceCurrentWindow.admit(original(),stamp(50),NONCE,TARGET,NOW,0,clock,ticker);
        assertEquals(NOW,value.recordedAt());assertFalse(value.until().isAfter(NOW.plusMillis(49)));
        assertTrue(value.budget().remaining().compareTo(Duration.ofMillis(49))<=0);
        nanos.set(Duration.ofMillis(49).toNanos());assertThrows(RuntimeException.class,()->value.budget().remaining());
    }
    @Test void ioElapsedBeforeStampCannotRenewTheOriginalNativeBudget(){
        var retained=original();nanos.set(Duration.ofMillis(40).toNanos());
        var value=DocumentReferenceCurrentWindow.admit(retained,stamp(50),NONCE,TARGET,NOW,0,clock,ticker);
        assertTrue(value.budget().remaining().compareTo(Duration.ofMillis(9))<=0);
    }
    @Test void exhaustedQuantumAndUnscopedStampRefuse(){
        assertThrows(RuntimeException.class,()->DocumentReferenceCurrentWindow.admit(original(),stamp(1),NONCE,TARGET,NOW,0,clock,ticker));
        assertThrows(RuntimeException.class,()->DocumentReferenceCurrentWindow.admit(original(),stamp(50),UUID.randomUUID(),TARGET,NOW,0,clock,ticker));
    }
    @Test void nativeAbsoluteCapCannotExceedTheRetainedOriginalCap(){
        assertThrows(RuntimeException.class,()->DocumentReferenceCurrentWindow.admit(original(),stamp(2500),NONCE,TARGET,NOW,0,clock,ticker));
    }
    @Test void incompatibleOriginalTimeSourcesRefuseInsteadOfReconstruction(){
        assertThrows(IllegalArgumentException.class,()->DocumentReferenceCurrentWindow.admit(original(),stamp(50),NONCE,TARGET,NOW,0,clock,()->nanos.get()));
    }
}
