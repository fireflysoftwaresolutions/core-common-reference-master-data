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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.type.LogicalType;

import io.fireflysoftware.masters.core.services.currency.policy.CurrencyPolicyRules;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;

import java.nio.charset.StandardCharsets;

public final class CurrencyPolicyRequestDecoder {
    private static final ObjectMapper JSON =
            JsonMapper.builder(
                            JsonFactory.builder()
                                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                                    .streamReadConstraints(
                                            StreamReadConstraints.builder()
                                                    .maxNestingDepth(8)
                                                    .maxStringLength(4096)
                                                    .maxNumberLength(32)
                                                    .build())
                                    .build())
                    .enable(
                            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                    .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                    .build();

    static {
        for (var type : new LogicalType[] {LogicalType.Textual, LogicalType.Integer}) {
            var config = JSON.coercionConfigFor(type);
            for (var shape :
                    new CoercionInputShape[] {
                        CoercionInputShape.String,
                        CoercionInputShape.Float,
                        CoercionInputShape.Boolean,
                        CoercionInputShape.Integer
                    })
                if (!(type == LogicalType.Textual && shape == CoercionInputShape.String)
                        && !(type == LogicalType.Integer && shape == CoercionInputShape.Integer))
                    config.setCoercion(shape, CoercionAction.Fail);
        }
    }

    private CurrencyPolicyRequestDecoder() {}

    public static CurrencyPolicyPublishRequest publish(String raw) {
        var tree = tree(raw);
        exact(tree, "expectedHeadVersion", false);
        exact(tree, "postedScale", false);
        exact(tree, "effectiveFromEpochMillis", false);
        exact(tree, "effectiveToEpochMillis", true);
        var r = read(tree, CurrencyPolicyPublishRequest.class);
        CurrencyPolicyRules.publicationIntent(r);
        return r;
    }

    public static CurrencyPolicyRetireRequest retire(String policyId, String raw) {
        var tree = tree(raw);
        exact(tree, "expectedStateVersion", false);
        var r = read(tree, CurrencyPolicyRetireRequest.class);
        CurrencyPolicyRules.retirementIntent(policyId, r);
        return r;
    }

    private static void exact(JsonNode tree, String key, boolean optional) {
        var n = tree.get(key);
        if (optional && (n == null || n.isNull())) return;
        if (n == null || !n.isIntegralNumber() || !n.canConvertToLong())
            throw CurrencyPolicyRules.bad("Exact bounded JSON integer required for " + key);
    }

    private static JsonNode tree(String raw) {
        try {
            if (raw == null || raw.getBytes(StandardCharsets.UTF_8).length > 8192)
                throw new IllegalArgumentException();
            var tree = JSON.readTree(raw);
            if (tree == null || !tree.isObject()) throw new IllegalArgumentException();
            return tree;
        } catch (Exception e) {
            throw CurrencyPolicyRules.bad("Bounded closed currency policy JSON required");
        }
    }

    private static <T> T read(JsonNode tree, Class<T> type) {
        try {
            return JSON.treeToValue(tree, type);
        } catch (Exception e) {
            throw CurrencyPolicyRules.bad("Currency policy fields must match exact types");
        }
    }
}
