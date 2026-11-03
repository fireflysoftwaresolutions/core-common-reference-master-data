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

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.fireflysoftware.masters.core.services.document.layout.v1.DocumentLayoutRules;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;

import java.nio.charset.StandardCharsets;

/** Local decoding leaves the existing masters JSON contract unchanged. */
public final class DocumentLayoutRequestDecoder {
    private static final ObjectMapper JSON =
            JsonMapper.builder(
                            com.fasterxml.jackson.core.JsonFactory.builder()
                                    .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                                    .streamReadConstraints(
                                            StreamReadConstraints.builder()
                                                    .maxNestingDepth(32)
                                                    .maxStringLength(32768)
                                                    .maxNumberLength(128)
                                                    .build())
                                    .build())
                    .enable(
                            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                            DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                            DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                    .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                    .build();

    static {
        var text = JSON.coercionConfigFor(com.fasterxml.jackson.databind.type.LogicalType.Textual);
        for (var shape :
                java.util.List.of(
                        com.fasterxml.jackson.databind.cfg.CoercionInputShape.Integer,
                        com.fasterxml.jackson.databind.cfg.CoercionInputShape.Float,
                        com.fasterxml.jackson.databind.cfg.CoercionInputShape.Boolean))
            text.setCoercion(shape, com.fasterxml.jackson.databind.cfg.CoercionAction.Fail);
    }

    private DocumentLayoutRequestDecoder() {}

    public static DocumentLayoutPublishRequest publish(String raw) {
        return read(tree(raw), DocumentLayoutPublishRequest.class);
    }

    public static DocumentLayoutRetireRequest retire(String raw) {
        var tree = tree(raw);
        var n = tree.get("expectedStateVersion");
        if (n == null || !n.isIntegralNumber() || !n.canConvertToInt())
            throw DocumentLayoutRules.bad(
                    "Retirement requires an exact bounded JSON integer expected state version");
        return read(tree, DocumentLayoutRetireRequest.class);
    }

    private static JsonNode tree(String raw) {
        try {
            if (raw == null || raw.getBytes(StandardCharsets.UTF_8).length > 32768)
                throw new IllegalArgumentException();
            var tree = JSON.readTree(raw);
            if (tree == null || !tree.isObject()) throw new IllegalArgumentException();
            return tree;
        } catch (Exception e) {
            throw DocumentLayoutRules.bad("A bounded closed layout JSON object is required");
        }
    }

    private static <T> T read(JsonNode tree, Class<T> type) {
        try {
            return JSON.treeToValue(tree, type);
        } catch (Exception e) {
            throw DocumentLayoutRules.bad("Layout request fields must match their exact types");
        }
    }
}
