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

package io.fireflysoftware.masters.sdk;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.fireflysoftware.common.reference.master.data.sdk.api.*;
import io.fireflysoftware.common.reference.master.data.sdk.invoker.ApiClient;
import io.fireflysoftware.common.reference.master.data.sdk.model.*;
import io.fireflysoftware.masters.core.services.document.layout.v1.DocumentLayoutRules;
import io.r2dbc.spi.ConnectionFactories;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Duration;
import java.util.*;

@Testcontainers
class DocumentLayoutPublicationRuntimeIT {
    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("synthetic_layout_http")
                    .withUsername("layout_http")
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

    static final Duration WAIT = Duration.ofSeconds(20);
    static final JsonMapper JSON = JsonMapper.builder().build();
    static final String CREDENTIAL = UUID.randomUUID().toString() + UUID.randomUUID();
    static Process application;
    static int port;

    @BeforeAll
    static void start() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        boot(true, true);
    }

    @AfterAll
    static void stop() {
        if (application == null) return;
        application.destroy();
        try {
            if (!application.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                application.destroyForcibly();
                assertThat(application.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    static void boot(boolean trust, boolean profile) {
        try {
            try (var socket =
                    new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
                port = socket.getLocalPort();
            }
            var jar =
                    java.nio.file.Path.of(
                                    "../core-common-reference-master-data-web/target/core-common-reference-master-data.jar")
                            .toAbsolutePath()
                            .normalize();
            var args = new ArrayList<String>();
            args.add(
                    java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java")
                            .toString());
            args.add("-Xmx384m");
            if (trust) {
                var classes =
                        java.nio.file.Path.of("target/layout-bootstrap-classes").toAbsolutePath();
                for (String suffix : List.of(".class", "$Fixture.class", "$CacheControl.class")) {
                    String resource =
                            "io/fireflysoftware/qualification/layout/DocumentLayoutRuntimeBootstrap"
                                    + suffix;
                    var target = classes.resolve(resource);
                    java.nio.file.Files.createDirectories(target.getParent());
                    try (var bytes =
                            DocumentLayoutPublicationRuntimeIT.class
                                    .getClassLoader()
                                    .getResourceAsStream(resource)) {
                        if (bytes == null)
                            throw new IllegalStateException("Private layout bootstrap is absent");
                        java.nio.file.Files.copy(
                                bytes, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                args.add("-Dloader.path=" + classes);
                args.add(
                        "-Dloader.main=io.fireflysoftware.qualification.layout.DocumentLayoutRuntimeBootstrap");
                args.add("-cp");
                args.add(jar.toString());
                args.add("org.springframework.boot.loader.launch.PropertiesLauncher");
            } else {
                args.add("-jar");
                args.add(jar.toString());
            }
            args.addAll(
                    List.of(
                            "--server.address=127.0.0.1",
                            "--server.port=" + port,
                            "--DB_HOST=" + postgres.getHost(),
                            "--DB_PORT=" + postgres.getMappedPort(5432),
                            "--DB_NAME=" + postgres.getDatabaseName(),
                            "--DB_USERNAME=" + postgres.getUsername(),
                            "--DB_PASSWORD=" + postgres.getPassword(),
                            "--DB_SSL_MODE=disable",
                            "--spring.flyway.enabled=false",
                            "--firefly.eda.enabled=false",
                            "--firefly.stepevents.enabled=false"));
            if (trust && profile)
                args.addAll(
                        List.of(
                                "--firefly.documents.layout.output-profiles[0].reference.code=SYNTHETIC_A4",
                                "--firefly.documents.layout.output-profiles[0].reference.version=1",
                                "--firefly.documents.layout.output-profiles[0].reference.media-type=text/html",
                                "--firefly.documents.layout.output-profiles[0].reference.manifest-digest="
                                        + "2".repeat(64),
                                "--firefly.documents.layout.output-profiles[0].section-mode=AUTHORED_PAGES",
                                "--firefly.documents.layout.output-profiles[0].page-size=A4",
                                "--firefly.documents.layout.output-profiles[0].orientation=PORTRAIT",
                                "--firefly.documents.layout.output-profiles[0].allowed-class-tokens[0]=document-page",
                                "--firefly.documents.layout.output-profiles[0].allowed-class-tokens[1]=body-note",
                                "--firefly.documents.layout.output-profiles[0].max-sections=16",
                                "--firefly.documents.layout.output-profiles[0].max-content-bytes=262144"));
            var log =
                    java.nio.file.Path.of(
                            "target/layout-runtime-"
                                    + (trust ? "trusted-" : "unconfigured-")
                                    + port
                                    + ".log");
            var builder =
                    new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log.toFile());
            builder.environment().put("DOCUMENT_LAYOUT_FIXTURE_CREDENTIAL", CREDENTIAL);
            application = builder.start();
            for (int i = 0; i < 120; i++) {
                if (!application.isAlive())
                    throw new IllegalStateException("Private layout runtime exited; see " + log);
                try {
                    if (call("GET", "/v3/api-docs", null, null, null).status() == 200) return;
                } catch (Exception ignored) {
                }
                Thread.sleep(250);
            }
            throw new IllegalStateException("Private layout runtime startup timed out; see " + log);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static ApiClient client() {
        var client = new ApiClient();
        client.setBasePath("http://127.0.0.1:" + port);
        client.addDefaultHeader("Authorization", "Bearer " + CREDENTIAL);
        return client;
    }

    static DatabaseClient database() {
        return DatabaseClient.create(
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
                                + postgres.getDatabaseName()));
    }

    record Response(int status, JsonNode body) {}

    static Response call(String method, String path, Object body, String credential, String key) {
        try {
            var request =
                    java.net.http.HttpRequest.newBuilder(
                                    java.net.URI.create("http://127.0.0.1:" + port + path))
                            .timeout(Duration.ofSeconds(3));
            if (credential != null) request.header("Authorization", "Bearer " + credential);
            if (key != null) request.header("X-Idempotency-Key", key);
            if (body != null) request.header("Content-Type", "application/json");
            request.method(
                    method,
                    body == null
                            ? java.net.http.HttpRequest.BodyPublishers.noBody()
                            : java.net.http.HttpRequest.BodyPublishers.ofString(
                                    body instanceof String s ? s : JSON.writeValueAsString(body)));
            var response =
                    java.net.http.HttpClient.newHttpClient()
                            .send(
                                    request.build(),
                                    java.net.http.HttpResponse.BodyHandlers.ofString());
            return new Response(
                    response.statusCode(),
                    response.body().isEmpty() ? JSON.nullNode() : JSON.readTree(response.body()));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    record Draft(UUID id, String code, String locale) {}

    static Draft draft(boolean notice) {
        String locale = "en-" + UUID.randomUUID().toString().substring(0, 7);
        var loc =
                new LanguageLocaleApi(client())
                        .createLanguageLocale(
                                new LanguageLocaleDTO()
                                        .languageCode("en")
                                        .localeCode(locale)
                                        .languageName("Synthetic layout English")
                                        .status(LanguageLocaleDTO.StatusEnum.ACTIVE),
                                null)
                        .block(WAIT);
        UUID type =
                database()
                        .sql(
                                "SELECT type_id FROM document_template_type_catalog WHERE"
                                        + " type_code='HTML'")
                        .map((r, m) -> r.get("type_id", UUID.class))
                        .one()
                        .block(WAIT);
        String code = "LAYOUT_" + UUID.randomUUID().toString().replace("-", "");
        String schema =
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false}";
        var catalog =
                new DocumentTemplatesApi(client())
                        .createDocumentTemplate(
                                new DocumentTemplateCatalogDTO()
                                        .templateCode(code)
                                        .typeId(type)
                                        .category(notice ? "LEGAL_NOTICE" : "SYNTHETIC")
                                        .templateName("Synthetic authored layout")
                                        .templateContent("<p>Synthetic base é.</p>")
                                        .mediaType("text/html")
                                        .styleContent("")
                                        .version("1")
                                        .status(DocumentTemplateCatalogDTO.StatusEnum.ACTIVE)
                                        .inputSchema(null)
                                        .inputSchemaJson(schema),
                                null)
                        .block(WAIT);
        new DocumentTemplateLocalizationsApi(client())
                .createDocumentTemplateLocalization(
                        new DocumentTemplateLocalizationDTO()
                                .templateId(catalog.getTemplateId())
                                .localeId(loc.getLocaleId())
                                .templateName("Synthetic authored layout")
                                .templateContent(
                                        notice
                                                ? "<p>Fictional notice é.</p>"
                                                : "<section class=\"document-page\"><p>Fictional"
                                                        + " source é.</p></section>")
                                .status(DocumentTemplateLocalizationDTO.StatusEnum.ACTIVE),
                        null)
                .block(WAIT);
        return new Draft(catalog.getTemplateId(), code, locale);
    }

    record Intent(Draft draft, JsonNode request) {}

    static Intent intent() {
        var main = draft(false);
        var noticeId = UUID.randomUUID();
        String code = "NOTICE_" + noticeId.toString().replace("-", "");
        UUID locale =
                database()
                        .sql("SELECT locale_id FROM language_locale WHERE locale_code=:code")
                        .bind("code", main.locale())
                        .map((r, m) -> r.get("locale_id", UUID.class))
                        .one()
                        .block(WAIT);
        UUID type =
                database()
                        .sql(
                                "SELECT type_id FROM document_template_type_catalog WHERE"
                                        + " type_code='HTML'")
                        .map((r, m) -> r.get("type_id", UUID.class))
                        .one()
                        .block(WAIT);
        String schema =
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false}";
        var note =
                new DocumentTemplatesApi(client())
                        .createDocumentTemplate(
                                new DocumentTemplateCatalogDTO()
                                        .templateCode(code)
                                        .typeId(type)
                                        .category("LEGAL_NOTICE")
                                        .templateName("Synthetic disclosure")
                                        .templateContent("<p>Fictional notice é.</p>")
                                        .mediaType("text/html")
                                        .styleContent("")
                                        .version("1")
                                        .status(DocumentTemplateCatalogDTO.StatusEnum.ACTIVE)
                                        .inputSchema(null)
                                        .inputSchemaJson(schema),
                                null)
                        .block(WAIT);
        new DocumentTemplateLocalizationsApi(client())
                .createDocumentTemplateLocalization(
                        new DocumentTemplateLocalizationDTO()
                                .templateId(note.getTemplateId())
                                .localeId(locale)
                                .templateName("Synthetic disclosure")
                                .templateContent("<p>Fictional notice é.</p>")
                                .status(DocumentTemplateLocalizationDTO.StatusEnum.ACTIVE),
                        null)
                .block(WAIT);
        var published =
                new DocumentTemplateRevisionsApi(client())
                        .publishDocumentTemplateRevision(
                                note.getTemplateId(),
                                new DocumentTemplatePublicationRequest()
                                        .version("1")
                                        .localeCode(main.locale()),
                                null)
                        .block(WAIT);
        var ref =
                Map.ofEntries(
                        Map.entry("revisionId", published.getRevisionId().toString()),
                        Map.entry("templateId", published.getTemplateId().toString()),
                        Map.entry("templateCode", published.getTemplateCode()),
                        Map.entry("version", published.getVersion()),
                        Map.entry("localeCode", published.getLocaleCode()),
                        Map.entry("mediaType", published.getMediaType()),
                        Map.entry(
                                "canonicalizationVersion", published.getCanonicalizationVersion()),
                        Map.entry("sourceDigest", published.getSourceDigest()),
                        Map.entry("contentDigest", published.getContentDigest()),
                        Map.entry("schemaDigest", published.getSchemaDigest()),
                        Map.entry("styleDigest", published.getStyleDigest()));
        var observed =
                call(
                        "GET",
                        base(main) + "/source-observation?localeCode=" + main.locale(),
                        null,
                        CREDENTIAL,
                        null);
        assertThat(observed.status()).isEqualTo(200);
        var request =
                JSON.valueToTree(
                        Map.of(
                                "actionId",
                                UUID.randomUUID().toString(),
                                "expectedDraftReference",
                                observed.body().get("draftReference"),
                                "outputProfile",
                                Map.of(
                                        "code",
                                        "SYNTHETIC_A4",
                                        "version",
                                        "1",
                                        "mediaType",
                                        "text/html",
                                        "manifestDigest",
                                        "2".repeat(64)),
                                "semanticDefinition",
                                Map.of(
                                        "sectionMode",
                                        "AUTHORED_PAGES",
                                        "pageSize",
                                        "A4",
                                        "orientation",
                                        "PORTRAIT",
                                        "legalNotices",
                                        List.of(
                                                Map.of(
                                                        "role",
                                                        "GENERAL_DISCLOSURE",
                                                        "placement",
                                                        "FOOTER",
                                                        "masterReference",
                                                        ref)))));
        return new Intent(main, request);
    }

    static String base(Draft draft) {
        return "/api/v1/document-layout-publications/templates/" + draft.id();
    }

    @Test
    void actualNativeHttpPublishReplayReadResolveRetireAndRetainedCanonicalStrings() {
        var intent = intent();
        var first = call("POST", base(intent.draft()), intent.request(), CREDENTIAL, null);
        assertThat(first.status()).isEqualTo(201);
        var row = first.body();
        assertThat(row.path("stateVersion").asInt()).isZero();
        assertThat(row.path("publishedBy").asText()).isEqualTo("synthetic-layout-admin");
        assertThat(row.path("canonicalJson").isTextual()).isTrue();
        assertThat(row.path("definitionDigest").asText())
                .isEqualTo(
                        DocumentLayoutRules.digest(
                                "MASTERS_DOCUMENT_LAYOUT_V1\n"
                                        + row.path("canonicalJson").asText()));
        String id = row.path("layoutRevisionId").asText();
        assertThat(
                        call(
                                        "GET",
                                        "/api/v1/document-layout-publications/" + id,
                                        null,
                                        CREDENTIAL,
                                        null)
                                .body())
                .isEqualTo(row);
        assertThat(
                        call(
                                        "POST",
                                        base(intent.draft()),
                                        intent.request(),
                                        CREDENTIAL,
                                        "same-native-action")
                                .body())
                .isEqualTo(row);
        assertThat(call("GET", resolve(intent.draft()), null, CREDENTIAL, null).status())
                .isEqualTo(200);
        var retire =
                Map.of(
                        "actionId",
                        UUID.randomUUID().toString(),
                        "expectedStateVersion",
                        0,
                        "reason",
                        "Fictional withdrawal é");
        var retired =
                call(
                        "POST",
                        "/api/v1/document-layout-publications/" + id + "/retire",
                        retire,
                        CREDENTIAL,
                        null);
        assertThat(retired.status()).isEqualTo(200);
        assertThat(retired.body().path("stateVersion").asInt()).isEqualTo(1);
        assertThat(
                        call(
                                        "POST",
                                        "/api/v1/document-layout-publications/" + id + "/retire",
                                        retire,
                                        CREDENTIAL,
                                        "ignored-native")
                                .body())
                .isEqualTo(retired.body());
        assertThat(call("POST", base(intent.draft()), intent.request(), CREDENTIAL, null).body())
                .isEqualTo(retired.body());
        assertThat(call("GET", resolve(intent.draft()), null, CREDENTIAL, null).status())
                .isEqualTo(409);
    }

    static String resolve(Draft draft) {
        return "/api/v1/document-layout-publications/resolve?templateCode="
                + draft.code()
                + "&version=1&localeCode="
                + draft.locale()
                + "&outputProfileCode=SYNTHETIC_A4&outputProfileVersion=1&outputProfileManifestDigest="
                + "2".repeat(64);
    }

    @Test
    void runtimeStrictBodyRejectsUnknownDuplicateAndNonintegralBeforeEffects() {
        var intent = intent();
        var changed = intent.request().deepCopy();
        ((com.fasterxml.jackson.databind.node.ObjectNode) changed).put("css", "body{}");
        assertThat(call("POST", base(intent.draft()), changed, CREDENTIAL, null).status())
                .isEqualTo(400);
        assertThat(
                        call(
                                        "POST",
                                        base(intent.draft()),
                                        "{\"actionId\":\"a\",\"actionId\":\"b\"}",
                                        CREDENTIAL,
                                        null)
                                .status())
                .isEqualTo(400);
        var row = call("POST", base(intent.draft()), intent.request(), CREDENTIAL, null).body();
        assertThat(
                        call(
                                        "POST",
                                        "/api/v1/document-layout-publications/"
                                                + row.path("layoutRevisionId").asText()
                                                + "/retire",
                                        "{\"actionId\":\""
                                                + UUID.randomUUID()
                                                + "\",\"expectedStateVersion\":0.0,\"reason\":\"Withdraw\"}",
                                        CREDENTIAL,
                                        null)
                                .status())
                .isEqualTo(400);
        assertThat(
                        call(
                                        "POST",
                                        "/api/v1/document-layout-publications/"
                                                + row.path("layoutRevisionId").asText()
                                                + "/retire",
                                        Map.of(
                                                "actionId",
                                                UUID.randomUUID().toString(),
                                                "expectedStateVersion",
                                                1,
                                                "reason",
                                                "Withdraw"),
                                        CREDENTIAL,
                                        null)
                                .status())
                .isEqualTo(409);
        assertThat(
                        call(
                                        "GET",
                                        "/api/v1/document-layout-publications/"
                                                + row.path("layoutRevisionId").asText(),
                                        null,
                                        CREDENTIAL,
                                        null)
                                .body()
                                .path("stateVersion")
                                .asInt())
                .isZero();
    }

    @Test
    void currentPolicyIsEvaluatedForSameKeyAndHeaderlessNativeReplay() {
        String cacheKey = UUID.randomUUID().toString();
        var cacheBody = Map.of("nonce", UUID.randomUUID().toString());
        var cachedFirst =
                call("POST", "/synthetic/layout-cache-control", cacheBody, CREDENTIAL, cacheKey);
        assertThat(cachedFirst.status()).isEqualTo(200);
        assertThat(cachedFirst.body().path("invocations").asInt()).isEqualTo(1);
        assertThat(
                        call(
                                        "POST",
                                        "/synthetic/layout-cache-control",
                                        cacheBody,
                                        CREDENTIAL + "-denied",
                                        cacheKey)
                                .body())
                .isEqualTo(cachedFirst.body());
        var intent = intent();
        String key = UUID.randomUUID().toString();
        assertThat(call("POST", base(intent.draft()), intent.request(), CREDENTIAL, key).status())
                .isEqualTo(201);
        assertThat(call("POST", base(intent.draft()), intent.request(), CREDENTIAL, key).status())
                .isEqualTo(200);
        assertThat(
                        call(
                                        "POST",
                                        base(intent.draft()),
                                        intent.request(),
                                        CREDENTIAL + "-denied",
                                        key)
                                .status())
                .isEqualTo(403);
        assertThat(
                        call(
                                        "POST",
                                        base(intent.draft()),
                                        intent.request(),
                                        CREDENTIAL + "-denied",
                                        null)
                                .status())
                .isEqualTo(403);
    }

    @Test
    void runtimeSpecKeepsTypedClosedRequestsAndHasNoTransportKeyParameter() {
        var spec = call("GET", "/v3/api-docs", null, null, null).body();
        for (String path :
                List.of(
                        "/api/v1/document-layout-publications/templates/{templateId}",
                        "/api/v1/document-layout-publications/{layoutRevisionId}/retire")) {
            var post = spec.path("paths").path(path).path("post");
            assertThat(
                            post.path("requestBody")
                                    .path("content")
                                    .path("application/json")
                                    .path("schema")
                                    .path("$ref")
                                    .asText())
                    .startsWith("#/components/schemas/DocumentLayout");
            for (var parameter : post.path("parameters"))
                assertThat(parameter.path("name").asText()).isNotEqualTo("X-Idempotency-Key");
        }
        for (String path :
                List.of(
                        "/api/v1/document-layout-publications/templates/{templateId}/source-observation",
                        "/api/v1/document-layout-publications/{layoutRevisionId}",
                        "/api/v1/document-layout-publications/resolve"))
            for (var parameter : spec.path("paths").path(path).path("get").path("parameters"))
                assertThat(parameter.path("name").asText()).isNotEqualTo("X-Idempotency-Key");
        assertThat(
                        spec.path("components")
                                .path("schemas")
                                .path("DocumentLayoutPublishRequest")
                                .path("additionalProperties")
                                .asBoolean(true))
                .isFalse();
        assertThat(
                        spec.path("components")
                                .path("schemas")
                                .path("DocumentLayoutSemanticDefinition")
                                .path("properties")
                                .path("legalNotices")
                                .path("maxItems")
                                .asInt())
                .isEqualTo(4);
        assertThat(
                        spec.path("components")
                                .path("schemas")
                                .path("DocumentLayoutOutputProfileMetadata")
                                .path("properties")
                                .path("allowedClassTokens")
                                .path("maxItems")
                                .asInt())
                .isEqualTo(64);
        assertThat(
                        spec.path("components")
                                .path("schemas")
                                .path("DocumentLayoutPublicationDTO")
                                .path("properties")
                                .path("canonicalJson")
                                .path("type")
                                .asText())
                .isEqualTo("string");
    }

    @Test
    void restartWithoutProfilesPreservesAcceptedHistoryButPreventsNewPublication() {
        var accepted = intent();
        var first = call("POST", base(accepted.draft()), accepted.request(), CREDENTIAL, null);
        assertThat(first.status()).isEqualTo(201);
        var next = intent();
        stop();
        boot(true, false);
        try {
            assertThat(
                            call(
                                            "POST",
                                            base(accepted.draft()),
                                            accepted.request(),
                                            CREDENTIAL,
                                            null)
                                    .body())
                    .isEqualTo(first.body());
            assertThat(
                            call(
                                            "GET",
                                            "/api/v1/document-layout-publications/"
                                                    + first.body()
                                                            .path("layoutRevisionId")
                                                            .asText(),
                                            null,
                                            CREDENTIAL,
                                            null)
                                    .body())
                    .isEqualTo(first.body());
            assertThat(call("GET", resolve(accepted.draft()), null, CREDENTIAL, null).status())
                    .isEqualTo(503);
            assertThat(call("POST", base(next.draft()), next.request(), CREDENTIAL, null).status())
                    .isEqualTo(503);
        } finally {
            stop();
            boot(true, true);
        }
    }

    @Test
    void ordinaryExecutableHasNoImplicitLayoutTrust() {
        stop();
        boot(false, true);
        try {
            assertThat(
                            call(
                                            "GET",
                                            "/api/v1/document-layout-publications/templates/"
                                                    + UUID.randomUUID()
                                                    + "/source-observation?localeCode=en-US",
                                            null,
                                            CREDENTIAL,
                                            null)
                                    .status())
                    .isEqualTo(403);
            assertThat(
                            call(
                                            "GET",
                                            "/api/v1/document-layout-publications/"
                                                    + UUID.randomUUID(),
                                            null,
                                            CREDENTIAL,
                                            null)
                                    .status())
                    .isEqualTo(403);
        } finally {
            stop();
            boot(true, true);
        }
    }

    static DocumentLayoutPublicationsApi layoutApi() {
        return new DocumentLayoutPublicationsApi(client());
    }

    static DocumentLayoutPublishRequest typedRequest(Intent intent) {
        try {
            return JSON.treeToValue(intent.request(), DocumentLayoutPublishRequest.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static void sdkStatus(int status, org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(
                        org.springframework.web.reactive.function.client.WebClientResponseException
                                .class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(status));
    }

    @Test
    void genuineLayoutSdkRetainsExactGeneralSchemaSourcePinsCanonicalAndHttpStatuses() {
        var intent = intent();
        var catalogApi = new DocumentTemplatesApi(client());
        var catalog = catalogApi.getDocumentTemplate(intent.draft().id(), null).block(WAIT);
        catalog.setInputSchema(null);
        catalog.setInputSchemaJson(
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"\uE000\":{\"type\":\"integer\",\"minimum\":9007199254740993},\"😀\":{\"type\":\"number\",\"minimum\":0.123456789012345678}}}");
        catalog.setTemplateVariables(
                new java.util.HashMap<>(
                        Map.of(
                                "\uE000",
                                "BMP",
                                "😀",
                                "supplementary",
                                "control",
                                String.valueOf((char) 31),
                                "literal",
                                "\\u001f")));
        catalogApi.updateDocumentTemplate(intent.draft().id(), catalog, null).block(WAIT);
        var observation =
                layoutApi()
                        .observeDocumentLayoutSource(intent.draft().id(), intent.draft().locale())
                        .block(WAIT);
        assertThat(observation.getDraftReference().getTemplateId()).isEqualTo(intent.draft().id());
        assertThat(observation.getSourceSnapshotCanonicalJson())
                .contains("9007199254740993", "0.123456789012345678", "\\u001F", "\\\\u001f");
        assertThat(DocumentLayoutRules.digest(observation.getSourceSnapshotCanonicalJson()))
                .isEqualTo(observation.getDraftReference().getSourceDigest());
        var request = typedRequest(intent);
        request.setExpectedDraftReference(observation.getDraftReference());
        long before = System.currentTimeMillis();
        var created =
                layoutApi()
                        .publishDocumentLayoutRevisionWithHttpInfo(intent.draft().id(), request)
                        .block(WAIT);
        assertThat(created.getStatusCode().value()).isEqualTo(201);
        var row = created.getBody();
        assertThat(row.getPublishedBy()).isEqualTo("synthetic-layout-admin");
        assertThat(row.getPublishedAtEpochMillis()).isBetween(before, System.currentTimeMillis());
        assertThat(row.getDefinitionDigest())
                .isEqualTo(
                        DocumentLayoutRules.digest(
                                "MASTERS_DOCUMENT_LAYOUT_V1\n" + row.getCanonicalJson()));
        assertThat(DocumentLayoutRules.json(row.getDefinition())).isEqualTo(row.getCanonicalJson());
        assertThat(row.getDefinition().getSourceReference().getSourceDigest())
                .isEqualTo(observation.getDraftReference().getSourceDigest());
        assertThat(
                        row.getDefinition()
                                .getSemanticDefinition()
                                .getLegalNotices()
                                .getFirst()
                                .getMasterReference()
                                .getRevisionId())
                .isEqualTo(
                        request.getSemanticDefinition()
                                .getLegalNotices()
                                .getFirst()
                                .getMasterReference()
                                .getRevisionId());
        var replay =
                layoutApi()
                        .publishDocumentLayoutRevisionWithHttpInfo(intent.draft().id(), request)
                        .block(WAIT);
        assertThat(replay.getStatusCode().value()).isEqualTo(200);
        assertThat(replay.getBody()).isEqualTo(row);
        assertThat(layoutApi().getDocumentLayoutRevision(row.getLayoutRevisionId()).block(WAIT))
                .isEqualTo(row);
        assertThat(
                        layoutApi()
                                .resolveDocumentLayoutRevision(
                                        intent.draft().code(),
                                        "1",
                                        intent.draft().locale(),
                                        "SYNTHETIC_A4",
                                        "1",
                                        "2".repeat(64))
                                .block(WAIT))
                .isEqualTo(row);
        var source =
                new DocumentTemplateRevisionsApi(client())
                        .getDocumentTemplateRevision(row.getSourceRevisionId(), null)
                        .block(WAIT);
        assertThat(source.getInputSchemaCanonicalJson())
                .contains("9007199254740993", "0.123456789012345678");
        assertThat(DocumentLayoutRules.digest(source.getInputSchemaCanonicalJson()))
                .isEqualTo(row.getDefinition().getSourceReference().getSchemaDigest());
    }

    @Test
    void genuineLayoutSdkRetirementReplayAndCurrentPolicyRetainOriginalAction() {
        var intent = intent();
        var request = typedRequest(intent);
        var row =
                layoutApi().publishDocumentLayoutRevision(intent.draft().id(), request).block(WAIT);
        var changed = typedRequest(intent);
        changed.getOutputProfile().setVersion("2");
        sdkStatus(
                409,
                () ->
                        layoutApi()
                                .publishDocumentLayoutRevision(intent.draft().id(), changed)
                                .block(WAIT));
        var retire =
                new DocumentLayoutRetireRequest()
                        .actionId(UUID.randomUUID())
                        .expectedStateVersion(0)
                        .reason("Fictional SDK withdrawal é");
        var retired =
                layoutApi()
                        .retireDocumentLayoutRevision(row.getLayoutRevisionId(), retire)
                        .block(WAIT);
        assertThat(retired.getStateVersion()).isEqualTo(1);
        assertThat(retired.getRetirementActionId()).isEqualTo(retire.getActionId());
        assertThat(retired.getCanonicalJson()).isEqualTo(row.getCanonicalJson());
        assertThat(
                        layoutApi()
                                .retireDocumentLayoutRevision(row.getLayoutRevisionId(), retire)
                                .block(WAIT))
                .isEqualTo(retired);
        assertThat(
                        layoutApi()
                                .publishDocumentLayoutRevision(intent.draft().id(), request)
                                .block(WAIT))
                .isEqualTo(retired);
        var deniedClient = new ApiClient();
        deniedClient.setBasePath("http://127.0.0.1:" + port);
        deniedClient.addDefaultHeader("Authorization", "Bearer " + CREDENTIAL + "-denied");
        var denied = new DocumentLayoutPublicationsApi(deniedClient);
        sdkStatus(
                403,
                () ->
                        denied.publishDocumentLayoutRevision(intent.draft().id(), request)
                                .block(WAIT));
        sdkStatus(
                403,
                () ->
                        denied.retireDocumentLayoutRevision(row.getLayoutRevisionId(), retire)
                                .block(WAIT));
        sdkStatus(
                403, () -> denied.getDocumentLayoutRevision(row.getLayoutRevisionId()).block(WAIT));
    }

    @Test
    void genuineLayoutSdkRebuildsTransportAfterRestartAndUsesRetainedNativeReceipt() {
        var intent = intent();
        var request = typedRequest(intent);
        var row =
                layoutApi().publishDocumentLayoutRevision(intent.draft().id(), request).block(WAIT);
        stop();
        boot(true, true);
        assertThat(layoutApi().getDocumentLayoutRevision(row.getLayoutRevisionId()).block(WAIT))
                .isEqualTo(row);
        var replay =
                layoutApi()
                        .publishDocumentLayoutRevisionWithHttpInfo(intent.draft().id(), request)
                        .block(WAIT);
        assertThat(replay.getStatusCode().value()).isEqualTo(200);
        assertThat(replay.getBody()).isEqualTo(row);
    }
}
