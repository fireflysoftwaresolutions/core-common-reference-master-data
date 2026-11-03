package io.fireflysoftware.masters.core.services.currency.observation;

import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.masters.core.services.currency.observation.impl.*;
import io.fireflysoftware.masters.core.services.currency.policy.CurrencyPolicyAuthority;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target;
import java.time.*;
import java.util.*;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/** Ordinary mocked native-boundary cases, never native installation or authentication acceptance. */
class CurrencyPolicyNativeCurrentServiceTest {
    final CurrencyPolicyCurrentObservationServiceTest data=new CurrencyPolicyCurrentObservationServiceTest();
    final SecurityPrincipal actor=data.principal.toBuilder().tenantId("platform").build();
    final Target target=new Target(data.value().reference(),2);
    final Authority authority=new Authority();
    boolean missing,wrong;long finalDelay;
    CurrencyPolicyCurrentObservationService service(){
        CurrencyPolicyCurrentReader reader=(expected,caller,original)->Mono.defer(()->{
            assertEquals(target,expected);assertEquals(actor,caller);
            if(missing)return Mono.error(new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.NOT_CURRENT));
            var stamp=new CurrencyPolicyCurrentNativeContext.Stamp(UUID.randomUUID(),target,data.clock.millis(),data.clock.instant(),data.clock.instant().plusMillis(50));
            var window=CurrencyPolicyCurrentWindow.admit(original,stamp,stamp.nonce(),target,data.clock.instant(),data.time.getAsLong(),data.clock,data.time);
            var policy=data.value();if(wrong)policy=CurrencyPolicyCurrentObservationServiceTest.copy(policy,policy.sourceEncoding(),policy.sourceDigest(),"changed",policy.reference(),data.clock.millis());
            return Mono.just(new CurrencyPolicyCurrentReader.Snapshot(policy,stamp,window.budget(),window.until()));
        });
        return new NativeCurrencyPolicyCurrentObservationServiceImpl(reader,authority,data.clock,data.time);
    }
    CurrencyPolicyCurrentObservationService.OriginalBudget original(){return new CurrencyPolicyCurrentObservationService.OriginalBudget(data.budget(),data.clock.instant().plusSeconds(3));}
    @Test void exactRetainedDefinitionAndActualNativeWindowAreReturned(){
        var result=service().observeCurrent(target,original()).block();assertEquals(target,result.value().target());
        assertEquals(data.clock.instant(),result.value().recordedAt());assertFalse(result.value().effectiveUntil().isAfter(data.clock.instant().plusMillis(49)));assertEquals(2,authority.calls);
    }
    @Test void finalPolicyCannotExtendAnAlreadyNarrowerNativeWindow(){finalDelay=50;assertThrows(CurrencyPolicyCurrentFailure.class,()->service().observeCurrent(target,original()).block());}
    @Test void changedCallerAndCurrentDenialWithholdTheBody(){authority.changed=true;assertThrows(CurrencyPolicyCurrentFailure.class,()->service().observeCurrent(target,original()).block());}
    @Test void finalCurrentPolicyDenialIsNotReplacedByTheInitialDecision(){authority.denyFinal=true;var error=assertThrows(CurrencyPolicyCurrentFailure.class,()->service().observeCurrent(target,original()).block());assertEquals(CurrencyPolicyCurrentFailure.Kind.FORBIDDEN,error.kind());}
    @Test void unavailableExactScopeStaysNotCurrent(){missing=true;var error=assertThrows(CurrencyPolicyCurrentFailure.class,()->service().observeCurrent(target,original()).block());assertEquals(CurrencyPolicyCurrentFailure.Kind.NOT_CURRENT,error.kind());}
    @Test void changedImmutableDefinitionCannotBecomeCurrent(){wrong=true;assertThrows(CurrencyPolicyCurrentFailure.class,()->service().observeCurrent(target,original()).block());}
    @Test void legacyOwnerGetHelperIsNotAProtectedNativeCurrentFallback(){
        var error=assertThrows(CurrencyPolicyCurrentFailure.class,()->data.service().observeCurrent(target,original()).block());assertEquals(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE,error.kind());
    }
    final class Authority extends CurrencyPolicyAuthority {
        int calls;boolean changed,denyFinal;
        Authority(){super(null,null,null);}
        @Override public Mono<SecurityPrincipal> authorize(String operation,String currency,Map<String,Object> context){
            assertEquals("current",operation);assertEquals(target.reference().currencyId(),currency);assertEquals(target.reference().digest(),context.get("definitionDigest"));
            if(++calls==2){data.nanos.set(Duration.ofMillis(finalDelay).toNanos());if(denyFinal)return Mono.error(new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.FORBIDDEN));}return Mono.just(changed&&calls==2?actor.toBuilder().subject("different").build():actor);
        }
    }
}
