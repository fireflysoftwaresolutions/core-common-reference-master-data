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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.fireflysoftware.masters.core.services.document.v1.DocumentTemplateCanonicalJson;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;
import io.fireflysoftware.masters.models.repositories.document.v1.DocumentTemplateRevisionRepository;
import io.r2dbc.spi.ConnectionFactories;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.*;
import java.util.*;

@Testcontainers
public class DocumentLayoutDatabaseTest {
    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("synthetic_layout_native")
                    .withUsername("layout_test")
                    .withPassword("synthetic-only")
                    .withCreateContainerCmdModifier(
                            c ->
                                    c.getHostConfig()
                                            .withPortBindings(
                                                    new com.github.dockerjava.api.model.PortBinding(
                                                            com.github.dockerjava.api.model.Ports
                                                                    .Binding.bindIp("127.0.0.1"),
                                                            new com.github.dockerjava.api.model
                                                                    .ExposedPort(5432))));

    static final com.fasterxml.jackson.databind.ObjectMapper JSON = JsonMapper.builder().build();
    static final UUID LOCALE = UUID.randomUUID();

    @BeforeAll
    static void migrate() throws Exception {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        execute(
                "INSERT INTO"
                    + " language_locale(locale_id,language_code,locale_code,language_name,status)"
                    + " VALUES(?,'en','en-US','Synthetic English','ACTIVE')",
                LOCALE);
    }

