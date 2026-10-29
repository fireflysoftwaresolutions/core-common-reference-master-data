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

import io.fireflysoftware.common.reference.master.data.sdk.api.CurrencyExecutionPoliciesApi;
import io.fireflysoftware.common.reference.master.data.sdk.invoker.ApiClient;
import io.fireflysoftware.common.reference.master.data.sdk.model.*;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.sql.DriverManager;
import java.time.*;
import java.util.*;

@Testcontainers
class CurrencyPolicySdkRuntimeIT {
    @Container
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:17-alpine")
                    .withDatabaseName("synthetic_currency_http")
                    .withUsername("currency_owner")
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
    static final ObjectMapper JSON = ApiClient.createDefaultMapper(null);
    static final UUID CURRENCY = UUID.randomUUID();
    static final String TOKEN = UUID.randomUUID().toString() + UUID.randomUUID();
    static final Instant EXPIRY = Instant.now().plusSeconds(600);
    static final Path POLICY = Path.of("target/currency-fixture-policy.txt").toAbsolutePath();
    static Process app;
    static int port;

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
    static void start() throws Exception {
        owner(
                "CREATE ROLE firefly_currency_policy_guard NOLOGIN NOSUPERUSER NOBYPASSRLS; CREATE"
                        + " ROLE currency_http_app LOGIN PASSWORD 'synthetic-only' NOSUPERUSER"
                        + " NOBYPASSRLS");
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        owner(
                "REVOKE CREATE ON SCHEMA public FROM PUBLIC; GRANT USAGE ON SCHEMA public TO"
                    + " currency_http_app; GRANT SELECT,INSERT,UPDATE,DELETE ON ALL TABLES IN"
                    + " SCHEMA public TO currency_http_app; INSERT INTO"
                    + " currencies(currency_id,iso_code,currency_name,symbol,decimal_precision,is_major,status)"
                    + " VALUES('"
                        + CURRENCY
                        + "','USD','Synthetic US Dollar','$',2,false,'ACTIVE')");
        Files.writeString(POLICY, "PERMIT");
        boot();
    }

