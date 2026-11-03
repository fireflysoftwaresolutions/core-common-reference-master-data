package io.fireflysoftware.masters.core.services.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Proxy;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.atomic.*;
import java.util.function.LongSupplier;
import io.r2dbc.spi.*;
import org.reactivestreams.Publisher;
import org.junit.jupiter.api.Test;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.fireflyframework.core.transaction.*;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;

class InsuranceReferenceInheritedBudgetTest {
    private static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    static final class Driver implements ConnectionFactory {
        int acquires,begins,closes;boolean stallAcquire,stallBegin;
        public Publisher<? extends Connection> create(){return Mono.defer(()->{
            acquires++;if(stallAcquire)return Mono.never();
            return Mono.just((Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,method,args)->switch(method.getName()){
                case "getMetadata"->new ConnectionMetadata(){public String getDatabaseProductName(){return "PostgreSQL";}public String getDatabaseVersion(){return "17";}};
                case "isAutoCommit"->true;
                case "getTransactionIsolationLevel"->IsolationLevel.READ_COMMITTED;
                case "beginTransaction"->{begins++;yield stallBegin?Mono.never():Mono.empty();}
                case "setAutoCommit","setTransactionIsolationLevel","commitTransaction","rollbackTransaction"->Mono.empty();
                case "close"->{closes++;yield Mono.empty();}
                case "validate"->Mono.just(true);
                default->throw new UnsupportedOperationException(method.getName());
            }));
        });}
        public ConnectionFactoryMetadata getMetadata(){return ()->"PostgreSQL";}
    }
    private void verifyInheritedDeadline(boolean stallBegin){
        var driver=new Driver();var factory=new OwnedLeaseConnectionFactory(driver);var client=DatabaseClient.create(factory);
        var template=new R2dbcEntityTemplate(client,PostgresDialect.INSTANCE);var manager=new R2dbcTransactionManager(factory);
        var clock=Clock.fixed(NOW,ZoneOffset.UTC);LongSupplier nanos=()->0;
        var context=new InsuranceReferenceNativeContext(){
            public DatabaseClient databaseClient(){return client;}
            public Mono<Stamp> begin(Request request){return Mono.just(new Stamp(request.nonce(),request.recordId(),request.kind(),NOW.toEpochMilli(),NOW,NOW.plusSeconds(1)));}
            public Mono<Boolean> complete(Request request){return Mono.just(true);}
        };
        var session=new InsuranceReferencePersistence.Session(template,manager,context);
        var persistence=new InsuranceReferencePersistence(session,session,clock,nanos);
        var original=R2dbcOperationBudget.observed(NOW,NOW.plusSeconds(5),0,Duration.ofSeconds(5),clock,nanos);
        var request=new InsuranceReferenceNativeContext.Request(new UUID(1,1),new UUID(1,2),Kind.COVERAGE_WORDING,new UUID(1,3),null,"RECOVER","https://synthetic.invalid","owned-test",NOW.plusSeconds(5));
        var refused=new AtomicBoolean();var secondWork=new AtomicInteger();
        StepVerifier.withVirtualTime(()->persistence.execute(original,request,true,null,work->Mono.just("first"))
            .flatMap(first->{driver.stallAcquire=!stallBegin;driver.stallBegin=stallBegin;
                return persistence.execute(original,request,false,first.window(),work->{secondWork.incrementAndGet();return Mono.just("unexpected");});})
            .map(done->"UNEXPECTED_SUCCESS").onErrorResume(error->{refused.set(true);return Mono.just("REFUSED");}))
            .thenAwait(Duration.ofMillis(1100))
            .then(()->assertTrue(refused.get(),"Inherited one-second window must bound acquisition and begin before the original five seconds"))
            .expectNext("REFUSED").expectComplete().verify(Duration.ofSeconds(3));
        assertEquals(0,secondWork.get());assertEquals(2,driver.acquires);
        assertEquals(stallBegin?2:1,driver.begins);assertEquals(stallBegin?2:1,driver.closes);
    }
    @Test void inheritedNativeWindowBoundsAStalledSecondAcquisition(){verifyInheritedDeadline(false);}
    @Test void inheritedNativeWindowBoundsAStalledSecondBegin(){verifyInheritedDeadline(true);}
}
