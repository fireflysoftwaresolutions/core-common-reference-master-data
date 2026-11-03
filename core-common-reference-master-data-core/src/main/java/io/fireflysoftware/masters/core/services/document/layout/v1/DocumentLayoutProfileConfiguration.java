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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;

import java.util.*;

@AutoConfiguration
@EnableConfigurationProperties(DocumentLayoutProfileConfiguration.Properties.class)
public class DocumentLayoutProfileConfiguration {
    @Bean("documentLayoutClock")
    public java.time.Clock documentLayoutClock() {
        return java.time.Clock.systemUTC();
    }

    @ConfigurationProperties(prefix = "firefly.documents.layout")
    public static class Properties {
        private List<DocumentLayoutOutputProfileMetadata> outputProfiles = new ArrayList<>();

        public List<DocumentLayoutOutputProfileMetadata> getOutputProfiles() {
            return outputProfiles;
        }

        public void setOutputProfiles(List<DocumentLayoutOutputProfileMetadata> value) {
            outputProfiles = value;
        }
    }

    @Bean
    @ConditionalOnMissingBean(DocumentLayoutProfileRegistry.class)
    public DocumentLayoutProfileRegistry documentLayoutProfileRegistry(Properties properties) {
        var map =
                new HashMap<
                        DocumentLayoutOutputProfileReference,
                        DocumentLayoutOutputProfileMetadata>();
        for (var p :
                properties.getOutputProfiles() == null
                        ? List.<DocumentLayoutOutputProfileMetadata>of()
                        : properties.getOutputProfiles()) {
            DocumentLayoutRules.profileMetadata(p);
            var snapshot =
                    new DocumentLayoutOutputProfileMetadata(
                            p.reference(),
                            p.sectionMode(),
                            p.pageSize(),
                            p.orientation(),
                            List.copyOf(p.allowedClassTokens()),
                            p.maxSections(),
                            p.maxContentBytes());
            if (map.putIfAbsent(snapshot.reference(), snapshot) != null)
                throw new IllegalArgumentException("Duplicate output profile metadata reference");
        }
        var known = Map.copyOf(map);
        return reference -> {
            if (known.isEmpty()) throw DocumentLayoutRules.unavailable();
            var found = known.get(reference);
            if (found == null)
                throw DocumentLayoutRules.conflict(
                        "Exact output profile reference is not recognized");
            return found;
        };
    }
}
