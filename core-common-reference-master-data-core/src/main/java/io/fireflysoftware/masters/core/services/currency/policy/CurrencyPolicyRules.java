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

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.v1.CurrencyDTO;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class CurrencyPolicyRules {
    public static final String SOURCE_ENCODING = "MASTERS_CURRENCY_SOURCE_V1";
    public static final String POLICY_ENCODING = "MASTERS_CURRENCY_POLICY_V1";
    public static final long MIN_TIME = -2208988800000L, MAX_TIME = 253402300799999L;
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
            JsonMapper.builder().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).build();

    private CurrencyPolicyRules() {}

    public record Canonical(String encoding, String digest) {}

    public static UUID uuid(String value) {
        if (value == null
                || !value.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
            throw bad("Canonical currency policy UUID required");
        return UUID.fromString(value);
    }

    public static void digest(String value) {
        if (value == null || !value.matches("[0-9a-f]{64}"))
            throw bad("Exact lowercase SHA-256 digest required");
    }

    public static void iso(String value) {
        if (value == null || !value.matches("[A-Z]{3}"))
            throw bad("Exact uppercase ISO currency code required");
    }

    public static void text(String value, int limit) {
        if (value == null
                || value.isBlank()
                || value.length() > limit
                || value.indexOf('\u0000') >= 0) throw bad("Bounded currency policy text required");
        requireScalarUnicode(value);
    }

    private static void requireScalarUnicode(String value) {
        if (value == null) return;
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (++index >= value.length() || !Character.isLowSurrogate(value.charAt(index)))
                    throw bad("Exact scalar Unicode currency policy text required");
            } else if (Character.isLowSurrogate(current)) {
                throw bad("Exact scalar Unicode currency policy text required");
            }
        }
    }

    public static void time(Long value) {
        if (value == null || value < MIN_TIME || value > MAX_TIME)
            throw bad("Exact UTC time within1900 through9999 required");
    }

    public static long nextVersion(long head) {
        if (head < 0) throw bad("Nonnegative expected publication head required");
        if (head == Long.MAX_VALUE) throw conflict("Currency policy version exhausted");
        return head + 1;
    }

    public static Canonical source(CurrencyDTO value) {
        if (value == null || value.getCurrencyId() == null)
            throw bad("Exact canonical currency metadata required");
        requireScalarUnicode(value.getIsoCode());
        requireScalarUnicode(value.getCurrencyName());
        requireScalarUnicode(value.getSymbol());
        var data = new TreeMap<String, Object>();
        data.put("currencyId", value.getCurrencyId().toString());
        data.put("isoCode", value.getIsoCode());
        data.put("currencyName", value.getCurrencyName());
        data.put("symbol", value.getSymbol());
        data.put("decimalPrecision", value.getDecimalPrecision());
        data.put("isMajor", value.getIsMajor());
        data.put("status", value.getStatus() == null ? null : value.getStatus().name());
        data.put(
                "dateCreated",
                value.getDateCreated() == null ? null : value.getDateCreated().toString());
        data.put(
                "dateUpdated",
                value.getDateUpdated() == null ? null : value.getDateUpdated().toString());
        return framed(SOURCE_ENCODING, data);
    }

    public static Canonical publicationIntent(CurrencyPolicyPublishRequest r) {
        if (r == null) throw bad("Currency policy publication request required");
        uuid(r.actionId());
        uuid(r.currencyId());
        iso(r.isoCode());
        digest(r.expectedCurrencyDigest());
        if (r.expectedHeadVersion() == null || r.expectedHeadVersion() < 0)
            throw bad("Exact nonnegative expected head required");
        if (r.postedScale() == null || r.postedScale() < 0 || r.postedScale() > 10)
            throw bad("Explicit currency precision from0 through10 required");
        time(r.effectiveFromEpochMillis());
        if (r.effectiveToEpochMillis() != null) {
            time(r.effectiveToEpochMillis());
            if (r.effectiveToEpochMillis() <= r.effectiveFromEpochMillis())
                throw bad("Effectivity end must follow start");
        }
        text(r.provenanceReference(), 500);
        return framed("MASTERS_CURRENCY_ACTION_V1", Map.of("kind", "PUBLISH", "request", r));
    }

    public static Canonical retirementIntent(String policyId, CurrencyPolicyRetireRequest r) {
        uuid(policyId);
        if (r == null) throw bad("Currency policy retirement request required");
        uuid(r.actionId());
        uuid(r.currencyId());
        text(r.reason(), 500);
        if (r.expectedStateVersion() == null || r.expectedStateVersion() != 0)
            throw conflict("Retirement requires original state version zero");
        return framed(
                "MASTERS_CURRENCY_ACTION_V1",
                Map.of("kind", "RETIRE", "policyId", policyId, "request", r));
    }

    public static Canonical definition(
            CurrencyPolicyPublishRequest r, Canonical source, long version) {
        publicationIntent(r);
        var data = new TreeMap<String, Object>();
        data.put("currencyId", r.currencyId());
        data.put("isoCode", r.isoCode());
        data.put("version", version);
        data.put("sourceEncoding", source.encoding());
        data.put("sourceDigest", source.digest());
        data.put("postedScale", r.postedScale());
        data.put("effectiveFromEpochMillis", r.effectiveFromEpochMillis());
        data.put("effectiveToEpochMillis", r.effectiveToEpochMillis());
        data.put("provenanceReference", r.provenanceReference());
        return framed(POLICY_ENCODING, data);
    }

    public static String event(
            CurrencyPolicyReference reference,
            String kind,
            String actionId,
            Canonical intent,
            long observedAt) {
        if (reference == null
                || reference.version() == null
                || reference.version() < 1
                || !POLICY_ENCODING.equals(reference.encodingVersion())
                || kind == null
                || !Set.of("PUBLISH", "RETIRE").contains(kind)
                || intent == null) {
            throw bad("Exact versioned currency event reference required");
        }
        uuid(reference.policyId());
        uuid(reference.currencyId());
        uuid(actionId);
        iso(reference.isoCode());
        digest(reference.digest());
        digest(intent.digest());
        time(observedAt);
        try {
            return io.fireflysoftware.masters.core.services.document.v1
                    .DocumentTemplateCanonicalJson.canonicalize(
                    JSON.writeValueAsString(
                            new CurrencyPolicyEvent(
                                    "MASTERS_CURRENCY_POLICY_EVENT_V1",
                                    kind,
                                    actionId,
                                    reference,
                                    intent.encoding(),
                                    intent.digest(),
                                    observedAt)));
        } catch (Exception error) {
            throw bad("Currency policy event encoding failed");
        }
    }

    private static Canonical framed(String version, Object data) {
        try {
            String canonical =
                    io.fireflysoftware.masters.core.services.document.v1
                            .DocumentTemplateCanonicalJson.canonicalize(
                            JSON.writeValueAsString(data));
            String encoding = version + "\n" + canonical;
            return new Canonical(
                    encoding,
                    HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256")
                                            .digest(encoding.getBytes(StandardCharsets.UTF_8))));
        } catch (Exception e) {
            throw bad("Currency policy canonical encoding failed");
        }
    }

    public static ResponseStatusException bad(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

    public static ResponseStatusException conflict(String reason) {
        return new ResponseStatusException(HttpStatus.CONFLICT, reason);
    }
}
