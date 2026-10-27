package io.fireflysoftware.masters.web.config.insurance.reference;

import java.time.*;
import java.util.function.LongSupplier;
import io.r2dbc.pool.ConnectionPool;
import org.fireflyframework.core.transaction.OwnedLeaseConnectionFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import io.fireflysoftware.masters.core.services.insurance.source.InsuranceSandboxFactPersistence;

/** Two owned source-role graphs, encapsulated without application-wide CF/template beans. */
final class InsuranceSandboxSourceResources implements AutoCloseable {
    final Clock clock=Clock.tick(Clock.systemUTC(),Duration.ofNanos(1000));
    final LongSupplier nanos=System::nanoTime;
    final Graph writer,reader;
    final InsuranceSandboxFactPersistence persistence;
    InsuranceSandboxSourceResources(ConnectionPool writer,ConnectionPool reader){
        if(writer==reader)throw new IllegalArgumentException("Distinct source resource graphs required");
        this.writer=new Graph(writer,true);this.reader=new Graph(reader,false);
        persistence=new InsuranceSandboxFactPersistence(this.writer.session,this.reader.session,clock,nanos);
    }
    @Override public void close(){try{writer.pool.disposeLater().block(Duration.ofSeconds(5));}finally{reader.pool.disposeLater().block(Duration.ofSeconds(5));}}
    static final class Graph {
        final ConnectionPool pool;
        final OwnedLeaseConnectionFactory factory;
        final DatabaseClient client;
        final R2dbcEntityTemplate operations;
        final R2dbcTransactionManager manager;
        final InsuranceSandboxFactPersistence.Session session;
        Graph(ConnectionPool pool,boolean writer){
            this.pool=java.util.Objects.requireNonNull(pool);factory=new OwnedLeaseConnectionFactory(pool);
            client=DatabaseClient.create(factory);operations=new R2dbcEntityTemplate(client,PostgresDialect.INSTANCE);
            manager=new R2dbcTransactionManager(factory);session=new InsuranceSandboxFactPersistence.Session(operations,manager,writer);
        }
    }
}
