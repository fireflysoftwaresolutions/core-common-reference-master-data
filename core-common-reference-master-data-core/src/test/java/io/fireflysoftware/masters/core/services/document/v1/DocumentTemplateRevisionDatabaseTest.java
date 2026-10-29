package io.fireflysoftware.masters.core.services.document.v1;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplatePublicationRequest;
import io.fireflysoftware.masters.models.repositories.document.v1.DocumentTemplateRevisionRepository;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.fireflyframework.security.api.domain.Decision;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.fireflyframework.security.spi.SecurityContextPort;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class DocumentTemplateRevisionDatabaseTest {
    @Container static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("document_revision_qualification").withUsername("document_test").withPassword("synthetic-only")
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig().withPortBindings(
                    new com.github.dockerjava.api.model.PortBinding(
                            com.github.dockerjava.api.model.Ports.Binding.bindIp("127.0.0.1"),
                            new com.github.dockerjava.api.model.ExposedPort(5432))));

    @BeforeAll static void migrate() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration").load().migrate();
    }

    private ConnectionFactory connections() {
        return ConnectionFactories.get("r2dbc:postgresql://" + postgres.getUsername() + ":" + postgres.getPassword()
                + "@" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/" + postgres.getDatabaseName());
    }

    private DatabaseClient database() { return DatabaseClient.create(connections()); }

    private DocumentTemplateRevisionService service() {
        return service(() -> Mono.just(SecurityPrincipal.builder().subject("synthetic-document-administrator").build()),
                (principal, action, resource, context) -> Mono.just(Decision.permit()));
    }

    private DocumentTemplateRevisionService service(SecurityContextPort context, PolicyDecisionPort policy) {
        var beans = new StaticListableBeanFactory();
        if (context != null) beans.addBean("syntheticSecurityContext", context);
        if (policy != null) beans.addBean("syntheticPolicyDecision", policy);
        var factory = connections();
        return new DocumentTemplateRevisionService(new DocumentTemplateRevisionRepository(DatabaseClient.create(factory)), JsonMapper.builder().build(),
                new R2dbcTransactionManager(factory), new DocumentTemplateAccessGuard(beans.getBeanProvider(SecurityContextPort.class),
                beans.getBeanProvider(PolicyDecisionPort.class)));
    }

    private UUID draft() {
        UUID id = UUID.randomUUID();
        database().sql("""
                INSERT INTO language_locale(locale_id,language_code,locale_code,language_name,status)
                VALUES (:id,'en',:code,'Synthetic English','ACTIVE')
                """).bind("id", id).bind("code", "en-" + id.toString().substring(0, 7)).fetch().rowsUpdated().block();
        database().sql("""
                INSERT INTO document_template_catalog(template_id,template_code,type_id,category,template_name,
                    template_content,template_variables,version,status,input_schema,style_content,media_type)
                SELECT :id,:code,type_id,'SYNTHETIC','Synthetic statement','base content is not the translation',
                    '{"legacy":"captured"}'::jsonb,'1','ACTIVE',
                    '{"$schema":"https://json-schema.org/draft/2020-12/schema","type":"object","properties":{"name":{"type":"string"}}}'::jsonb,
                    'body { color: black; }','text/html'
                FROM document_template_type_catalog WHERE type_code='HTML'
                """).bind("id", id).bind("code", "synthetic-" + id).fetch().rowsUpdated().block();
        database().sql("""
                INSERT INTO document_template_localization(localization_id,template_id,locale_id,template_name,template_content,status)
                VALUES (:id,:id,:id,'Synthetic localized statement','Hello ${name}','ACTIVE')
                """).bind("id", id).fetch().rowsUpdated().block();
        return id;
    }

    private DocumentTemplatePublicationRequest request(UUID id) {
        return new DocumentTemplatePublicationRequest("1", "en-" + id.toString().substring(0, 7));
    }

    @Test void publicationSnapshotsLocalizedContentAndSurvivesDraftEditsAndNewService() {
        UUID id = draft();
        var first = service().publish(id, request(id)).block();
        assertThat(first.templateContent()).isEqualTo("Hello ${name}");
        assertThat(first.contentDigest()).isEqualTo("f31f8bb4f8c7b356e1abd8b446d0cedd6c529796df0460a027d10b2e8990fb0b");
        assertThat(first.inputSchema()).containsEntry("type", "object");
        assertThat(first.publishedBy()).isEqualTo("synthetic-document-administrator");
        database().sql("UPDATE document_template_localization SET template_content='Changed' WHERE template_id=:id")
                .bind("id", id).fetch().rowsUpdated().block();
        assertThat(service().get(first.revisionId()).block().templateContent()).isEqualTo("Hello ${name}");
        StepVerifier.create(service().publish(id, request(id)))
                .expectErrorSatisfies(e -> assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT)).verify();
    }

    @Test void parallelPublicationAndRetryReturnOneDurableRevision() {
        UUID id = draft();
        var revisions = Flux.range(0, 8).flatMap(i -> service().publish(id, request(id))).collectList().block();
        assertThat(revisions).hasSize(8).extracting(r -> r.revisionId()).containsOnly(revisions.getFirst().revisionId());
        assertThat(database().sql("SELECT count(*) AS n FROM document_template_revision WHERE template_id=:id")
                .bind("id", id).map((r,m) -> r.get("n", Long.class)).one().block()).isEqualTo(1L);
    }

    @Test void retirementBlocksIssuanceButKeepsHistoricalReadbackAndIdempotentRetirement() {
        UUID id = draft();
        var published = service().publish(id, request(id)).block();
        var retired = service().retire(published.revisionId()).block();
        assertThat(retired.lifecycle()).isEqualTo("RETIRED");
        assertThat(service().retire(published.revisionId()).block().retiredAt()).isEqualTo(retired.retiredAt());
        assertThat(service().get(published.revisionId()).block().templateContent()).isEqualTo("Hello ${name}");
        StepVerifier.create(database().sql("UPDATE document_template_revision SET lifecycle='PUBLISHED',retired_at=NULL,retired_by=NULL WHERE revision_id=:id")
                .bind("id", published.revisionId()).fetch().rowsUpdated()).expectErrorSatisfies(e ->
                assertThat(e).hasStackTraceContaining("DOCUMENT_REVISION_IMMUTABLE")).verify();
        StepVerifier.create(service().resolve("synthetic-" + id, "1", "en-" + id.toString().substring(0, 7))).expectErrorSatisfies(e ->
                assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT)).verify();
    }

    @Test void exactResolutionRejectsAbsentVersionOrLocaleAndInactiveSources() {
        UUID id = draft();
        var published = service().publish(id, request(id)).block();
        assertThat(service().resolve("synthetic-" + id, "1", "en-" + id.toString().substring(0, 7)).block().revisionId()).isEqualTo(published.revisionId());
        StepVerifier.create(service().resolve("synthetic-" + id, "2", "en-" + id.toString().substring(0, 7))).expectErrorSatisfies(e ->
                assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)).verify();
        StepVerifier.create(service().resolve("synthetic-" + id, "1", "es-ES")).expectErrorSatisfies(e ->
                assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)).verify();
        database().sql("UPDATE document_template_catalog SET status='INACTIVE' WHERE template_id=:id")
                .bind("id", id).fetch().rowsUpdated().block();
        StepVerifier.create(service().resolve("synthetic-" + id, "1", "en-" + id.toString().substring(0, 7))).expectErrorSatisfies(e ->
                assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT)).verify();
        assertThat(service().get(published.revisionId()).block()).isNotNull();
    }

    @Test void missingOrDeniedTrustedAdministrationCannotPublishOrRetire() {
        UUID id = draft();
        for (var denied : java.util.List.of(service(null, null), service(Mono::empty, (p,a,r,c) -> Mono.just(Decision.permit())),
                service(() -> Mono.just(SecurityPrincipal.builder().subject("synthetic-reader").build()),
                        (p,a,r,c) -> Mono.just(Decision.deny("synthetic denial"))))) {
            StepVerifier.create(denied.publish(id, request(id))).expectErrorSatisfies(e ->
                    assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)).verify();
        }
        assertThat(database().sql("SELECT count(*) AS n FROM document_template_revision WHERE template_id=:id")
                .bind("id", id).map((r,m) -> r.get("n", Long.class)).one().block()).isZero();
        var published = service().publish(id, request(id)).block();
        StepVerifier.create(service(null, null).retire(published.revisionId())).expectErrorSatisfies(e ->
                assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)).verify();
    }

    @Test void historicalReadAndExactResolutionAlsoRequireTrustedPermissionBeforeDatabaseAccess() {
        var denied = service(null, null);
        StepVerifier.create(denied.get(UUID.randomUUID())).expectErrorSatisfies(e ->
                assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)).verify();
        StepVerifier.create(denied.resolve("unknown", "1", "en-US")).expectErrorSatisfies(e ->
                assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)).verify();
    }

    @Test void malformedUnsupportedSchemaAndMediaNeverProduceARevision() {
        UUID id = draft();
        for (String schema : java.util.List.of("{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"invented\"}",
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"properties\":{\"x\":{\"type\":12}}}",
                "{\"$schema\":\"http://json-schema.org/draft-07/schema#\",\"type\":\"object\"}")) {
            database().sql("UPDATE document_template_catalog SET input_schema=CAST(:schema AS jsonb) WHERE template_id=:id")
                    .bind("id", id).bind("schema", schema).fetch().rowsUpdated().block();
            StepVerifier.create(service().publish(id, request(id))).expectErrorSatisfies(e ->
                    assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)).verify();
        }
        assertThat(database().sql("SELECT count(*) AS n FROM document_template_revision WHERE template_id=:id")
                .bind("id", id).map((r,m) -> r.get("n", Long.class)).one().block()).isZero();
    }

    @Test void legacyCatalogCrudPersistsStructuredSchemaAndCannotDeletePublishedParent() {
        var factory = connections();
        var repositories = new org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory(
                new org.springframework.data.r2dbc.core.R2dbcEntityTemplate(factory));
        var catalogRepository = repositories.getRepository(io.fireflysoftware.masters.models.repositories.document.v1.DocumentTemplateCatalogRepository.class);
        var mapper = org.mapstruct.factory.Mappers.getMapper(io.fireflysoftware.masters.core.mappers.document.v1.DocumentTemplateCatalogMapper.class);
        org.springframework.test.util.ReflectionTestUtils.setField(mapper, "objectMapper", JsonMapper.builder().build());
        var beans = new StaticListableBeanFactory();
        beans.addBean("syntheticContext", (SecurityContextPort) () -> Mono.just(SecurityPrincipal.builder().subject("synthetic-admin").build()));
        beans.addBean("syntheticPolicy", (PolicyDecisionPort) (p,a,r,c) -> Mono.just(Decision.permit()));
        var catalog = new DocumentTemplateCatalogServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(catalog, "repository", catalogRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(catalog, "mapper", mapper);
        org.springframework.test.util.ReflectionTestUtils.setField(catalog, "access", new DocumentTemplateAccessGuard(
                beans.getBeanProvider(SecurityContextPort.class), beans.getBeanProvider(PolicyDecisionPort.class)));
        UUID typeId = database().sql("SELECT type_id FROM document_template_type_catalog WHERE type_code='HTML'")
                .map((r,m) -> r.get("type_id", UUID.class)).one().block();
        var request = io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplateCatalogDTO.builder()
                .templateCode("synthetic-crud-" + UUID.randomUUID()).typeId(typeId).category("SYNTHETIC")
                .templateName("Synthetic legacy draft").templateContent("Synthetic content").mediaType("text/html")
                .templateVariables(java.util.Map.of("legacy", "captured"))
                .inputSchema(java.util.Map.of("$schema", "https://json-schema.org/draft/2020-12/schema", "type", "object"))
                .version("1").status(io.fireflysoftware.masters.interfaces.enums.commons.v1.StatusEnum.ACTIVE).build();
        var created = catalog.createDocumentTemplate(request).block();
        assertThat(created.getTypeId()).isEqualTo(typeId);
        assertThat(created.getInputSchema()).containsEntry("type", "object");
        assertThat(created.getTemplateVariables()).containsEntry("legacy", "captured");
        catalog.deleteDocumentTemplate(created.getTemplateId()).block();
        assertThat(catalogRepository.findById(created.getTemplateId()).block()).isNull();
        UUID publishedParent = draft();
        service().publish(publishedParent, request(publishedParent)).block();
        StepVerifier.create(catalog.deleteDocumentTemplate(publishedParent)).expectErrorSatisfies(e ->
                assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT)).verify();
    }

    @Test void legacyDraftServicesDenyBeforeTheirRepositoriesAreAvailable() {
        var beans = new StaticListableBeanFactory();
        var guard = new DocumentTemplateAccessGuard(beans.getBeanProvider(SecurityContextPort.class), beans.getBeanProvider(PolicyDecisionPort.class));
        var catalog = new DocumentTemplateCatalogServiceImpl();
        var localization = new DocumentTemplateLocalizationServiceImpl();
        var types = new DocumentTemplateTypeCatalogServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(catalog, "access", guard);
        org.springframework.test.util.ReflectionTestUtils.setField(localization, "access", guard);
        org.springframework.test.util.ReflectionTestUtils.setField(types, "access", guard);
        for (Mono<?> denied : java.util.List.of(catalog.getDocumentTemplate(UUID.randomUUID()), catalog.deleteDocumentTemplate(UUID.randomUUID()),
                localization.deleteLocalizationsByTemplateId(UUID.randomUUID()), types.getDocumentTemplateType(UUID.randomUUID()))) {
            StepVerifier.create(denied).expectErrorSatisfies(e ->
                    assertThat(((ResponseStatusException)e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)).verify();
        }
    }

    @Test void sourceIdentityCannotBeReassignedAfterPublication() {
        UUID id = draft();
        var revision = service().publish(id, request(id)).block();
        UUID other = draft();
        StepVerifier.create(database().sql("UPDATE document_template_localization SET template_id=:other WHERE localization_id=:id")
                .bind("id", id).bind("other", other).fetch().rowsUpdated()).expectErrorSatisfies(e ->
                assertThat(e).hasStackTraceContaining("DOCUMENT_LOCALIZATION_IDENTITY_IMMUTABLE")).verify();
        assertThat(service().get(revision.revisionId()).block().templateId()).isEqualTo(id);
    }

    @Test void parentTypeIdentityCannotChangeAfterPublication() {
        UUID id = draft();
        service().publish(id, request(id)).block();
        StepVerifier.create(database().sql("UPDATE document_template_catalog SET type_id=(SELECT type_id FROM document_template_type_catalog WHERE type_code='PDF') WHERE template_id=:id")
                .bind("id", id).fetch().rowsUpdated()).expectErrorSatisfies(e ->
                assertThat(e).hasStackTraceContaining("DOCUMENT_TEMPLATE_IDENTITY_IMMUTABLE")).verify();
    }

    @Test void canonicalSnapshotsPreserveUnicodeAndExactNumbersAcrossEquivalentDraftSerialization() {
        UUID id = draft();
        database().sql("UPDATE document_template_catalog SET template_variables=CAST(:json AS jsonb) WHERE template_id=:id")
                .bind("id", id).bind("json", "{\"z\":\"é漢😀\",\"n\":9007199254740993,\"amount\":1.230000000000000000}")
                .fetch().rowsUpdated().block();
        var first = service().publish(id, request(id)).block();
        database().sql("UPDATE document_template_catalog SET template_variables=CAST(:json AS jsonb) WHERE template_id=:id")
                .bind("id", id).bind("json", "{\"amount\":1.23,\"n\":9007199254740993,\"z\":\"é漢😀\"}")
                .fetch().rowsUpdated().block();
        var second = service().publish(id, request(id)).block();
        assertThat(second.revisionId()).isEqualTo(first.revisionId());
        assertThat(second.sourceDigest()).isEqualTo(first.sourceDigest());
        assertThat(second.templateVariables()).containsEntry("z", "é漢😀");
    }

    @Test void failedInsertRollsBackAndASeparateClientCanRetry() {
        UUID id = draft();
        database().sql("CREATE FUNCTION synthetic_document_failure() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
                + "IF NEW.template_id='" + id + "'::uuid THEN RAISE EXCEPTION 'SYNTHETIC_PUBLICATION_ROLLBACK'; END IF; RETURN NEW; END; $$")
                .fetch().rowsUpdated().block();
        database().sql("CREATE TRIGGER synthetic_document_failure AFTER INSERT ON document_template_revision FOR EACH ROW EXECUTE FUNCTION synthetic_document_failure()")
                .fetch().rowsUpdated().block();
        try {
            StepVerifier.create(service().publish(id, request(id))).expectError().verify();
            assertThat(database().sql("SELECT count(*) AS n FROM document_template_revision WHERE template_id=:id")
                    .bind("id", id).map((r,m) -> r.get("n", Long.class)).one().block()).isZero();
        } finally {
            database().sql("DROP TRIGGER synthetic_document_failure ON document_template_revision").fetch().rowsUpdated().block();
            database().sql("DROP FUNCTION synthetic_document_failure()").fetch().rowsUpdated().block();
        }
        assertThat(service().publish(id, request(id)).block().lifecycle()).isEqualTo("PUBLISHED");
    }

    @Test void legacyOversizedCanonicalNumbersRejectPublicationWithoutARevision() {
        for (String field : java.util.List.of("input_schema", "template_variables")) {
            UUID id = draft();
            String json = field.equals("input_schema")
                    ? "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"properties\":{\"x\":{\"minimum\":1e-129}}}"
                    : "{\"n\":1e128}";
            database().sql("UPDATE document_template_catalog SET " + field + "=CAST(:json AS jsonb) WHERE template_id=:id")
                    .bind("id", id).bind("json", json).fetch().rowsUpdated().block();
            StepVerifier.create(service().publish(id, request(id))).expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ResponseStatusException.class);
                assertThat(((ResponseStatusException) error).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            }).verify();
            assertThat(database().sql("SELECT count(*) AS n FROM document_template_revision WHERE template_id=:id")
                    .bind("id", id).map((r,m) -> r.get("n", Long.class)).one().block()).isZero();
        }
    }

    @Test void directMutationDeleteAndLegacyCascadeCannotErasePublishedContent() {
        UUID id = draft();
        var published = service().publish(id, request(id)).block();
        for (String sql : java.util.List.of(
                "UPDATE document_template_revision SET template_content='tampered' WHERE revision_id=:id",
                "UPDATE document_template_revision SET source_digest='tampered' WHERE revision_id=:id",
                "DELETE FROM document_template_revision WHERE revision_id=:id")) {
            StepVerifier.create(database().sql(sql).bind("id", published.revisionId()).fetch().rowsUpdated())
                    .expectErrorSatisfies(e -> assertThat(e).hasStackTraceContaining("DOCUMENT_REVISION_IMMUTABLE")).verify();
        }
        for (String sql : java.util.List.of("DELETE FROM document_template_catalog WHERE template_id=:id",
                "DELETE FROM document_template_localization WHERE template_id=:id",
                "DELETE FROM document_template_localization WHERE localization_id=:id")) {
            StepVerifier.create(database().sql(sql).bind("id", id).fetch().rowsUpdated()).expectError().verify();
        }
        StepVerifier.create(database().sql("UPDATE document_template_catalog SET template_code='tampered' WHERE template_id=:id")
                .bind("id", id).fetch().rowsUpdated()).expectErrorSatisfies(e ->
                assertThat(e).hasStackTraceContaining("DOCUMENT_TEMPLATE_IDENTITY_IMMUTABLE")).verify();
        assertThat(service().get(published.revisionId()).block().templateContent()).isEqualTo("Hello ${name}");
    }
}
