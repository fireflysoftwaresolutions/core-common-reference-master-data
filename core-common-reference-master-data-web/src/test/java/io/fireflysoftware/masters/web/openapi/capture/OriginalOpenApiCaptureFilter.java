/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.openapi.capture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

final class OriginalOpenApiCaptureFilter implements WebFilter {
    private static final Set<PosixFilePermission> DIRECTORY_MODE =
            PosixFilePermissions.fromString("rwx------");
    private static final Set<String> YAML_MEDIA_TYPES =
            Set.of("application/vnd.oai.openapi", "application/yaml", "application/x-yaml", "text/yaml");
    private static final String ROUTE = "/v3/api-docs.yaml";
    private static final int MAX_RESPONSE_BYTES = 4 * 1024 * 1024;
    private final Path directory;
    private final String sourceSha;
    private final byte[] invocation;
    private final AtomicBoolean claimed = new AtomicBoolean();

    OriginalOpenApiCaptureFilter(Path directory, String sourceSha, String invocation) {
        if (sourceSha == null || !sourceSha.matches("[0-9a-f]{40}")
                || invocation == null || !invocation.matches("[A-Za-z0-9_-]{32,128}")) {
            throw new IllegalArgumentException("Exact source and bounded invocation are required");
        }
        this.directory = directory.toAbsolutePath().normalize();
        this.sourceSha = sourceSha;
        this.invocation = invocation.getBytes(StandardCharsets.US_ASCII);
        validateDirectory();
        if (Files.exists(this.directory.resolve("receipt.json"), LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("Capture output already exists");
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (exchange.getRequest().getMethod() != HttpMethod.GET
                || !ROUTE.equals(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        return Mono.defer(() -> {
            var header = exchange.getRequest().getHeaders().get("X-Firefly-OpenAPI-Capture");
            if (header == null || header.size() != 1
                    || !MessageDigest.isEqual(invocation, header.getFirst().getBytes(StandardCharsets.UTF_8))) {
                return Mono.error(new ResponseStatusException(HttpStatus.PRECONDITION_FAILED));
            }
            if (!claimed.compareAndSet(false, true)) {
                return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT));
            }
            var capture = new Capture();
            var response = new ServerHttpResponseDecorator(exchange.getResponse()) {
                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    capture.expectPart();
                    return super.writeWith(observe(body).doOnComplete(capture::publisherCompleted));
                }

                @Override
                public Mono<Void> writeAndFlushWith(
                        Publisher<? extends Publisher<? extends DataBuffer>> body) {
                    return super.writeAndFlushWith(Flux.from(body)
                            .map(part -> {
                                capture.expectPart();
                                return observe(part);
                            }).doOnComplete(capture::publisherCompleted));
                }

                private Flux<? extends DataBuffer> observe(Publisher<? extends DataBuffer> body) {
                    return Flux.from(body).doOnNext(capture::update)
                            .doOnComplete(capture::partCompleted)
                            .doOnDiscard(DataBuffer.class, DataBufferUtils::release);
                }
            };
            return chain.filter(exchange.mutate().response(response).build())
                    .then(Mono.fromRunnable(() -> capture.complete(exchange)));
        });
    }

    private void validateDirectory() {
        try {
            for (Path component = directory; component != null; component = component.getParent()) {
                if (Files.isSymbolicLink(component)) {
                    throw new IllegalStateException("Capture directory cannot contain symlinks");
                }
            }
            if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                    || !Files.getPosixFilePermissions(directory, LinkOption.NOFOLLOW_LINKS).equals(DIRECTORY_MODE)) {
                throw new IllegalStateException("Capture requires an existing private directory");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Capture directory cannot be verified", exception);
        }
    }

    private final class Capture {
        private final MessageDigest digest = sha256();
        private int bytes;
        // A writer can finish successfully after cancelling an unfinished body publisher.
        private int pendingParts;
        private boolean publisherComplete;

        synchronized void expectPart() {
            pendingParts++;
        }

        synchronized void partCompleted() {
            pendingParts--;
        }

        synchronized void publisherCompleted() {
            publisherComplete = true;
        }

        synchronized void update(DataBuffer buffer) {
            boolean forwarded = false;
            try {
                int next = buffer.readableByteCount();
                if (next > MAX_RESPONSE_BYTES - bytes) {
                    throw new IllegalStateException("Original response exceeds the capture bound");
                }
                try (var chunks = buffer.readableByteBuffers()) {
                    while (chunks.hasNext()) {
                        ByteBuffer chunk = chunks.next().asReadOnlyBuffer();
                        digest.update(chunk);
                    }
                }
                bytes += next;
                forwarded = true;
            } finally {
                if (!forwarded) DataBufferUtils.release(buffer);
            }
        }

        synchronized void complete(ServerWebExchange exchange) {
            var response = exchange.getResponse();
            var status = response.getStatusCode();
            var contentType = response.getHeaders().getContentType();
            if (!publisherComplete || pendingParts != 0 || !response.isCommitted()
                    || (status != null && status.value() != 200) || bytes == 0
                    || contentType == null || !YAML_MEDIA_TYPES.contains(contentType.getType() + "/" + contentType.getSubtype())) {
                throw new IllegalStateException("Only a completed original YAML response can be recorded");
            }
            validateDirectory();
            String receipt = "{\"schema\":1,\"source_sha\":\"" + sourceSha
                    + "\",\"invocation_sha256\":\"" + HexFormat.of().formatHex(sha256().digest(invocation))
                    + "\",\"producer_application\":\"io.fireflysoftware.masters.web.openapi.OpenApiGenApplication\""
                    + ",\"route\":\"" + ROUTE + "\",\"http_status\":200,\"response_bytes\":" + bytes
                    + ",\"response_sha256\":\"" + HexFormat.of().formatHex(digest.digest())
                    + "\",\"state\":\"RESPONSE_COMPLETED\"}\n";
            try (var stream = Files.newDirectoryStream(directory)) {
                if (!(stream instanceof SecureDirectoryStream<Path> secure)) {
                    throw new IllegalStateException("Capture requires secure directory operations");
                }
                if (!secure.getFileAttributeView(PosixFileAttributeView.class).readAttributes().permissions().equals(DIRECTORY_MODE)) {
                    throw new IllegalStateException("Capture directory permissions changed");
                }
                try (var channel = secure.newByteChannel(Path.of("receipt.json"),
                        Set.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS),
                        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
                     var output = Channels.newOutputStream(channel)) {
                    output.write(receipt.getBytes(StandardCharsets.UTF_8));
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Original response receipt could not be recorded", exception);
            }
        }
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
