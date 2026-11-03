/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.openapi.capture;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class OriginalOpenApiCaptureFilterTest {
    private static final String SOURCE = "a4e25d3e46f1880ab7909bb78ee57b24c1f91200";
    private static final String NONCE = "isolated-original-producer-build-0001";
    private static final byte[] SPEC = "openapi: 3.1.0\npaths: {}\n".getBytes(StandardCharsets.UTF_8);
    private static final DefaultDataBufferFactory BUFFERS = new DefaultDataBufferFactory();

    @TempDir Path directory;

    private OriginalOpenApiCaptureFilter filter() throws Exception {
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
        return new OriginalOpenApiCaptureFilter(directory, SOURCE, NONCE);
    }

    private MockServerWebExchange exchange(String path, String nonce) {
        return MockServerWebExchange.from(MockServerHttpRequest.get(path)
                .header("X-Firefly-OpenAPI-Capture", nonce).build());
    }

    @Test
    void completedResponseBindsTheActualDeliveredBytesWithoutChangingTheBody() throws Exception {
        var filter = filter();
        var exchange = exchange("/v3/api-docs.yaml", NONCE);
        filter.filter(exchange, current -> {
            current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
            return current.getResponse().writeWith(Flux.just(BUFFERS.wrap(SPEC)));
        }).block(Duration.ofSeconds(3));
        assertEquals(new String(SPEC, StandardCharsets.UTF_8), exchange.getResponse().getBodyAsString().block());
        var receipt = new ObjectMapper().readTree(directory.resolve("receipt.json").toFile());
        assertEquals(SOURCE, receipt.get("source_sha").asText());
        assertEquals("591376d036294574c649b1eef67413f22425b7d3692c95242c5ab4699b6fef8a", receipt.get("response_sha256").asText());
        assertEquals(SPEC.length, receipt.get("response_bytes").asInt());
        assertEquals("RESPONSE_COMPLETED", receipt.get("state").asText());
        assertEquals(200, receipt.get("http_status").asInt());
        assertFalse(receipt.toString().contains(NONCE));
        assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(directory.resolve("receipt.json")));
    }

    @Test
    void flushedResponsePartsAreHashedInOrder() throws Exception {
        var filter = filter();
        var exchange = exchange("/v3/api-docs.yaml", NONCE);
        filter.filter(exchange, current -> {
            current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
            return current.getResponse().writeAndFlushWith(Flux.just(
                    Flux.just(BUFFERS.wrap(java.util.Arrays.copyOfRange(SPEC, 0, 10))),
                    Flux.just(BUFFERS.wrap(java.util.Arrays.copyOfRange(SPEC, 10, SPEC.length)))));
        }).block(Duration.ofSeconds(3));
        var receipt = new ObjectMapper().readTree(directory.resolve("receipt.json").toFile());
        assertEquals("591376d036294574c649b1eef67413f22425b7d3692c95242c5ab4699b6fef8a", receipt.get("response_sha256").asText());
        assertEquals(SPEC.length, receipt.get("response_bytes").asInt());
    }

    @Test
    void repeatedInvocationAndLateExistingOutputCannotReplaceTheFirstReceipt() throws Exception {
        var filter = filter();
        filter.filter(exchange("/v3/api-docs.yaml", NONCE), current -> {
            current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
            return current.getResponse().writeWith(Flux.just(BUFFERS.wrap(SPEC)));
        }).block();
        byte[] original = Files.readAllBytes(directory.resolve("receipt.json"));
        var error = assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> filter.filter(exchange("/v3/api-docs.yaml", NONCE), ignored -> Mono.error(new AssertionError("duplicate reached producer"))).block());
        assertEquals(409, error.getStatusCode().value());
        assertArrayEquals(original, Files.readAllBytes(directory.resolve("receipt.json")));
        Path next = Files.createDirectory(directory.resolve("next"), PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        var fresh = new OriginalOpenApiCaptureFilter(next, SOURCE, NONCE);
        Files.writeString(next.resolve("receipt.json"), "preserved-after-construction");
        assertThrows(RuntimeException.class, () -> fresh.filter(exchange("/v3/api-docs.yaml", NONCE), current -> {
            current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
            return current.getResponse().writeWith(Flux.just(BUFFERS.wrap(SPEC)));
        }).block());
        assertEquals("preserved-after-construction", Files.readString(next.resolve("receipt.json")));
    }

    @Test
    void wrongInvocationCannotProduceAReceiptOrCallTheOriginalHandler() throws Exception {
        var filter = filter();
        var exchange = exchange("/v3/api-docs.yaml", "different-build-invocation-0000000");
        var error = assertThrows(RuntimeException.class,
                () -> filter.filter(exchange, ignored -> Mono.error(new AssertionError("wrong invocation reached producer"))).block());
        assertTrue(error instanceof org.springframework.web.server.ResponseStatusException);
        assertFalse(Files.exists(directory.resolve("receipt.json")));
    }

    @Test
    void nonDocumentationRequestsDoNotConsumeTheCapture() throws Exception {
        var filter = filter();
        filter.filter(exchange("/actuator/health", NONCE), ignored -> Mono.empty()).block();
        assertFalse(Files.exists(directory.resolve("receipt.json")));
        filter.filter(exchange("/v3/api-docs.yaml", NONCE), current -> {
            current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
            return current.getResponse().writeWith(Flux.just(BUFFERS.wrap(SPEC)));
        }).block();
        assertTrue(Files.isRegularFile(directory.resolve("receipt.json")));
    }

    @Test
    void unsuccessfulResponseCannotCreateASuccessReceipt() throws Exception {
        var filter = filter();
        assertThrows(RuntimeException.class, () -> filter.filter(exchange("/v3/api-docs.yaml", NONCE), current -> {
            current.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return current.getResponse().writeWith(Flux.just(BUFFERS.wrap(SPEC)));
        }).block());
        assertFalse(Files.exists(directory.resolve("receipt.json")));
    }

    @Test
    void partialBodyFailureCannotCreateASuccessReceipt() throws Exception {
        var filter = filter();
        assertThrows(RuntimeException.class, () -> filter.filter(exchange("/v3/api-docs.yaml", NONCE), current -> {
            current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
            return current.getResponse().writeWith(Flux.concat(Flux.just(BUFFERS.wrap(SPEC)), Mono.error(new IllegalStateException("body refused"))));
        }).block());
        assertFalse(Files.exists(directory.resolve("receipt.json")));
    }

    @Test
    void cancellationBeforeResponseCompletionCannotCreateAReceipt() throws Exception {
        var filter = filter();
        StepVerifier.create(filter.filter(exchange("/v3/api-docs.yaml", NONCE), current -> {
            current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
            return current.getResponse().writeWith(Flux.concat(Flux.just(BUFFERS.wrap(SPEC)), Flux.never()));
        })).thenCancel().verify(Duration.ofSeconds(3));
        assertFalse(Files.exists(directory.resolve("receipt.json")));
    }

    @Test
    void writerCompletionAfterOnlyTheFirstChunkCannotCertifyThePartialResponse() throws Exception {
        var filter = filter();
        var exchange = exchange("/v3/api-docs.yaml", NONCE);
        exchange.getResponse().setWriteHandler(body -> body.take(1).then());
        assertThrows(RuntimeException.class, () -> filter.filter(exchange, current -> {
            current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
            return current.getResponse().writeWith(Flux.just(BUFFERS.wrap(SPEC), BUFFERS.wrap(SPEC)));
        }).block());
        assertFalse(Files.exists(directory.resolve("receipt.json")));
    }

    @Test
    void oversizedResponseIsRefusedWithoutAnUnboundedBufferOrReceipt() throws Exception {
        var filter = filter();
        assertThrows(RuntimeException.class, () -> filter.filter(exchange("/v3/api-docs.yaml", NONCE), current -> {
            current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
            return current.getResponse().writeWith(Flux.range(0, 65).map(ignored -> BUFFERS.wrap(new byte[65536])));
        }).block());
        assertFalse(Files.exists(directory.resolve("receipt.json")));
    }

    @Test
    void refusedPooledResponseBufferIsReleased() throws Exception {
        var filter = filter();
        var factory = new org.springframework.core.io.buffer.NettyDataBufferFactory(io.netty.buffer.PooledByteBufAllocator.DEFAULT);
        var buffer = factory.allocateBuffer(4 * 1024 * 1024 + 1);
        buffer.write(new byte[4 * 1024 * 1024 + 1]);
        try {
            assertThrows(RuntimeException.class, () -> filter.filter(exchange("/v3/api-docs.yaml", NONCE), current -> {
                current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
                return current.getResponse().writeWith(Flux.just(buffer));
            }).block());
            assertEquals(0, buffer.getNativeBuffer().refCnt());
            assertFalse(Files.exists(directory.resolve("receipt.json")));
        } finally {
            if (buffer.getNativeBuffer().refCnt() > 0) buffer.release();
        }
    }

    @Test
    void emptyResponseCannotCreateAReceipt() throws Exception {
        var filter = filter();
        assertThrows(RuntimeException.class, () -> filter.filter(exchange("/v3/api-docs.yaml", NONCE), current -> {
            current.getResponse().getHeaders().setContentType(MediaType.parseMediaType("application/vnd.oai.openapi"));
            return current.getResponse().setComplete();
        }).block());
        assertFalse(Files.exists(directory.resolve("receipt.json")));
    }

    @Test
    void existingReceiptIsNeverReplaced() throws Exception {
        Files.writeString(directory.resolve("receipt.json"), "preserved");
        assertThrows(RuntimeException.class, () -> filter());
        assertEquals("preserved", Files.readString(directory.resolve("receipt.json")));
    }

    @Test
    void symlinkAndNonPrivateDirectoriesAreRefused() throws Exception {
        var alias = directory.resolve("alias");
        Files.createSymbolicLink(alias, directory);
        assertThrows(RuntimeException.class, () -> new OriginalOpenApiCaptureFilter(alias, SOURCE, NONCE));
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwxr-xr-x"));
        assertThrows(RuntimeException.class, () -> new OriginalOpenApiCaptureFilter(directory, SOURCE, NONCE));
    }
}
