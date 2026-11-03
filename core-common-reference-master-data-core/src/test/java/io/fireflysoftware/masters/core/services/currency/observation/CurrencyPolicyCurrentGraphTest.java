package io.fireflysoftware.masters.core.services.currency.observation;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Clock;
import java.util.concurrent.atomic.AtomicInteger;
import io.fireflysoftware.masters.core.services.currency.observation.impl.*;
import io.r2dbc.spi.*;
import org.reactivestreams.Publisher;
import org.fireflyframework.core.transaction.OwnedLeaseConnectionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import reactor.core.publisher.Mono;

/** Genuine repository factories and graph identity only; never a native SQL/connection acceptance. */
class CurrencyPolicyCurrentGraphTest {
    final AtomicInteger connects=new AtomicInteger();
    ConnectionFactory base(){return new ConnectionFactory(){
        public ConnectionFactoryMetadata getMetadata(){return ()->"PostgreSQL";}
        public Publisher<? extends Connection> create(){connects.incrementAndGet();return Mono.error(new AssertionError("No source test connection permitted"));}
    };}
    @Test void actualSpringDataContextAndProjectionProxiesKeepOneVerifiedGraphWithoutConnecting(){
        var factory=new OwnedLeaseConnectionFactory(base());var client=DatabaseClient.create(factory);var operations=new R2dbcEntityTemplate(client,PostgresDialect.INSTANCE);
        var context=new CurrencyPolicyCurrentNativeContextImpl(operations);assertSame(client,context.databaseClient());
        assertNotNull(new CurrencyPolicyCurrentReaderImpl(operations,new R2dbcTransactionManager(factory),context,Clock.systemUTC(),System::nanoTime));assertEquals(0,connects.get());
    }
    @Test void differentContextAndManagerFactoriesRefuseBeforeAcquisition(){
        var factory=new OwnedLeaseConnectionFactory(base());var operations=new R2dbcEntityTemplate(DatabaseClient.create(factory),PostgresDialect.INSTANCE);
        var other=new OwnedLeaseConnectionFactory(base());var foreignOperations=new R2dbcEntityTemplate(DatabaseClient.create(other),PostgresDialect.INSTANCE);
        assertThrows(IllegalArgumentException.class,()->new CurrencyPolicyCurrentReaderImpl(operations,new R2dbcTransactionManager(factory),new CurrencyPolicyCurrentNativeContextImpl(foreignOperations),Clock.systemUTC(),System::nanoTime));
        assertThrows(IllegalArgumentException.class,()->new CurrencyPolicyCurrentReaderImpl(operations,new R2dbcTransactionManager(other),new CurrencyPolicyCurrentNativeContextImpl(operations),Clock.systemUTC(),System::nanoTime));assertEquals(0,connects.get());
    }
}
