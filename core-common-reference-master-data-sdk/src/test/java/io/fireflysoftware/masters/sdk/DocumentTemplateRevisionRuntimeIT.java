package io.fireflysoftware.masters.sdk;

import io.fireflysoftware.common.reference.master.data.sdk.api.DocumentTemplateLocalizationsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.DocumentTemplateRevisionsApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.DocumentTemplatesApi;
import io.fireflysoftware.common.reference.master.data.sdk.api.LanguageLocaleApi;
import io.fireflysoftware.common.reference.master.data.sdk.invoker.ApiClient;
import io.fireflysoftware.common.reference.master.data.sdk.model.DocumentTemplateCatalogDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.DocumentTemplateLocalizationDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.DocumentTemplatePublicationRequest;
import io.fireflysoftware.common.reference.master.data.sdk.model.DocumentTemplateRevisionDTO;
import io.fireflysoftware.common.reference.master.data.sdk.model.LanguageLocaleDTO;
import io.r2dbc.spi.ConnectionFactories;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class DocumentTemplateRevisionRuntimeIT {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("document_http_qualification").withUsername("document_http").withPassword("synthetic-only")
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withPortBindings(
                    new com.github.dockerjava.api.model.PortBinding(
                            com.github.dockerjava.api.model.Ports.Binding.bindIp("127.0.0.1"),
                            new com.github.dockerjava.api.model.ExposedPort(5432))));
    private static Process application;
    private static int port;
    private static final Duration TIMEOUT = Duration.ofSeconds(20);

    @BeforeAll static void start() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").load().migrate();
        boot();
    }

    private static void boot() {
        boot(true);
    }

    private static void boot(boolean syntheticTrust) {
        try {
            try (var socket = new java.net.ServerSocket(0,1,java.net.InetAddress.getLoopbackAddress())) { port = socket.getLocalPort(); }
            var jar = java.nio.file.Path.of("../core-common-reference-master-data-web/target/core-common-reference-master-data.jar").toAbsolutePath().normalize();
            assertThat(jar).isRegularFile();
            var args = new java.util.ArrayList<String>();
            args.add(java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString());
            args.add("-Xmx384m");
            if (syntheticTrust) {
                args.add("-Dloader.path=" + java.nio.file.Path.of("target/test-classes").toAbsolutePath());
                args.add("-Dloader.main=io.fireflysoftware.masters.sdk.DocumentTemplateRuntimeBootstrap");
                args.add("-cp"); args.add(jar.toString());
                args.add("org.springframework.boot.loader.launch.PropertiesLauncher");
            } else {
                args.add("-jar"); args.add(jar.toString());
            }
            args.addAll(java.util.List.of("--server.address=127.0.0.1", "--server.port=" + port,
                    "--DB_HOST=" + postgres.getHost(), "--DB_PORT=" + postgres.getMappedPort(5432),
                    "--DB_NAME=" + postgres.getDatabaseName(), "--DB_USERNAME=" + postgres.getUsername(),
                    "--DB_PASSWORD=" + postgres.getPassword(), "--DB_SSL_MODE=disable", "--spring.flyway.enabled=false",
                    "--firefly.eda.enabled=false", "--firefly.stepevents.enabled=false"));
            var log = java.nio.file.Path.of("target/document-runtime-" + (syntheticTrust ? "trusted-" : "unconfigured-") + port + ".log").toFile();
            application = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log).start();
            var http = java.net.http.HttpClient.newHttpClient();
            for (int attempt=0; attempt<120; attempt++) {
                if (!application.isAlive()) throw new IllegalStateException("Document runtime exited; see " + log);
                try {
                    var response = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:" + port + "/v3/api-docs"))
                            .timeout(Duration.ofSeconds(1)).build(), java.net.http.HttpResponse.BodyHandlers.discarding());
                    if (response.statusCode()==200) return;
                } catch (java.io.IOException ignored) { }
                Thread.sleep(250);
            }
            throw new IllegalStateException("Document runtime startup timed out; see " + log);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Document runtime could not start", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Document runtime startup was interrupted", error);
        }
    }

    @AfterAll static void stop() {
        if (application == null) return;
        application.destroy();
        try {
            if (!application.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                application.destroyForcibly();
                if (!application.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Private document runtime did not exit");
            }
        }
        catch (InterruptedException error) { Thread.currentThread().interrupt(); application.destroyForcibly(); }
    }

    private static DatabaseClient database() {
        return DatabaseClient.create(ConnectionFactories.get("r2dbc:postgresql://" + postgres.getUsername() + ":" + postgres.getPassword()
                + "@" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + postgres.getDatabaseName()));
    }

    private static ApiClient client(String token) {
        var client = new ApiClient();
        client.setBasePath("http://127.0.0.1:" + port);
        if (token != null) client.addDefaultHeader("Authorization", "Bearer " + token);
        return client;
    }

    private record Draft(DocumentTemplateCatalogDTO catalog, String localeCode) { }

    private Draft draft() {
        UUID localeId = UUID.randomUUID();
        String localeCode = "en-" + localeId.toString().substring(0,7);
        database().sql("INSERT INTO language_locale(locale_id,language_code,locale_code,language_name,status) VALUES (:id,'en',:code,'Synthetic English','ACTIVE')")
                .bind("id", localeId).bind("code", localeCode).fetch().rowsUpdated().block(TIMEOUT);
        UUID typeId = database().sql("SELECT type_id FROM document_template_type_catalog WHERE type_code='HTML'")
                .map((row,metadata) -> row.get("type_id", UUID.class)).one().block(TIMEOUT);
        var request = new DocumentTemplateCatalogDTO().templateCode("sdk-" + UUID.randomUUID()).typeId(typeId)
                .category("SYNTHETIC").templateName("Synthetic SDK template").templateContent("Base draft content")
                .mediaType("text/html").styleContent("body { color: black; }").version("1")
                .status(DocumentTemplateCatalogDTO.StatusEnum.ACTIVE)
                .inputSchema(null).inputSchemaJson("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"properties\":{"
                        + "\"name\":{\"type\":\"string\"},\"amount\":{\"type\":\"number\",\"minimum\":0.123456789012345678},"
                        + "\"large\":{\"type\":\"integer\",\"minimum\":9007199254740993}}}");
        var catalog = new DocumentTemplatesApi(client("synthetic-admin")).createDocumentTemplate(request, null).block(TIMEOUT);
        var localized = new DocumentTemplateLocalizationDTO().templateId(catalog.getTemplateId()).localeId(localeId)
                .templateName("Synthetic SDK statement").templateContent("<html><body>é漢😀 Hello ${name}</body></html>")
                .status(DocumentTemplateLocalizationDTO.StatusEnum.ACTIVE);
        new DocumentTemplateLocalizationsApi(client("synthetic-admin")).createDocumentTemplateLocalization(localized, null).block(TIMEOUT);
        return new Draft(catalog, localeCode);
    }

    private DocumentTemplatePublicationRequest publication(Draft draft) {
        return new DocumentTemplatePublicationRequest().version("1").localeCode(draft.localeCode());
    }

    @Test void languageLocaleSdkCreatesAndReadsServerGeneratedIdentity() {
        var locales = new LanguageLocaleApi(client("synthetic-admin"));
        UUID supplied = UUID.randomUUID();
        var created = locales.createLanguageLocale(new LanguageLocaleDTO()
                .localeId(supplied).languageCode("en").countryCode("US").localeCode("en-US")
                .languageName("English").nativeName("English").regionName("United States")
                .rtl(false).sortOrder(1).status(LanguageLocaleDTO.StatusEnum.ACTIVE), null).block(TIMEOUT);
        assertThat(created).isNotNull();
        assertThat(created.getLocaleId()).isNotNull().isNotEqualTo(supplied);
        var retained = locales.getLanguageLocale(created.getLocaleId(), null).block(TIMEOUT);
        assertThat(retained.getLocaleId()).isEqualTo(created.getLocaleId());
        assertThat(retained.getLocaleCode()).isEqualTo("en-US");
        assertThat(retained.getStatus()).isEqualTo(LanguageLocaleDTO.StatusEnum.ACTIVE);
    }

    @Test void relatedMasterSdkCreatesAndReadsServerGeneratedIdentity() {
        var countries = new io.fireflysoftware.common.reference.master.data.sdk.api.CountriesApi(client("synthetic-admin"));
        var country = countries.createCountry(new io.fireflysoftware.common.reference.master.data.sdk.model.CountryDTO()
                .isoCode("US").countryName("United States")
                .region(io.fireflysoftware.common.reference.master.data.sdk.model.CountryDTO.RegionEnum.AMERICAS)
                .status(io.fireflysoftware.common.reference.master.data.sdk.model.CountryDTO.StatusEnum.ACTIVE), null).block(TIMEOUT);
        assertThat(country.getCountryId()).isNotNull();
        assertThat(countries.getCountry(country.getCountryId(), null).block(TIMEOUT).getIsoCode()).isEqualTo("US");

        var currencies = new io.fireflysoftware.common.reference.master.data.sdk.api.CurrenciesApi(client("synthetic-admin"));
        var currency = currencies.createCurrency(new io.fireflysoftware.common.reference.master.data.sdk.model.CurrencyDTO()
                .isoCode("USD").currencyName("US dollar").symbol("$").decimalPrecision(2)
                .status(io.fireflysoftware.common.reference.master.data.sdk.model.CurrencyDTO.StatusEnum.ACTIVE), null).block(TIMEOUT);
        assertThat(currency.getCurrencyId()).isNotNull();
        assertThat(currencies.getCurrency(currency.getCurrencyId(), null).block(TIMEOUT).getDecimalPrecision()).isEqualTo(2);

        var institutions = new io.fireflysoftware.common.reference.master.data.sdk.api.BankInstitutionCodesApi(client("synthetic-admin"));
        var institution = institutions.createBankInstitutionCode(new io.fireflysoftware.common.reference.master.data.sdk.model.BankInstitutionCodeDTO()
                .bankName("Example Sandbox Institution").swiftCode("EXAMUS00XXX").countryId(country.getCountryId())
                .status(io.fireflysoftware.common.reference.master.data.sdk.model.BankInstitutionCodeDTO.StatusEnum.ACTIVE), null).block(TIMEOUT);
        assertThat(institution.getInstitutionId()).isNotNull();
        assertThat(institutions.getBankInstitutionCode(institution.getInstitutionId(), null).block(TIMEOUT).getCountryId())
                .isEqualTo(country.getCountryId());
    }

    @Test void everyDeclaredCountryRegionSurvivesSdkCreationAndUpdate() {
        var countries = new io.fireflysoftware.common.reference.master.data.sdk.api.CountriesApi(client("synthetic-admin"));
        for (var region : io.fireflysoftware.common.reference.master.data.sdk.model.CountryDTO.RegionEnum.values()) {
            var created = countries.createCountry(new io.fireflysoftware.common.reference.master.data.sdk.model.CountryDTO()
                    .isoCode("ZZ").countryName("Synthetic " + region).region(region)
                    .status(io.fireflysoftware.common.reference.master.data.sdk.model.CountryDTO.StatusEnum.ACTIVE), null).block(TIMEOUT);
            assertThat(created.getCountryId()).isNotNull();
            assertThat(created.getRegion()).isEqualTo(region);
            created.setCountryName("Updated synthetic " + region);
            var updated = countries.updateCountry(created.getCountryId(), created, null).block(TIMEOUT);
            assertThat(updated.getCountryId()).isEqualTo(created.getCountryId());
            var retained = countries.getCountry(created.getCountryId(), null).block(TIMEOUT);
            assertThat(retained.getRegion()).isEqualTo(region);
            assertThat(retained.getCountryName()).isEqualTo("Updated synthetic " + region);
        }
    }

    @Test void generatedSdkPublishesExactContentAndCanonicalPrecisionThroughActualProducer() {
        Draft draft = draft();
        assertThat(draft.catalog().getInputSchemaJson()).contains("0.123456789012345678", "9007199254740993");
        var api = new DocumentTemplateRevisionsApi(client("synthetic-admin"));
        DocumentTemplateRevisionDTO published = api.publishDocumentTemplateRevision(draft.catalog().getTemplateId(), publication(draft), null).block(TIMEOUT);
        assertThat(published.getTemplateContent()).contains("é漢😀", "Hello ${name}");
        assertThat(published.getInputSchemaCanonicalJson()).contains("0.123456789012345678", "9007199254740993");
        assertThat(published.getSourceSnapshotCanonicalJson()).contains("é漢😀");
        assertThat(published.getCanonicalizationVersion()).isEqualTo("MASTERS_JSON_V1");
        assertThat(published.getPublishedBy()).isEqualTo("synthetic-document-admin");
        assertThat(new DocumentTemplateRevisionsApi(client("synthetic-reader")).getDocumentTemplateRevision(published.getRevisionId(), null)
                .block(TIMEOUT).getInputSchemaCanonicalJson()).isEqualTo(published.getInputSchemaCanonicalJson());
        var resolved = new DocumentTemplateRevisionsApi(client("synthetic-generator")).resolveDocumentTemplateRevision(
                draft.catalog().getTemplateCode(), "1", draft.localeCode(), null).block(TIMEOUT);
        assertThat(resolved.getRevisionId()).isEqualTo(published.getRevisionId());
        var validator = new org.fireflyframework.validators.schema.OfflineJsonSchemaValidator();
        assertThat(validator.validateInstance(resolved.getInputSchemaCanonicalJson(),
                "{\"amount\":0.123456789012345678,\"large\":9007199254740993}").valid()).isTrue();
        assertThat(validator.validateInstance(resolved.getInputSchemaCanonicalJson(),
                "{\"amount\":0.123456789012345677,\"large\":9007199254740993}").valid()).isFalse();
        assertThat(validator.validateInstance(resolved.getInputSchemaCanonicalJson(),
                "{\"amount\":0.123456789012345678,\"large\":9007199254740992}").valid()).isFalse();
    }

    @Test void actualHttpDeniesForgedHeadersAndReadersCannotPublishOrResolve() {
        Draft draft = draft();
        var publicHttp = WebClient.create("http://127.0.0.1:" + port);
        for (String path : java.util.List.of("/api/v1/document-templates/" + draft.catalog().getTemplateId(),
                "/api/v1/document-template-types", "/api/v1/document-template-localizations/template/" + draft.catalog().getTemplateId())) {
            assertThat(publicHttp.get().uri(path).header("X-Actor", "synthetic-document-admin").header("X-Tenant-Id", "synthetic-admin")
                    .exchangeToMono(response -> Mono.just(response.statusCode().value())).block(TIMEOUT)).isEqualTo(403);
        }
        assertDenied(() -> new DocumentTemplateRevisionsApi(client("synthetic-reader"))
                .publishDocumentTemplateRevision(draft.catalog().getTemplateId(), publication(draft), null).block(TIMEOUT));
        assertDenied(() -> new DocumentTemplateRevisionsApi(client(null)).getDocumentTemplateRevision(UUID.randomUUID(), null).block(TIMEOUT));
        assertDenied(() -> new DocumentTemplateRevisionsApi(client("synthetic-reader")).resolveDocumentTemplateRevision(
                draft.catalog().getTemplateCode(), "1", draft.localeCode(), null).block(TIMEOUT));
    }

    @Test void concurrentSdkReplayHasOneRevisionAndChangedDraftConflicts() {
        Draft draft = draft();
        var records = Flux.range(0, 8).flatMap(index -> new DocumentTemplateRevisionsApi(client("synthetic-admin"))
                .publishDocumentTemplateRevision(draft.catalog().getTemplateId(), publication(draft), null)).collectList().block(TIMEOUT);
        assertThat(records).hasSize(8).extracting(DocumentTemplateRevisionDTO::getRevisionId).containsOnly(records.getFirst().getRevisionId());
        draft.catalog().setTemplateContent("Changed base draft after publication");
        draft.catalog().setInputSchema(null);
        new DocumentTemplatesApi(client("synthetic-admin")).updateDocumentTemplate(draft.catalog().getTemplateId(), draft.catalog(), null).block(TIMEOUT);
        assertThatThrownBy(() -> new DocumentTemplateRevisionsApi(client("synthetic-admin"))
                .publishDocumentTemplateRevision(draft.catalog().getTemplateId(), publication(draft), null).block(TIMEOUT))
                .isInstanceOfSatisfying(WebClientResponseException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
    }

    @Test void retiredReadbackAndIssuanceDenialSurviveActualApplicationRestart() {
        Draft draft = draft();
        var admin = new DocumentTemplateRevisionsApi(client("synthetic-admin"));
        var revision = admin.publishDocumentTemplateRevision(draft.catalog().getTemplateId(), publication(draft), null).block(TIMEOUT);
        var retired = admin.retireDocumentTemplateRevision(revision.getRevisionId(), null).block(TIMEOUT);
        stop();
        boot();
        var reread = new DocumentTemplateRevisionsApi(client("synthetic-reader")).getDocumentTemplateRevision(revision.getRevisionId(), null).block(TIMEOUT);
        assertThat(reread.getLifecycle()).isEqualTo(retired.getLifecycle());
        assertThat(reread.getSourceDigest()).isEqualTo(revision.getSourceDigest());
        assertThat(reread.getTemplateContent()).isEqualTo(revision.getTemplateContent());
        assertThatThrownBy(() -> new DocumentTemplateRevisionsApi(client("synthetic-generator")).resolveDocumentTemplateRevision(
                draft.catalog().getTemplateCode(), "1", draft.localeCode(), null).block(TIMEOUT))
                .isInstanceOfSatisfying(WebClientResponseException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(409));
        assertDenied(() -> new DocumentTemplateRevisionsApi(client("synthetic-reader")).retireDocumentTemplateRevision(revision.getRevisionId(), null).block(TIMEOUT));
    }

    @Test void transportReplayMustReturnRetainedRetirementInsteadOfCachedPublication() {
        Draft draft = draft();
        var cachedClient = client("synthetic-admin");
        cachedClient.addDefaultHeader("X-Idempotency-Key", UUID.randomUUID().toString());
        var replay = new DocumentTemplateRevisionsApi(cachedClient);
        var published = replay.publishDocumentTemplateRevision(draft.catalog().getTemplateId(), publication(draft), null).block(TIMEOUT);
        var retired = new DocumentTemplateRevisionsApi(client("synthetic-admin"))
                .retireDocumentTemplateRevision(published.getRevisionId(), null).block(TIMEOUT);
        var current = replay.publishDocumentTemplateRevision(draft.catalog().getTemplateId(), publication(draft), null).block(TIMEOUT);
        assertThat(current.getRevisionId()).isEqualTo(published.getRevisionId());
        assertThat(current.getLifecycle()).isEqualTo(retired.getLifecycle());
        assertThat(current.getRetiredAt()).isEqualTo(retired.getRetiredAt());
        assertThat(current.getSourceDigest()).isEqualTo(published.getSourceDigest());
    }

    @Test void cachedPublicationCannotReplaceCurrentAdministrationPolicy() {
        Draft draft = draft();
        String key = UUID.randomUUID().toString();
        var allowed = client("synthetic-admin");
        allowed.addDefaultHeader("X-Idempotency-Key", key);
        var denied = client("synthetic-admin-current-policy-denied");
        denied.addDefaultHeader("X-Idempotency-Key", key);
        new DocumentTemplateRevisionsApi(allowed).publishDocumentTemplateRevision(
                draft.catalog().getTemplateId(), publication(draft), null).block(TIMEOUT);
        assertDenied(() -> new DocumentTemplateRevisionsApi(denied).publishDocumentTemplateRevision(
                draft.catalog().getTemplateId(), publication(draft), null).block(TIMEOUT));
    }

    @Test void cachedRetirementCannotReplaceCurrentAdministrationPolicy() {
        Draft draft = draft();
        var published = new DocumentTemplateRevisionsApi(client("synthetic-admin")).publishDocumentTemplateRevision(
                draft.catalog().getTemplateId(), publication(draft), null).block(TIMEOUT);
        String key = UUID.randomUUID().toString();
        var allowed = client("synthetic-admin");
        allowed.addDefaultHeader("X-Idempotency-Key", key);
        var denied = client("synthetic-admin-current-policy-denied");
        denied.addDefaultHeader("X-Idempotency-Key", key);
        new DocumentTemplateRevisionsApi(allowed).retireDocumentTemplateRevision(published.getRevisionId(), null).block(TIMEOUT);
        assertDenied(() -> new DocumentTemplateRevisionsApi(denied).retireDocumentTemplateRevision(published.getRevisionId(), null).block(TIMEOUT));
    }

    @Test void nativeReplayOpenApiKeepsIgnoredCompatibilityHeaderAndExplicitCacheOptOut() throws Exception {
        String raw = WebClient.create("http://127.0.0.1:" + port).get().uri("/v3/api-docs")
                .retrieve().bodyToMono(String.class).block(TIMEOUT);
        var paths = new com.fasterxml.jackson.databind.ObjectMapper().readTree(raw).path("paths");
        for (String path : java.util.List.of("/api/v1/document-template-revisions/templates/{templateId}/publish",
                "/api/v1/document-template-revisions/{revisionId}/retire")) {
            var operation = paths.path(path).path("post");
            assertThat(operation.path("x-disable-idempotency").asBoolean()).isTrue();
            var header = java.util.stream.StreamSupport.stream(operation.path("parameters").spliterator(), false)
                    .filter(parameter -> "X-Idempotency-Key".equals(parameter.path("name").asText())
                            && "header".equals(parameter.path("in").asText())).findFirst();
            assertThat(header).isPresent();
            assertThat(header.orElseThrow().path("required").asBoolean()).isFalse();
            assertThat(header.orElseThrow().path("description").asText()).contains("ignored");
        }
    }

    @Test void exactSchemaAuthoringRejectsDualRepresentationDuplicatesTrailingTokensAndExternalReferences() {
        Draft draft = draft();
        var api = new DocumentTemplatesApi(client("synthetic-admin"));
        String original = draft.catalog().getInputSchemaJson();
        assertThatThrownBy(() -> api.updateDocumentTemplate(draft.catalog().getTemplateId(), draft.catalog(), null).block(TIMEOUT))
                .isInstanceOfSatisfying(WebClientResponseException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
        draft.catalog().setInputSchema(null);
        for (String invalid : java.util.List.of(original + " {}",
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"properties\":{\"x\":{\"minimum\":1e-129}}}",
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"type\":\"string\"}",
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"$ref\":\"https://example.invalid/schema\"}")) {
            draft.catalog().setInputSchemaJson(invalid);
            assertThatThrownBy(() -> api.updateDocumentTemplate(draft.catalog().getTemplateId(), draft.catalog(), null).block(TIMEOUT))
                    .isInstanceOfSatisfying(WebClientResponseException.class, error -> assertThat(error.getStatusCode().value()).isEqualTo(400));
            assertThat(api.getDocumentTemplate(draft.catalog().getTemplateId(), null).block(TIMEOUT).getInputSchemaJson()).isEqualTo(original);
        }
    }

    @Test void ordinaryExecutableWithoutTrustedPortsFailsClosed() {
        stop();
        boot(false);
        try {
            assertDenied(() -> new DocumentTemplateRevisionsApi(client("synthetic-admin"))
                    .getDocumentTemplateRevision(UUID.randomUUID(), null).block(TIMEOUT));
            assertDenied(() -> new DocumentTemplatesApi(client("synthetic-admin"))
                    .getDocumentTemplate(UUID.randomUUID(), null).block(TIMEOUT));
        } finally {
            stop();
            boot();
        }
    }

    private static void assertDenied(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation) {
        assertThatThrownBy(operation).isInstanceOfSatisfying(WebClientResponseException.class,
                error -> assertThat(error.getStatusCode().value()).isEqualTo(403));
    }

}
