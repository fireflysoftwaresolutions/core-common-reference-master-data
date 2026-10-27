package io.fireflysoftware.masters.core.services.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import io.r2dbc.spi.*;
import org.reactivestreams.Publisher;
import org.junit.jupiter.api.Test;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.fireflyframework.core.transaction.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;
import reactor.core.publisher.*;

class InsuranceReferenceReadPurposeTest {
    static final Instant NOW=Instant.parse("2026-10-27T00:00:00Z");
    static final UUID RECORD=new UUID(1,20);
    static final class Driver implements ConnectionFactory {
        int acquires,closes,rollbacks;Boolean physicalReadOnly;final List<String> statements=new ArrayList<>();
        public Publisher<? extends Connection> create(){acquires++;return Mono.just((Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->switch(m.getName()){
            case "getMetadata"->new ConnectionMetadata(){public String getDatabaseProductName(){return "PostgreSQL";}public String getDatabaseVersion(){return "17";}};
            case "isAutoCommit"->true;case "getTransactionIsolationLevel"->IsolationLevel.READ_COMMITTED;
            case "beginTransaction"->{if(a!=null&&a.length==1)physicalReadOnly=((io.r2dbc.spi.TransactionDefinition)a[0]).getAttribute(io.r2dbc.spi.TransactionDefinition.READ_ONLY);yield Mono.empty();}
            case "createStatement"->statement((String)a[0]);case "commitTransaction","setAutoCommit","setTransactionIsolationLevel"->Mono.empty();case "rollbackTransaction"->{rollbacks++;yield Mono.empty();}case "close"->{closes++;yield Mono.empty();}case "validate"->Mono.just(true);default->throw new UnsupportedOperationException(m.getName());}));}
        Statement statement(String sql){statements.add(sql);return (Statement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Statement.class},(p,m,a)->{if(m.getName().equals("execute"))return Mono.just(new Result(){public Publisher<Long> getRowsUpdated(){return Mono.just(1L);}public <T>Publisher<T>map(BiFunction<Row,RowMetadata,? extends T> f){return Flux.empty();}public <T>Publisher<T>map(Function<? super io.r2dbc.spi.Readable,? extends T> f){return Flux.empty();}public Result filter(Predicate<Segment> f){return this;}public <T>Publisher<T>flatMap(Function<Segment,? extends Publisher<? extends T>>f){return Flux.empty();}});if(m.getReturnType()==Statement.class)return p;throw new UnsupportedOperationException(m.getName());});}
        public ConnectionFactoryMetadata getMetadata(){return ()->"PostgreSQL";}
    }
    static final class Fixture {
        final Driver writer=new Driver(),reader=new Driver();final Clock clock=Clock.fixed(NOW,ZoneOffset.UTC);final LongSupplier nanos=()->0;boolean readerAvailable=true;
        InsuranceReferencePersistence.Session session(Driver driver,boolean read){var factory=new OwnedLeaseConnectionFactory(driver);var client=DatabaseClient.create(factory);var template=new R2dbcEntityTemplate(client,PostgresDialect.INSTANCE);var manager=new R2dbcTransactionManager(factory);var nativeContext=new InsuranceReferenceNativeContext(){public DatabaseClient databaseClient(){return client;}public Mono<Stamp>begin(Request request){return read&&!readerAvailable?Mono.error(new Unavailable()):Mono.just(new Stamp(request.nonce(),request.recordId(),request.kind(),NOW.toEpochMilli(),NOW,NOW.plusSeconds(1)));}public Mono<Boolean>complete(Request request){return Mono.just(true);}};return new InsuranceReferencePersistence.Session(template,manager,nativeContext);}
        final InsuranceReferencePersistence persistence=new InsuranceReferencePersistence(session(writer,false),session(reader,true),clock,nanos);
        R2dbcOperationBudget budget(){return R2dbcOperationBudget.observed(NOW,NOW.plusSeconds(5),0,Duration.ofSeconds(5),clock,nanos);}
        InsuranceReferenceNativeContext.Request request(String mode){return new InsuranceReferenceNativeContext.Request(new UUID(1,21),RECORD,Kind.COVERAGE_WORDING,new UUID(1,22),null,mode,"https://synthetic.invalid","test-only",NOW.plusSeconds(5));}
        Function<InsuranceReferencePersistence.Work,Mono<String>> select(){return work->work.session().repository().identity(RECORD,Kind.COVERAGE_WORDING).map(row->"ROW").defaultIfEmpty("EMPTY");}
    }
    void assertReadSelection(Fixture f,Object completed){assertNotNull(completed);assertEquals(1,f.reader.acquires);assertEquals(0,f.writer.acquires);assertEquals(Boolean.FALSE,f.reader.physicalReadOnly,"Only physical transaction access changes; native private context requires writes");assertEquals(1,f.reader.statements.size());assertTrue(f.reader.statements.getFirst().startsWith("SELECT"));assertTrue(f.reader.statements.stream().noneMatch(s->s.startsWith("INSERT")||s.startsWith("UPDATE")||s.startsWith("DELETE")));assertEquals(1,f.reader.closes);}
    @Test void legacyBusinessReadSelectorStillChoosesReaderButPermitsTechnicalContextWrites(){var f=new Fixture();assertReadSelection(f,f.persistence.execute(f.budget(),f.request("RECOVER"),true,null,f.select()).block());}
    @Test void explicitReadPurposeSelectsReaderAndNeverCommandSession() throws Exception {var f=new Fixture();Class<?> purpose=assertDoesNotThrow(()->Class.forName("io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferencePersistence$Purpose"));Object read=Enum.valueOf((Class)purpose,"READ");Method method=f.persistence.getClass().getMethod("execute",R2dbcOperationBudget.class,InsuranceReferenceNativeContext.Request.class,purpose,InsuranceReferencePersistence.Window.class,Function.class);var result=(Mono<?>)method.invoke(f.persistence,f.budget(),f.request("RECOVER"),read,null,f.select());assertReadSelection(f,result.block());}
    @Test void writePurposeKeepsTheCommandSession(){var f=new Fixture();assertNotNull(f.persistence.execute(f.budget(),f.request("SUBMIT_PUBLICATION"),false,null,work->Mono.just("CONTROLLED_WORK")).block());assertEquals(1,f.writer.acquires);assertEquals(0,f.reader.acquires);assertEquals(Boolean.FALSE,f.writer.physicalReadOnly);assertEquals(1,f.writer.closes);}
    @Test void unavailableReaderContextNeverFallsBackToWriter(){var f=new Fixture();f.readerAvailable=false;assertThrows(RuntimeException.class,()->f.persistence.execute(f.budget(),f.request("RECOVER"),true,null,f.select()).block());assertEquals(1,f.reader.acquires);assertEquals(0,f.writer.acquires);assertTrue(f.reader.statements.isEmpty());assertEquals(1,f.reader.rollbacks);assertEquals(1,f.reader.closes);}
}
