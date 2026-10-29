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
public record DocumentLayoutExpectedDraftReference(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") String templateId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") String localizationId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, format = "uuid") String localeId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100) String templateCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 20) String version,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50) String localeCode,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        allowableValues = {"text/html"})
                String mediaType,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        allowableValues = {"MASTERS_JSON_V1"})
                String canonicalizationVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[0-9a-f]{64}$")
                String sourceDigest,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[0-9a-f]{64}$")
                String contentDigest,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[0-9a-f]{64}$")
                String schemaDigest,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "^[0-9a-f]{64}$")
                String styleDigest) {}
