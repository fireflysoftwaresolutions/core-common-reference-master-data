package io.fireflysoftware.masters.web.config.insurance.reference;

import java.time.*;
import java.util.function.LongSupplier;
import io.r2dbc.pool.ConnectionPool;
import org.fireflyframework.core.transaction.OwnedLeaseConnectionFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferenceCurrentReaderImpl;

/** Reader-only application-owned graph, without ambiguous global resource beans. */
final class InsuranceReferenceCurrentResources implements AutoCloseable {
    final Clock clock=Clock.tick(Clock.systemUTC(),Duration.ofNanos(1000));
    final LongSupplier nanos=System::nanoTime;
    final ConnectionPool pool;
    final OwnedLeaseConnectionFactory factory;
    final DatabaseClient client;
    final R2dbcEntityTemplate operations;
    final R2dbcTransactionManager manager;
    final InsuranceReferenceCurrentReader reader;
    InsuranceReferenceCurrentResources(ConnectionPool pool,InsuranceReferenceCurrentNativeContext.Factory contexts) {
        this.pool=pool;factory=new OwnedLeaseConnectionFactory(pool);client=DatabaseClient.create(factory);
        operations=new R2dbcEntityTemplate(client,PostgresDialect.INSTANCE);manager=new R2dbcTransactionManager(factory);
        reader=new InsuranceReferenceCurrentReaderImpl(operations,manager,contexts.create(operations),clock,nanos);
    }
    @Override public void close(){pool.disposeLater().block(Duration.ofSeconds(5));}
}
