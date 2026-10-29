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

package io.fireflysoftware.masters.web.controllers.document.layout.v1;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.fireflysoftware.masters.core.services.document.v1.DocumentLayoutPublicationService;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;

import org.fireflyframework.web.idempotency.annotation.DisableIdempotency;
import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

class DocumentLayoutPublicationControllerTest {
    @Test
    void newPublicationIs201AndNativeAcceptedReplayIs200() {
        var service = mock(DocumentLayoutPublicationService.class);
        var controller = new DocumentLayoutPublicationController(service);
        var id = java.util.UUID.randomUUID();
        when(service.publish(eq(id), any()))
                .thenReturn(
                        Mono.just(
                                new DocumentLayoutPublicationService.PublicationOutcome(
                                        null, false)),
                        Mono.just(
                                new DocumentLayoutPublicationService.PublicationOutcome(
                                        null, true)));
        assertThat(
                        controller
                                .publishDocumentLayoutRevision(id, "{}")
                                .block()
                                .getStatusCode()
                                .value())
                .isEqualTo(201);
        assertThat(
                        controller
                                .publishDocumentLayoutRevision(id, "{}")
                                .block()
                                .getStatusCode()
                                .value())
                .isEqualTo(200);
    }

    @Test
    void allNewLayoutHandlersOmitTransportRetryParameters() throws Exception {
        var type = DocumentLayoutPublicationController.class;
        assertThat(
                        type.getMethod(
                                        "publishDocumentLayoutRevision",
                                        java.util.UUID.class,
                                        String.class)
                                .isAnnotationPresent(DisableIdempotency.class))
                .isTrue();
        assertThat(
                        type.getMethod(
                                        "retireDocumentLayoutRevision",
                                        java.util.UUID.class,
                                        String.class)
                                .isAnnotationPresent(DisableIdempotency.class))
                .isTrue();
        assertThat(
                        java.util.Arrays.stream(type.getDeclaredMethods())
                                .filter(m -> m.isAnnotationPresent(DisableIdempotency.class))
                                .count())
                .isEqualTo(5);
    }

    @Test
    void semanticAndProfileArraysAdvertiseTheirExactBounds() throws Exception {
        var notices =
                DocumentLayoutSemanticDefinition.class
                        .getMethod("legalNotices")
                        .getAnnotation(io.swagger.v3.oas.annotations.media.ArraySchema.class);
        assertThat(notices).isNotNull();
        assertThat(notices.minItems()).isEqualTo(1);
        assertThat(notices.maxItems()).isEqualTo(4);
        var classes =
                DocumentLayoutOutputProfileMetadata.class
                        .getMethod("allowedClassTokens")
                        .getAnnotation(io.swagger.v3.oas.annotations.media.ArraySchema.class);
        assertThat(classes).isNotNull();
        assertThat(classes.minItems()).isEqualTo(1);
        assertThat(classes.maxItems()).isEqualTo(64);
    }
}
