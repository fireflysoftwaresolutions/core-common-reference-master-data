package io.fireflysoftware.masters.core.services.currency.observation;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import io.fireflysoftware.masters.core.services.insurance.source.InsuranceSandboxFactReader;
import io.fireflysoftware.masters.core.services.insurance.source.impl.DurableInsuranceSandboxSourceVerifier;
import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferenceCodec;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

/** Controlled source/current port boundary only, not positive durable/native/provider admission. */
class CurrencyPolicyTaxBudgetPropagationTest {
    static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    static final UUID ID=UUID.fromString("10000000-0000-4000-8000-000000000001");
    @Test void durableTaxCallerPassesExactFactCapAndNeverPromotesTheLongerBudget(){
        Clock clock=Clock.fixed(NOW,ZoneOffset.UTC);var ticks=new AtomicLong();java.util.function.LongSupplier nanos=ticks::get;
        var reference=new CurrencyPolicyReference(ID.toString(),new UUID(1,2).toString(),"USD",1L,"a".repeat(64),"MASTERS_CURRENCY_POLICY_V1");
        var body=new PremiumTax(TaxBasis.NET_PREMIUM_EXCLUSIVE,1000,Rounding.HALF_EVEN,2,reference,new Applicability(List.of("US"),List.of("TERM_PC_INSURANCE_V1"),List.of(Event.NEW_BUSINESS)));
        var definition=new Revision(Kind.PREMIUM_TAX,new UUID(1,3),new UUID(1,4),1,"SOURCE_VECTOR",NOW.minusSeconds(1),null,body,new Provenance(Origin.SANDBOX,"urn:source","v1","b".repeat(64),"urn:rights"));
        var actor=SecurityPrincipal.builder().issuer("https://issuer.example.test").subject("source-only").tenantId("platform").build();var factCap=NOW.plusMillis(80);var nativeCap=NOW.plusMillis(30);var reads=new AtomicInteger();var captured=new AtomicReference<Instant>();
        InsuranceSandboxFactReader source=expected->{reads.incrementAndGet();return Mono.just(new InsuranceSandboxFactReader.Observation(new Reference(Type.SOURCE,new UUID(1,5),1,0,"c".repeat(64)),
            new Reference(Type.RIGHTS,new UUID(1,6),1,0,"d".repeat(64)),null,InsuranceReferenceCodec.digest(expected),NOW,factCap,
            R2dbcOperationBudget.observed(NOW,NOW.plusMillis(500),0,Duration.ofMillis(500),clock,nanos),actor));};
        CurrencyPolicyCurrentObservationService currency=new CurrencyPolicyCurrentObservationService(){
            public Mono<Observation> observe(CurrencyPolicyReference expected,int scale,R2dbcOperationBudget budget){throw new AssertionError("No old owner.get fallback");}
            public Mono<CurrentObservation> observeCurrent(Target target,OriginalBudget original){
                captured.set(original.until());assertEquals(reference,target.reference());assertEquals(2,target.postedScale());
                var view=new CurrencyPolicyView(reference,"boundary-vector","boundary-vector","e".repeat(64),2,NOW.minusSeconds(1).toEpochMilli(),null,"unit-boundary",ID.toString(),NOW.minusMillis(1).toEpochMilli(),"PUBLISHED",0L,NOW.toEpochMilli(),true);
                var budget=original.budget().intersect(R2dbcOperationBudget.observed(NOW,nativeCap,0,Duration.ofMillis(30),clock,nanos));return Mono.just(new CurrentObservation(new Current(target,view,NOW,nativeCap),budget,nativeCap));
            }
        };
        var observed=new DurableInsuranceSandboxSourceVerifier(source,currency,clock,nanos).verify(definition).block();
        assertEquals(factCap,captured.get());assertFalse(observed.effectiveUntil().isAfter(nativeCap));assertEquals(2,reads.get());
    }
}
