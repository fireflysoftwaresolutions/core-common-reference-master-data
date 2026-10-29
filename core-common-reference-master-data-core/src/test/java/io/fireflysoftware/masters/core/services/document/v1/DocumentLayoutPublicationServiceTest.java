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

import com.fasterxml.jackson.databind.json.JsonMapper;

import io.fireflysoftware.masters.core.services.document.layout.v1.*;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;
import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplatePublicationRequest;
import io.fireflysoftware.masters.models.repositories.document.layout.v1.DocumentLayoutRepository;
import io.fireflysoftware.masters.models.repositories.document.v1.DocumentTemplateRevisionRepository;
import io.r2dbc.spi.*;

import org.fireflyframework.security.api.domain.*;
import org.fireflyframework.security.spi.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

import reactor.core.publisher.*;

import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

@Testcontainers
class DocumentLayoutPublicationServiceTest {
    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("synthetic_layout_service")
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

    static final UUID LOCALE = UUID.randomUUID();
    static final Duration WAIT = Duration.ofSeconds(20);

    @BeforeAll
    static void migrate() throws Exception {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        try (var c =
                        DriverManager.getConnection(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword());
                var p =
                        c.prepareStatement(
                                "INSERT INTO"
                                    + " language_locale(locale_id,language_code,locale_code,language_name,status)"
                                    + " VALUES(?,'en','en-US','Synthetic English','ACTIVE')")) {
            p.setObject(1, LOCALE);
            p.executeUpdate();
        }
    }

