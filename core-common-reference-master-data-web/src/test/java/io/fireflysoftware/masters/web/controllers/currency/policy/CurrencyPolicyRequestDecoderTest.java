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

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CurrencyPolicyRequestDecoderTest {
    static final String JSON =
            "{\"actionId\":\"22345678-1234-4234-8234-123456789abc\",\"currencyId\":\"12345678-1234-4234-8234-123456789abc\",\"isoCode\":\"USD\",\"expectedCurrencyDigest\":\""
                    + "a".repeat(64)
                    + "\",\"expectedHeadVersion\":0,\"postedScale\":2,\"effectiveFromEpochMillis\":-1,\"provenanceReference\":\"Synthetic"
                    + " reference\"}";

    static void bad(String raw) {
        assertThatThrownBy(() -> CurrencyPolicyRequestDecoder.publish(raw))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void signedEpochAndOptionalEndDecodeExactly() {
        var r = CurrencyPolicyRequestDecoder.publish(JSON);
        assertThat(r.effectiveFromEpochMillis()).isEqualTo(-1L);
        assertThat(r.effectiveToEpochMillis()).isNull();
    }

    @Test
    void numericCoercionAndOverflowAreRejected() {
        for (String n : new String[] {"0.0", "0e0", "\"0\"", "null", "9223372036854775808", "true"})
            bad(JSON.replace("\"expectedHeadVersion\":0", "\"expectedHeadVersion\":" + n));
    }

    @Test
    void scaleAndDatesRequireExactJsonIntegers() {
        for (String field : new String[] {"postedScale", "effectiveFromEpochMillis"})
            for (String n : new String[] {"1.0", "1e0", "\"1\"", "null"})
                bad(
                        JSON.replace(
                                field.equals("postedScale")
                                        ? "\"postedScale\":2"
                                        : "\"effectiveFromEpochMillis\":-1",
                                "\"" + field + "\":" + n));
    }

    @Test
    void unknownDuplicateTrailingAndNoncanonicalIdentityAreRejected() {
        bad(JSON.replace("\"isoCode\":\"USD\"", "\"isoCode\":\"USD\",\"isoCode\":\"EUR\""));
        bad(JSON.replace("}", ",\"tenantId\":\"untrusted\"}"));
        bad(JSON + "{}");
        bad(JSON.replace("123456789abc", "123456789ABC"));
        bad(JSON.replace("\"USD\"", "123"));
    }
}
