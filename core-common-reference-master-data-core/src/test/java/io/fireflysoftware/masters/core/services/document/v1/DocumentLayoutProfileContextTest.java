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

package io.fireflysoftware.masters.core.services.document.v1;

import static org.assertj.core.api.Assertions.*;

import io.fireflysoftware.masters.core.services.document.layout.v1.*;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.web.server.ResponseStatusException;

class DocumentLayoutProfileContextTest {
    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(ApplicationScan.class)
                .withConfiguration(AutoConfigurations.of(DocumentLayoutProfileConfiguration.class));
    }

    private static final String[] METADATA = {
        "firefly.documents.layout.output-profiles[0].reference.code=SYNTHETIC_A4",
        "firefly.documents.layout.output-profiles[0].reference.version=1",
        "firefly.documents.layout.output-profiles[0].reference.media-type=text/html",
        "firefly.documents.layout.output-profiles[0].reference.manifest-digest=" + "2".repeat(64),
        "firefly.documents.layout.output-profiles[0].section-mode=AUTHORED_PAGES",
        "firefly.documents.layout.output-profiles[0].page-size=A4",
        "firefly.documents.layout.output-profiles[0].orientation=PORTRAIT",
        "firefly.documents.layout.output-profiles[0].allowed-class-tokens[0]=document-page",
        "firefly.documents.layout.output-profiles[0].max-sections=16",
        "firefly.documents.layout.output-profiles[0].max-content-bytes=262144"
    };

    static DocumentLayoutOutputProfileReference reference() {
        return new DocumentLayoutOutputProfileReference(
                "SYNTHETIC_A4", "1", "text/html", "2".repeat(64));
    }

    @Test
    void absentProjectRegistryUsesOneClosedDefault() {
        runner().run(
                        context -> {
                            assertThat(context)
                                    .hasNotFailed()
                                    .hasSingleBean(DocumentLayoutProfileRegistry.class);
                            assertThatThrownBy(
                                            () ->
                                                    context.getBean(
                                                                    DocumentLayoutProfileRegistry
                                                                            .class)
                                                            .requireRecognized(reference()))
                                    .isInstanceOfSatisfying(
                                            ResponseStatusException.class,
                                            e ->
                                                    assertThat(e.getStatusCode().value())
                                                            .isEqualTo(503));
                        });
    }

    @Test
    void defaultPropertiesCreateOneImmutableRecognizedMetadataRegistry() {
        runner().withPropertyValues(METADATA)
                .run(
                        context -> {
                            assertThat(context)
                                    .hasNotFailed()
                                    .hasSingleBean(DocumentLayoutProfileRegistry.class);
                            var registry = context.getBean(DocumentLayoutProfileRegistry.class);
                            assertThat(registry.requireRecognized(reference()).maxSections())
                                    .isEqualTo(16);
                            context.getBean(DocumentLayoutProfileConfiguration.Properties.class)
                                    .getOutputProfiles()
                                    .clear();
                            assertThat(registry.requireRecognized(reference()).allowedClassTokens())
                                    .containsExactly("document-page");
                        });
    }

    @Test
    void plainProjectBeanWinsAfterApplicationScanEvenWhenDefaultPropertiesArePresent() {
        new ApplicationContextRunner()
                .withUserConfiguration(ApplicationScan.class, ProjectProfiles.class)
                .withPropertyValues(METADATA)
                .run(
                        context -> {
                            assertThat(context)
                                    .hasNotFailed()
                                    .hasSingleBean(DocumentLayoutProfileRegistry.class);
                            assertThat(context.getBean(DocumentLayoutProfileRegistry.class))
                                    .isSameAs(ProjectProfiles.CUSTOM);
                            assertThat(
                                            context.getBean(DocumentLayoutProfileRegistry.class)
                                                    .requireRecognized(reference())
                                                    .maxSections())
                                    .isEqualTo(5);
                            assertThat(context).doesNotHaveBean("documentLayoutProfileRegistry");
                        });
    }

    @Test
    void deferredBootDefaultBacksOffToPlainProjectBeanWithoutPrimary() {
        runner().withUserConfiguration(ProjectProfiles.class)
                .withPropertyValues(METADATA)
                .run(
                        context -> {
                            assertThat(context)
                                    .hasNotFailed()
                                    .hasSingleBean(DocumentLayoutProfileRegistry.class);
                            assertThat(context.getBean(DocumentLayoutProfileRegistry.class))
                                    .isSameAs(ProjectProfiles.CUSTOM);
                            assertThat(context).doesNotHaveBean("documentLayoutProfileRegistry");
                        });
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan(
            basePackageClasses = DocumentLayoutProfileConfiguration.class,
            excludeFilters =
                    @ComponentScan.Filter(
                            type = FilterType.CUSTOM,
                            classes = AutoConfigurationExcludeFilter.class))
    static class ApplicationScan {}

    @Configuration(proxyBeanMethods = false)
    static class ProjectProfiles {
        static final DocumentLayoutProfileRegistry CUSTOM =
                ref -> {
                    if (!reference().equals(ref))
                        throw DocumentLayoutRules.conflict("Unknown synthetic project profile");
                    return new DocumentLayoutOutputProfileMetadata(
                            reference(),
                            "AUTHORED_PAGES",
                            "A4",
                            "PORTRAIT",
                            java.util.List.of("document-page"),
                            5,
                            262144);
                };

        @Bean
        DocumentLayoutProfileRegistry projectDocumentLayoutProfiles() {
            return CUSTOM;
        }
    }
}
