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

package io.fireflysoftware.masters.core.services.currency.policy;

import static org.assertj.core.api.Assertions.*;

import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.v1.CurrencyDTO;
import io.fireflysoftware.masters.interfaces.enums.commons.v1.StatusEnum;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

class CurrencyPolicyRulesTest {
    static final String CURRENCY = "12345678-1234-4234-8234-123456789abc";
    static final String ACTION = "22345678-1234-4234-8234-123456789abc";

    static CurrencyDTO currency() {
        return CurrencyDTO.builder()
                .currencyId(UUID.fromString(CURRENCY))
                .isoCode("USD")
                .currencyName("US Dollar")
                .symbol("$")
                .decimalPrecision(2)
                .isMajor(true)
                .status(StatusEnum.ACTIVE)
                .dateCreated(LocalDateTime.of(1900, 1, 1, 0, 0))
                .build();
    }

    static CurrencyPolicyPublishRequest request(Integer scale, Long from, Long to) {
        return new CurrencyPolicyPublishRequest(
                ACTION,
                CURRENCY,
                "USD",
                "a".repeat(64),
                0L,
                scale,
                from,
                to,
                "Synthetic reference v1");
    }

    static void bad(Runnable work) {
        assertThatThrownBy(work::run)
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(400));
    }

    @Test
    void sourceIsExactStableAndPreservesNaiveMetadataWithoutInventingTimezone() {
        var one = CurrencyPolicyRules.source(currency());
        var two = CurrencyPolicyRules.source(currency());
        assertThat(one).isEqualTo(two);
        assertThat(one.encoding())
                .contains("MASTERS_CURRENCY_SOURCE_V1", "1900-01-01T00:00", "\"dateUpdated\":null");
        assertThat(one.digest())
                .isEqualTo("0cc9c9520b4df1f8e636a0dba910718b89b068f411b959204f04ca6309ba8544");
        var changed = currency();
        changed.setCurrencyName("US dollar revised");
        assertThat(CurrencyPolicyRules.source(changed).digest()).isNotEqualTo(one.digest());
    }

    @Test
    void precisionMustBeExplicitAndExactlyWithinExistingCatalogBounds() {
        for (int scale : new int[] {0, 1, 2, 10})
            CurrencyPolicyRules.publicationIntent(request(scale, 0L, null));
        for (Integer scale : new Integer[] {null, -1, 11})
            bad(() -> CurrencyPolicyRules.publicationIntent(request(scale, 0L, null)));
    }

    @Test
    void utcEffectivityIncludesLegitimatePreEpochAndUpperBound() {
        CurrencyPolicyRules.publicationIntent(request(2, -2208988800000L, 253402300799999L));
        CurrencyPolicyRules.publicationIntent(request(2, -1L, 0L));
        bad(() -> CurrencyPolicyRules.publicationIntent(request(2, -2208988800001L, null)));
        bad(() -> CurrencyPolicyRules.publicationIntent(request(2, 253402300800000L, null)));
        bad(() -> CurrencyPolicyRules.publicationIntent(request(2, 0L, 0L)));
        bad(() -> CurrencyPolicyRules.publicationIntent(request(2, null, null)));
    }

    @Test
    void immutableIntentBindsOptionalEndAndOriginalHead() {
        var a = CurrencyPolicyRules.publicationIntent(request(2, 0L, null));
        assertThat(CurrencyPolicyRules.publicationIntent(request(2, 0L, 1L)).digest())
                .isNotEqualTo(a.digest());
        var r = request(2, 0L, null);
        var changed =
                new CurrencyPolicyPublishRequest(
                        r.actionId(),
                        r.currencyId(),
                        r.isoCode(),
                        r.expectedCurrencyDigest(),
                        1L,
                        r.postedScale(),
                        r.effectiveFromEpochMillis(),
                        r.effectiveToEpochMillis(),
                        r.provenanceReference());
        assertThat(CurrencyPolicyRules.publicationIntent(changed).digest())
                .isNotEqualTo(a.digest());
    }

    @Test
    void identitiesAndDigestsDoNotCoerceOrNormalize() {
        bad(() -> CurrencyPolicyRules.uuid(CURRENCY.toUpperCase()));
        bad(() -> CurrencyPolicyRules.uuid("1-1-1-1-1"));
        bad(() -> CurrencyPolicyRules.digest("A".repeat(64)));
        bad(() -> CurrencyPolicyRules.iso("usd"));
        bad(() -> CurrencyPolicyRules.iso("US D"));
    }

    @Test
    void counterExhaustionCannotWrap() {
        assertThat(CurrencyPolicyRules.nextVersion(0L)).isEqualTo(1L);
        assertThat(CurrencyPolicyRules.nextVersion(Long.MAX_VALUE - 1)).isEqualTo(Long.MAX_VALUE);
        assertThatThrownBy(() -> CurrencyPolicyRules.nextVersion(Long.MAX_VALUE))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        bad(() -> CurrencyPolicyRules.nextVersion(-1L));
    }

    @Test
    void eventRoundtripRetainsExactReferenceVersionAndOriginalIntent() throws Exception {
        var intent = CurrencyPolicyRules.publicationIntent(request(2, -1L, null));
        var ref =
                new CurrencyPolicyReference(
                        "32345678-1234-4234-8234-123456789abc",
                        CURRENCY,
                        "USD",
                        3L,
                        "b".repeat(64),
                        CurrencyPolicyRules.POLICY_ENCODING);
        var encoded = CurrencyPolicyRules.event(ref, "PUBLISH", ACTION, intent, 0L);
        var json = com.fasterxml.jackson.databind.json.JsonMapper.builder().build();
        var event = json.readValue(encoded, CurrencyPolicyEvent.class);
        assertThat(event.payloadVersion()).isEqualTo("MASTERS_CURRENCY_POLICY_EVENT_V1");
        assertThat(event.policyReference()).isEqualTo(ref);
        assertThat(event.intentEncoding()).isEqualTo(intent.encoding());
        assertThat(event.intentDigest()).isEqualTo(intent.digest());
        assertThat(event.observedAtEpochMillis()).isZero();
        assertThat(json.readTree(encoded).size()).isEqualTo(7);
    }

    @Test
    void nonScalarUnicodeCannotEnterIntentsOrCanonicalSource() {
        for (String invalid :
                new String[] {"bad-\uD800", "bad-\uDC00", "bad-\uD800x", "bad-\uDC00\uD800"}) {
            var r = request(2, -1L, null);
            var malformed =
                    new CurrencyPolicyPublishRequest(
                            r.actionId(),
                            r.currencyId(),
                            r.isoCode(),
                            r.expectedCurrencyDigest(),
                            r.expectedHeadVersion(),
                            r.postedScale(),
                            r.effectiveFromEpochMillis(),
                            r.effectiveToEpochMillis(),
                            invalid);
            bad(() -> CurrencyPolicyRules.publicationIntent(malformed));
            bad(
                    () ->
                            CurrencyPolicyRules.retirementIntent(
                                    ACTION,
                                    new CurrencyPolicyRetireRequest(
                                            ACTION, CURRENCY, 0L, invalid)));
            var source = currency();
            source.setCurrencyName(invalid);
            bad(() -> CurrencyPolicyRules.source(source));
        }
        var source = currency();
        source.setCurrencyName("ordinary Ω \uD83D\uDE42");
        assertThat(CurrencyPolicyRules.source(source).encoding())
                .contains("ordinary Ω \uD83D\uDE42");
        CurrencyPolicyRules.text("ordinary Ω \uD83D\uDE42", 500);
        CurrencyPolicyRules.text("e\u0301", 500);
        assertThat(CurrencyPolicyRules.source(source).digest())
                .isNotEqualTo(CurrencyPolicyRules.source(currency()).digest());
    }
}
