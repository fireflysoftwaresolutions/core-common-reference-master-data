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

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DocumentLayoutRequestDecoderTest {
    private static final String RETIRE =
            "{\"actionId\":\"77777777-7777-4777-8777-777777777777\",\"expectedStateVersion\":0,\"reason\":\"Fictional"
                + " withdrawal é\"}";

    @Test
    void retirementRetainsExactIntegerAndUnicode() {
        var r = DocumentLayoutRequestDecoder.retire(RETIRE);
        assertThat(r.expectedStateVersion()).isZero();
        assertThat(r.reason()).isEqualTo("Fictional withdrawal é");
    }

    @Test
    void noUnknownOrDuplicateFieldsAtAnyDepth() {
        for (String json :
                java.util.List.of(
                        "{\"actionId\":\"77777777-7777-4777-8777-777777777777\",\"css\":\"body{}\"}",
                        "{\"actionId\":\"a\",\"actionId\":\"b\"}",
                        "{\"semanticDefinition\":{\"pageSize\":\"A4\",\"pageSize\":\"A4\"}}",
                        "{\"outputProfile\":{\"unknown\":1}}"))
            bad(() -> DocumentLayoutRequestDecoder.publish(json));
    }

    @Test
    void nonintegralCoercedAndOverflowStateVersionReject() {
        for (String n : java.util.List.of("0.0", "0e0", "\"0\"", "false", "null", "2147483648"))
            bad(() -> DocumentLayoutRequestDecoder.retire(RETIRE.replace(":0,", ":" + n + ",")));
    }

    @Test
    void scalarStringsAndEnumsAreExact() {
        bad(
                () ->
                        DocumentLayoutRequestDecoder.retire(
                                RETIRE.replace("\"Fictional withdrawal é\"", "3")));
        bad(
                () ->
                        DocumentLayoutRequestDecoder.publish(
                                "{\"semanticDefinition\":{\"legalNotices\":[{\"role\":0}]}}"));
    }

    @Test
    void rootTrailingAndByteBoundsReject() {
        for (String json :
                java.util.List.of(
                        "null", "[]", "{}{}", "{\"actionId\":\"" + "é".repeat(16385) + "\"}"))
            bad(() -> DocumentLayoutRequestDecoder.publish(json));
    }

    @Test
    void wellFormedOtherStateVersionRemainsAServiceConflict() {
        for (String n : java.util.List.of("-1", "1", "2147483647"))
            assertThat(
                            DocumentLayoutRequestDecoder.retire(
                                            RETIRE.replace(":0,", ":" + n + ","))
                                    .expectedStateVersion())
                    .isEqualTo(Integer.parseInt(n));
    }

    static void bad(org.assertj.core.api.ThrowableAssert.ThrowingCallable work) {
        assertThatThrownBy(work)
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }
}
