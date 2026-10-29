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

import com.fasterxml.jackson.databind.json.JsonMapper;

import io.fireflysoftware.masters.core.services.document.v1.DocumentTemplateCanonicalJson;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public final class DocumentLayoutRules {
    public static final String CANONICALIZATION = "MASTERS_DOCUMENT_LAYOUT_V1";
    public static final String EMPTY_STYLE_DIGEST =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    private static final com.fasterxml.jackson.databind.ObjectMapper JSON =
            JsonMapper.builder().build();

    private DocumentLayoutRules() {}

    public record Canonical(String json, String digest) {}

    private record Intent(
            String kind,
            String targetTemplateId,
            String targetLayoutRevisionId,
            DocumentLayoutExpectedDraftReference expectedDraftReference,
            DocumentLayoutOutputProfileReference outputProfile,
            DocumentLayoutSemanticDefinition semanticDefinition,
            Integer expectedStateVersion,
            String reason) {}

    public static Canonical canonical(DocumentLayoutDefinition d) {
        if (d == null) throw bad("Layout definition is required");
        masterReference(d.sourceReference());
        profileMetadata(d.profileMetadata());
        semantics(d.semanticDefinition(), d.sourceReference().localeCode());
        if (!EMPTY_STYLE_DIGEST.equals(d.sourceReference().styleDigest()))
            throw conflict("Controlled layout source styles must be empty");
        return framed(CANONICALIZATION, d);
    }

    public static Canonical publicationIntent(String templateId, DocumentLayoutPublishRequest r) {
        uuid(templateId);
        if (r == null) throw bad("Publication request is required");
        uuid(r.actionId());
        if (r.expectedDraftReference() == null)
            throw conflict("An exact draft observation is required");
        draft(r.expectedDraftReference());
        if (!templateId.equals(r.expectedDraftReference().templateId()))
            throw conflict("Draft observation belongs to another template");
        profileReference(r.outputProfile());
        semantics(r.semanticDefinition(), r.expectedDraftReference().localeCode());
        return framed(
                "MASTERS_DOCUMENT_LAYOUT_ACTION_V1",
                new Intent(
                        "PUBLISH",
                        templateId,
                        null,
                        r.expectedDraftReference(),
                        r.outputProfile(),
                        r.semanticDefinition(),
                        null,
                        null));
    }

    public static Canonical retirementIntent(String layoutId, DocumentLayoutRetireRequest r) {
        uuid(layoutId);
        if (r == null) throw bad("Retirement request is required");
        uuid(r.actionId());
        if (r.expectedStateVersion() == null || r.expectedStateVersion() != 0)
            throw conflict("Layout retirement requires original expected state version zero");
        text(r.reason(), 500);
        return framed(
                "MASTERS_DOCUMENT_LAYOUT_ACTION_V1",
                new Intent(
                        "RETIRE",
                        null,
                        layoutId,
                        null,
                        null,
                        null,
                        r.expectedStateVersion(),
                        r.reason()));
    }

    public static void draft(DocumentLayoutExpectedDraftReference r) {
        if (r == null) throw conflict("An exact draft observation is required");
        uuid(r.templateId());
        uuid(r.localizationId());
        uuid(r.localeId());
        identity(r.templateCode(), r.version(), r.localeCode());
        media(r.mediaType());
        canonicalization(r.canonicalizationVersion());
        digests(r.sourceDigest(), r.contentDigest(), r.schemaDigest(), r.styleDigest());
    }

    public static void masterReference(DocumentLayoutMasterReference r) {
        if (r == null) throw bad("An exact master revision reference is required");
        uuid(r.revisionId());
        uuid(r.templateId());
        identity(r.templateCode(), r.version(), r.localeCode());
        media(r.mediaType());
        canonicalization(r.canonicalizationVersion());
        digests(r.sourceDigest(), r.contentDigest(), r.schemaDigest(), r.styleDigest());
    }

    public static void identity(String code, String version, String locale) {
        text(code, 100);
        if (version == null || !version.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,19}"))
            throw bad("An exact bounded template version is required");
        if (locale == null || !locale.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,49}"))
            throw bad("An exact bounded masters locale is required");
    }

    public static void profileReference(DocumentLayoutOutputProfileReference p) {
        if (p == null
                || p.code() == null
                || !p.code().matches("[A-Z][A-Z0-9_]{0,63}")
                || p.version() == null
                || !p.version().matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}"))
            throw bad("An exact output profile reference is required");
        media(p.mediaType());
        digests(p.manifestDigest());
    }

    public static void profileMetadata(DocumentLayoutOutputProfileMetadata p) {
        if (p == null) throw bad("Recognized profile metadata is required");
        profileReference(p.reference());
        fixed(p.sectionMode(), p.pageSize(), p.orientation());
        if (p.maxSections() == null
                || p.maxSections() < 1
                || p.maxSections() > 32
                || p.maxContentBytes() == null
                || p.maxContentBytes() < 1
                || p.maxContentBytes() > 1048576
                || p.allowedClassTokens() == null
                || p.allowedClassTokens().isEmpty()
                || p.allowedClassTokens().size() > 64)
            throw bad("Qualified profile limits and tokens are required");
        var seen = new HashSet<String>();
        for (String token : p.allowedClassTokens())
            if (token == null || !token.matches("[a-z][a-z0-9-]{0,63}") || !seen.add(token))
                throw bad("Profile class tokens must be bounded and unique");
    }

    public static void semantics(DocumentLayoutSemanticDefinition s, String locale) {
        if (s == null) throw bad("A closed semantic definition is required");
        fixed(s.sectionMode(), s.pageSize(), s.orientation());
        if (s.legalNotices() == null || s.legalNotices().isEmpty() || s.legalNotices().size() > 4)
            throw bad("One to four exact legal notices are required");
        var seen = EnumSet.noneOf(DocumentLayoutNoticeReference.Role.class);
        for (var n : s.legalNotices()) {
            if (n == null || n.role() == null || n.placement() == null || !seen.add(n.role()))
                throw bad("Notice roles must be present and unique");
            boolean disclosure = n.role() == DocumentLayoutNoticeReference.Role.GENERAL_DISCLOSURE;
            if (n.placement()
                    != (disclosure
                            ? DocumentLayoutNoticeReference.Placement.FOOTER
                            : DocumentLayoutNoticeReference.Placement.BODY_NOTE))
                throw bad("Notice placement does not match its closed role");
            masterReference(n.masterReference());
            if (!locale.equals(n.masterReference().localeCode()))
                throw conflict("Legal notices must use the exact source locale");
            if (!EMPTY_STYLE_DIGEST.equals(n.masterReference().styleDigest()))
                throw conflict("Legal notice styles must be empty");
        }
        if (!seen.contains(DocumentLayoutNoticeReference.Role.GENERAL_DISCLOSURE))
            throw bad("The general disclosure notice is required");
    }

    public static void noticeSchema(String schema) {
        try {
            var n = JSON.readTree(schema);
            var allowed =
                    Set.of("$schema", "type", "additionalProperties", "properties", "required");
            if (!n.isObject()
                    || !"object".equals(n.path("type").asText())
                    || !n.path("additionalProperties").isBoolean()
                    || n.path("additionalProperties").asBoolean())
                throw conflict("Legal notices require a closed empty-facts schema");
            for (var names = n.fieldNames(); names.hasNext(); )
                if (!allowed.contains(names.next()))
                    throw conflict("Legal notice schema contains an unsupported keyword");
            if (!n.has("$schema")
                    || !"https://json-schema.org/draft/2020-12/schema"
                            .equals(n.get("$schema").asText()))
                throw conflict("Legal notice schema dialect is unsupported");
            if (n.has("properties")
                    && (!n.get("properties").isObject() || !n.get("properties").isEmpty()))
                throw conflict("Legal notice schema cannot declare facts");
            if (n.has("required") && (!n.get("required").isArray() || !n.get("required").isEmpty()))
                throw conflict("Legal notice schema cannot require facts");
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException e) {
            throw conflict("Legal notice schema is invalid");
        }
    }

    public static UUID uuid(String value) {
        try {
            if (value == null) throw new IllegalArgumentException();
            UUID id = UUID.fromString(value);
            if (!id.toString().equals(value)) throw new IllegalArgumentException();
            return id;
        } catch (IllegalArgumentException e) {
            throw bad("A canonical UUID is required");
        }
    }

    public static String text(String value, int bound) {
        if (value == null
                || value.isEmpty()
                || value.codePointCount(0, value.length()) > bound
                || space(value.codePointAt(0))
                || space(value.codePointBefore(value.length()))
                || value.codePoints().anyMatch(Character::isISOControl))
            throw bad("Bounded exact text without controls or boundary whitespace is required");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(++i)))
                    throw bad("Invalid Unicode text");
            } else if (Character.isLowSurrogate(c)) throw bad("Invalid Unicode text");
        }
        return value;
    }

    private static boolean space(int cp) {
        return Character.isWhitespace(cp) || Character.isSpaceChar(cp);
    }

    private static void fixed(String mode, String page, String orientation) {
        if (!"AUTHORED_PAGES".equals(mode) || !"A4".equals(page) || !"PORTRAIT".equals(orientation))
            throw bad("Only authored A4 portrait semantics are supported");
    }

    private static void media(String media) {
        if (!"text/html".equals(media)) throw bad("The profile supports only text/html");
    }

    private static void canonicalization(String version) {
        if (!"MASTERS_JSON_V1".equals(version))
            throw bad("The exact masters canonicalization version is required");
    }

    private static void digests(String... values) {
        for (String s : values)
            if (s == null || !s.matches("[0-9a-f]{64}"))
                throw bad("Exact lower-case SHA256 references are required");
    }

    public static String json(Object value) {
        try {
            return DocumentTemplateCanonicalJson.canonicalize(JSON.writeValueAsString(value));
        } catch (com.fasterxml.jackson.core.JsonProcessingException | IllegalArgumentException e) {
            throw bad("Layout canonical JSON is invalid");
        }
    }

    public static Canonical framed(String frame, Object value) {
        String json = json(value);
        if (json.getBytes(StandardCharsets.UTF_8).length > 32768)
            throw bad("Layout canonical data exceeds its byte bound");
        return new Canonical(json, digest(frame + "\n" + json));
    }

    public static String digest(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    public static ResponseStatusException unavailable() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE, "Output profile registry is unavailable");
    }
}
