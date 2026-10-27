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

package io.fireflysoftware.masters.core.services.currency.policy;

import static org.assertj.core.api.Assertions.*;

import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.models.repositories.currency.policy.CurrencyPolicyRepository;
import io.r2dbc.spi.*;

import org.fireflyframework.security.api.domain.*;
import org.fireflyframework.security.spi.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

import reactor.core.publisher.*;

import java.sql.DriverManager;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Testcontainers
class CurrencyPolicyDatabaseTest {
    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("synthetic_currency_policy")
                    .withUsername("currency_bootstrap")
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

    static void owner(String sql) throws Exception {
        try (var c =
                        DriverManager.getConnection(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword());
                var s = c.createStatement()) {
            s.execute(sql);
        }
    }

    @BeforeAll
    static void migrate() throws Exception {
        owner(
                "CREATE ROLE firefly_currency_policy_guard NOLOGIN NOSUPERUSER NOBYPASSRLS; CREATE"
                    + " ROLE currency_app LOGIN PASSWORD 'synthetic-only' NOSUPERUSER NOBYPASSRLS");
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        owner(
                "REVOKE CREATE ON SCHEMA public FROM PUBLIC; GRANT USAGE ON SCHEMA public TO"
                        + " currency_app; GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN SCHEMA"
                        + " public TO currency_app");
    }

    static final class Fixture {
        final ConnectionFactory cf =
                ConnectionFactories.get(
                        "r2dbc:postgresql://currency_app:synthetic-only@"
                                + postgres.getHost()
                                + ":"
                                + postgres.getMappedPort(5432)
                                + "/"
                                + postgres.getDatabaseName());
        final DatabaseClient db = DatabaseClient.create(cf);
        final AtomicBoolean revoked = new AtomicBoolean();
        final CurrencyExecutionPolicyService service;
        final UUID currency = UUID.randomUUID();
        final String iso;

        Fixture(String iso) throws Exception {
            this.iso = iso;
            owner(
                    "INSERT INTO"
                        + " currencies(currency_id,iso_code,currency_name,symbol,decimal_precision,is_major,status)"
                        + " VALUES('"
                            + currency
                            + "','"
                            + iso
                            + "','Synthetic Currency','$',2,false,'ACTIVE')");
            var beans = new StaticListableBeanFactory();
            beans.addBean(
                    "context",
                    (SecurityContextPort)
                            () ->
                                    Mono.just(
                                            SecurityPrincipal.builder()
                                                    .subject("synthetic-author")
                                                    .issuer("https://synthetic.invalid")
                                                    .claims(
                                                            Map.of(
                                                                    "exp",
                                                                    Instant.now().plusSeconds(120)))
                                                    .build()));
            beans.addBean(
                    "policy",
                    (PolicyDecisionPort)
                            (p, a, r, c) ->
                                    Mono.just(
                                            revoked.get()
                                                    ? Decision.deny("withdrawn")
                                                    : Decision.permit()));
            var operations=new org.springframework.data.r2dbc.core.R2dbcEntityTemplate(db,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE);
            service =
                    new io.fireflysoftware.masters.core.services.currency.policy.impl.CurrencyExecutionPolicyServiceImpl(
                            new io.fireflysoftware.masters.models.repositories.currency.policy.impl.CurrencyPolicyRepositoryImpl(operations),
                            new CurrencyPolicyDatabaseContext(operations),
                            new R2dbcTransactionManager(cf),
                            new CurrencyPolicyAuthority(
                                    beans.getBeanProvider(SecurityContextPort.class),
                                    beans.getBeanProvider(PolicyDecisionPort.class),
                                    new org.springframework.mock.env.MockEnvironment()
                                            .withProperty(
                                                    CurrencyPolicyAuthority.STRICT_POLICY,
                                                    "true")),operations);
        }

        CurrencyPolicyPublishRequest request() {
            var o = service.observe(currency).block(WAIT);
            return new CurrencyPolicyPublishRequest(
                    UUID.randomUUID().toString(),
                    currency.toString(),
                    iso,
                    o.digest(),
                    o.headVersion(),
                    2,
                    -1L,
                    null,
                    "Synthetic authority record v1");
        }

