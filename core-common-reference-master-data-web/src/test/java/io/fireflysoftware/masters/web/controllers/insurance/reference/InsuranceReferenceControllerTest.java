package io.fireflysoftware.masters.web.controllers.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCommands.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.bind.annotation.PostMapping;
import reactor.core.publisher.Mono;

class InsuranceReferenceControllerTest {
    @Test void allSixRoutesDelegateTheirExactExistingCommandAndReturnHistoricalReceipt() {
        var service = new RecordingService();
        var controller = new InsuranceReferenceController(service);
        var action = InsuranceReferenceRequestDecoderTest.action();
        var recovery = action.replace("\"intentId\":\"" + InsuranceReferenceRequestDecoderTest.INTENT + "\",", "")
                .replace(",\"expectedIntentGeneration\":1", "");
        var withdrawal = "{\"actionId\":\"" + InsuranceReferenceRequestDecoderTest.ACTION + "\",\"recordId\":\""
                + InsuranceReferenceRequestDecoderTest.RECORD + "\",\"kind\":\"PREMIUM_TAX\",\"expected\":{\"publicationId\":\""
                + InsuranceReferenceRequestDecoderTest.PUBLICATION + "\",\"revision\":1,\"generation\":0}}";
        var responses = java.util.List.of(
                controller.submitPublication(exchange(InsuranceReferenceRequestDecoderTest.tax())).block(),
                controller.submitWithdrawal(exchange(withdrawal)).block(),
                controller.recover(exchange(recovery)).block(),
                controller.approve(exchange(action)).block(),
                controller.execute(exchange(action)).block(),
                controller.withdrawIntent(exchange(action)).block());
        assertEquals(java.util.List.of("submit-publication", "submit-withdrawal", "recover", "approve", "execute", "withdraw-intent"), service.calls);
        for (var response : responses) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals("no-store", response.getHeaders().getCacheControl());
            assertSame(service.receipt, response.getBody());
        }
    }

    @Test void decoderAndTransportByteLimitRefuseBeforeAnyServiceCall() {
        var service = new RecordingService();
        var controller = new InsuranceReferenceController(service);
        assertEquals(HttpStatus.BAD_REQUEST, controller.execute(exchange("{}")).block().getStatusCode());
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE,
                controller.submitPublication(exchange(" ".repeat(512 * 1024 + 1))).block().getStatusCode());
        assertTrue(service.calls.isEmpty());
    }

    @Test void protectedOutcomeNeverLeaksReceiptCommitFactsOrUnderlyingSql() {
        var service = new RecordingService();
        service.failure = new InsuranceReferenceOutcome(UUID.fromString(InsuranceReferenceRequestDecoderTest.ACTION), true, true, false);
        var response = new InsuranceReferenceController(service).execute(exchange(InsuranceReferenceRequestDecoderTest.action())).block();
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertEquals(new InsuranceReferenceController.Error("INSURANCE_REFERENCE_PROTECTED_RESULT_UNAVAILABLE"), response.getBody());
        service.failure = new IllegalStateException("private SQL and protected values");
        response = new InsuranceReferenceController(service).execute(exchange(InsuranceReferenceRequestDecoderTest.action())).block();
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(new InsuranceReferenceController.Error("INSURANCE_REFERENCE_CURRENT_OPERATION_UNAVAILABLE"), response.getBody());
    }

    @Test void everyNativePostOptsOutOfGenericIdempotencyWithoutAddingAReadRoute() {
        var methods = java.util.Arrays.stream(InsuranceReferenceController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostMapping.class)).toList();
        assertEquals(6, methods.size());
        for (var method : methods) assertTrue(method.isAnnotationPresent(DisableIdempotency.class));
        assertEquals(0, java.util.Arrays.stream(InsuranceReferenceController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(org.springframework.web.bind.annotation.GetMapping.class)).count());
    }

    @Test void aggregationReleasesTheActualReferenceCountedBodyBuffer() {
        var service = new RecordingService();
        var factory = new org.springframework.core.io.buffer.NettyDataBufferFactory(io.netty.buffer.UnpooledByteBufAllocator.DEFAULT);
        var nativeBuffer = io.netty.buffer.Unpooled.wrappedBuffer(InsuranceReferenceRequestDecoderTest.action().getBytes(StandardCharsets.UTF_8));
        var buffer = factory.wrap(nativeBuffer);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/insurance-references/intents/execute")
                .body(reactor.core.publisher.Flux.just(buffer)));
        assertEquals(HttpStatus.OK, new InsuranceReferenceController(service).execute(exchange).block().getStatusCode());
        assertEquals(0, nativeBuffer.refCnt());
    }

    @Test void emptyServiceResultDoesNotBecomeAMalformedRequestOrSuccess() {
        var service = new RecordingService();
        service.empty = true;
        assertEquals(HttpStatus.FORBIDDEN, new InsuranceReferenceController(service)
                .execute(exchange(InsuranceReferenceRequestDecoderTest.action())).block().getStatusCode());
    }

    private static MockServerWebExchange exchange(String body) {
        return MockServerWebExchange.from(MockServerHttpRequest.post("/api/v1/insurance-references/intents/execute")
                .header("Content-Type", "application/json").body(body));
    }

    private static final class RecordingService implements InsuranceReferencePublicationService {
        final java.util.List<String> calls = new java.util.ArrayList<>();
        final Receipt receipt = new Receipt(UUID.fromString(InsuranceReferenceRequestDecoderTest.ACTION),
                UUID.fromString(InsuranceReferenceRequestDecoderTest.RECORD), Kind.PREMIUM_TAX,
                UUID.fromString(InsuranceReferenceRequestDecoderTest.INTENT), InsuranceReferenceRequestDecoderTest.DIGEST,
                null, 0, 1, "SUBMIT_PUBLICATION");
        RuntimeException failure;
        boolean empty;
        Mono<Receipt> call(String operation) { calls.add(operation); return empty ? Mono.empty() : failure == null ? Mono.just(receipt) : Mono.error(failure); }
        public Mono<Receipt> submitPublication(SubmitPublication command) { assertNotNull(command); return call("submit-publication"); }
        public Mono<Receipt> submitWithdrawal(SubmitWithdrawal command) { assertNotNull(command); return call("submit-withdrawal"); }
        public Mono<Receipt> recover(Recovery command) { assertNotNull(command); return call("recover"); }
        public Mono<Receipt> approve(IntentAction command) { assertNotNull(command); return call("approve"); }
        public Mono<Receipt> execute(IntentAction command) { assertNotNull(command); return call("execute"); }
        public Mono<Receipt> withdrawIntent(IntentAction command) { assertNotNull(command); return call("withdraw-intent"); }
    }
}