    @AfterAll
    static void stop() {
        if (app == null) return;
        app.destroy();
        try {
            if (!app.waitFor(10, java.util.concurrent.TimeUnit.SECONDS)) {
                app.destroyForcibly();
                assertThat(app.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    static void boot() throws Exception {
        boot(true);
    }

    static void boot(boolean projectPolicy) throws Exception {
        boot(projectPolicy, null);
    }

    static void boot(boolean projectPolicy, Boolean strictOverride) throws Exception {
        boot(projectPolicy, strictOverride, false);
    }

    static void boot(boolean projectPolicy, Boolean strictOverride, boolean frameworkFallback)
            throws Exception {
        try (var socket = new java.net.ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        var classes = Path.of("target/currency-bootstrap-classes").toAbsolutePath();
        for (String suffix : List.of(".class", "$Fixture.class")) {
            String resource =
                    "io/fireflysoftware/qualification/currency/CurrencyPolicyRuntimeBootstrap"
                            + suffix;
            var target = classes.resolve(resource);
            Files.createDirectories(target.getParent());
            try (var bytes =
                    CurrencyPolicySdkRuntimeIT.class
                            .getClassLoader()
                            .getResourceAsStream(resource)) {
                if (bytes == null)
                    throw new IllegalStateException("Private currency bootstrap missing");
                Files.copy(bytes, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        String loaderPath = classes.toString();
        if (frameworkFallback) {
            String resource =
                    "io/fireflysoftware/qualification/currency/CurrencyPolicyFrameworkFallbackConfiguration.class";
            var target = classes.resolve(resource);
            Files.createDirectories(target.getParent());
            try (var bytes =
                    CurrencyPolicySdkRuntimeIT.class
                            .getClassLoader()
                            .getResourceAsStream(resource)) {
                if (bytes == null)
                    throw new IllegalStateException("Private native factory fixture missing");
                Files.copy(bytes, target, StandardCopyOption.REPLACE_EXISTING);
            }
            var libraries = Path.of("target/currency-framework-fixture-libs").toAbsolutePath();
            Files.createDirectories(libraries);
            var prefixes =
                    List.of(
                            "fireflyframework-security-core-",
                            "fireflyframework-security-resource-server-",
                            "fireflyframework-security-webflux-",
                            "spring-security-config-",
                            "spring-security-web-",
                            "spring-security-oauth2-",
                            "nimbus-jose-jwt-",
                            "json-smart-",
                            "accessors-smart-",
                            "content-type-");
            int frameworkJars = 0;
            for (String entry :
                    System.getProperty(
                                    "surefire.test.class.path",
                                    System.getProperty("java.class.path"))
                            .split(java.io.File.pathSeparator)) {
                var library = Path.of(entry);
                String filename = library.getFileName().toString();
                if (Files.isRegularFile(library)
                        && filename.endsWith(".jar")
                        && prefixes.stream().anyMatch(filename::startsWith)) {
                    Files.copy(
                            library,
                            libraries.resolve(filename),
                            StandardCopyOption.REPLACE_EXISTING);
                    if (filename.startsWith("fireflyframework-security-")) frameworkJars++;
                }
            }
            if (frameworkJars != 3)
                throw new IllegalStateException(
                        "Exact core/resource-server/webflux private fixture libraries required");
            loaderPath += "," + libraries;
        }
        var jar =
                Path.of(
                                "../core-common-reference-master-data-web/target/core-common-reference-master-data.jar")
                        .toAbsolutePath()
                        .normalize();
        var args =
                new ArrayList<>(
                        List.of(
                                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                                "-Xmx384m",
                                "-Dloader.path=" + loaderPath,
                                "-Dloader.main=io.fireflysoftware.qualification.currency.CurrencyPolicyRuntimeBootstrap",
                                "-cp",
                                jar.toString(),
                                "org.springframework.boot.loader.launch.PropertiesLauncher",
                                "--server.address=127.0.0.1",
                                "--server.port=" + port,
                                "--DB_HOST=" + postgres.getHost(),
                                "--DB_PORT=" + postgres.getMappedPort(5432),
                                "--DB_NAME=" + postgres.getDatabaseName(),
                                "--DB_USERNAME=currency_http_app",
                                "--DB_PASSWORD=synthetic-only",
                                "--DB_SSL_MODE=disable",
                                "--spring.flyway.enabled=false",
                                "--firefly.eda.enabled=false",
                                "--firefly.stepevents.enabled=false"));
        if (!projectPolicy) args.add("--fixture.currency.policy.enabled=false");
        if (frameworkFallback)
            args.add(
                    "--spring.autoconfigure.exclude=org.fireflyframework.security.rs.ResourceServerAutoConfiguration");
        if (strictOverride != null)
            args.add("--firefly.security.resource-server.strict-policy=" + strictOverride);
        var builder =
                new ProcessBuilder(args)
                        .redirectErrorStream(true)
                        .redirectOutput(
                                Path.of("target/currency-native-" + port + ".log").toFile());
        builder.environment().put("CURRENCY_FIXTURE_CREDENTIAL", TOKEN);
        builder.environment().put("CURRENCY_FIXTURE_RESOURCE", CURRENCY.toString());
        builder.environment().put("CURRENCY_FIXTURE_EXPIRY", EXPIRY.toString());
        builder.environment().put("CURRENCY_FIXTURE_POLICY_FILE", POLICY.toString());
        if (frameworkFallback)
            builder.environment().put("CURRENCY_FIXTURE_FRAMEWORK_FALLBACK", "true");
        app = builder.start();
        for (int i = 0; i < 160; i++) {
            if (!app.isAlive()) throw new IllegalStateException("Private currency app exited");
            try {
                if (call("GET", "/v3/api-docs", null, null, null).statusCode() == 200) return;
            } catch (Exception ignored) {
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException("Private currency app startup timeout");
    }

    static CurrencyExecutionPoliciesApi api(String token) {
        var c = new ApiClient();
        c.setBasePath("http://127.0.0.1:" + port);
        c.addDefaultHeader("Authorization", "Bearer " + token);
        return new CurrencyExecutionPoliciesApi(c);
    }

    static CurrencyPolicyPublishRequest request() {
        var o = api(TOKEN).observeCurrencyPolicySource(CURRENCY.toString()).block(WAIT);
        return new CurrencyPolicyPublishRequest()
                .actionId(UUID.randomUUID().toString())
                .currencyId(CURRENCY.toString())
                .isoCode("USD")
                .expectedCurrencyDigest(o.getDigest())
                .expectedHeadVersion(o.getHeadVersion())
                .postedScale(2)
                .effectiveFromEpochMillis(-2208988800000L)
                .effectiveToEpochMillis(253402300799999L)
                .provenanceReference("Synthetic representation approval v1");
    }

    static HttpResponse<String> call(
            String method, String path, String body, String token, String key) throws Exception {
        var b =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .timeout(Duration.ofSeconds(5));
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (key != null) b.header("X-Idempotency-Key", key);
        if (body != null) b.header("Content-Type", "application/json");
        return HttpClient.newHttpClient()
                .send(
                        b.method(
                                        method,
                                        body == null
                                                ? HttpRequest.BodyPublishers.noBody()
                                                : HttpRequest.BodyPublishers.ofString(body))
                                .build(),
                        HttpResponse.BodyHandlers.ofString());
    }

    static void status(int code, Runnable work) {
        assertThatThrownBy(work::run)
                .isInstanceOfSatisfying(
                        WebClientResponseException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(code));
    }

    @Test
    void genuineFiveOperationsRetainExactSignedDatesAndRestartReceipt() throws Exception {
        Files.writeString(POLICY, "PERMIT");
        var r = request();
        var first = api(TOKEN).publishCurrencyExecutionPolicyWithHttpInfo(r).block(WAIT);
        assertThat(first.getStatusCode().value()).isEqualTo(201);
        var v = first.getBody();
        assertThat(v.getEffectiveFromEpochMillis()).isEqualTo(-2208988800000L);
        assertThat(v.getEffectiveToEpochMillis()).isEqualTo(253402300799999L);
        assertThat(v.getReference().getDigest()).matches("[0-9a-f]{64}");
        assertThat(
                        api(TOKEN)
                                .getCurrencyExecutionPolicyByAction(
                                        r.getActionId(), CURRENCY.toString())
                                .block(WAIT)
                                .getReference())
                .isEqualTo(v.getReference());
        stop();
        boot();
        assertThat(
                        api(TOKEN)
                                .publishCurrencyExecutionPolicyWithHttpInfo(r)
                                .block(WAIT)
                                .getStatusCode()
                                .value())
                .isEqualTo(200);
        var retired =
                api(TOKEN)
                        .retireCurrencyExecutionPolicy(
                                v.getReference().getPolicyId(),
                                new CurrencyPolicyRetireRequest()
                                        .actionId(UUID.randomUUID().toString())
                                        .currencyId(CURRENCY.toString())
                                        .expectedStateVersion(0L)
                                        .reason("Synthetic withdrawal"))
                        .block(WAIT);
        assertThat(retired.getLifecycle()).isEqualTo("RETIRED");
        assertThat(retired.getEffectiveNow()).isFalse();
        assertThat(
                        api(TOKEN)
                                .getCurrencyExecutionPolicy(
                                        v.getReference().getPolicyId(), CURRENCY.toString())
                                .block(WAIT)
                                .getCanonicalEncoding())
                .isEqualTo(v.getCanonicalEncoding());
        var openEnded = request().effectiveToEpochMillis(null);
        var omitted = JSON.valueToTree(openEnded);
        ((com.fasterxml.jackson.databind.node.ObjectNode) omitted).remove("effectiveToEpochMillis");
        assertThat(
                        call(
                                        "POST",
                                        "/api/v1/currency-execution-policies",
                                        JSON.writeValueAsString(omitted),
                                        TOKEN,
                                        null)
                                .statusCode())
                .isEqualTo(201);
        var openReplay =
                api(TOKEN).publishCurrencyExecutionPolicyWithHttpInfo(openEnded).block(WAIT);
        assertThat(openReplay.getStatusCode().value()).isEqualTo(200);
        assertThat(openReplay.getBody().getEffectiveToEpochMillis()).isNull();
        ((com.fasterxml.jackson.databind.node.ObjectNode) omitted)
                .putNull("effectiveToEpochMillis");
        assertThat(
                        call(
                                        "POST",
                                        "/api/v1/currency-execution-policies",
                                        JSON.writeValueAsString(omitted),
                                        TOKEN,
                                        null)
                                .statusCode())
                .isEqualTo(200);
    }

    @Test
    void sameBearerAndWarmTransportKeyCannotBypassCurrentPolicy() throws Exception {
        Files.writeString(POLICY, "PERMIT");
        var r = request();
        String body = JSON.writeValueAsString(r), key = UUID.randomUUID().toString();
        assertThat(
                        call("POST", "/api/v1/currency-execution-policies", body, TOKEN, key)
                                .statusCode())
                .isEqualTo(201);
        Files.writeString(POLICY, "DENY");
        try {
            assertThat(
                            call("POST", "/api/v1/currency-execution-policies", body, TOKEN, key)
                                    .statusCode())
                    .isEqualTo(403);
            status(
                    403,
                    () -> api(TOKEN).observeCurrencyPolicySource(CURRENCY.toString()).block(WAIT));
        } finally {
            Files.writeString(POLICY, "PERMIT");
        }
        assertThat(
                        call("POST", "/api/v1/currency-execution-policies", body, TOKEN, key)
                                .statusCode())
                .isEqualTo(200);
    }

    @Test
    void missingExpiryExpiredIdentityWrongResourceAndStrictUnknownInputsFailClosed()
            throws Exception {
        Files.writeString(POLICY, "PERMIT");
        status(
                403,
                () ->
                        api(TOKEN + "-missing-exp")
                                .observeCurrencyPolicySource(CURRENCY.toString())
                                .block(WAIT));
        status(
                403,
                () ->
                        api(TOKEN + "-expired")
                                .observeCurrencyPolicySource(CURRENCY.toString())
                                .block(WAIT));
        status(
                403,
                () ->
                        api(TOKEN)
                                .observeCurrencyPolicySource(UUID.randomUUID().toString())
                                .block(WAIT));
        var r = request();
        String invalid = JSON.writeValueAsString(r).replace("}", ",\"tenantId\":\"untrusted\"}");
        assertThat(
                        call("POST", "/api/v1/currency-execution-policies", invalid, TOKEN, null)
                                .statusCode())
                .isEqualTo(400);
        var spec = JSON.readTree(call("GET", "/v3/api-docs", null, null, null).body());
        assertThat(
                        spec.path("paths")
                                .has(
                                        "/api/v1/currency-execution-policies/currencies/{currencyId}/source-observation"))
                .isTrue();
        assertThat(
                        spec.path("components")
                                .path("schemas")
                                .path("CurrencyPolicyPublishRequest")
                                .path("properties")
                                .has("principal"))
                .isFalse();
    }

    @Test
    void actualOpenApiRequiresExactInputsAndRepresentsOpenEndedEffectivity() throws Exception {
        var spec = JSON.readTree(call("GET", "/v3/api-docs", null, null, null).body());
        Files.writeString(Path.of("target/currency-actual-openapi.json"), spec.toPrettyString());
        var schemas = spec.path("components").path("schemas");
        var publish = schemas.path("CurrencyPolicyPublishRequest");
        var required = new ArrayList<String>();
        publish.path("required").forEach(value -> required.add(value.asText()));
        assertThat(required)
                .containsExactlyInAnyOrder(
                        "actionId",
                        "currencyId",
                        "isoCode",
                        "expectedCurrencyDigest",
                        "expectedHeadVersion",
                        "postedScale",
                        "effectiveFromEpochMillis",
                        "provenanceReference");
        for (String model : List.of("CurrencyPolicyPublishRequest", "CurrencyPolicyView")) {
            var end = schemas.path(model).path("properties").path("effectiveToEpochMillis");
            assertThat(end.path("format").asText()).isEqualTo("int64");
            if (spec.path("openapi").asText().startsWith("3.1.")) {
                var types = new ArrayList<String>();
                end.path("type").forEach(value -> types.add(value.asText()));
                assertThat(types).containsExactlyInAnyOrder("integer", "null");
                assertThat(end.has("nullable")).isFalse();
            } else {
                assertThat(spec.path("openapi").asText()).startsWith("3.0.");
                assertThat(end.path("type").asText()).isEqualTo("integer");
                assertThat(end.path("nullable").asBoolean()).isTrue();
            }
        }
        var retireRequired = new ArrayList<String>();
        schemas.path("CurrencyPolicyRetireRequest")
                .path("required")
                .forEach(value -> retireRequired.add(value.asText()));
        assertThat(retireRequired)
                .containsExactlyInAnyOrder(
                        "actionId", "currencyId", "expectedStateVersion", "reason");
    }

    @Test
    void absentProjectPolicyCannotGrantNativeCurrencyPermission() throws Exception {
        Files.writeString(POLICY, "PERMIT");
        stop();
        try {
            boot(false);
            status(
                    403,
                    () -> api(TOKEN).observeCurrencyPolicySource(CURRENCY.toString()).block(WAIT));
        } finally {
            stop();
            boot(true);
        }
    }

    @Test
    void strictModeCannotBeDisabledEvenWithPermittingProjectPolicy() throws Exception {
        Files.writeString(POLICY, "PERMIT");
        stop();
        try {
            boot(true, false);
            status(
                    403,
                    () -> api(TOKEN).observeCurrencyPolicySource(CURRENCY.toString()).block(WAIT));
        } finally {
            stop();
            boot(true);
        }
    }

    @Test
    void nativeFrameworkEmptyRuleFallbackCannotGrantCurrencyPermission() throws Exception {
        Files.writeString(POLICY, "PERMIT");
        var original = request();
        var publication = api(TOKEN).publishCurrencyExecutionPolicy(original).block(WAIT);
        var next = request();
        stop();
        try {
            boot(false, null, true);
            status(
                    403,
                    () -> api(TOKEN).observeCurrencyPolicySource(CURRENCY.toString()).block(WAIT));
            status(403, () -> api(TOKEN).publishCurrencyExecutionPolicy(original).block(WAIT));
            status(403, () -> api(TOKEN).publishCurrencyExecutionPolicy(next).block(WAIT));
            status(
                    403,
                    () ->
                            api(TOKEN)
                                    .getCurrencyExecutionPolicy(
                                            publication.getReference().getPolicyId(),
                                            CURRENCY.toString())
                                    .block(WAIT));
            status(
                    403,
                    () ->
                            api(TOKEN)
                                    .retireCurrencyExecutionPolicy(
                                            publication.getReference().getPolicyId(),
                                            new CurrencyPolicyRetireRequest()
                                                    .actionId(UUID.randomUUID().toString())
                                                    .currencyId(CURRENCY.toString())
                                                    .expectedStateVersion(0L)
                                                    .reason("Must be denied"))
                                    .block(WAIT));
        } finally {
            stop();
            boot(true);
        }
        assertThat(
                        api(TOKEN)
                                .getCurrencyExecutionPolicy(
                                        publication.getReference().getPolicyId(),
                                        CURRENCY.toString())
                                .block(WAIT)
                                .getLifecycle())
                .isEqualTo("PUBLISHED");
        status(
                404,
                () ->
                        api(TOKEN)
                                .getCurrencyExecutionPolicyByAction(
                                        next.getActionId(), CURRENCY.toString())
                                .block(WAIT));
    }

    @Test
    void invalidUnicodeFailsBeforeEffectsWhileScalarPairsRetainExactReplay() throws Exception {
        Files.writeString(POLICY, "PERMIT");
        for (String escaped : List.of("\\uD800", "\\uDC00", "\\uD800x", "\\uDC00\\uD800")) {
            var request = request().provenanceReference("UNICODE_SENTINEL");
            String body =
                    JSON.writeValueAsString(request)
                            .replace("UNICODE_SENTINEL", "unpaired-" + escaped);
            assertThat(
                            call("POST", "/api/v1/currency-execution-policies", body, TOKEN, null)
                                    .statusCode())
                    .isEqualTo(400);
            status(
                    404,
                    () ->
                            api(TOKEN)
                                    .getCurrencyExecutionPolicyByAction(
                                            request.getActionId(), CURRENCY.toString())
                                    .block(WAIT));
        }
        String valid = "ordinary Ω \uD83D\uDE42";
        var request = request().provenanceReference(valid);
        var publication =
                api(TOKEN).publishCurrencyExecutionPolicyWithHttpInfo(request).block(WAIT);
        assertThat(publication.getStatusCode().value()).isEqualTo(201);
        assertThat(publication.getBody().getProvenanceReference()).isEqualTo(valid);
        assertThat(publication.getBody().getCanonicalEncoding()).contains(valid);
        assertThat(
                        api(TOKEN)
                                .publishCurrencyExecutionPolicyWithHttpInfo(request)
                                .block(WAIT)
                                .getStatusCode()
                                .value())
                .isEqualTo(200);
        for (String escaped : List.of("\\uD800", "\\uDC00")) {
            String body =
                    "{\"actionId\":\""
                            + UUID.randomUUID()
                            + "\",\"currencyId\":\""
                            + CURRENCY
                            + "\",\"expectedStateVersion\":0,\"reason\":\"unpaired-"
                            + escaped
                            + "\"}";
            assertThat(
                            call(
                                            "POST",
                                            "/api/v1/currency-execution-policies/"
                                                    + publication
                                                            .getBody()
                                                            .getReference()
                                                            .getPolicyId()
                                                    + "/retire",
                                            body,
                                            TOKEN,
                                            null)
                                    .statusCode())
                    .isEqualTo(400);
        }
        assertThat(
                        api(TOKEN)
                                .getCurrencyExecutionPolicy(
                                        publication.getBody().getReference().getPolicyId(),
                                        CURRENCY.toString())
                                .block(WAIT)
                                .getLifecycle())
                .isEqualTo("PUBLISHED");
    }
}
