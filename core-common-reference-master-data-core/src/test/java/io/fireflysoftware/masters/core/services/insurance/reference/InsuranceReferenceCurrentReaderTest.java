package io.fireflysoftware.masters.core.services.insurance.reference;

import java.time.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import io.r2dbc.spi.*;
import org.junit.jupiter.api.Test;
import org.fireflyframework.core.transaction.*;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferenceCurrentReaderImpl;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.Target;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

/** Genuine mapped operations/transaction boundaries over a controlled driver, never a native server. */
class InsuranceReferenceCurrentReaderTest {
    static final Instant NOW=InsuranceReferenceServiceTest.NOW;
    static final class Fixture {
        final InsuranceReferenceServiceTest.Harness base=new InsuranceReferenceServiceTest.Harness();
        final List<io.r2dbc.spi.TransactionDefinition> definitions=new ArrayList<>();
        boolean failClose,failRollback,notCurrentBegin;long advanceOnRollback;
        final ConnectionFactory delegate=new ConnectionFactory(){
            public org.reactivestreams.Publisher<? extends Connection> create(){return Mono.from(base.driver.create()).map(connection->(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                if(m.getName().equals("close")&&failClose){base.driver.closes++;return Mono.error(new IllegalStateException("controlled close failure"));}
                if(m.getName().equals("rollbackTransaction")){base.nanos.addAndGet(advanceOnRollback);if(failRollback){base.driver.rollbacks++;return Mono.error(new IllegalStateException("controlled rollback failure"));}}
                if(m.getName().equals("beginTransaction")&&a!=null&&a.length==1)definitions.add((io.r2dbc.spi.TransactionDefinition)a[0]);
                try{return m.invoke(connection,a);}catch(InvocationTargetException e){throw e.getCause();}
            }));}
            public ConnectionFactoryMetadata getMetadata(){return base.driver.getMetadata();}
        };
        final OwnedLeaseConnectionFactory factory=new OwnedLeaseConnectionFactory(delegate);
        final DatabaseClient client=DatabaseClient.create(factory);
        final R2dbcEntityTemplate operations=new R2dbcEntityTemplate(client,PostgresDialect.INSTANCE);
        final R2dbcTransactionManager manager=new R2dbcTransactionManager(factory);
        long nativeMillis=3000,advanceDuringNative;boolean complete=true;int begins,completes;
        final InsuranceReferenceCurrentNativeContext context=new InsuranceReferenceCurrentNativeContext(){
            public DatabaseClient databaseClient(){return client;}
            public Mono<Stamp> begin(Request request){begins++;base.nanos.addAndGet(advanceDuringNative);if(notCurrentBegin)return Mono.error(new InsuranceReferenceCurrentFailure(InsuranceReferenceCurrentFailure.Kind.NOT_CURRENT));return Mono.just(new Stamp(request.nonce(),request.target(),NOW.toEpochMilli(),NOW,NOW.plusMillis(nativeMillis)));}
            public Mono<Boolean> complete(Request request){completes++;return Mono.just(complete);}
        };
        final InsuranceReferenceCurrentReader reader=new InsuranceReferenceCurrentReaderImpl(operations,manager,context,base.clock,base.monotonic);
        final io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Revision value=InsuranceReferenceServiceTest.definition();
        Target target(){return new Target(value.kind(),value.recordId(),value.publicationId(),value.revision(),InsuranceReferenceCodec.digest(value),0);}
        R2dbcOperationBudget budget(){return R2dbcOperationBudget.observed(NOW,NOW.plusSeconds(10),0,Duration.ofSeconds(30),base.clock,base.monotonic);}
        void rows(){
            InsuranceReferenceServiceTest.seed(base,InsuranceReferenceServiceTest.row("Identity","GLOBAL_MASTER",value.recordId(),value.kind(),value.code()),2);
            InsuranceReferenceServiceTest.seed(base,InsuranceReferenceServiceTest.row("Revision","GLOBAL_MASTER",value.publicationId(),value.recordId(),value.kind(),value.revision(),InsuranceReferenceCodec.encode(value),InsuranceReferenceCodec.digest(value),value.effectiveFrom(),value.effectiveUntil(),new UUID(1,1),NOW.minusSeconds(1)),2);
            InsuranceReferenceServiceTest.seed(base,InsuranceReferenceServiceTest.row("ReferenceControl","GLOBAL_MASTER",value.recordId(),value.kind(),value.publicationId(),value.revision(),0L,"ACTIVE"),2);
        }
        InsuranceReferenceCurrentReader.Snapshot read(){return reader.read(target(),base.actor,budget(),NOW.plusSeconds(10)).block();}
    }
    @Test void actualMappedSnapshotUsesSameReaderCfAndRepeatableReadWithoutBusinessWrites(){var f=new Fixture();f.rows();assertNotNull(f.read());assertEquals(1,f.base.driver.acquires);assertEquals(1,f.base.driver.commits);assertEquals(1,f.base.driver.closes);assertEquals(3,f.base.driver.sql.size());assertTrue(f.base.driver.sql.stream().allMatch(s->s.startsWith("SELECT")));assertEquals(Boolean.FALSE,f.definitions.getFirst().getAttribute(io.r2dbc.spi.TransactionDefinition.READ_ONLY));assertEquals(IsolationLevel.REPEATABLE_READ,f.definitions.getFirst().getAttribute(io.r2dbc.spi.TransactionDefinition.ISOLATION_LEVEL));assertEquals(1,f.begins);assertEquals(1,f.completes);}
    @Test void unavailableCurrentNativeContextRollsBackBeforeAnyBusinessRead(){var f=new Fixture();var reader=new InsuranceReferenceCurrentReaderImpl(f.operations,f.manager,InsuranceReferenceCurrentNativeContext.unavailable(f.client),f.base.clock,f.base.monotonic);assertThrows(RuntimeException.class,()->reader.read(f.target(),f.base.actor,f.budget(),NOW.plusSeconds(10)).block());assertTrue(f.base.driver.sql.isEmpty());assertEquals(1,f.base.driver.rollbacks);assertEquals(1,f.base.driver.closes);}
    @Test void nativeCompletionRefusalRollsBackAndWithholdsSnapshot(){var f=new Fixture();f.rows();f.complete=false;assertThrows(RuntimeException.class,f::read);assertEquals(0,f.base.driver.commits);assertEquals(1,f.base.driver.rollbacks);assertEquals(1,f.completes);}
    @Test void nativeFloorQuantumAndPreObservationElapsedTimeCannotBeRenewed(){var f=new Fixture();f.rows();f.nativeMillis=2;f.advanceDuringNative=1_000_000;assertThrows(RuntimeException.class,f::read);assertTrue(f.base.driver.sql.isEmpty());assertEquals(1,f.base.driver.rollbacks);}
    @Test void typedAbsentTargetSurvivesOnlyAcknowledgedCompleteRollback(){
        var f=new Fixture();f.notCurrentBegin=true;
        var failure=assertThrows(InsuranceReferenceCurrentFailure.class,f::read);assertEquals(InsuranceReferenceCurrentFailure.Kind.NOT_CURRENT,failure.kind());
        assertEquals(1,f.base.driver.rollbacks);assertEquals(1,f.base.driver.closes);assertEquals(0,f.base.driver.commits);assertTrue(f.base.driver.sql.isEmpty());
    }
    @Test void mappedMissingRowRetainsSameKnownNotCurrentClassification(){
        var f=new Fixture();var failure=assertThrows(InsuranceReferenceCurrentFailure.class,f::read);assertEquals(InsuranceReferenceCurrentFailure.Kind.NOT_CURRENT,failure.kind());
        assertEquals(1,f.base.driver.rollbacks);assertEquals(0,f.base.driver.commits);assertEquals(1,f.base.driver.sql.size());
    }
    @Test void uncertainRollbackOrCloseNeverRestoresAbsence(){
        for(boolean rollback:List.of(false,true)){
            var f=new Fixture();f.notCurrentBegin=true;f.failRollback=rollback;f.failClose=!rollback;
            assertThrows(RootTransactionException.class,f::read);assertEquals(0,f.base.driver.commits);
        }
    }
    @Test void expiredOriginalOrNarrowerNativeCapNeverRestoresAbsence(){
        var original=new Fixture();original.notCurrentBegin=true;original.advanceOnRollback=11_000_000_000L;assertThrows(RootTransactionException.class,original::read);
        var nativeCap=new Fixture();nativeCap.advanceOnRollback=4_000_000_000L;assertThrows(RootTransactionException.class,nativeCap::read);
    }
    @Test void differentContextClientCannotAcquireTheReader(){var f=new Fixture();var wrong=DatabaseClient.create(new OwnedLeaseConnectionFactory(new InsuranceReferenceServiceTest.Driver()));assertThrows(IllegalArgumentException.class,()->new InsuranceReferenceCurrentReaderImpl(f.operations,f.manager,InsuranceReferenceCurrentNativeContext.unavailable(wrong),f.base.clock,f.base.monotonic));assertEquals(0,f.base.driver.acquires);}
}
