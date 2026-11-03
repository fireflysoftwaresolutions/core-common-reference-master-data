package io.fireflysoftware.masters.core.services.currency.observation;

import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.masters.core.services.currency.observation.impl.CurrencyPolicyCurrentWindow;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.junit.jupiter.api.Test;

class CurrencyPolicyFiniteWindowTest {
    static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    static final UUID NONCE=UUID.fromString("10000000-0000-4000-8000-000000000001");
    static final Target TARGET=new Target(new CurrencyPolicyReference("10000000-0000-4000-8000-000000000002",
            "10000000-0000-4000-8000-000000000003","USD",1L,"a".repeat(64),"MASTERS_CURRENCY_POLICY_V1"),2);
    final Clock clock=Clock.fixed(NOW,ZoneOffset.UTC);
    final AtomicLong nanos=new AtomicLong();
    final java.util.function.LongSupplier ticker=nanos::get;
    CurrencyPolicyCurrentObservationService.OriginalBudget original(){
        return new CurrencyPolicyCurrentObservationService.OriginalBudget(
                R2dbcOperationBudget.observed(NOW,NOW.plusSeconds(2),0,Duration.ofSeconds(2),clock,ticker),NOW.plusSeconds(2));
    }
    CurrencyPolicyCurrentNativeContext.Stamp stamp(long millis){
        return new CurrencyPolicyCurrentNativeContext.Stamp(NONCE,TARGET,NOW.toEpochMilli(),NOW,NOW.plusMillis(millis));
    }
    @Test void actualNativeCutoffIsRetainedWithFloorQuantumCharged(){
        var value=CurrencyPolicyCurrentWindow.admit(original(),stamp(50),NONCE,TARGET,NOW,0,clock,ticker);
        assertEquals(NOW,value.recordedAt());assertFalse(value.until().isAfter(NOW.plusMillis(49)));
        assertTrue(value.budget().remaining().compareTo(Duration.ofMillis(49))<=0);
        nanos.set(Duration.ofMillis(49).toNanos());assertThrows(RuntimeException.class,()->value.budget().remaining());
    }
    @Test void ioElapsedBeforeStampCannotRenewTheOriginalNativeBudget(){
        var retained=original();nanos.set(Duration.ofMillis(40).toNanos());
        var value=CurrencyPolicyCurrentWindow.admit(retained,stamp(50),NONCE,TARGET,NOW,0,clock,ticker);
        assertTrue(value.budget().remaining().compareTo(Duration.ofMillis(9))<=0);
    }
    @Test void exhaustedQuantumAndUnscopedStampRefuse(){
        assertThrows(RuntimeException.class,()->CurrencyPolicyCurrentWindow.admit(original(),stamp(1),NONCE,TARGET,NOW,0,clock,ticker));
        assertThrows(RuntimeException.class,()->CurrencyPolicyCurrentWindow.admit(original(),stamp(50),UUID.randomUUID(),TARGET,NOW,0,clock,ticker));
    }
    @Test void nativeAbsoluteCapCannotExceedTheRetainedOriginalCap(){
        assertThrows(RuntimeException.class,()->CurrencyPolicyCurrentWindow.admit(original(),stamp(2500),NONCE,TARGET,NOW,0,clock,ticker));
    }
    @Test void incompatibleOriginalTimeSourcesRefuseInsteadOfReconstruction(){
        assertThrows(IllegalArgumentException.class,()->CurrencyPolicyCurrentWindow.admit(original(),stamp(50),NONCE,TARGET,NOW,0,clock,()->nanos.get()));
    }
}
