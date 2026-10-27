package io.fireflysoftware.masters.web.controllers.insurance.reference;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InsuranceReferenceCurrentControllerTest {
    static final String RECORD="11111111-2222-3333-4444-555555555555",PUBLICATION="22222222-2222-3333-4444-555555555555",DIGEST="a".repeat(64);
    static MockServerWebExchange exchange(String query){return MockServerWebExchange.from(MockServerHttpRequest.get("/current?"+query));}
    static MockServerWebExchange exact(){return exchange("definitionDigest="+DIGEST+"&generation=0");}
    @Test void routeIsExactReadOnlyAndSeparatelyDefaultOff()throws Exception {
        var type=InsuranceReferenceCurrentController.class;var condition=type.getAnnotation(ConditionalOnProperty.class);
        assertEquals("firefly.masters.insurance-reference.current",condition.prefix());assertFalse(condition.matchIfMissing());
        var method=Arrays.stream(type.getMethods()).filter(m->m.getName().equals("current")).findFirst().orElseThrow();
        assertEquals("/{kind}/{recordId}/publications/{publicationId}/revisions/{revision}/current",method.getAnnotation(GetMapping.class).value()[0]);
        assertEquals("observeCurrentInsuranceReference",method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class).operationId());
    }
    @Test void strictTargetRefusesExtraDuplicateAndNoncanonicalWireFields(){
        for(var e:List.of(exchange("definitionDigest="+DIGEST+"&generation=0&extra=x"),exchange("definitionDigest="+DIGEST+"&generation=0&generation=0")))
            assertThrows(IllegalArgumentException.class,()->InsuranceReferenceCurrentController.decode("COVERAGE_WORDING",RECORD,PUBLICATION,"1",DIGEST,"0",e));
        assertThrows(IllegalArgumentException.class,()->InsuranceReferenceCurrentController.decode("COVERAGE_WORDING",RECORD,PUBLICATION,"01",DIGEST,"0",exact()));
        assertThrows(IllegalArgumentException.class,()->InsuranceReferenceCurrentController.decode("COVERAGE_WORDING",RECORD,PUBLICATION,"1",DIGEST,"-1",exact()));
        assertThrows(IllegalArgumentException.class,()->InsuranceReferenceCurrentController.decode("COVERAGE_WORDING",RECORD,PUBLICATION,"1",DIGEST.toUpperCase(),"0",exact()));
        assertThrows(IllegalArgumentException.class,()->InsuranceReferenceCurrentController.decode("OTHER",RECORD,PUBLICATION,"1",DIGEST,"0",exact()));
    }
    @Test void boundaryReturnsStaticNoStoreRefusalsWithoutPrivateCause(){
        for(var kind:InsuranceReferenceCurrentFailure.Kind.values()){
            var service=mock(InsuranceReferenceCurrentObservationService.class);when(service.observe(any(Target.class))).thenReturn(Mono.error(new InsuranceReferenceCurrentFailure(kind)));
            var result=new InsuranceReferenceCurrentController(service).current("COVERAGE_WORDING",RECORD,PUBLICATION,"1",DIGEST,"0",exact()).block();
            assertNotNull(result);assertEquals(switch(kind){case FORBIDDEN->403;case NOT_CURRENT->409;case UNAVAILABLE->503;},result.getStatusCode().value());
            assertEquals("no-store",result.getHeaders().getCacheControl());assertFalse(result.getBody().toString().contains("private"));
        }
        var service=mock(InsuranceReferenceCurrentObservationService.class);when(service.observe(any(Target.class))).thenReturn(Mono.error(new IllegalStateException("private database/provider content")));
        var result=new InsuranceReferenceCurrentController(service).current("COVERAGE_WORDING",RECORD,PUBLICATION,"1",DIGEST,"0",exact()).block();
        assertEquals(503,result.getStatusCode().value());assertFalse(result.getBody().toString().contains("database"));
    }
    @Test void invalidTargetDoesNotReachTheCurrentService(){var service=mock(InsuranceReferenceCurrentObservationService.class);var result=new InsuranceReferenceCurrentController(service).current("COVERAGE_WORDING",RECORD,PUBLICATION,"0",DIGEST,"0",exact()).block();assertEquals(400,result.getStatusCode().value());verifyNoInteractions(service);}
    @Test void successfulDelegationReturnsTheActualBoundedValueAndNoStore(){
        var service=mock(InsuranceReferenceCurrentObservationService.class);var value=mock(Current.class);
        var now=Instant.parse("2026-11-01T00:00:00Z");var clock=Clock.fixed(now,ZoneOffset.UTC);java.util.function.LongSupplier ticks=()->0;
        var budget=R2dbcOperationBudget.observed(now,now.plusSeconds(1),0,Duration.ofSeconds(1),clock,ticks);
        when(service.observe(any(Target.class))).thenReturn(Mono.just(new InsuranceReferenceCurrentObservationService.Observation(value,budget)));
        var result=new InsuranceReferenceCurrentController(service).current("COVERAGE_WORDING",RECORD,PUBLICATION,"1",DIGEST,"0",exact()).block();
        assertEquals(200,result.getStatusCode().value());assertSame(value,result.getBody());assertEquals("no-store",result.getHeaders().getCacheControl());
    }

}
