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

package io.fireflysoftware.masters.core.services.document.layout.v1;

import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;

/** Recognized immutable metadata only; no renderer, files, network or asset bytes. */
@FunctionalInterface
public interface DocumentLayoutProfileRegistry {
    DocumentLayoutOutputProfileMetadata requireRecognized(
            DocumentLayoutOutputProfileReference reference);
}