    static void status(int expected, org.assertj.core.api.ThrowableAssert.ThrowingCallable work) {
        assertThatThrownBy(work)
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(expected));
    }

    static final class Fixture {
        final ConnectionFactory cf =
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
        final DatabaseClient db = DatabaseClient.create(cf);
        final R2dbcTransactionManager manager = new R2dbcTransactionManager(cf);
        final AtomicBoolean storedContextRevoked = new AtomicBoolean();
        final AtomicBoolean revoked = new AtomicBoolean(),
                registryAvailable = new AtomicBoolean(true),
                policyInsideTransaction = new AtomicBoolean();
        final AtomicInteger policyCalls = new AtomicInteger();
        final AtomicReference<java.util.function.Function<Map<String, Object>, Mono<Void>>>
                policyHook = new AtomicReference<>(c -> Mono.empty());
        final AtomicBoolean changedProfile = new AtomicBoolean();
        final DocumentTemplateRevisionRepository masters =
                new DocumentTemplateRevisionRepository(db);
        final DocumentTemplateRevisionService v1;
        final DocumentLayoutPublicationService service;
        final UUID template;
        final DocumentLayoutMasterReference notice;

        Fixture() {
            var legacyBeans = new StaticListableBeanFactory();
            legacyBeans.addBean(
                    "context",
                    (SecurityContextPort)
                            () ->
                                    Mono.just(
                                            SecurityPrincipal.builder()
                                                    .subject("synthetic-layout-author")
                                                    .build()));
            legacyBeans.addBean(
                    "policy", (PolicyDecisionPort) (p, a, r, c) -> Mono.just(Decision.permit()));
            v1 =
                    new DocumentTemplateRevisionService(
                            masters,
                            JsonMapper.builder().build(),
                            manager,
                            new DocumentTemplateAccessGuard(
                                    legacyBeans.getBeanProvider(SecurityContextPort.class),
                                    legacyBeans.getBeanProvider(PolicyDecisionPort.class)));
            UUID note = draft(true);
            var published =
                    v1.publish(note, new DocumentTemplatePublicationRequest("1", "en-US"))
                            .block(WAIT);
            notice =
                    new DocumentLayoutMasterReference(
                            published.revisionId().toString(),
                            published.templateId().toString(),
                            published.templateCode(),
                            published.version(),
                            published.localeCode(),
                            published.mediaType(),
                            published.canonicalizationVersion(),
                            published.sourceDigest(),
                            published.contentDigest(),
                            published.schemaDigest(),
                            published.styleDigest());
            template = draft(false);
            var beans = new StaticListableBeanFactory();
            beans.addBean(
                    "context",
                    (SecurityContextPort)
                            () ->
                                    Mono.just(
                                            SecurityPrincipal.builder()
                                                    .subject("synthetic-layout-author")
                                                    .build()));
            beans.addBean(
                    "policy",
                    (PolicyDecisionPort)
                            (p, a, r, c) -> {
                                policyCalls.incrementAndGet();
                                assertThat(a).startsWith("masters.document-template.layout-");
                                assertThat(c).containsEntry("scope", "GLOBAL_MASTER");
                                assertThat(c.toString())
                                        .doesNotContain("Fictional legal notice")
                                        .doesNotContain("${facts.name}");
                                return policyHook
                                        .get()
                                        .apply(c)
                                        .then(
                                                org.springframework.transaction.reactive
                                                        .TransactionSynchronizationManager
                                                        .forCurrentTransaction()
                                                        .map(
                                                                m -> {
                                                                    policyInsideTransaction.set(
                                                                            m
                                                                                    .isActualTransactionActive());
                                                                    return Decision.deny(
                                                                            "Policy invoked in"
                                                                                + " transaction");
                                                                })
                                                        .onErrorResume(
                                                                org.springframework.transaction
                                                                        .NoTransactionException
                                                                        .class,
                                                                e ->
                                                                        Mono.just(
                                                                                (revoked.get()
                                                                                                || (storedContextRevoked
                                                                                                                .get()
                                                                                                        && c
                                                                                                                .containsKey(
                                                                                                                        "sourceReference")))
                                                                                        ? Decision
                                                                                                .deny(
                                                                                                        "Synthetic"
                                                                                                            + " current"
                                                                                                            + " denial")
                                                                                        : Decision
                                                                                                .permit())));
                            });
            service =
                    new DocumentLayoutPublicationService(
                            v1,
                            masters,
                            new DocumentLayoutRepository(db),
                            manager,
                            new DocumentTemplateAccessGuard(
                                    beans.getBeanProvider(SecurityContextPort.class),
                                    beans.getBeanProvider(PolicyDecisionPort.class)),
                            ref -> {
                                if (!registryAvailable.get())
                                    throw DocumentLayoutRules.unavailable();
                                if (!ref.equals(DocumentLayoutRulesTest.profileReference()))
                                    throw DocumentLayoutRules.conflict("Unknown synthetic profile");
                                var p = DocumentLayoutRulesTest.profile();
                                return changedProfile.get()
                                        ? new DocumentLayoutOutputProfileMetadata(
                                                p.reference(),
                                                p.sectionMode(),
                                                p.pageSize(),
                                                p.orientation(),
                                                p.allowedClassTokens(),
                                                p.maxSections() - 1,
                                                p.maxContentBytes())
                                        : p;
                            },
                            Clock.systemUTC());
        }

        UUID draft(boolean legal) {
            UUID id = UUID.randomUUID();
            String code = (legal ? "NOTICE_" : "BODY_") + id.toString().replace("-", "");
            String body =
                    legal
                            ? "<p>Fictional legal notice é.</p>"
                            : "<section class=\"document-page\"><p>${facts.name}</p></section>";
            String schema =
                    legal
                            ? "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false}"
                            : "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"additionalProperties\":false,\"properties\":{\"name\":{\"type\":\"string\"}}}";
            db.sql(
                            "INSERT INTO"
                                + " document_template_catalog(template_id,template_code,type_id,category,template_name,template_content,version,status,input_schema,style_content,media_type)"
                                + " SELECT :id,:code,type_id,:category,'Synthetic"
                                + " layout',:body,'1','ACTIVE',CAST(:schema AS"
                                + " jsonb),'','text/html' FROM document_template_type_catalog WHERE"
                                + " type_code='HTML'")
                    .bind("id", id)
                    .bind("code", code)
                    .bind("category", legal ? "LEGAL_NOTICE" : "SYNTHETIC")
                    .bind("body", body)
                    .bind("schema", schema)
                    .fetch()
                    .rowsUpdated()
                    .block(WAIT);
            db.sql(
                            "INSERT INTO"
                                + " document_template_localization(localization_id,template_id,locale_id,template_name,template_content,status)"
                                + " VALUES(:id,:id,:locale,'Synthetic localized"
                                + " layout',:body,'ACTIVE')")
                    .bind("id", id)
                    .bind("locale", LOCALE)
                    .bind("body", body)
                    .fetch()
                    .rowsUpdated()
                    .block(WAIT);
            return id;
        }

        DocumentLayoutPublishRequest request() {
            var observation = service.observe(template, "en-US").block(WAIT);
            return new DocumentLayoutPublishRequest(
                    UUID.randomUUID().toString(),
                    observation.draftReference(),
                    DocumentLayoutRulesTest.profileReference(),
                    new DocumentLayoutSemanticDefinition(
                            "AUTHORED_PAGES",
                            "A4",
                            "PORTRAIT",
                            List.of(
                                    new DocumentLayoutNoticeReference(
                                            DocumentLayoutNoticeReference.Role.GENERAL_DISCLOSURE,
                                            DocumentLayoutNoticeReference.Placement.FOOTER,
                                            notice))));
        }

        long count(String table, String column, UUID value) {
            return db.sql("SELECT count(*) AS n FROM " + table + " WHERE " + column + "=:id")
                    .bind("id", value)
                    .map((r, m) -> r.get("n", Long.class))
                    .one()
                    .block(WAIT);
        }
    }

    @Test
    void sourceObservationPublicationAndCurrentReplayRetainOneAtomicAuthority() {
        var f = new Fixture();
        var request = f.request();
        var first = f.service.publish(f.template, request).block(WAIT);
        assertThat(first.replayed()).isFalse();
        var row = first.publication();
        assertThat(row.lifecycle()).isEqualTo("PUBLISHED");
        assertThat(row.stateVersion()).isZero();
        assertThat(row.publishedBy()).isEqualTo("synthetic-layout-author");
        assertThat(DocumentLayoutRules.digest("MASTERS_DOCUMENT_LAYOUT_V1\n" + row.canonicalJson()))
                .isEqualTo(row.definitionDigest());
        assertThat(f.v1.get(row.sourceRevisionId()).block(WAIT).sourceDigest())
                .isEqualTo(request.expectedDraftReference().sourceDigest());
        var replay = f.service.publish(f.template, request).block(WAIT);
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.publication().layoutRevisionId()).isEqualTo(row.layoutRevisionId());
        assertThat(f.count("document_template_revision", "template_id", f.template)).isEqualTo(1);
        for (String t :
                List.of(
                        "document_layout_action",
                        "document_layout_history",
                        "document_layout_outbox"))
            assertThat(f.count(t, "layout_revision_id", row.layoutRevisionId())).isEqualTo(1);
        assertThat(f.policyInsideTransaction).isFalse();
    }

    @Test
    void currentPolicyRunsBeforeAcceptedActionReplayAndStoredRead() {
        var f = new Fixture();
        var request = f.request();
        var row = f.service.publish(f.template, request).block(WAIT).publication();
        f.revoked.set(true);
        status(403, () -> f.service.publish(f.template, request).block(WAIT));
        status(403, () -> f.service.get(row.layoutRevisionId()).block(WAIT));
        status(
                403,
                () ->
                        f.service
                                .retire(
                                        row.layoutRevisionId(),
                                        new DocumentLayoutRetireRequest(
                                                UUID.randomUUID().toString(), 0, "Withdraw"))
                                .block(WAIT));
        assertThat(f.policyInsideTransaction).isFalse();
    }

    @Test
    void changedDraftAndMissingRegistryDoNotRewriteHistoricalAcceptedActions() {
        var f = new Fixture();
        var request = f.request();
        var row = f.service.publish(f.template, request).block(WAIT).publication();
        f.db.sql("UPDATE document_template_catalog SET version='2' WHERE template_id=:id")
                .bind("id", f.template)
                .fetch()
                .rowsUpdated()
                .block(WAIT);
        f.registryAvailable.set(false);
        assertThat(f.service.publish(f.template, request).block(WAIT).publication().canonicalJson())
                .isEqualTo(row.canonicalJson());
        assertThat(f.service.get(row.layoutRevisionId()).block(WAIT).canonicalJson())
                .isEqualTo(row.canonicalJson());
        var retire =
                new DocumentLayoutRetireRequest(UUID.randomUUID().toString(), 0, "Withdrawal é");
        assertThat(f.service.retire(row.layoutRevisionId(), retire).block(WAIT).lifecycle())
                .isEqualTo("RETIRED");
        assertThat(f.service.retire(row.layoutRevisionId(), retire).block(WAIT).stateVersion())
                .isEqualTo(1);
        assertThat(f.service.publish(f.template, request).block(WAIT).publication().lifecycle())
                .isEqualTo("RETIRED");
        assertThat(f.count("document_layout_history", "layout_revision_id", row.layoutRevisionId()))
                .isEqualTo(2);
    }

    @Test
    void committedV1IdentityAndChangedDraftObservationCannotBeAdopted() {
        var f = new Fixture();
        var request = f.request();
        f.v1.publish(f.template, new DocumentTemplatePublicationRequest("1", "en-US")).block(WAIT);
        status(409, () -> f.service.publish(f.template, request).block(WAIT));
        assertThat(f.count("document_layout_publication", "template_id", f.template)).isZero();
        var other = new Fixture();
        var stale = other.request();
        other.db
                .sql(
                        "UPDATE document_template_localization SET"
                                + " template_content='<p>Changed</p>' WHERE template_id=:id")
                .bind("id", other.template)
                .fetch()
                .rowsUpdated()
                .block(WAIT);
        status(409, () -> other.service.publish(other.template, stale).block(WAIT));
        assertThat(other.count("document_template_revision", "template_id", other.template))
                .isZero();
    }

    @Test
    void concurrentIdenticalPublicationHasOneSourceAssociationAndEvidence() {
        var f = new Fixture();
        var request = f.request();
        var results =
                Flux.range(0, 4)
                        .flatMap(
                                i ->
                                        f.service
                                                .publish(f.template, request)
                                                .subscribeOn(
                                                        reactor.core.scheduler.Schedulers
                                                                .parallel()),
                                4)
                        .collectList()
                        .block(WAIT);
        assertThat(results).hasSize(4);
        assertThat(results.stream().map(r -> r.publication().layoutRevisionId()).distinct().count())
                .isEqualTo(1);
        assertThat(results.stream().filter(r -> !r.replayed()).count()).isEqualTo(1);
        assertThat(f.count("document_template_revision", "template_id", f.template)).isEqualTo(1);
        assertThat(
                        f.count(
                                "document_layout_action",
                                "layout_revision_id",
                                results.getFirst().publication().layoutRevisionId()))
                .isEqualTo(1);
        assertThat(f.policyInsideTransaction).isFalse();
    }

    @Test
    void ambientSqlTransactionRejectsBeforeAnyPolicyInvocation() {
        var f = new Fixture();
        var request = f.request();
        int before = f.policyCalls.get();
        status(
                409,
                () ->
                        f.service
                                .publish(f.template, request)
                                .as(TransactionalOperator.create(f.manager)::transactional)
                                .block(WAIT));
        assertThat(f.policyCalls).hasValue(before);
        assertThat(f.count("document_layout_publication", "template_id", f.template)).isZero();
    }

    @Test
    void currentResolutionRequiresProfileAndSourceNoticeEligibility() {
        var f = new Fixture();
        var request = f.request();
        var row = f.service.publish(f.template, request).block(WAIT).publication();
        assertThat(
                        f.service
                                .resolve(
                                        row.templateCode(),
                                        "1",
                                        "en-US",
                                        DocumentLayoutRulesTest.profileReference())
                                .block(WAIT)
                                .layoutRevisionId())
                .isEqualTo(row.layoutRevisionId());
        f.v1.retire(UUID.fromString(f.notice.revisionId())).block(WAIT);
        status(
                409,
                () ->
                        f.service
                                .resolve(
                                        row.templateCode(),
                                        "1",
                                        "en-US",
                                        DocumentLayoutRulesTest.profileReference())
                                .block(WAIT));
        assertThat(f.service.get(row.layoutRevisionId()).block(WAIT).lifecycle())
                .isEqualTo("PUBLISHED");
        assertThat(
                        f.service
                                .publish(f.template, request)
                                .block(WAIT)
                                .publication()
                                .layoutRevisionId())
                .isEqualTo(row.layoutRevisionId());
    }

    @Test
    void fullActionIntentAndGlobalKindRemainImmutableWithCurrentPolicyFirst() {
        var f = new Fixture();
        var request = f.request();
        var row = f.service.publish(f.template, request).block(WAIT).publication();
        var changed =
                new DocumentLayoutPublishRequest(
                        request.actionId(),
                        request.expectedDraftReference(),
                        new DocumentLayoutOutputProfileReference(
                                "SYNTHETIC_A4", "2", "text/html", "2".repeat(64)),
                        request.semanticDefinition());
        status(409, () -> f.service.publish(f.template, changed).block(WAIT));
        status(
                409,
                () ->
                        f.service
                                .retire(
                                        row.layoutRevisionId(),
                                        new DocumentLayoutRetireRequest(
                                                request.actionId(), 0, "Withdraw"))
                                .block(WAIT));
        f.revoked.set(true);
        status(403, () -> f.service.publish(f.template, changed).block(WAIT));
        assertThat(f.count("document_layout_action", "layout_revision_id", row.layoutRevisionId()))
                .isEqualTo(1);
    }

    @Test
    void currentRegistryAndRetainedMetadataAreDistinctFromHistoricalReadback() {
        var f = new Fixture();
        var request = f.request();
        f.registryAvailable.set(false);
        status(503, () -> f.service.publish(f.template, request).block(WAIT));
        assertThat(f.count("document_template_revision", "template_id", f.template)).isZero();
        f.registryAvailable.set(true);
        var row = f.service.publish(f.template, request).block(WAIT).publication();
        f.changedProfile.set(true);
        status(
                409,
                () ->
                        f.service
                                .resolve(
                                        row.templateCode(),
                                        row.version(),
                                        row.localeCode(),
                                        request.outputProfile())
                                .block(WAIT));
        assertThat(f.service.get(row.layoutRevisionId()).block(WAIT).definition())
                .isEqualTo(row.definition());
        assertThat(f.service.publish(f.template, request).block(WAIT).publication().definition())
                .isEqualTo(row.definition());
    }

    @Test
    void failedEvidenceInsertionRollsBackNewSourceAssociationAndAllNativeEvidence() {
        var f = new Fixture();
        var request = f.request();
        f.db.sql(
                        "CREATE FUNCTION synthetic_layout_fail_outbox() RETURNS trigger LANGUAGE"
                                + " plpgsql AS $$ BEGIN RAISE EXCEPTION 'Synthetic outbox failure'"
                                + " USING ERRCODE='23514'; END $$")
                .fetch()
                .rowsUpdated()
                .block(WAIT);
        f.db.sql(
                        "CREATE TRIGGER synthetic_layout_fail BEFORE INSERT ON"
                                + " document_layout_outbox FOR EACH ROW EXECUTE FUNCTION"
                                + " synthetic_layout_fail_outbox()")
                .fetch()
                .rowsUpdated()
                .block(WAIT);
        try {
            status(409, () -> f.service.publish(f.template, request).block(WAIT));
        } finally {
            f.db.sql("DROP TRIGGER synthetic_layout_fail ON document_layout_outbox")
                    .fetch()
                    .rowsUpdated()
                    .block(WAIT);
            f.db.sql("DROP FUNCTION synthetic_layout_fail_outbox()")
                    .fetch()
                    .rowsUpdated()
                    .block(WAIT);
        }
        assertThat(f.count("document_template_revision", "template_id", f.template)).isZero();
        assertThat(f.count("document_layout_publication", "template_id", f.template)).isZero();
        assertThat(
                        f.count(
                                "document_layout_action",
                                "action_id",
                                UUID.fromString(request.actionId())))
                .isZero();
        assertThat(
                        f.count(
                                "document_layout_history",
                                "action_id",
                                UUID.fromString(request.actionId())))
                .isZero();
        assertThat(
                        f.count(
                                "document_layout_outbox",
                                "action_id",
                                UUID.fromString(request.actionId())))
                .isZero();
    }

    @Test
    void completeSortedTemplateSetPreventsOppositeMainNoticeFanoutDeadlock() {
        var f = new Fixture();
        UUID a = f.draft(true), b = f.draft(true);
        var a1 = f.v1.publish(a, new DocumentTemplatePublicationRequest("1", "en-US")).block(WAIT);
        var b1 = f.v1.publish(b, new DocumentTemplatePublicationRequest("1", "en-US")).block(WAIT);
        f.db.sql("UPDATE document_template_catalog SET version='2' WHERE template_id IN (:a,:b)")
                .bind("a", a)
                .bind("b", b)
                .fetch()
                .rowsUpdated()
                .block(WAIT);
        var ar =
                new DocumentLayoutMasterReference(
                        a1.revisionId().toString(),
                        a.toString(),
                        a1.templateCode(),
                        "1",
                        "en-US",
                        "text/html",
                        "MASTERS_JSON_V1",
                        a1.sourceDigest(),
                        a1.contentDigest(),
                        a1.schemaDigest(),
                        a1.styleDigest());
        var br =
                new DocumentLayoutMasterReference(
                        b1.revisionId().toString(),
                        b.toString(),
                        b1.templateCode(),
                        "1",
                        "en-US",
                        "text/html",
                        "MASTERS_JSON_V1",
                        b1.sourceDigest(),
                        b1.contentDigest(),
                        b1.schemaDigest(),
                        b1.styleDigest());
        var ra =
                new DocumentLayoutPublishRequest(
                        UUID.randomUUID().toString(),
                        f.service.observe(a, "en-US").block(WAIT).draftReference(),
                        DocumentLayoutRulesTest.profileReference(),
                        new DocumentLayoutSemanticDefinition(
                                "AUTHORED_PAGES",
                                "A4",
                                "PORTRAIT",
                                List.of(
                                        new DocumentLayoutNoticeReference(
                                                DocumentLayoutNoticeReference.Role
                                                        .GENERAL_DISCLOSURE,
                                                DocumentLayoutNoticeReference.Placement.FOOTER,
                                                br))));
        var rb =
                new DocumentLayoutPublishRequest(
                        UUID.randomUUID().toString(),
                        f.service.observe(b, "en-US").block(WAIT).draftReference(),
                        DocumentLayoutRulesTest.profileReference(),
                        new DocumentLayoutSemanticDefinition(
                                "AUTHORED_PAGES",
                                "A4",
                                "PORTRAIT",
                                List.of(
                                        new DocumentLayoutNoticeReference(
                                                DocumentLayoutNoticeReference.Role
                                                        .GENERAL_DISCLOSURE,
                                                DocumentLayoutNoticeReference.Placement.FOOTER,
                                                ar))));
        var both = Sinks.<Void>one();
        var entered = new AtomicInteger();
        f.policyHook.set(
                context -> {
                    if (!context.containsKey("outputProfile")) return Mono.empty();
                    if (entered.incrementAndGet() == 2) both.tryEmitEmpty();
                    return both.asMono().timeout(Duration.ofSeconds(5));
                });
        var rows =
                Flux.merge(f.service.publish(a, ra), f.service.publish(b, rb))
                        .collectList()
                        .block(WAIT);
        assertThat(rows).hasSize(2);
        assertThat(rows).allSatisfy(result -> assertThat(result.replayed()).isFalse());
        assertThat(f.count("document_template_revision", "template_id", a)).isEqualTo(2);
        assertThat(f.count("document_template_revision", "template_id", b)).isEqualTo(2);
        assertThat(f.policyInsideTransaction).isFalse();
    }

    @Test
    void noticeRetirementDuringOutsideTransactionPolicyPreventsNewPublication() {
        var f = new Fixture();
        var request = f.request();
        var entered = Sinks.<Void>one();
        var continuePolicy = Sinks.<Void>one();
        f.policyHook.set(
                context -> {
                    if (!context.containsKey("outputProfile")) return Mono.empty();
                    entered.tryEmitEmpty();
                    return continuePolicy.asMono().timeout(Duration.ofSeconds(5));
                });
        var outcome = f.service.publish(f.template, request).materialize().cache();
        outcome.subscribe();
        entered.asMono().block(WAIT);
        f.v1.retire(UUID.fromString(f.notice.revisionId())).block(WAIT);
        continuePolicy.tryEmitEmpty();
        var result = outcome.block(WAIT);
        assertThat(result.isOnError()).isTrue();
        assertThat(result.getThrowable())
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        assertThat(f.count("document_template_revision", "template_id", f.template)).isZero();
        assertThat(f.count("document_layout_publication", "template_id", f.template)).isZero();
        assertThat(f.policyInsideTransaction).isFalse();
    }

    @Test
    void storedContextAuthorizationRunsBeforeReplayIntentComparisonAndHistoricalRead() {
        var f = new Fixture();
        var request = f.request();
        var row = f.service.publish(f.template, request).block(WAIT).publication();
        f.storedContextRevoked.set(true);
        var changed =
                new DocumentLayoutPublishRequest(
                        request.actionId(),
                        request.expectedDraftReference(),
                        new DocumentLayoutOutputProfileReference(
                                "SYNTHETIC_A4", "2", "text/html", "2".repeat(64)),
                        request.semanticDefinition());
        status(403, () -> f.service.publish(f.template, request).block(WAIT));
        status(403, () -> f.service.publish(f.template, changed).block(WAIT));
        status(403, () -> f.service.get(row.layoutRevisionId()).block(WAIT));
        status(
                403,
                () ->
                        f.service
                                .retire(
                                        row.layoutRevisionId(),
                                        new DocumentLayoutRetireRequest(
                                                UUID.randomUUID().toString(), 0, "Withdraw"))
                                .block(WAIT));
        f.storedContextRevoked.set(false);
        assertThat(f.service.get(row.layoutRevisionId()).block(WAIT).stateVersion()).isZero();
    }

    @Test
    void
            existingV1CanonicalSourceWithSupplementaryUnicodeKeysAndControlEscapesPublishesUnchanged() {
        var f = new Fixture();
        f.db.sql(
                        "UPDATE document_template_catalog SET template_variables=CAST(:variables AS"
                                + " jsonb) WHERE template_id=:id")
                .bind("id", f.template)
                .bind(
                        "variables",
                        "{\"\uE000\":1,\"😀\":2,\"control\":\"\\u001f\",\"literal\":\"\\\\u001f\"}")
                .fetch()
                .rowsUpdated()
                .block(WAIT);
        var request = f.request();
        var row = f.service.publish(f.template, request).block(WAIT).publication();
        assertThat(f.v1.get(row.sourceRevisionId()).block(WAIT).sourceDigest())
                .isEqualTo(request.expectedDraftReference().sourceDigest());
        assertThat(f.service.publish(f.template, request).block(WAIT).publication().canonicalJson())
                .isEqualTo(row.canonicalJson());
    }
}
