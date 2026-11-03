package io.fireflysoftware.masters.web.controllers.currency.policy;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import io.fireflysoftware.masters.core.services.currency.observation.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

/** Controller boundary vectors only; no HTTP listener, native read or caller authentication. */
class CurrencyPolicyCurrentControllerTest {
    static final String POLICY="10000000-0000-4000-8000-000000000001",CURRENCY="10000000-0000-4000-8000-000000000002",DIGEST="a".repeat(64);
    static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    static final String QUERY="currencyId="+CURRENCY+"&isoCode=USD&version=1&digest="+DIGEST+"&encodingVersion=MASTERS_CURRENCY_POLICY_V1&postedScale=2";
    final Clock clock=Clock.fixed(NOW,ZoneOffset.UTC);final AtomicLong nanos=new AtomicLong();final java.util.function.LongSupplier ticks=nanos::get;
    Mono<ResponseEntity<?>> call(CurrencyPolicyCurrentObservationService service,String query,String version,String scale){
        return new CurrencyPolicyCurrentController(service).current(POLICY,CURRENCY,"USD",version,DIGEST,"MASTERS_CURRENCY_POLICY_V1",scale,
            MockServerWebExchange.from(MockServerHttpRequest.get("/current?"+query)));
    }
    CurrencyPolicyCurrentObservationService error(CurrencyPolicyCurrentFailure.Kind kind){return new CurrencyPolicyCurrentObservationService(){
        public Mono<Observation> observe(CurrencyPolicyReference ref,int scale,R2dbcOperationBudget budget){throw new AssertionError("No legacy receipt fallback");}
        public Mono<CurrentObservation> observeCurrent(Target target){return Mono.error(new CurrencyPolicyCurrentFailure(kind));}
    };}
    @Test void declarationIsSeparateDefaultOffCurrentNotLegacyGet(){
        var flag=CurrencyPolicyCurrentController.class.getAnnotation(ConditionalOnProperty.class);assertEquals("firefly.masters.currency-policy.current",flag.prefix());assertFalse(flag.matchIfMissing());
    }
    @Test void typedNativeObservationIsNoStoreAtDelivery(){
        var ref=new CurrencyPolicyReference(POLICY,CURRENCY,"USD",1L,DIGEST,"MASTERS_CURRENCY_POLICY_V1");
        var policy=new CurrencyPolicyView(ref,"unit-boundary","unit-boundary",DIGEST,2,NOW.minusSeconds(1).toEpochMilli(),null,"unit-boundary",POLICY,NOW.minusMillis(1).toEpochMilli(),"PUBLISHED",0L,NOW.toEpochMilli(),true);
        CurrencyPolicyCurrentObservationService service=new CurrencyPolicyCurrentObservationService(){
            public Mono<Observation> observe(CurrencyPolicyReference ref,int scale,R2dbcOperationBudget budget){throw new AssertionError();}
            public Mono<CurrentObservation> observeCurrent(Target target){return Mono.just(new CurrentObservation(new Current(target,policy,NOW,NOW.plusSeconds(1)),R2dbcOperationBudget.observed(NOW,NOW.plusSeconds(1),0,Duration.ofSeconds(1),clock,ticks),NOW.plusSeconds(1)));}
        };
        var result=call(service,QUERY,"1","2").block();assertEquals(200,result.getStatusCode().value());assertEquals("no-store",result.getHeaders().getCacheControl());assertEquals(ref,((Current)result.getBody()).target().reference());
        nanos.set(Duration.ofSeconds(1).toNanos());assertEquals(503,call(service,QUERY,"1","2").block().getStatusCode().value());
    }
    @Test void actualFailureKindsRemainDistinctAndNoStore(){
        for(var entry:Map.of(CurrencyPolicyCurrentFailure.Kind.FORBIDDEN,403,CurrencyPolicyCurrentFailure.Kind.NOT_CURRENT,409,CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE,503).entrySet()){
            var result=call(error(entry.getKey()),QUERY,"1","2").block();assertEquals(entry.getValue(),result.getStatusCode().value());assertEquals("no-store",result.getHeaders().getCacheControl());assertEquals(new CurrencyPolicyCurrentFailure(entry.getKey()).getMessage(),((CurrencyPolicyCurrentController.Error)result.getBody()).code());
        }
    }
    @Test void unknownDuplicateAndNoncanonicalNumbersAreInvalidBeforeService(){
        var service=CurrencyPolicyCurrentObservationService.unavailable();
        for(String query:List.of(QUERY+"&extra=x",QUERY+"&version=1"))assertEquals(400,call(service,query,"1","2").block().getStatusCode().value());
        assertEquals(400,call(service,QUERY,"01","2").block().getStatusCode().value());assertEquals(400,call(service,QUERY,"1","02").block().getStatusCode().value());
    }
    @Test void oldJavaObservationCannotBecomeCurrent(){assertEquals(503,call(CurrencyPolicyCurrentObservationService.unavailable(),QUERY,"1","2").block().getStatusCode().value());}
}
