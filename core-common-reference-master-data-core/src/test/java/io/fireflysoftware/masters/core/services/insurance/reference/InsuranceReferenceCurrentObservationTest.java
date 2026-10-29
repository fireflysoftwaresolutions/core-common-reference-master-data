package io.fireflysoftware.masters.core.services.insurance.reference;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.junit.jupiter.api.Test;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.*;
import org.fireflyframework.security.spi.*;
import io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferenceCurrentObservationServiceImpl;
import io.fireflysoftware.masters.core.services.currency.observation.CurrencyPolicyCurrentObservationService;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.*;
import io.fireflysoftware.masters.models.entities.insurance.reference.InsuranceReferenceRows.*;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Controlled owner-boundary evidence; these tests do not qualify real source/native authority. */
class InsuranceReferenceCurrentObservationTest {
    static final Instant NOW=InsuranceReferenceServiceTest.NOW;
    static final String ISSUER="https://synthetic.invalid";
    static final class Harness {
        final AtomicLong nanos=new AtomicLong();final AtomicReference<Instant> now=new AtomicReference<>(NOW);
        final Deque<Instant> clockSequence=new ArrayDeque<>();
        final Clock clock=new Clock(){public java.time.ZoneId getZone(){return ZoneOffset.UTC;}public Clock withZone(java.time.ZoneId z){return this;}public Instant instant(){return clockSequence.isEmpty()?now.get():clockSequence.removeFirst();}};
        final LongSupplier ticks=nanos::get;
        InsuranceReferenceContracts.Revision definition=InsuranceReferenceServiceTest.definition();
        SecurityPrincipal actor=principal("reader");int reads,policies,sourceCalls;int denyAt;boolean sourceAvailable=true;
        boolean changeGeneration,changeActor,wrongDigest,withdrawn,sourceWrongDigest,currencyUnavailable,currencyWrong,interleavedClock;long finishAdvance;Instant sourceUntil=NOW.plusSeconds(3);Integer sourceScale;Instant currencyOriginalUntil;
        SecurityPrincipal principal(String subject){return new SecurityPrincipal(subject,ISSUER,"platform",Set.of(),Set.of(),Map.of("exp",NOW.plusSeconds(60)),null,null,Set.of(),Map.of());}
        Target target(){return new Target(definition.kind(),definition.recordId(),definition.publicationId(),definition.revision(),InsuranceReferenceCodec.digest(definition),0);}
        R2dbcOperationBudget budget(Instant until){return R2dbcOperationBudget.observed(NOW,until,0,Duration.ofSeconds(30),clock,ticks);}
        InsuranceReferenceCurrentReader.Snapshot snapshot(Target target,R2dbcOperationBudget budget,Instant until){
            var d=definition;var identity=new Identity("GLOBAL_MASTER",d.recordId(),d.kind(),d.code());
            var row=new Revision("GLOBAL_MASTER",d.publicationId(),d.recordId(),d.kind(),d.revision(),InsuranceReferenceCodec.encode(d),wrongDigest?"c".repeat(64):InsuranceReferenceCodec.digest(d),d.effectiveFrom(),d.effectiveUntil(),new UUID(1,1),NOW.minusMillis(100));
            var control=new ReferenceControl("GLOBAL_MASTER",d.recordId(),d.kind(),d.publicationId(),d.revision(),reads==2&&changeGeneration?2:0,withdrawn?"WITHDRAWN":"ACTIVE");
            var stamp=new InsuranceReferenceCurrentNativeContext.Stamp(new UUID(1,2),target,NOW.toEpochMilli(),NOW,until.isBefore(NOW.plusSeconds(4))?until:NOW.plusSeconds(4));
            return new InsuranceReferenceCurrentReader.Snapshot(identity,row,control,stamp,budget.intersect(budget(NOW.plusSeconds(4))),until.isBefore(NOW.plusSeconds(4))?until:NOW.plusSeconds(4));
        }
        InsuranceReferenceCurrentObservationService service(){
            SecurityContextPort context=()->Mono.just(actor);
            PolicyDecisionPort policy=(p,a,r,c)->{assertEquals("masters.insurance-reference.current",a);assertEquals("GLOBAL_MASTER",c.get("scope"));policies++;if(policies==4&&interleavedClock)for(int i=0;i<30;i++)clockSequence.add(NOW.plusMillis(i%2==0?400:100));return Mono.just(policies==denyAt?Decision.deny("controlled"):Decision.permit());};
            InsuranceReferenceCurrentReader reader=(target,p,b,u)->{reads++;return Mono.fromSupplier(()->snapshot(target,b,u));};
            InsuranceReferenceSourceVerifier source=d->{sourceCalls++;if(!sourceAvailable)return Mono.error(new IllegalStateException("controlled unavailable"));if(changeActor)actor=principal("other");if(finishAdvance!=0)nanos.addAndGet(finishAdvance);
                var tax=d.definition() instanceof InsuranceReferenceContracts.PremiumTax t?t:null;
                return Mono.just(new InsuranceReferenceSourceVerifier.Evidence(sourceWrongDigest?"f".repeat(64):InsuranceReferenceCodec.digest(d),tax==null?null:tax.currencyPolicy(),tax==null?null:sourceScale==null?tax.postedScale():sourceScale,NOW,sourceUntil));};
            CurrencyPolicyCurrentObservationService currency=new CurrencyPolicyCurrentObservationService(){
                public Mono<Observation> observe(io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyReference ref,int scale,R2dbcOperationBudget original){throw new AssertionError("Strict paired current call required");}
                public Mono<CurrentObservation> observeCurrent(io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target target,OriginalBudget original){
                    currencyOriginalUntil=original.until();var expected=target.reference();int scale=target.postedScale();
                    if(currencyUnavailable)return Mono.error(new IllegalStateException("controlled unavailable"));
                    var view=mock(io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyView.class);
                    when(view.reference()).thenReturn(currencyWrong?new io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyReference(expected.policyId(),expected.currencyId(),expected.isoCode(),expected.version()+1,expected.digest(),expected.encodingVersion()):expected);when(view.postedScale()).thenReturn(scale);when(view.effectiveToEpochMillis()).thenReturn(null);
                    Instant until=NOW.plusMillis(100);var retained=original.budget().intersect(budget(until));
                    return Mono.just(new CurrentObservation(new io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Current(target,view,NOW,until),retained,until));
                }
            };
            return new InsuranceReferenceCurrentObservationServiceImpl(context,policy,Set.of(ISSUER),reader,source,currency,clock,ticks);
        }
    }
    @Test void currentObservationHasSeparateServiceAndImplementationRatherThanReceiptRecovery(){assertTrue(InsuranceReferenceCurrentObservationService.class.isInterface());assertTrue(InsuranceReferenceCurrentObservationService.class.isAssignableFrom(InsuranceReferenceCurrentObservationServiceImpl.class));}
    @Test void exactActiveWordingUsesTwoFreshSnapshotsAndIndependentCurrentPermission(){var h=new Harness();var value=h.service().observe(h.target()).block();assertNotNull(value);assertEquals(h.target(),value.value().reference());assertEquals(2,h.reads);assertEquals(1,h.sourceCalls);assertTrue(h.policies>=4);assertFalse(value.value().publication().effectiveUntil().isAfter(h.sourceUntil));}
    @Test void currentTaxRequiresExactTypedCurrencyAndScaleBoundary(){var h=new Harness();h.definition=InsuranceReferenceCodec.decode(InsuranceReferenceCodecTest.fixture("PREMIUM_TAX").toString());var observed=h.service().observe(h.target()).block();assertNotNull(observed);assertEquals(h.sourceUntil.minusMillis(1),h.currencyOriginalUntil);assertFalse(observed.value().publication().effectiveUntil().isAfter(NOW.plusMillis(100)));assertEquals(2,h.reads);h=new Harness();h.definition=InsuranceReferenceCodec.decode(InsuranceReferenceCodecTest.fixture("PREMIUM_TAX").toString());h.sourceScale=3;var bad=h;assertThrows(RuntimeException.class,()->bad.service().observe(bad.target()).block());}
    @Test void restoredOrDifferentControlGenerationCannotSatisfyRetainedTarget(){var h=new Harness();h.changeGeneration=true;assertThrows(RuntimeException.class,()->h.service().observe(h.target()).block());assertEquals(2,h.reads);}
    @Test void withdrawnOrCorruptImmutableSnapshotWithholdsBody(){for(boolean withdrawn:List.of(false,true)){var h=new Harness();h.withdrawn=withdrawn;h.wrongDigest=!withdrawn;assertThrows(RuntimeException.class,()->h.service().observe(h.target()).block());assertEquals(0,h.sourceCalls);}}
    @Test void sourceUnavailableNeverPromotesHistoricalReceiptToCurrent(){var h=new Harness();h.sourceAvailable=false;assertThrows(RuntimeException.class,()->h.service().observe(h.target()).block());assertEquals(1,h.reads);}
    @Test void changedCurrentActorAfterProviderIoWithholdsBody(){var h=new Harness();h.changeActor=true;assertThrows(RuntimeException.class,()->h.service().observe(h.target()).block());assertEquals(1,h.reads);}
    @Test void policyDeniedBeforeOrAfterProviderDoesNotEmit(){for(int call:List.of(1,3,4)){var h=new Harness();h.denyAt=call;assertThrows(RuntimeException.class,()->h.service().observe(h.target()).block());}}
    @Test void originalAbsoluteCapIsRetainedAlongsideBudget(){var h=new Harness();Instant cap=NOW.plusMillis(500);var value=h.service().observe(h.target(),new InsuranceReferenceCurrentObservationService.OriginalBudget(h.budget(cap),cap)).block();assertNotNull(value);assertFalse(value.value().publication().effectiveUntil().isAfter(cap));}
    @Test void exhaustedOriginalBudgetCannotBeRenewedByFreshProvider(){var h=new Harness();h.finishAdvance=500_000_000L;Instant cap=NOW.plusMillis(100);assertThrows(RuntimeException.class,()->h.service().observe(h.target(),new InsuranceReferenceCurrentObservationService.OriginalBudget(h.budget(cap),cap)).block());assertEquals(1,h.reads);}
    @Test void nullTenantAndWrongIssuerDoNotReachMappedReader(){for(boolean tenant:List.of(false,true)){var h=new Harness();h.actor=new SecurityPrincipal("reader",tenant?"https://other.invalid":ISSUER,tenant?"platform":null,Set.of(),Set.of(),Map.of("exp",NOW.plusSeconds(60)),null,null,Set.of(),Map.of());assertThrows(RuntimeException.class,()->h.service().observe(h.target()).block());assertEquals(0,h.reads);}}
    @Test void targetRejectsMissingIdentityAndInvalidRevisionDigestGeneration(){var h=new Harness();var t=h.target();assertThrows(RuntimeException.class,()->new Target(t.kind(),new UUID(0,0),t.publicationId(),1,t.definitionDigest(),0));assertThrows(RuntimeException.class,()->new Target(t.kind(),t.recordId(),t.publicationId(),0,t.definitionDigest(),0));assertThrows(RuntimeException.class,()->new Target(t.kind(),t.recordId(),t.publicationId(),1,"A".repeat(64),0));assertThrows(RuntimeException.class,()->new Target(t.kind(),t.recordId(),t.publicationId(),1,t.definitionDigest(),-1));}
    @Test void unrelatedSourceDefinitionDigestCannotEstablishCurrent(){var h=new Harness();h.sourceWrongDigest=true;assertThrows(RuntimeException.class,()->h.service().observe(h.target()).block());assertEquals(1,h.reads);}
    @Test void unavailableOrDifferentCurrencyObservationWithholdsTaxCurrent(){for(boolean unavailable:List.of(false,true)){var h=new Harness();h.definition=InsuranceReferenceCodec.decode(InsuranceReferenceCodecTest.fixture("PREMIUM_TAX").toString());h.currencyUnavailable=unavailable;h.currencyWrong=!unavailable;assertThrows(RuntimeException.class,()->h.service().observe(h.target()).block());assertEquals(1,h.reads);}}

    @Test void interleavedBackwardClockReadsNeverExtendThePairedOriginalAbsoluteCap(){var h=new Harness();h.interleavedClock=true;Instant cap=NOW.plusMillis(500);var value=h.service().observe(h.target(),new InsuranceReferenceCurrentObservationService.OriginalBudget(h.budget(cap),cap)).block();assertNotNull(value);assertFalse(value.value().publication().effectiveUntil().isAfter(cap));}

    @Test void subMicrosecondElapsedTimeFloorsTheWireHorizonWithoutExtendingIt(){var h=new Harness();h.finishAdvance=1;var value=h.service().observe(h.target()).block();assertNotNull(value);assertEquals(0,value.value().publication().effectiveUntil().getNano()%1000);assertFalse(value.value().publication().effectiveUntil().isAfter(h.sourceUntil));}

}