    static Connection connection() throws SQLException {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    static void execute(String sql, Object... values) throws SQLException {
        try (var c = connection();
                var p = c.prepareStatement(sql)) {
            for (int i = 0; i < values.length; i++) p.setObject(i + 1, values[i]);
            p.executeUpdate();
        }
    }

    static long count(String table, String column, UUID value) throws SQLException {
        try (var c = connection();
                var p =
                        c.prepareStatement(
                                "SELECT count(*) FROM " + table + " WHERE " + column + "=?")) {
            p.setObject(1, value);
            try (var rows = p.executeQuery()) {
                rows.next();
                return rows.getLong(1);
            }
        }
    }

    static UUID draft(boolean notice) throws SQLException {
        UUID id = UUID.randomUUID();
        String code = (notice ? "NOTICE_" : "BODY_") + id.toString().replace("-", "");
        String category = notice ? "LEGAL_NOTICE" : "SYNTHETIC";
        String body =
                notice
                        ? "<p>Fictional legal notice é.</p>"
                        : "<section class=\"document-page\"><p>${facts.name}</p></section>";
        String schema =
                notice
                        ? "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false}"
                        : "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"name\":{\"type\":\"string\"}}}";
        execute(
                "INSERT INTO"
                    + " document_template_catalog(template_id,template_code,type_id,category,template_name,template_content,version,status,input_schema,style_content,media_type)"
                    + " SELECT ?,?,type_id,?,'Synthetic layout source',?,'1','ACTIVE',CAST(? AS"
                    + " jsonb),'','text/html' FROM document_template_type_catalog WHERE"
                    + " type_code='HTML'",
                id,
                code,
                category,
                body,
                schema);
        execute(
                "INSERT INTO"
                    + " document_template_localization(localization_id,template_id,locale_id,template_name,template_content,status)"
                    + " VALUES(?,?,?,'Synthetic localized source',?,'ACTIVE')",
                id,
                id,
                LOCALE,
                body);
        return id;
    }

    record Source(
            UUID id,
            DocumentLayoutMasterReference reference,
            DocumentLayoutExpectedDraftReference draft) {}

    static Source newSource(Connection c, UUID template) throws Exception {
        var factory =
                ConnectionFactories.get(
                        "r2dbc:postgresql://"
                                + postgres.getUsername()
                                + ":"
                                + postgres.getPassword()
                                + "@"
                                + postgres.getHost()
                                + ":"
                                + postgres.getMappedPort(5432)
                                + "/"
                                + postgres.getDatabaseName());
        String snapshot =
                new DocumentTemplateRevisionRepository(DatabaseClient.create(factory))
                        .lockSource(template, "en-US")
                        .block();
        JsonNode n = JSON.readTree(snapshot);
        String canonical = DocumentTemplateCanonicalJson.canonicalize(snapshot);
        String schema =
                DocumentTemplateCanonicalJson.canonicalize(
                        JSON.writeValueAsString(n.get("inputSchema")));
        String content = n.path("templateContent").asText(),
                style = n.path("styleContent").asText();
        UUID id = UUID.randomUUID();
        String code = n.path("templateCode").asText();
        String sourceDigest = DocumentLayoutRules.digest(canonical),
                contentDigest = DocumentLayoutRules.digest(content),
                schemaDigest = DocumentLayoutRules.digest(schema),
                styleDigest = DocumentLayoutRules.digest(style);
        try (var p =
                c.prepareStatement(
                        "INSERT INTO"
                            + " document_template_revision(revision_id,template_id,localization_id,locale_id,template_code,version,locale_code,template_name,media_type,template_content,input_schema,style_content,template_variables,source_snapshot,content_digest,schema_digest,style_digest,source_digest,published_at,published_by,lifecycle)"
                            + " VALUES(?,?,?,?,?,'1','en-US','Synthetic localized"
                            + " source','text/html',?,CAST(? AS jsonb),?,CAST(? AS jsonb),CAST(? AS"
                            + " jsonb),?,?,?,?,CURRENT_TIMESTAMP,'synthetic-layout-publisher','PUBLISHED')")) {
            Object[] args = {
                id,
                template,
                template,
                LOCALE,
                code,
                content,
                schema,
                style,
                DocumentTemplateCanonicalJson.canonicalize(
                        JSON.writeValueAsString(n.get("templateVariables"))),
                canonical,
                contentDigest,
                schemaDigest,
                styleDigest,
                sourceDigest
            };
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            p.executeUpdate();
        }
        var ref =
                new DocumentLayoutMasterReference(
                        id.toString(),
                        template.toString(),
                        code,
                        "1",
                        "en-US",
                        "text/html",
                        "MASTERS_JSON_V1",
                        sourceDigest,
                        contentDigest,
                        schemaDigest,
                        styleDigest);
        var expected =
                new DocumentLayoutExpectedDraftReference(
                        template.toString(),
                        template.toString(),
                        LOCALE.toString(),
                        code,
                        "1",
                        "en-US",
                        "text/html",
                        "MASTERS_JSON_V1",
                        sourceDigest,
                        contentDigest,
                        schemaDigest,
                        styleDigest);
        return new Source(id, ref, expected);
    }

    static Source notice() throws Exception {
        UUID draft = draft(true);
        try (var c = connection()) {
            return newSource(c, draft);
        }
    }

    static DocumentLayoutDefinition definition(Source source, Source notice) {
        return new DocumentLayoutDefinition(
                source.reference(),
                DocumentLayoutRulesTest.profile(),
                new DocumentLayoutSemanticDefinition(
                        "AUTHORED_PAGES",
                        "A4",
                        "PORTRAIT",
                        List.of(
                                new DocumentLayoutNoticeReference(
                                        DocumentLayoutNoticeReference.Role.GENERAL_DISCLOSURE,
                                        DocumentLayoutNoticeReference.Placement.FOOTER,
                                        notice.reference()))));
    }

    static UUID insert(Connection c, Source source, Source notice, UUID action, String override)
            throws Exception {
        var d = definition(source, notice);
        var canonical = DocumentLayoutRules.canonical(d);
        String raw = override == null ? canonical.json() : override;
        var request =
                new DocumentLayoutPublishRequest(
                        action.toString(),
                        source.draft(),
                        d.profileMetadata().reference(),
                        d.semanticDefinition());
        var intent =
                DocumentLayoutRules.publicationIntent(source.reference().templateId(), request);
        UUID id = UUID.randomUUID();
        try (var p =
                c.prepareStatement(
                        "INSERT INTO"
                            + " document_layout_publication(layout_revision_id,source_revision_id,template_id,template_code,version,locale_code,canonical_json,definition_digest,publication_intent_canonical,publication_intent_digest,publication_action_id,published_at_epoch_millis,published_by,lifecycle,state_version)"
                            + " VALUES(?,?,?,?,?,'en-US',?,?,?,?,?,1000,'synthetic-layout-publisher','PUBLISHED',0)")) {
            Object[] args = {
                id,
                source.id(),
                UUID.fromString(source.reference().templateId()),
                source.reference().templateCode(),
                "1",
                raw,
                DocumentLayoutRules.digest("MASTERS_DOCUMENT_LAYOUT_V1\n" + raw),
                intent.json(),
                intent.digest(),
                action
            };
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            p.executeUpdate();
        }
        return id;
    }

    static void rejected(String state, org.assertj.core.api.ThrowableAssert.ThrowingCallable work) {
        assertThatThrownBy(work)
                .isInstanceOfSatisfying(
                        SQLException.class, e -> assertThat(e.getSQLState()).isEqualTo(state));
    }

    @Test
    void nativeCanonicalAndRetainedTablesMustExist() throws Exception {
        try (var c = connection();
                var s = c.createStatement();
                var r = s.executeQuery("SELECT to_regclass('document_layout_publication')::text")) {
            r.next();
            assertThat(r.getString(1)).isEqualTo("document_layout_publication");
        }
    }

    @Test
    void javaNativeCanonicalBytesAndFramedDigestAgree() throws Exception {
        try (var c = connection();
                var s =
                        c.prepareStatement(
                                "SELECT document_layout_canonical(CAST(? AS"
                                    + " jsonb)),encode(sha256(convert_to('MASTERS_DOCUMENT_LAYOUT_V1'||chr(10)||?,'UTF8')),'hex')")) {
            s.setString(1, DocumentLayoutRulesTest.EXPECTED);
            s.setString(2, DocumentLayoutRulesTest.EXPECTED);
            try (var r = s.executeQuery()) {
                r.next();
                assertThat(r.getString(1)).isEqualTo(DocumentLayoutRulesTest.EXPECTED);
                assertThat(r.getString(2)).isEqualTo(DocumentLayoutRulesTest.DIGEST);
            }
        }
    }

    @Test
    void genuinelyNewSourceAssociationAndEvidenceCommitTogether() throws Exception {
        var n = notice();
        UUID template = draft(false), layout, action = UUID.randomUUID();
        try (var c = connection()) {
            c.setAutoCommit(false);
            var source = newSource(c, template);
            layout = insert(c, source, n, action, null);
            c.commit();
        }
        assertThat(count("document_layout_publication", "layout_revision_id", layout)).isEqualTo(1);
        for (String t :
                List.of(
                        "document_layout_action",
                        "document_layout_history",
                        "document_layout_outbox"))
            assertThat(count(t, "layout_revision_id", layout)).isEqualTo(1);
    }

    @Test
    void committedV1SourceCannotBeAdopted() throws Exception {
        var n = notice();
        Source s;
        try (var c = connection()) {
            s = newSource(c, draft(false));
        }
        try (var c = connection()) {
            c.setAutoCommit(false);
            rejected("23514", () -> insert(c, s, n, UUID.randomUUID(), null));
            c.rollback();
        }
        assertThat(count("document_layout_publication", "source_revision_id", s.id())).isZero();
    }

    @Test
    void committedSourceCannotAcquireFreshInsertIdentityThroughNoopOrRetirementUpdate()
            throws Exception {
        var n = notice();
        Source source;
        try (var c = connection()) {
            source = newSource(c, draft(false));
        }
        try (var c = connection()) {
            c.setAutoCommit(false);
            var savepoint = c.setSavepoint();
            rejected(
                    "23514",
                    () -> {
                        try (var p =
                                c.prepareStatement(
                                        "UPDATE document_template_revision SET version=version"
                                                + " WHERE revision_id=?")) {
                            p.setObject(1, source.id());
                            p.executeUpdate();
                        }
                    });
            c.rollback(savepoint);
            rejected("23514", () -> insert(c, source, n, UUID.randomUUID(), null));
            c.rollback();
        }
        try (var c = connection()) {
            c.setAutoCommit(false);
            try (var p =
                    c.prepareStatement(
                            "UPDATE document_template_revision SET"
                                + " lifecycle='RETIRED',retired_at=CURRENT_TIMESTAMP,retired_by='synthetic-retirer'"
                                + " WHERE revision_id=?")) {
                p.setObject(1, source.id());
                assertThat(p.executeUpdate()).isEqualTo(1);
            }
            rejected("23514", () -> insert(c, source, n, UUID.randomUUID(), null));
            c.rollback();
        }
        assertThat(count("document_layout_publication", "source_revision_id", source.id()))
                .isZero();
    }

    @Test
    void rollbackLeavesNoNewSourceAssociationOrEvidence() throws Exception {
        var n = notice();
        UUID template = draft(false), sourceId, layout;
        try (var c = connection()) {
            c.setAutoCommit(false);
            var s = newSource(c, template);
            sourceId = s.id();
            layout = insert(c, s, n, UUID.randomUUID(), null);
            c.rollback();
        }
        assertThat(count("document_template_revision", "revision_id", sourceId)).isZero();
        for (String t :
                List.of(
                        "document_layout_publication",
                        "document_layout_notice",
                        "document_layout_action",
                        "document_layout_history",
                        "document_layout_outbox"))
            assertThat(count(t, "layout_revision_id", layout)).isZero();
    }

    @Test
    void actualPinsAndClosedSemanticSnapshotRejectCorruptionBeforeCommit() throws Exception {
        var n = notice();
        for (String mutation : List.of("sourceDigest", "unknownCss", "emptyClasses", "wrongRole")) {
            UUID template = draft(false);
            try (var c = connection()) {
                c.setAutoCommit(false);
                var s = newSource(c, template);
                var raw = JSON.readTree(DocumentLayoutRules.canonical(definition(s, n)).json());
                if (mutation.equals("sourceDigest"))
                    ((com.fasterxml.jackson.databind.node.ObjectNode) raw.path("sourceReference"))
                            .put("sourceDigest", "f".repeat(64));
                if (mutation.equals("unknownCss"))
                    ((com.fasterxml.jackson.databind.node.ObjectNode) raw).put("css", "body{}");
                if (mutation.equals("emptyClasses"))
                    ((com.fasterxml.jackson.databind.node.ObjectNode) raw.path("profileMetadata"))
                            .putArray("allowedClassTokens");
                if (mutation.equals("wrongRole"))
                    ((com.fasterxml.jackson.databind.node.ObjectNode)
                                    raw.path("semanticDefinition").path("legalNotices").get(0))
                            .put("placement", "BODY_NOTE");
                String canonical =
                        DocumentTemplateCanonicalJson.canonicalize(JSON.writeValueAsString(raw));
                rejected("23514", () -> insert(c, s, n, UUID.randomUUID(), canonical));
                c.rollback();
            }
        }
    }

    @Test
    void noticeSchemaNativeProfileRequiresExactDialectAndOptionalEmptyFields() throws Exception {
        for (String schema :
                List.of(
                        "{\"type\":\"object\",\"additionalProperties\":false}",
                        "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false}",
                        "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{},\"required\":[]}",
                        "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false,\"minProperties\":1}")) {
            try (var c = connection();
                    var p =
                            c.prepareStatement(
                                    "SELECT document_layout_notice_schema_valid(CAST(? AS"
                                            + " jsonb))")) {
                p.setString(1, schema);
                try (var rows = p.executeQuery()) {
                    rows.next();
                    assertThat(rows.getBoolean(1))
                            .isEqualTo(
                                    schema.contains("$schema")
                                            && !schema.contains("minProperties"));
                }
            }
        }
    }

    @Test
    void retirementIsOneImmutableTransitionWithRetainedEvidence() throws Exception {
        var n = notice();
        UUID layout;
        try (var c = connection()) {
            c.setAutoCommit(false);
            layout = insert(c, newSource(c, draft(false)), n, UUID.randomUUID(), null);
            c.commit();
        }
        UUID action = UUID.randomUUID();
        execute(
                "UPDATE document_layout_publication SET"
                    + " lifecycle='RETIRED',state_version=1,retirement_action_id=?,retired_at_epoch_millis=2000,retired_by='synthetic-retirer',retirement_reason='Withdraw"
                    + " é' WHERE layout_revision_id=?",
                action,
                layout);
        for (String t :
                List.of(
                        "document_layout_action",
                        "document_layout_history",
                        "document_layout_outbox"))
            assertThat(count(t, "layout_revision_id", layout)).isEqualTo(2);
        rejected(
                "23514",
                () ->
                        execute(
                                "UPDATE document_layout_publication SET published_by='changed'"
                                        + " WHERE layout_revision_id=?",
                                layout));
        rejected(
                "23514",
                () ->
                        execute(
                                "DELETE FROM document_layout_publication WHERE"
                                        + " layout_revision_id=?",
                                layout));
        for (String t :
                List.of(
                        "document_layout_action",
                        "document_layout_history",
                        "document_layout_outbox"))
            rejected(
                    "23514",
                    () -> execute("DELETE FROM " + t + " WHERE layout_revision_id=?", layout));
    }

    @Test
    void exactOrderedNoticeLinksHaveRealForeignKeysAndCannotBeReboundOrInvented() throws Exception {
        var notice = notice();
        UUID template = draft(false), layout;
        try (var c = connection()) {
            c.setAutoCommit(false);
            layout = insert(c, newSource(c, template), notice, UUID.randomUUID(), null);
            c.commit();
        }
        assertThat(count("document_layout_notice", "layout_revision_id", layout)).isEqualTo(1);
        try (var c = connection();
                var p =
                        c.prepareStatement(
                                "SELECT role,placement,revision_id FROM document_layout_notice"
                                        + " WHERE layout_revision_id=? AND notice_position=0")) {
            p.setObject(1, layout);
            try (var rows = p.executeQuery()) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo("GENERAL_DISCLOSURE");
                assertThat(rows.getString(2)).isEqualTo("FOOTER");
                assertThat(rows.getObject(3, UUID.class)).isEqualTo(notice.id());
            }
        }
        try (var c = connection();
                var statement = c.createStatement();
                var rows =
                        statement.executeQuery(
                                "SELECT count(*) FROM pg_constraint WHERE"
                                        + " conrelid='document_layout_notice'::regclass AND"
                                        + " contype='f'")) {
            rows.next();
            assertThat(rows.getInt(1)).isEqualTo(2);
        }
        rejected(
                "23514",
                () ->
                        execute(
                                "UPDATE document_layout_notice SET revision_id=revision_id WHERE"
                                        + " layout_revision_id=?",
                                layout));
        rejected(
                "23514",
                () ->
                        execute(
                                "DELETE FROM document_layout_notice WHERE layout_revision_id=?",
                                layout));
        rejected(
                "23514",
                () ->
                        execute(
                                "INSERT INTO"
                                    + " document_layout_notice(layout_revision_id,notice_position,role,placement,revision_id)"
                                    + " VALUES(?,1,'PRIVACY_NOTICE','BODY_NOTE',?)",
                                layout,
                                notice.id()));
    }