        CurrencyPolicyView publish(CurrencyPolicyPublishRequest r) {
            return service.publish(r).block(WAIT).publication();
        }
    }

    static void status(int code, Runnable r) {
        assertThatThrownBy(r::run)
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(code));
    }

    @Test
    void publicationAndCurrentReplayRetainBytesAcrossMetadataChanges() throws Exception {
        var f = new Fixture("USD");
        var r = f.request();
        var first = f.publish(r);
        assertThat(first.reference().version()).isEqualTo(1L);
        assertThat(first.lifecycle()).isEqualTo("PUBLISHED");
        assertThat(first.effectiveNow()).isTrue();
        assertThat(first.reference().digest()).matches("[0-9a-f]{64}");
        owner(
                "UPDATE currencies SET"
                        + " currency_name='Changed',decimal_precision=3,status='INACTIVE' WHERE"
                        + " currency_id='"
                        + f.currency
                        + "'");
        var replay = f.service.publish(r).block(WAIT);
        assertThat(replay.replayed()).isTrue();
        assertThat(replay.publication().canonicalEncoding()).isEqualTo(first.canonicalEncoding());
        assertThat(
                        f.service
                                .get(UUID.fromString(first.reference().policyId()), f.currency)
                                .block(WAIT)
                                .reference())
                .isEqualTo(first.reference());
        f.revoked.set(true);
        status(403, () -> f.service.publish(r).block(WAIT));
        status(403, () -> f.service.observe(f.currency).block(WAIT));
    }

    @Test
    void nullPrecisionStaleDigestAndScaleMismatchHaveNoEffects() throws Exception {
        var f = new Fixture("EUR");
        var r = f.request();
        owner(
                "UPDATE currencies SET decimal_precision=NULL WHERE currency_id='"
                        + f.currency
                        + "'");
        status(409, () -> f.publish(r));
        assertThat(f.service.observe(f.currency).block(WAIT).headVersion()).isZero();
        owner("UPDATE currencies SET decimal_precision=3 WHERE currency_id='" + f.currency + "'");
        status(409, () -> f.publish(r));
        var fresh = f.request();
        status(409, () -> f.publish(fresh));
    }

    @Test
    void sameHeadAndIsoIdentityCannotBeClaimedTwice() throws Exception {
        var f = new Fixture("GBP");
        var r = f.request();
        f.publish(r);
        var second =
                new CurrencyPolicyPublishRequest(
                        UUID.randomUUID().toString(),
                        r.currencyId(),
                        r.isoCode(),
                        r.expectedCurrencyDigest(),
                        0L,
                        2,
                        -1L,
                        null,
                        r.provenanceReference());
        status(409, () -> f.publish(second));
        var other = new Fixture("GBP");
        status(409, () -> other.publish(other.request()));
    }

    @Test
    void hiddenGlobalActionCollisionIsExplicitConflict() throws Exception {
        var a = new Fixture("JPY");
        var b = new Fixture("CAD");
        var ra = a.request();
        a.publish(ra);
        var rb = b.request();
        rb =
                new CurrencyPolicyPublishRequest(
                        ra.actionId(),
                        rb.currencyId(),
                        rb.isoCode(),
                        rb.expectedCurrencyDigest(),
                        rb.expectedHeadVersion(),
                        rb.postedScale(),
                        rb.effectiveFromEpochMillis(),
                        rb.effectiveToEpochMillis(),
                        rb.provenanceReference());
        var conflict = rb;
        status(409, () -> b.publish(conflict));
        assertThat(b.service.observe(b.currency).block(WAIT).headVersion()).isZero();
    }

    @Test
    void retirementPreservesHistoryAndNeverAutoSelectsAnotherPolicy() throws Exception {
        var f = new Fixture("CHF");
        var r = f.request();
        var view = f.publish(r);
        var id = UUID.fromString(view.reference().policyId());
        var retirement =
                new CurrencyPolicyRetireRequest(
                        UUID.randomUUID().toString(),
                        f.currency.toString(),
                        0L,
                        "Synthetic retirement");
        var retired = f.service.retire(id, retirement).block(WAIT);
        assertThat(retired.lifecycle()).isEqualTo("RETIRED");
        assertThat(retired.effectiveNow()).isFalse();
        assertThat(f.service.retire(id, retirement).block(WAIT).stateVersion()).isEqualTo(1L);
        assertThat(f.service.get(id, f.currency).block(WAIT).canonicalEncoding())
                .isEqualTo(view.canonicalEncoding());
    }

    @Test
    void rawIdentityMutationDeleteAndUnscopedNativeReadAreRejected() throws Exception {
        var f = new Fixture("AUD");
        f.publish(f.request());
        assertThatThrownBy(
                () ->
                        owner(
                                "UPDATE currencies SET iso_code='NZD' WHERE currency_id='"
                                        + f.currency
                                        + "'"));
        assertThatThrownBy(
                () -> owner("DELETE FROM currencies WHERE currency_id='" + f.currency + "'"));
        assertThat(
                        f.db.sql("SELECT count(*) n FROM currency_execution_policy")
                                .map((r, m) -> r.get("n", Long.class))
                                .one()
                                .block(WAIT))
                .isZero();
        assertThat(
                        f.db.sql("SELECT current_setting('firefly.currency.currency_id',true) v")
                                .map((r, m) -> Objects.toString(r.get("v"), ""))
                                .one()
                                .block(WAIT))
                .isEmpty();
    }

    @Test
    void auditFailureRollsBackClaimMappingAndHead() throws Exception {
        var f = new Fixture("NOK");
        var r = f.request();
        owner(
                "CREATE FUNCTION fail_currency_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN"
                    + " RAISE EXCEPTION 'synthetic audit failure'; END $$; CREATE TRIGGER"
                    + " synthetic_currency_audit BEFORE INSERT ON currency_policy_audit FOR EACH"
                    + " ROW EXECUTE FUNCTION fail_currency_audit()");
        try {
            assertThatThrownBy(() -> f.publish(r));
        } finally {
            owner(
                    "DROP TRIGGER synthetic_currency_audit ON currency_policy_audit; DROP FUNCTION"
                            + " fail_currency_audit()");
        }
        assertThat(f.service.observe(f.currency).block(WAIT).headVersion()).isZero();
        assertThat(f.publish(r).reference().version()).isEqualTo(1L);
    }

    @Test
    void concurrentIdenticalActionsAndHeadCompetitorsRetainOneVersion() throws Exception {
        var f = new Fixture("SEK");
        var r = f.request();
        var identical =
                Flux.range(0, 6).flatMap(i -> f.service.publish(r), 6).collectList().block(WAIT);
        assertThat(identical).hasSize(6);
        assertThat(identical.stream().map(x -> x.publication().reference().policyId()).distinct())
                .hasSize(1);
        assertThat(identical.stream().filter(x -> !x.replayed())).hasSize(1);
        var next = f.request();
        var rival =
                new CurrencyPolicyPublishRequest(
                        UUID.randomUUID().toString(),
                        next.currencyId(),
                        next.isoCode(),
                        next.expectedCurrencyDigest(),
                        next.expectedHeadVersion(),
                        next.postedScale(),
                        next.effectiveFromEpochMillis(),
                        next.effectiveToEpochMillis(),
                        next.provenanceReference());
        var attempts =
                Flux.just(next, rival)
                        .flatMap(
                                x ->
                                        f.service
                                                .publish(x)
                                                .map(y -> 201)
                                                .onErrorResume(
                                                        ResponseStatusException.class,
                                                        e -> Mono.just(e.getStatusCode().value())),
                                2)
                        .collectList()
                        .block(WAIT);
        assertThat(attempts).containsExactlyInAnyOrder(201, 409);
        assertThat(f.service.observe(f.currency).block(WAIT).headVersion()).isEqualTo(2L);
    }

    @Test
    void outboxFailureAlsoRollsBackAllEffects() throws Exception {
        var f = new Fixture("DKK");
        var r = f.request();
        owner(
                "CREATE FUNCTION fail_currency_outbox() RETURNS trigger LANGUAGE plpgsql AS $$"
                    + " BEGIN RAISE EXCEPTION 'synthetic outbox failure'; END $$; CREATE TRIGGER"
                    + " synthetic_currency_outbox BEFORE INSERT ON currency_policy_outbox FOR EACH"
                    + " ROW EXECUTE FUNCTION fail_currency_outbox()");
        try {
            assertThatThrownBy(() -> f.publish(r));
        } finally {
            owner(
                    "DROP TRIGGER synthetic_currency_outbox ON currency_policy_outbox; DROP"
                            + " FUNCTION fail_currency_outbox()");
        }
        assertThat(f.service.observe(f.currency).block(WAIT).headVersion()).isZero();
        assertThat(f.publish(r).reference().version()).isEqualTo(1L);
    }

    @Test
    void nativeRuntimeRejectsPartialTableOwnershipAndUnprivilegedRoleCannotReplaceGuard()
            throws Exception {
        var f = new Fixture("PLN");
        f.publish(f.request());
        assertThat(
                        f.db.sql(
                                        "SELECT"
                                            + " has_schema_privilege(current_user,'public','CREATE')"
                                            + " OR pg_has_role(current_user,'firefly_currency_policy_guard','MEMBER')"
                                            + " OR has_function_privilege(current_user,'public.guard_currency_catalog_identity()','EXECUTE')"
                                            + " allowed")
                                .map((r, m) -> r.get("allowed", Boolean.class))
                                .one()
                                .block(WAIT))
                .isFalse();
        owner("ALTER TABLE currency_policy_outbox OWNER TO currency_app");
        try {
            status(503, () -> f.service.observe(f.currency).block(WAIT));
        } finally {
            owner(
                    "ALTER TABLE currency_policy_outbox OWNER TO "
                            + postgres.getUsername()
                            + "; GRANT SELECT,INSERT,UPDATE,DELETE ON currency_policy_outbox TO"
                            + " currency_app");
        }
        assertThat(f.service.observe(f.currency).block(WAIT).headVersion()).isEqualTo(1L);
    }

    @Test
    void pooledTransactionContextClearsOnCommitRollbackAndCancellation() throws Exception {
        var f = new Fixture("CZK");
        var pool =
                new io.r2dbc.pool.ConnectionPool(
                        io.r2dbc.pool.ConnectionPoolConfiguration.builder(f.cf)
                                .maxSize(1)
                                .initialSize(1)
                                .build());
        try {
            var db = DatabaseClient.create(pool);
            var ctx = new CurrencyPolicyDatabaseContext(new org.springframework.data.r2dbc.core.R2dbcEntityTemplate(db,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE));
            var tx =
                    org.springframework.transaction.reactive.TransactionalOperator.create(
                            new R2dbcTransactionManager(pool));
            var p =
                    SecurityPrincipal.builder()
                            .subject("synthetic-pool")
                            .issuer("https://synthetic.invalid")
                            .claims(Map.of("exp", Instant.now().plusSeconds(60)))
                            .build();
            java.util.function.Supplier<String> setting =
                    () ->
                            db.sql("SELECT current_setting('firefly.currency.currency_id',true) v")
                                    .map((r, m) -> Objects.toString(r.get("v"), ""))
                                    .one()
                                    .block(WAIT);
            tx.transactional(ctx.establish(p, f.currency, "READ").thenReturn(true)).block(WAIT);
            assertThat(setting.get()).isEmpty();
            assertThatThrownBy(
                    () ->
                            tx.transactional(
                                            ctx.establish(p, f.currency, "READ")
                                                    .then(
                                                            Mono.error(
                                                                    new IllegalStateException(
                                                                            "synthetic rollback"))))
                                    .block(WAIT));
            assertThat(setting.get()).isEmpty();
            reactor.test.StepVerifier.create(
                            tx.transactional(
                                    ctx.establish(p, f.currency, "READ").then(Mono.never())))
                    .thenAwait(Duration.ofMillis(150))
                    .thenCancel()
                    .verify(WAIT);
            assertThat(setting.get()).isEmpty();
        } finally {
            pool.disposeLater().block(WAIT);
        }
    }

    @Test
    void legacyWriterHoldingRowFailsFastAgainstNativeGateWithoutInversion() throws Exception {
        var f = new Fixture("HUF");
        try (var nativeConnection =
                        DriverManager.getConnection(
                                postgres.getJdbcUrl(),
                                postgres.getUsername(),
                                postgres.getPassword());
                var writer =
                        DriverManager.getConnection(
                                postgres.getJdbcUrl(), "currency_app", "synthetic-only")) {
            nativeConnection.setAutoCommit(false);
            writer.setAutoCommit(false);
            try (var q = writer.createStatement()) {
                q.execute(
                        "SELECT currency_id FROM currencies WHERE currency_id='"
                                + f.currency
                                + "' FOR NO KEY UPDATE");
            }
            try (var q = nativeConnection.createStatement()) {
                q.execute(
                        "SELECT pg_advisory_xact_lock(hashtextextended('MASTERS_CURRENCY:CURRENCY:"
                                + f.currency
                                + "',0))");
            }
            assertThatThrownBy(
                            () -> {
                                try (var q = writer.createStatement()) {
                                    q.execute(
                                            "SET LOCAL statement_timeout='2s'; UPDATE currencies"
                                                    + " SET currency_name='Contending' WHERE"
                                                    + " currency_id='"
                                                    + f.currency
                                                    + "'");
                                }
                            })
                    .isInstanceOfSatisfying(
                            java.sql.SQLException.class,
                            e -> assertThat(e.getSQLState()).isEqualTo("23514"));
            writer.rollback();
            nativeConnection.rollback();
        }
        assertThat(f.publish(f.request()).reference().version()).isEqualTo(1L);
    }

    @Test
    void standaloneZeroHeadIdentityCannotReserveAnIsoWithoutPublication() throws Exception {
        var f = new Fixture("RON");
        var principal =
                SecurityPrincipal.builder()
                        .subject("synthetic-source-backstop")
                        .issuer("https://synthetic.invalid")
                        .claims(Map.of("exp", Instant.now().plusSeconds(60)))
                        .build();
        var tx =
                org.springframework.transaction.reactive.TransactionalOperator.create(
                        new R2dbcTransactionManager(f.cf));
        assertThatThrownBy(
                        () ->
                                tx.transactional(
                                                new CurrencyPolicyDatabaseContext(new org.springframework.data.r2dbc.core.R2dbcEntityTemplate(f.db,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE))
                                                        .establish(principal, f.currency, "PUBLISH")
                                                        .then(
                                                                f.db.sql(
                                                                                "INSERT INTO"
                                                                                    + " currency_policy_identity(currency_id,iso_code,head_version)"
                                                                                    + " VALUES(:id,'RON',0)")
                                                                        .bind("id", f.currency)
                                                                        .fetch()
                                                                        .rowsUpdated())
                                                        .thenReturn(true))
                                        .block(WAIT))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(f.service.observe(f.currency).block(WAIT).headVersion()).isZero();
        assertThat(f.publish(f.request()).reference().version()).isEqualTo(1L);
    }

    @Test
    void nondefaultSearchPathCannotSubstituteCanonicalCurrencyMetadata() throws Exception {
        var f = new Fixture("ZAR");
        owner(
                "CREATE SCHEMA currency_shadow AUTHORIZATION currency_app; CREATE TABLE"
                    + " currency_shadow.currencies (LIKE public.currencies INCLUDING ALL); GRANT"
                    + " SELECT,UPDATE ON currency_shadow.currencies TO currency_app; INSERT INTO"
                    + " currency_shadow.currencies(currency_id,iso_code,currency_name,symbol,decimal_precision,is_major,status)"
                    + " VALUES('"
                        + f.currency
                        + "','ZAR','Forged shadow catalog','$',9,false,'ACTIVE'); ALTER ROLE"
                        + " currency_app SET search_path TO currency_shadow,public");
        try {
            assertThat(f.service.observe(f.currency).block(WAIT).metadata().getCurrencyName())
                    .isEqualTo("Synthetic Currency");
            assertThat(f.publish(f.request()).postedScale()).isEqualTo(2);
        } finally {
            owner("ALTER ROLE currency_app RESET search_path");
        }
    }

    @Test
    void retirementControlCannotReusePublicationActionWithoutRetirementEvidence() throws Exception {
        var f = new Fixture("TRY");
        var view = f.publish(f.request());
        var principal =
                SecurityPrincipal.builder()
                        .subject("synthetic-control-backstop")
                        .issuer("https://synthetic.invalid")
                        .claims(Map.of("exp", Instant.now().plusSeconds(60)))
                        .build();
        var tx =
                org.springframework.transaction.reactive.TransactionalOperator.create(
                        new R2dbcTransactionManager(f.cf));
        assertThatThrownBy(
                        () ->
                                tx.transactional(
                                                new CurrencyPolicyDatabaseContext(new org.springframework.data.r2dbc.core.R2dbcEntityTemplate(f.db,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE))
                                                        .establish(principal, f.currency, "RETIRE")
                                                        .then(
                                                                f.db.sql(
                                                                                "UPDATE"
                                                                                    + " public.currency_policy_control"
                                                                                    + " SET lifecycle='RETIRED',state_version=1,retired_action_id=:action,retired_at=1,retired_by='synthetic-control-backstop',reason='Missing"
                                                                                    + " retirement"
                                                                                    + " evidence'"
                                                                                    + " WHERE"
                                                                                    + " policy_id=:policy")
                                                                        .bind(
                                                                                "action",
                                                                                UUID.fromString(
                                                                                        view
                                                                                                .publicationActionId()))
                                                                        .bind(
                                                                                "policy",
                                                                                UUID.fromString(
                                                                                        view.reference()
                                                                                                .policyId()))
                                                                        .fetch()
                                                                        .rowsUpdated())
                                                        .thenReturn(true))
                                        .block(WAIT))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(
                        f.service
                                .get(UUID.fromString(view.reference().policyId()), f.currency)
                                .block(WAIT)
                                .lifecycle())
                .isEqualTo("PUBLISHED");
    }

    @Test
    void initialIdentityStatementCannotBindAnotherCanonicalIso() throws Exception {
        var f = new Fixture("MXN");
        var principal =
                SecurityPrincipal.builder()
                        .subject("synthetic-identity-backstop")
                        .issuer("https://synthetic.invalid")
                        .claims(Map.of("exp", Instant.now().plusSeconds(60)))
                        .build();
        var tx =
                org.springframework.transaction.reactive.TransactionalOperator.create(
                        new R2dbcTransactionManager(f.cf));
        assertThatThrownBy(
                        () ->
                                tx.transactional(
                                                new CurrencyPolicyDatabaseContext(new org.springframework.data.r2dbc.core.R2dbcEntityTemplate(f.db,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE))
                                                        .establish(principal, f.currency, "PUBLISH")
                                                        .then(
                                                                f.db.sql(
                                                                                "INSERT INTO"
                                                                                    + " public.currency_policy_identity(currency_id,iso_code,head_version)"
                                                                                    + " VALUES(:id,'BRL',0)")
                                                                        .bind("id", f.currency)
                                                                        .fetch()
                                                                        .rowsUpdated())
                                                        .flatMap(
                                                                n ->
                                                                        Mono.error(
                                                                                new IllegalStateException(
                                                                                        "WRONG_ISO_ACCEPTED"))))
                                        .block(WAIT))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(f.service.observe(f.currency).block(WAIT).headVersion()).isZero();
        assertThat(f.publish(f.request()).reference().isoCode()).isEqualTo("MXN");
    }

    @Test
    void publicationAndRetirementEventsCarryExactRetainedPolicyReferences() throws Exception {
        var f = new Fixture("HKD");
        var request = f.request();
        var published = f.publish(request);
        var p =
                SecurityPrincipal.builder()
                        .subject("synthetic-event-reader")
                        .issuer("https://synthetic.invalid")
                        .claims(Map.of("exp", Instant.now().plusSeconds(60)))
                        .build();
        var tx =
                org.springframework.transaction.reactive.TransactionalOperator.create(
                        new R2dbcTransactionManager(f.cf));
        java.util.function.Function<String, String> payload =
                action ->
                        tx.transactional(
                                        new CurrencyPolicyDatabaseContext(new org.springframework.data.r2dbc.core.R2dbcEntityTemplate(f.db,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE))
                                                .establish(p, f.currency, "READ")
                                                .then(
                                                        f.db.sql(
                                                                        "SELECT payload FROM"
                                                                            + " public.currency_policy_outbox"
                                                                            + " WHERE"
                                                                            + " action_id=:action")
                                                                .bind(
                                                                        "action",
                                                                        UUID.fromString(action))
                                                                .map(
                                                                        (row, m) ->
                                                                                row.get(
                                                                                        "payload",
                                                                                        String
                                                                                                .class))
                                                                .one()))
                                .block(WAIT);
        var json = com.fasterxml.jackson.databind.json.JsonMapper.builder().build();
        var publication = json.readTree(payload.apply(request.actionId()));
        assertThat(publication.path("payloadVersion").asText())
                .isEqualTo("MASTERS_CURRENCY_POLICY_EVENT_V1");
        assertThat(
                        json.treeToValue(
                                publication.path("policyReference"), CurrencyPolicyReference.class))
                .isEqualTo(published.reference());
        assertThat(publication.path("actionKind").asText()).isEqualTo("PUBLISH");
        assertThat(publication.path("intentEncoding").asText())
                .isEqualTo(CurrencyPolicyRules.publicationIntent(request).encoding());
        assertThat(publication.path("observedAtEpochMillis").isIntegralNumber()).isTrue();
        var retirement =
                new CurrencyPolicyRetireRequest(
                        UUID.randomUUID().toString(),
                        f.currency.toString(),
                        0L,
                        "Explicit event retention");
        f.service.retire(UUID.fromString(published.reference().policyId()), retirement).block(WAIT);
        var retired = json.readTree(payload.apply(retirement.actionId()));
        assertThat(retired.path("policyReference")).isEqualTo(publication.path("policyReference"));
        assertThat(retired.path("actionKind").asText()).isEqualTo("RETIRE");
        assertThat(retired.path("intentDigest").asText())
                .isEqualTo(
                        CurrencyPolicyRules.retirementIntent(
                                        published.reference().policyId(), retirement)
                                .digest());
    }

    @Test
    void corruptedOutboxPolicyReferenceFailsDeferredCommitAndRollsBackPublication()
            throws Exception {
        var f = new Fixture("BRL");
        var request = f.request();
        owner(
                "CREATE FUNCTION synthetic_corrupt_currency_reference() RETURNS trigger LANGUAGE"
                    + " plpgsql AS $$ BEGIN"
                    + " NEW.payload:=jsonb_set(NEW.payload::jsonb,'{policyReference,digest}',to_jsonb(repeat('0',64)))::text;"
                    + " RETURN NEW; END $$; CREATE TRIGGER synthetic_corrupt_currency_reference"
                    + " BEFORE INSERT ON public.currency_policy_outbox FOR EACH ROW EXECUTE"
                    + " FUNCTION synthetic_corrupt_currency_reference()");
        try {
            status(409, () -> f.publish(request));
        } finally {
            owner(
                    "DROP TRIGGER synthetic_corrupt_currency_reference ON"
                            + " public.currency_policy_outbox; DROP FUNCTION"
                            + " synthetic_corrupt_currency_reference()");
        }
        assertThat(f.service.observe(f.currency).block(WAIT).headVersion()).isZero();
        assertThat(f.publish(request).reference().version()).isEqualTo(1L);
    }

    @Test
    void allPublicEntriesRejectAnAmbientLockedTransactionBeforePrincipalOrPolicyWork()
            throws Exception {
        var f = new Fixture("GHS");
        var original = f.publish(f.request());
        var publication = f.request();
        var policy = UUID.fromString(original.reference().policyId());
        var beans = new StaticListableBeanFactory();
        var contextCalls = new java.util.concurrent.atomic.AtomicInteger();
        var policyCalls = new java.util.concurrent.atomic.AtomicInteger();
        beans.addBean(
                "context",
                (SecurityContextPort)
                        () -> {
                            contextCalls.incrementAndGet();
                            return Mono.just(
                                    SecurityPrincipal.builder()
                                            .subject("synthetic-boundary")
                                            .issuer("https://synthetic.invalid")
                                            .claims(Map.of("exp", Instant.now().plusSeconds(120)))
                                            .build());
                        });
        beans.addBean(
                "policy",
                (PolicyDecisionPort)
                        (p, a, r, c) -> {
                            policyCalls.incrementAndGet();
                            return Mono.just(Decision.permit());
                        });
        var operations=new org.springframework.data.r2dbc.core.R2dbcEntityTemplate(f.db,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE);
        var service =
                new io.fireflysoftware.masters.core.services.currency.policy.impl.CurrencyExecutionPolicyServiceImpl(
                        new io.fireflysoftware.masters.models.repositories.currency.policy.impl.CurrencyPolicyRepositoryImpl(operations),
                        new CurrencyPolicyDatabaseContext(operations),
                        new R2dbcTransactionManager(f.cf),
                        new CurrencyPolicyAuthority(
                                beans.getBeanProvider(SecurityContextPort.class),
                                beans.getBeanProvider(PolicyDecisionPort.class),
                                new org.springframework.mock.env.MockEnvironment()
                                        .withProperty(
                                                CurrencyPolicyAuthority.STRICT_POLICY, "true")),operations);
        assertThat(service.observe(f.currency).block(WAIT)).isNotNull();
        assertThat(policyCalls).hasValue(1);
        contextCalls.set(0);
        policyCalls.set(0);
        var actions =
                List.<java.util.function.Supplier<Mono<?>>>of(
                        () -> service.observe(f.currency),
                        () -> service.publish(publication),
                        () -> service.get(policy, f.currency),
                        () ->
                                service.byAction(
                                        UUID.fromString(original.publicationActionId()),
                                        f.currency),
                        () ->
                                service.retire(
                                        policy,
                                        new CurrencyPolicyRetireRequest(
                                                UUID.randomUUID().toString(),
                                                f.currency.toString(),
                                                0L,
                                                "Synthetic withdrawal")));
        var outcomes = new ArrayList<Integer>();
        var preconditions = new ArrayList<String>();
        var tx =
                org.springframework.transaction.reactive.TransactionalOperator.create(
                        new R2dbcTransactionManager(f.cf));
        for (var action : actions) {
            tx.execute(
                            status ->
                                    f.db.sql(
                                                    "SELECT currency_id FROM public.currencies"
                                                            + " WHERE currency_id=:id FOR UPDATE")
                                            .bind("id", f.currency)
                                            .map((row, m) -> row.get("currency_id", UUID.class))
                                            .one()
                                            .doOnNext(
                                                    locked -> {
                                                        try (var connection =
                                                                        DriverManager.getConnection(
                                                                                postgres
                                                                                        .getJdbcUrl(),
                                                                                "currency_app",
                                                                                "synthetic-only");
                                                                var query =
                                                                        connection
                                                                                .createStatement()) {
                                                            query.executeQuery(
                                                                    "SELECT currency_id FROM"
                                                                        + " public.currencies WHERE"
                                                                        + " currency_id='"
                                                                            + f.currency
                                                                            + "' FOR UPDATE"
                                                                            + " NOWAIT");
                                                            preconditions.add("UNLOCKED");
                                                        } catch (java.sql.SQLException error) {
                                                            preconditions.add(error.getSQLState());
                                                        }
                                                    })
                                            .then(Mono.defer(action).materialize())
                                            .doOnNext(
                                                    signal -> {
                                                        status.setRollbackOnly();
                                                        outcomes.add(
                                                                signal.getThrowable()
                                                                                instanceof
                                                                                ResponseStatusException
                                                                                        failure
                                                                        ? failure.getStatusCode()
                                                                                .value()
                                                                        : 200);
                                                    })
                                            .then())
                    .then()
                    .block(WAIT);
        }
        assertThat(preconditions).containsExactly("55P03", "55P03", "55P03", "55P03", "55P03");
        assertThat(outcomes).containsExactly(409, 409, 409, 409, 409);
        assertThat(contextCalls).hasValue(0);
        assertThat(policyCalls).hasValue(0);
        assertThat(f.service.observe(f.currency).block(WAIT).headVersion()).isEqualTo(1L);
        assertThat(f.service.get(policy, f.currency).block(WAIT).lifecycle())
                .isEqualTo("PUBLISHED");
    }
}
