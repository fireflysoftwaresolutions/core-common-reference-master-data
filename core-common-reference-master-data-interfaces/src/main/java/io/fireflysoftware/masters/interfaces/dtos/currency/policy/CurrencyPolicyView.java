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

public record CurrencyPolicyView(
        CurrencyPolicyReference reference,
        String canonicalEncoding,
        String sourceEncoding,
        String sourceDigest,
        Integer postedScale,
        Long effectiveFromEpochMillis,
        @Schema(
                        nullable = true,
                        minimum = "-2208988800000",
                        maximum = "253402300799999",
                        description = "Exclusive end; omitted or null means no effectivity end")
                Long effectiveToEpochMillis,
        String provenanceReference,
        String publicationActionId,
        Long publishedAtEpochMillis,
        String lifecycle,
        Long stateVersion,
        Long observedAtEpochMillis,
        Boolean effectiveNow) {}
