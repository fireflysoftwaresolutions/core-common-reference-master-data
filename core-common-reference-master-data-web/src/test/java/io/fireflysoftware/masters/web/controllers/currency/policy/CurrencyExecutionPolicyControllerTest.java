/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.masters.web.controllers.currency.policy;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.fireflysoftware.masters.core.services.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;

import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import java.util.UUID;

class CurrencyExecutionPolicyControllerTest {
    final CurrencyExecutionPolicyService service = mock(CurrencyExecutionPolicyService.class);
    final WebTestClient client =
            WebTestClient.bindToController(new CurrencyExecutionPolicyController(service)).build();
    final UUID currency = UUID.fromString("12345678-1234-4234-8234-123456789abc");
    final UUID policy = UUID.fromString("32345678-1234-4234-8234-123456789abc");

    CurrencyPolicyView view() {
        return new CurrencyPolicyView(
                new CurrencyPolicyReference(
                        policy.toString(),
                        currency.toString(),
                        "USD",
                        1L,
                        "a".repeat(64),
                        "MASTERS_CURRENCY_POLICY_V1"),
                "retained",
                "source",
                "b".repeat(64),
                2,
                -1L,
                null,
                "Synthetic reference",
                "22345678-1234-4234-8234-123456789abc",
                0L,
                "PUBLISHED",
                0L,
                0L,
                true);
    }

    @Test
    void allFiveProtectedOperationsBypassTransportReceiptCache() {
        var methods =
                java.util.Arrays.stream(
                                CurrencyExecutionPolicyController.class.getDeclaredMethods())
                        .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                        .toArray(java.lang.reflect.Method[]::new);
        assertThat(methods).hasSize(5);
        for (var m : methods)
            assertThat(m.isAnnotationPresent(DisableIdempotency.class)).as(m.getName()).isTrue();
    }

    @Test
    void newPublication201AndNativeReplay200RetainExactTypedBody() {
        when(service.publish(any()))
                .thenReturn(
                        Mono.just(
                                new CurrencyExecutionPolicyService.PublicationOutcome(
                                        view(), false)),
                        Mono.just(
                                new CurrencyExecutionPolicyService.PublicationOutcome(
                                        view(), true)));
        for (int status : new int[] {201, 200})
            client.post()
                    .uri("/api/v1/currency-execution-policies")
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .bodyValue(CurrencyPolicyRequestDecoderTest.JSON)
                    .exchange()
                    .expectStatus()
                    .isEqualTo(status)
                    .expectBody()
                    .jsonPath("$.reference.policyId")
                    .isEqualTo(policy.toString())
                    .jsonPath("$.effectiveFromEpochMillis")
                    .isEqualTo(-1);
    }

    @Test
    void exactReadRequiresExplicitCurrencyAndPreservesCurrentDenial() {
        client.get()
                .uri("/api/v1/currency-execution-policies/" + policy)
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(service);
        when(service.get(policy, currency))
                .thenReturn(
                        Mono.error(
                                new org.springframework.web.server.ResponseStatusException(
                                        org.springframework.http.HttpStatus.FORBIDDEN,
                                        "Current policy denied")));
        client.get()
                .uri("/api/v1/currency-execution-policies/" + policy + "?currencyId=" + currency)
                .exchange()
                .expectStatus()
                .isForbidden();
    }

    @Test
    void strictUnknownBodyIsRejectedBeforeService() {
        client.post()
                .uri("/api/v1/currency-execution-policies")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .bodyValue(
                        CurrencyPolicyRequestDecoderTest.JSON.replace(
                                "}", ",\"scope\":\"untrusted\"}"))
                .exchange()
                .expectStatus()
                .isBadRequest();
        verifyNoInteractions(service);
    }
}
