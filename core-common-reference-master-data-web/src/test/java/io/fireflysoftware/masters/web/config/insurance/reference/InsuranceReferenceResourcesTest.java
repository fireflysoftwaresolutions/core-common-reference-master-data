package io.fireflysoftware.masters.web.config.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class InsuranceReferenceResourcesTest {
    @Test void twoEncapsulatedPoolGraphsKeepEachExactFactoryClientTemplateAndManager() {
        var acquisitions = new AtomicInteger();
        var writer = pool(acquisitions);
        var reader = pool(acquisitions);
        try (var resources = new InsuranceReferenceResources(writer, reader)) {
            for (var graph : java.util.List.of(resources.commands, resources.reads)) {
                assertSame(graph.factory, graph.client.getConnectionFactory());
                assertSame(graph.client, graph.operations.getDatabaseClient());
                assertSame(graph.factory, graph.manager.getConnectionFactory());
                assertSame(graph.client, graph.nativeContext.databaseClient());
                assertNotNull(graph.session);
            }
            assertNotSame(resources.commands.factory, resources.reads.factory);
            assertEquals(0, acquisitions.get());
            assertTrue(resources.persistence.usesTimeSources(resources.clock, resources.nanos));
            assertEquals(0, resources.clock.instant().getNano() % 1000);
        }
        assertEquals(0, acquisitions.get());
        assertTrue(writer.isDisposed());
        assertTrue(reader.isDisposed());
    }

    @Test void samePoolCannotMasqueradeAsSeparateReaderAndWriter() {
        var pool = pool(new AtomicInteger());
        try {
            assertThrows(IllegalArgumentException.class, () -> new InsuranceReferenceResources(pool, pool));
        } finally {
            pool.dispose();
        }
    }

    private static ConnectionPool pool(AtomicInteger acquisitions) {
        ConnectionFactory source = new ConnectionFactory() {
            public Mono<Connection> create() {
                return Mono.defer(() -> { acquisitions.incrementAndGet(); return Mono.error(new AssertionError("No source-context connection permitted")); });
            }
            public ConnectionFactoryMetadata getMetadata() { return () -> "PostgreSQL"; }
        };
        return new ConnectionPool(ConnectionPoolConfiguration.builder(source).initialSize(0).maxSize(1).build());
    }
}
