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

package io.fireflysoftware.masters.interfaces.dtos.document.layout.v1;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record DocumentLayoutOutputProfileMetadata(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
                DocumentLayoutOutputProfileReference reference,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        allowableValues = {"AUTHORED_PAGES"})
                String sectionMode,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        allowableValues = {"A4"})
                String pageSize,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        allowableValues = {"PORTRAIT"})
                String orientation,
        @io.swagger.v3.oas.annotations.media.ArraySchema(
                        arraySchema = @Schema(requiredMode = Schema.RequiredMode.REQUIRED),
                        schema = @Schema(type = "string", pattern = "[a-z][a-z0-9-]{0,63}"),
                        minItems = 1,
                        maxItems = 64,
                        uniqueItems = true)
                java.util.List<String> allowedClassTokens,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "32")
                Integer maxSections,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "1048576")
                Integer maxContentBytes) {
    public DocumentLayoutOutputProfileMetadata {
        allowedClassTokens =
                allowedClassTokens == null ? null : java.util.List.copyOf(allowedClassTokens);
    }
}
