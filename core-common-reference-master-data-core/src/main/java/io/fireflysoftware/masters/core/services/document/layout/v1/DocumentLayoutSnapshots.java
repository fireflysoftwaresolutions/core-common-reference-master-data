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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;
import io.fireflysoftware.masters.models.entities.document.layout.v1.DocumentLayoutPublication;
import io.fireflysoftware.masters.models.entities.document.v1.DocumentTemplateRevision;

import java.util.*;

/** Converts immutable snapshots without putting authored content in a policy context. */
public final class DocumentLayoutSnapshots {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private DocumentLayoutSnapshots() {}

    public static DocumentLayoutMasterReference reference(DocumentTemplateRevision r) {
        return new DocumentLayoutMasterReference(
                r.revisionId().toString(),
                r.templateId().toString(),
                r.templateCode(),
                r.version(),
                r.localeCode(),
                r.mediaType(),
                "MASTERS_JSON_V1",
                r.sourceDigest(),
                r.contentDigest(),
                r.schemaDigest(),
                r.styleDigest());
    }

    public static DocumentLayoutExpectedDraftReference draft(DocumentTemplateRevision r) {
        return new DocumentLayoutExpectedDraftReference(
                r.templateId().toString(),
                r.localizationId().toString(),
                r.localeId().toString(),
                r.templateCode(),
                r.version(),
                r.localeCode(),
                r.mediaType(),
                "MASTERS_JSON_V1",
                r.sourceDigest(),
                r.contentDigest(),
                r.schemaDigest(),
                r.styleDigest());
    }

    public static String version(String source) {
        try {
            return JSON.readTree(source).path("version").asText();
        } catch (JsonProcessingException e) {
            throw DocumentLayoutRules.conflict("Draft source cannot be read");
        }
    }

    public static String sourceCanonical(DocumentTemplateRevision r) {
        try {
            return io.fireflysoftware.masters.core.services.document.v1
                    .DocumentTemplateCanonicalJson.canonicalize(r.sourceSnapshot());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Stored source cannot be read", e);
        }
    }

    public static void source(DocumentTemplateRevision r) {
        DocumentLayoutRules.masterReference(reference(r));
        if (!r.styleContent().isEmpty())
            throw DocumentLayoutRules.conflict("Controlled layout source styles must be empty");
    }

    public static void notice(DocumentTemplateRevision r, DocumentLayoutMasterReference expected) {
        source(r);
        if (!"PUBLISHED".equals(r.lifecycle()))
            throw DocumentLayoutRules.conflict("Legal notice revision is no longer published");
        if (!expected.equals(reference(r)))
            throw DocumentLayoutRules.conflict(
                    "Legal notice does not match its exact revision pins");
        try {
            if (!"LEGAL_NOTICE".equals(JSON.readTree(r.sourceSnapshot()).path("category").asText()))
                throw DocumentLayoutRules.conflict("Legal notice category is required");
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored source cannot be read", e);
        }
        DocumentLayoutRules.noticeSchema(r.inputSchema());
    }

    public static DocumentLayoutDefinition definition(DocumentLayoutPublication r) {
        try {
            return JSON.readValue(r.canonicalJson(), DocumentLayoutDefinition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored layout definition cannot be read", e);
        }
    }

    public static DocumentLayoutPublicationDTO dto(DocumentLayoutPublication r) {
        return new DocumentLayoutPublicationDTO(
                r.layoutRevisionId(),
                r.sourceRevisionId(),
                r.templateId(),
                r.templateCode(),
                r.version(),
                r.localeCode(),
                DocumentLayoutRules.CANONICALIZATION,
                r.canonicalJson(),
                r.definitionDigest(),
                definition(r),
                r.lifecycle(),
                r.stateVersion(),
                r.publicationActionId(),
                r.publishedAtEpochMillis(),
                r.publishedBy(),
                r.retirementActionId(),
                r.retiredAtEpochMillis(),
                r.retiredBy(),
                r.retirementReason());
    }

    public static Map<String, Object> context(
            DocumentLayoutMasterReference source,
            DocumentLayoutOutputProfileMetadata profile,
            DocumentLayoutSemanticDefinition semantics) {
        var context = new LinkedHashMap<String, Object>();
        context.put("sourceReference", safeReference(source));
        if (profile != null) {
            context.put(
                    "outputProfile",
                    Map.of(
                            "code",
                            profile.reference().code(),
                            "version",
                            profile.reference().version(),
                            "mediaType",
                            profile.reference().mediaType(),
                            "manifestDigest",
                            profile.reference().manifestDigest(),
                            "metadataDigest",
                            DocumentLayoutRules.digest(DocumentLayoutRules.json(profile))));
        }
        if (semantics != null)
            context.put(
                    "legalNotices",
                    semantics.legalNotices().stream()
                            .map(
                                    n ->
                                            Map.of(
                                                    "role",
                                                    (Object) n.role().name(),
                                                    "placement",
                                                    n.placement().name(),
                                                    "masterReference",
                                                    safeReference(n.masterReference())))
                            .toList());
        return Map.copyOf(context);
    }

    public static Map<String, Object> context(DocumentLayoutPublication r) {
        var d = definition(r);
        var c =
                new LinkedHashMap<>(
                        context(d.sourceReference(), d.profileMetadata(), d.semanticDefinition()));
        c.put("layoutRevisionId", r.layoutRevisionId().toString());
        return Map.copyOf(c);
    }

    public static Map<String, Object> draftContext(
            DocumentTemplateRevision r,
            DocumentLayoutOutputProfileMetadata profile,
            DocumentLayoutSemanticDefinition semantics) {
        var c = new LinkedHashMap<>(context(reference(r), profile, semantics));
        var source = new LinkedHashMap<>(safeReference(reference(r)));
        source.remove("revisionId");
        source.put("localizationId", r.localizationId().toString());
        source.put("localeId", r.localeId().toString());
        c.put("sourceReference", Map.copyOf(source));
        return Map.copyOf(c);
    }

    private static Map<String, Object> safeReference(DocumentLayoutMasterReference r) {
        var c = new LinkedHashMap<String, Object>();
        c.put("revisionId", r.revisionId());
        c.put("templateId", r.templateId());
        c.put("templateCode", r.templateCode());
        c.put("version", r.version());
        c.put("localeCode", r.localeCode());
        c.put("mediaType", r.mediaType());
        c.put("canonicalizationVersion", r.canonicalizationVersion());
        c.put("sourceDigest", r.sourceDigest());
        c.put("contentDigest", r.contentDigest());
        c.put("schemaDigest", r.schemaDigest());
        c.put("styleDigest", r.styleDigest());
        return Map.copyOf(c);
    }
}
