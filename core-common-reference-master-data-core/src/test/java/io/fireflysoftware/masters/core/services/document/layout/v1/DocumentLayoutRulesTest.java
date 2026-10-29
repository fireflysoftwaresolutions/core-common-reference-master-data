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

import static org.assertj.core.api.Assertions.*;

import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

public class DocumentLayoutRulesTest {
    public static final String TEMPLATE = "22222222-2222-4222-8222-222222222222";

    public static DocumentLayoutMasterReference source() {
        return new DocumentLayoutMasterReference(
                "11111111-1111-4111-8111-111111111111",
                TEMPLATE,
                "BODY",
                "1",
                "en-US",
                "text/html",
                "MASTERS_JSON_V1",
                "a".repeat(64),
                "b".repeat(64),
                "c".repeat(64),
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    public static DocumentLayoutMasterReference notice() {
        return new DocumentLayoutMasterReference(
                "33333333-3333-4333-8333-333333333333",
                "44444444-4444-4444-8444-444444444444",
                "NOTICE_GENERAL",
                "1",
                "en-US",
                "text/html",
                "MASTERS_JSON_V1",
                "e".repeat(64),
                "f".repeat(64),
                "0".repeat(64),
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    public static DocumentLayoutOutputProfileReference profileReference() {
        return new DocumentLayoutOutputProfileReference(
                "SYNTHETIC_A4", "1", "text/html", "2".repeat(64));
    }

    public static DocumentLayoutOutputProfileMetadata profile() {
        return new DocumentLayoutOutputProfileMetadata(
                profileReference(),
                "AUTHORED_PAGES",
                "A4",
                "PORTRAIT",
                List.of("document-page", "body-note"),
                16,
                262144);
    }

    public static DocumentLayoutNoticeReference disclosure() {
        return new DocumentLayoutNoticeReference(
                DocumentLayoutNoticeReference.Role.GENERAL_DISCLOSURE,
                DocumentLayoutNoticeReference.Placement.FOOTER,
                notice());
    }

    public static DocumentLayoutSemanticDefinition semantics() {
        return new DocumentLayoutSemanticDefinition(
                "AUTHORED_PAGES", "A4", "PORTRAIT", List.of(disclosure()));
    }

    public static DocumentLayoutExpectedDraftReference draft() {
        return new DocumentLayoutExpectedDraftReference(
                TEMPLATE,
                "55555555-5555-4555-8555-555555555555",
                "66666666-6666-4666-8666-666666666666",
                "BODY",
                "1",
                "en-US",
                "text/html",
                "MASTERS_JSON_V1",
                "a".repeat(64),
                "b".repeat(64),
                "c".repeat(64),
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    public static DocumentLayoutPublishRequest request() {
        return new DocumentLayoutPublishRequest(
                "77777777-7777-4777-8777-777777777777", draft(), profileReference(), semantics());
    }

    public static DocumentLayoutDefinition definition() {
        return new DocumentLayoutDefinition(source(), profile(), semantics());
    }

    @Test
    void fixedDefinitionCanonicalAndFramedDigestRetainDeclaredShape() {
        var c = DocumentLayoutRules.canonical(definition());
        assertThat(c.json()).isEqualTo(EXPECTED);
        assertThat(c.digest()).isEqualTo(DIGEST);
    }

    @Test
    void
            publicationIntentExcludesActionAndFutureResultButBindsExactDraftProfileAndOrderedNotices() {
        var r = request();
        var a = DocumentLayoutRules.publicationIntent(TEMPLATE, r);
        assertThat(a)
                .isEqualTo(
                        DocumentLayoutRules.publicationIntent(
                                TEMPLATE,
                                new DocumentLayoutPublishRequest(
                                        UUID.randomUUID().toString(),
                                        r.expectedDraftReference(),
                                        r.outputProfile(),
                                        r.semanticDefinition())));
        assertThat(a.digest())
                .isNotEqualTo(
                        DocumentLayoutRules.publicationIntent(
                                        TEMPLATE,
                                        new DocumentLayoutPublishRequest(
                                                r.actionId(),
                                                r.expectedDraftReference(),
                                                new DocumentLayoutOutputProfileReference(
                                                        "SYNTHETIC_A4",
                                                        "2",
                                                        "text/html",
                                                        "2".repeat(64)),
                                                r.semanticDefinition()))
                                .digest());
        var retire =
                DocumentLayoutRules.retirementIntent(
                        "88888888-8888-4888-8888-888888888888",
                        new DocumentLayoutRetireRequest(r.actionId(), 0, "Retained é withdrawal"));
        assertThat(retire.digest()).isNotEqualTo(a.digest());
        assertThat(retire.json()).contains("Retained é withdrawal");
    }

    @Test
    void semanticRolesHaveClosedPlacementUniqueOrderAndOneRequiredDisclosure() {
        for (var bad :
                List.of(
                        new DocumentLayoutSemanticDefinition(
                                "FLOW", "A4", "PORTRAIT", List.of(disclosure())),
                        new DocumentLayoutSemanticDefinition(
                                "AUTHORED_PAGES", "LETTER", "PORTRAIT", List.of(disclosure())),
                        new DocumentLayoutSemanticDefinition(
                                "AUTHORED_PAGES", "A4", "PORTRAIT", List.of()),
                        new DocumentLayoutSemanticDefinition(
                                "AUTHORED_PAGES",
                                "A4",
                                "PORTRAIT",
                                List.of(disclosure(), disclosure())),
                        new DocumentLayoutSemanticDefinition(
                                "AUTHORED_PAGES",
                                "A4",
                                "PORTRAIT",
                                List.of(
                                        new DocumentLayoutNoticeReference(
                                                DocumentLayoutNoticeReference.Role
                                                        .GENERAL_DISCLOSURE,
                                                DocumentLayoutNoticeReference.Placement.BODY_NOTE,
                                                notice())))))
            assertThatThrownBy(() -> DocumentLayoutRules.semantics(bad, "en-US"))
                    .isInstanceOf(ResponseStatusException.class);
        assertThatCode(() -> DocumentLayoutRules.semantics(semantics(), "en-US"))
                .doesNotThrowAnyException();
    }

    @Test
    void referencesAreExactBoundedCanonicalUuidAndUnsupportedMediaRejects() {
        for (String value :
                List.of(
                        "1-1-1-1-1",
                        "33333333-3333-4333-8333-33333333333X",
                        " 11111111-1111-4111-8111-111111111111"))
            assertThatThrownBy(() -> DocumentLayoutRules.uuid(value))
                    .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(
                        () ->
                                DocumentLayoutRules.profileReference(
                                        new DocumentLayoutOutputProfileReference(
                                                "SYNTHETIC_A4",
                                                "1",
                                                "application/pdf",
                                                "a".repeat(64))))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(
                        () ->
                                DocumentLayoutRules.publicationIntent(
                                        TEMPLATE,
                                        new DocumentLayoutPublishRequest(
                                                request().actionId(),
                                                null,
                                                profileReference(),
                                                semantics())))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void noticeSchemaRequiresTheExistingExactDraft202012PublicationPrerequisite() {
        String dialect = "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",";
        for (String fields :
                List.of(
                        "\"type\":\"object\",\"additionalProperties\":false",
                        "\"type\":\"object\",\"additionalProperties\":false,\"properties\":{},\"required\":[]"))
            assertThatCode(() -> DocumentLayoutRules.noticeSchema("{" + dialect + fields + "}"))
                    .doesNotThrowAnyException();
        for (String schema :
                List.of(
                        "{}",
                        "{\"type\":\"object\",\"additionalProperties\":false}",
                        "{" + dialect + "\"type\":\"object\",\"additionalProperties\":true}",
                        "{"
                                + dialect
                                + "\"type\":\"object\",\"additionalProperties\":false,\"minProperties\":1}",
                        "{"
                                + dialect
                                + "\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"name\":{\"type\":\"string\"}}}"))
            assertThatThrownBy(() -> DocumentLayoutRules.noticeSchema(schema))
                    .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void profileRegistryIsUnavailableByDefaultAndRecognizesOnlyExactMetadataTuple() {
        var properties = new DocumentLayoutProfileConfiguration.Properties();
        var config = new DocumentLayoutProfileConfiguration();
        var empty = config.documentLayoutProfileRegistry(properties);
        assertThatThrownBy(() -> empty.requireRecognized(profileReference()))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(503));
        properties.setOutputProfiles(List.of(profile()));
        var known = config.documentLayoutProfileRegistry(properties);
        assertThat(known.requireRecognized(profileReference())).isEqualTo(profile());
        assertThatThrownBy(
                        () ->
                                known.requireRecognized(
                                        new DocumentLayoutOutputProfileReference(
                                                "SYNTHETIC_A4", "2", "text/html", "2".repeat(64))))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }

    @Test
    void retainedSemanticAndProfileListsCannotChangeAfterCapture() {
        var notices = new java.util.ArrayList<>(semantics().legalNotices());
        var semantic =
                new DocumentLayoutSemanticDefinition("AUTHORED_PAGES", "A4", "PORTRAIT", notices);
        notices.clear();
        assertThat(semantic.legalNotices()).hasSize(1);
        assertThatThrownBy(() -> semantic.legalNotices().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        var classes = new java.util.ArrayList<>(profile().allowedClassTokens());
        var metadata =
                new DocumentLayoutOutputProfileMetadata(
                        profileReference(),
                        "AUTHORED_PAGES",
                        "A4",
                        "PORTRAIT",
                        classes,
                        16,
                        262144);
        classes.clear();
        assertThat(metadata.allowedClassTokens()).hasSize(2);
        assertThatThrownBy(() -> metadata.allowedClassTokens().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void aNoticeReadAsRetiredCannotPassCurrentSnapshotEligibility() {
        var ref = notice();
        var r =
                new io.fireflysoftware.masters.models.entities.document.v1.DocumentTemplateRevision(
                        java.util.UUID.fromString(ref.revisionId()),
                        java.util.UUID.fromString(ref.templateId()),
                        java.util.UUID.randomUUID(),
                        java.util.UUID.randomUUID(),
                        ref.templateCode(),
                        ref.version(),
                        ref.localeCode(),
                        "Notice",
                        ref.mediaType(),
                        "<p>Notice</p>",
                        "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false}",
                        "",
                        "{}",
                        "{\"category\":\"LEGAL_NOTICE\"}",
                        ref.contentDigest(),
                        ref.schemaDigest(),
                        ref.styleDigest(),
                        ref.sourceDigest(),
                        java.time.Instant.EPOCH,
                        "synthetic",
                        "RETIRED",
                        java.time.Instant.EPOCH,
                        "synthetic");
        assertThatThrownBy(() -> DocumentLayoutSnapshots.notice(r, ref))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
    }

    static final String EXPECTED =
            "{\"profileMetadata\":{\"allowedClassTokens\":[\"document-page\",\"body-note\"],\"maxContentBytes\":262144,\"maxSections\":16,\"orientation\":\"PORTRAIT\",\"pageSize\":\"A4\",\"reference\":{\"code\":\"SYNTHETIC_A4\",\"manifestDigest\":\"2222222222222222222222222222222222222222222222222222222222222222\",\"mediaType\":\"text/html\",\"version\":\"1\"},\"sectionMode\":\"AUTHORED_PAGES\"},\"semanticDefinition\":{\"legalNotices\":[{\"masterReference\":{\"canonicalizationVersion\":\"MASTERS_JSON_V1\",\"contentDigest\":\"ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff\",\"localeCode\":\"en-US\",\"mediaType\":\"text/html\",\"revisionId\":\"33333333-3333-4333-8333-333333333333\",\"schemaDigest\":\"0000000000000000000000000000000000000000000000000000000000000000\",\"sourceDigest\":\"eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\",\"styleDigest\":\"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\",\"templateCode\":\"NOTICE_GENERAL\",\"templateId\":\"44444444-4444-4444-8444-444444444444\",\"version\":\"1\"},\"placement\":\"FOOTER\",\"role\":\"GENERAL_DISCLOSURE\"}],\"orientation\":\"PORTRAIT\",\"pageSize\":\"A4\",\"sectionMode\":\"AUTHORED_PAGES\"},\"sourceReference\":{\"canonicalizationVersion\":\"MASTERS_JSON_V1\",\"contentDigest\":\"bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb\",\"localeCode\":\"en-US\",\"mediaType\":\"text/html\",\"revisionId\":\"11111111-1111-4111-8111-111111111111\",\"schemaDigest\":\"cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc\",\"sourceDigest\":\"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa\",\"styleDigest\":\"e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\",\"templateCode\":\"BODY\",\"templateId\":\"22222222-2222-4222-8222-222222222222\",\"version\":\"1\"}}";
    static final String DIGEST = "bb55c66ab18378d765e696918e94a82c4119851345c3fc6cfab764ad6e3e98d9";
}
