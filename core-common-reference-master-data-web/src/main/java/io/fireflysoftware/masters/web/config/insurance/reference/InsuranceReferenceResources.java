/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.config.insurance.reference;

import io.fireflysoftware.masters.core.services.insurance.reference.InsuranceReferencePersistence;
import io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferenceNativeContextImpl;
import io.r2dbc.pool.ConnectionPool;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;
import org.fireflyframework.core.transaction.OwnedLeaseConnectionFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;

/** Owns only these two pools; graph members are not additional application-wide resource beans. */
final class InsuranceReferenceResources implements AutoCloseable {
    final Clock clock = Clock.tick(Clock.systemUTC(), Duration.ofNanos(1000));
    final LongSupplier nanos = System::nanoTime;
    final Graph commands;
    final Graph reads;
    final InsuranceReferencePersistence persistence;

    InsuranceReferenceResources(ConnectionPool writer, ConnectionPool reader) {
        Objects.requireNonNull(writer);
        Objects.requireNonNull(reader);
        if (writer == reader) throw new IllegalArgumentException("Distinct insurance reference resource graphs required");
        commands = new Graph(writer);
        reads = new Graph(reader);
        persistence = new InsuranceReferencePersistence(commands.session, reads.session, clock, nanos);
    }

    @Override
    public void close() {
        try {
            commands.pool.disposeLater().block(Duration.ofSeconds(5));
        } finally {
            reads.pool.disposeLater().block(Duration.ofSeconds(5));
        }
    }

    static final class Graph {
        final ConnectionPool pool;
        final OwnedLeaseConnectionFactory factory;
        final DatabaseClient client;
        final R2dbcEntityTemplate operations;
        final R2dbcTransactionManager manager;
        final InsuranceReferenceNativeContextImpl nativeContext;
        final InsuranceReferencePersistence.Session session;

        Graph(ConnectionPool pool) {
            this.pool = pool;
            factory = new OwnedLeaseConnectionFactory(pool);
            client = DatabaseClient.create(factory);
            operations = new R2dbcEntityTemplate(client, PostgresDialect.INSTANCE);
            manager = new R2dbcTransactionManager(factory);
            nativeContext = new InsuranceReferenceNativeContextImpl(operations);
            session = new InsuranceReferencePersistence.Session(operations, manager, nativeContext);
        }
    }
}
