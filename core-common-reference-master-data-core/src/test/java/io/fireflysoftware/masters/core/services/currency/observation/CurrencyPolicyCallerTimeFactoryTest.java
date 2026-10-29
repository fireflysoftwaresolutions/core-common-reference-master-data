package io.fireflysoftware.masters.core.services.currency.observation;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.LongSupplier;
import io.fireflysoftware.masters.core.services.currency.observation.impl.*;
import io.fireflysoftware.masters.core.services.currency.policy.CurrencyPolicyAuthority;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/** Controlled reader boundary only; no pooled/native/authentication acceptance. */
class CurrencyPolicyCallerTimeFactoryTest {
    final CurrencyPolicyCurrentObservationServiceTest data=new CurrencyPolicyCurrentObservationServiceTest();
    final AtomicInteger acquisitions=new AtomicInteger();
    final SecurityPrincipal actor=data.principal.toBuilder().tenantId("platform").build();
    final Target target=new Target(data.value().reference(),2);
    final CurrencyPolicyAuthority authority=new CurrencyPolicyAuthority(null,null,null){@Override public Mono<SecurityPrincipal> authorize(String op,String id,Map<String,Object> context){return Mono.just(actor);}};
    final CurrencyPolicyCurrentObservationService.Factory factory=(clock,ticks)->new NativeCurrencyPolicyCurrentObservationServiceImpl((expected,caller,original)->{
        acquisitions.incrementAndGet();var stamp=new CurrencyPolicyCurrentNativeContext.Stamp(UUID.randomUUID(),expected,clock.millis(),clock.instant(),clock.instant().plusMillis(50));
        var window=CurrencyPolicyCurrentWindow.admit(original,stamp,stamp.nonce(),expected,clock.instant(),ticks.getAsLong(),clock,ticks);
        return Mono.just(new CurrencyPolicyCurrentReader.Snapshot(data.value(),stamp,window.budget(),window.until()));
    },authority,clock,ticks);
    CurrencyPolicyCurrentObservationService.OriginalBudget original(Clock clock,LongSupplier ticks){
        return new CurrencyPolicyCurrentObservationService.OriginalBudget(R2dbcOperationBudget.observed(clock.instant(),clock.instant().plusSeconds(1),ticks.getAsLong(),Duration.ofSeconds(1),clock,ticks),clock.instant().plusSeconds(1));
    }
    @Test void twoDistinctCallerTimePairsWorkWithoutBeingMixed(){
        Clock one=Clock.fixed(data.clock.instant(),ZoneOffset.UTC),two=Clock.fixed(data.clock.instant(),ZoneOffset.UTC);
        LongSupplier first=new AtomicLong()::get,second=new AtomicLong()::get;
        assertNotNull(factory.create(one,first).observeCurrent(target,original(one,first)).block());
        assertNotNull(factory.create(two,second).observeCurrent(target,original(two,second)).block());assertEquals(2,acquisitions.get());
    }
    @Test void incompatiblePairRefusesBeforeReaderAcquisition(){
        Clock one=Clock.fixed(data.clock.instant(),ZoneOffset.UTC),two=Clock.fixed(data.clock.instant(),ZoneOffset.UTC);
        LongSupplier first=new AtomicLong()::get,second=new AtomicLong()::get;
        assertThrows(CurrencyPolicyCurrentFailure.class,()->factory.create(two,second).observeCurrent(target,original(one,first)).block());assertEquals(0,acquisitions.get());
    }
}
