package io.fireflysoftware.masters.web.config.currency.observation;

import java.time.*;
import java.util.function.LongSupplier;
import io.r2dbc.pool.ConnectionPool;
import org.fireflyframework.core.transaction.OwnedLeaseConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import io.fireflysoftware.masters.core.services.currency.observation.*;
import io.fireflysoftware.masters.core.services.currency.observation.impl.CurrencyPolicyCurrentReaderImpl;

/** One owned reader graph; caller-specific time objects do not create new pools or global beans. */
final class CurrencyPolicyCurrentResources implements AutoCloseable {
    final Clock clock=Clock.tick(Clock.systemUTC(),Duration.ofNanos(1000));
    final LongSupplier nanos=System::nanoTime;
    final ConnectionPool pool;
    final OwnedLeaseConnectionFactory factory;
    final DatabaseClient client;
    final R2dbcEntityTemplate operations;
    final R2dbcTransactionManager manager;
    final CurrencyPolicyCurrentNativeContext context;
    CurrencyPolicyCurrentResources(ConnectionPool pool,CurrencyPolicyCurrentNativeContext.Factory contexts){
        this.pool=pool;factory=new OwnedLeaseConnectionFactory(pool);client=DatabaseClient.create(factory);
        operations=new R2dbcEntityTemplate(client,PostgresDialect.INSTANCE);manager=new R2dbcTransactionManager(factory);context=contexts.create(operations);
        if(context.databaseClient()!=client)throw new IllegalArgumentException("Exact currency CURRENT context graph required");
    }
    CurrencyPolicyCurrentReader reader(Clock callerClock,LongSupplier callerNanos){return new CurrencyPolicyCurrentReaderImpl(operations,manager,context,callerClock,callerNanos);}
    @Override public void close(){pool.disposeLater().block(Duration.ofSeconds(5));}
}
