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

package io.fireflysoftware.masters.interfaces.dtos.currency.policy;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record CurrencyPolicyPublishRequest(
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                String actionId,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        pattern = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
                String currencyId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "[A-Z]{3}") String isoCode,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, pattern = "[0-9a-f]{64}")
                String expectedCurrencyDigest,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        minimum = "0",
                        maximum = "9223372036854775807")
                Long expectedHeadVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0", maximum = "10")
                Integer postedScale,
        @Schema(
                        requiredMode = Schema.RequiredMode.REQUIRED,
                        minimum = "-2208988800000",
                        maximum = "253402300799999")
                Long effectiveFromEpochMillis,
        @Schema(
                        nullable = true,
                        minimum = "-2208988800000",
                        maximum = "253402300799999",
                        description = "Exclusive end; omitted or null means no effectivity end")
                Long effectiveToEpochMillis,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 500)
                String provenanceReference) {}