    @Test
    void nativeSourceCanonicalizerPreservesExistingJavaUtf16ObjectKeyOrder() throws Exception {
        String raw = "{\"\uE000\":1,\"😀\":2}";
        String expected = DocumentTemplateCanonicalJson.canonicalize(raw);
        try (var c = connection();
                var p = c.prepareStatement("SELECT document_layout_canonical(CAST(? AS jsonb))")) {
            p.setString(1, raw);
            try (var rows = p.executeQuery()) {
                rows.next();
                assertThat(rows.getString(1)).isEqualTo(expected);
            }
        }
    }

    @Test
    void nativeSourceCanonicalizerPreservesJavaEscapesAndLiteralBackslashes() throws Exception {
        String raw = "{\"control\":\"\\u001f\",\"literal\":\"\\\\u001f\",\"line\":\"\\n\"}";
        String expected = DocumentTemplateCanonicalJson.canonicalize(raw);
        try (var c = connection();
                var p = c.prepareStatement("SELECT document_layout_canonical(CAST(? AS jsonb))")) {
            p.setString(1, raw);
            try (var rows = p.executeQuery()) {
                rows.next();
                assertThat(rows.getString(1)).isEqualTo(expected);
            }
        }
    }

    @Test
    void nativeCanonicalizerPreservesAllStorableControlsLiteralEscapesAndLargeUnicodeStrings()
            throws Exception {
        var controls = new StringBuilder();
        for (int cp = 1; cp < 32; cp++) controls.append((char) cp);
        controls.append("literal \\u001f \\u000B \\\\u001f");
        for (String text : List.of(controls.toString(), "Aé漢😀".repeat(5000))) {
            String raw = JSON.writeValueAsString(java.util.Map.of("value", text));
            try (var c = connection();
                    var p =
                            c.prepareStatement(
                                    "SELECT document_layout_canonical(CAST(? AS jsonb))")) {
                p.setString(1, raw);
                try (var rows = p.executeQuery()) {
                    rows.next();
                    assertThat(rows.getString(1))
                            .isEqualTo(DocumentTemplateCanonicalJson.canonicalize(raw));
                }
            }
        }
    }

    @Test
    void nativeCanonicalizerPreservesSupportedExactDecimalAndIntegerSourceValues()
            throws Exception {
        String raw =
                "{\"one\":1.2300,\"zero\":-0.0,\"positive\":1e3,\"tiny\":1e-30,\"large\":9007199254740993}";
        try (var c = connection();
                var p = c.prepareStatement("SELECT document_layout_canonical(CAST(? AS jsonb))")) {
            p.setString(1, raw);
            try (var rows = p.executeQuery()) {
                rows.next();
                assertThat(rows.getString(1))
                        .isEqualTo(DocumentTemplateCanonicalJson.canonicalize(raw));
            }
        }
    }
}
