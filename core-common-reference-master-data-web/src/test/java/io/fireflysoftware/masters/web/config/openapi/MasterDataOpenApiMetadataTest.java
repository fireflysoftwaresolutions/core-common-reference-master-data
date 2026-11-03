package io.fireflysoftware.masters.web.config.openapi;

import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCommands.SubmitPublication;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.junit.jupiter.api.Test;

class MasterDataOpenApiMetadataTest {
    @Test void anAdditionalOriginalOperationRetainsItsMetadataWhenSourceRoutesAreCompleted() {
        var original = new Operation().operationId("additionalOriginalOperation")
                .summary("Retained original metadata").addTagsItem("original");
        var api = new OpenAPI().paths(new Paths().addPathItem("/original-added-route", new PathItem().get(original)));
        verifySourceRoutes(api);
        assertSame(original, api.getPaths().get("/original-added-route").getGet());
    }

    @Test void globalBearerCoversEveryOriginalAndSourceDeclaredCommandOperation() throws Exception {
        var explicitSourceRoot = System.getProperty("source.root");
        java.nio.file.Path ownerRoot;
        if (explicitSourceRoot != null) {
            ownerRoot = java.nio.file.Path.of(explicitSourceRoot);
        } else {
            var basedir = System.getProperty("basedir");
            assertNotNull(basedir, "Maven Web module basedir or explicit source.root is required");
            var webRoot = java.nio.file.Path.of(basedir);
            assertEquals("core-common-reference-master-data-web", webRoot.getFileName().toString());
            ownerRoot = webRoot.getParent();
        }
        var resource = java.nio.file.Files.newInputStream(ownerRoot.resolve(
                "core-common-reference-master-data-sdk/src/main/resources/openapi.yml"));
        OpenAPI api;
        try (resource) { api = io.swagger.v3.core.util.Yaml.mapper().readValue(resource, OpenAPI.class); }
        var currentPath="/api/v1/insurance-references/{kind}/{recordId}/publications/{publicationId}/revisions/{revision}/current";
        var withCurrent=io.swagger.v3.core.util.Json.mapper().convertValue(api,OpenAPI.class);
        withCurrent.getPaths().computeIfAbsent(currentPath,ignored->new PathItem()).get(new Operation().operationId("observeCurrentInsuranceReference"));
        verifySourceRoutes(withCurrent);
        verifySourceRoutes(api);
        verifySourceRoutes(api);
        var withoutInsurance=new OpenAPI().paths(new Paths());
        api.getPaths().forEach((path,value)->{if(!path.startsWith("/api/v1/insurance-references"))withoutInsurance.getPaths().addPathItem(path,value);});
        verifySourceRoutes(withoutInsurance);
        var currentOnly=new OpenAPI().paths(new Paths());
        withoutInsurance.getPaths().forEach((path,value)->{if(!path.startsWith("/api/v1/insurance-references"))currentOnly.getPaths().addPathItem(path,value);});
        var current="/api/v1/insurance-references/{kind}/{recordId}/publications/{publicationId}/revisions/{revision}/current";
        currentOnly.getPaths().addPathItem(current,new PathItem().get(new Operation().operationId("observeCurrentInsuranceReference")));
        verifySourceRoutes(currentOnly);
    }

    private record DeclaredRoute(PathItem.HttpMethod verb, String path, String operationId) {}

    private static java.util.Map<String, DeclaredRoute> declaredRoutes() {
        var routes = new java.util.LinkedHashMap<String, DeclaredRoute>();
        for (var controller : List.of(
                io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceReferenceController.class,
                io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceReferenceCurrentController.class,
                io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceSandboxFactController.class,
                io.fireflysoftware.masters.web.controllers.document.observation.DocumentReferenceCurrentController.class)) {
            var root = org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation(
                    controller, org.springframework.web.bind.annotation.RequestMapping.class);
            assertNotNull(root);
            var prefixes = root.path().length == 0 ? new String[]{""} : root.path();
            for (var method : controller.getDeclaredMethods()) {
                var mapping = org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation(
                        method, org.springframework.web.bind.annotation.RequestMapping.class);
                if (mapping == null) continue;
                var operation = method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class);
                assertNotNull(operation);
                assertFalse(operation.operationId().isBlank());
                assertTrue(mapping.method().length > 0);
                var suffixes = mapping.path().length == 0 ? new String[]{""} : mapping.path();
                for (var prefix : prefixes) for (var suffix : suffixes) for (var requestMethod : mapping.method()) {
                    var verb = PathItem.HttpMethod.valueOf(requestMethod.name());
                    var route = new DeclaredRoute(verb, prefix + suffix, operation.operationId());
                    assertNull(routes.put(verb + " " + route.path(), route));
                }
            }
        }
        var documentCurrent = routes.get("POST /api/v1/document-reference-current-observations");
        assertNotNull(documentCurrent);
        assertEquals("observeCurrentDocumentReferences", documentCurrent.operationId());
        return routes;
    }

    private static java.util.Map<String, Operation> operations(OpenAPI api) {
        var result = new java.util.LinkedHashMap<String, Operation>();
        api.getPaths().forEach((path, item) -> item.readOperationsMap()
                .forEach((verb, operation) -> result.put(verb + " " + path, operation)));
        return result;
    }

    private static com.fasterxml.jackson.databind.JsonNode metadataWithoutSecurity(Operation operation) {
        var metadata = (com.fasterxml.jackson.databind.node.ObjectNode)
                io.swagger.v3.core.util.Json.mapper().valueToTree(operation);
        metadata.remove("security");
        return metadata;
    }

    private static void verifySourceRoutes(OpenAPI api) {
        var baseline = new java.util.LinkedHashMap<String, com.fasterxml.jackson.databind.JsonNode>();
        operations(api).forEach((route, operation) -> baseline.put(route, metadataWithoutSecurity(operation)));
        var declared = declaredRoutes();
        for (var route : declared.values()) {
            var path = api.getPaths().computeIfAbsent(route.path(), ignored -> new PathItem());
            var existing = path.readOperationsMap().get(route.verb());
            if (existing == null) path.operation(route.verb(), new Operation().operationId(route.operationId()));
            else assertEquals(route.operationId(), existing.getOperationId());
        }
        var expected = new java.util.HashSet<>(baseline.keySet());
        expected.addAll(declared.keySet());
        new MasterDataOpenApiMetadataConfiguration().masterDataBearerOpenApiCustomizer().customise(api);
        var actual = operations(api);
        assertEquals(expected, actual.keySet());
        baseline.forEach((route, metadata) -> assertEquals(metadata, metadataWithoutSecurity(actual.get(route)), route));
        declared.forEach((route, declaration) -> assertEquals(declaration.operationId(), actual.get(route).getOperationId(), route));
        assertEquals(SecurityScheme.Type.HTTP, api.getComponents().getSecuritySchemes().get("bearerAuth").getType());
        assertEquals("bearer", api.getComponents().getSecuritySchemes().get("bearerAuth").getScheme());
        assertEquals("JWT", api.getComponents().getSecuritySchemes().get("bearerAuth").getBearerFormat());
        for (var operation : actual.values()) {
            var effective = operation.getSecurity() == null ? api.getSecurity() : operation.getSecurity();
            assertNotNull(effective);
            assertFalse(effective.isEmpty());
            assertTrue(effective.stream().allMatch(requirement -> requirement.containsKey("bearerAuth")));
        }
        var before = io.swagger.v3.core.util.Json.pretty(api);
        new MasterDataOpenApiMetadataConfiguration().masterDataBearerOpenApiCustomizer().customise(api);
        assertEquals(before, io.swagger.v3.core.util.Json.pretty(api));
    }

    @Test void existingOperationOverridesCannotLeaveAnAnonymousOrAlternativeAuthBranch() {
        var operation = new Operation().security(List.of(new SecurityRequirement(), new SecurityRequirement().addList("operatorAuth", "scope")));
        var api = new OpenAPI().paths(new Paths().addPathItem("/legacy", new PathItem().get(operation)));
        var customizer = new MasterDataOpenApiMetadataConfiguration().masterDataBearerOpenApiCustomizer();
        customizer.customise(api);
        assertEquals(2, operation.getSecurity().size());
        assertTrue(operation.getSecurity().stream().allMatch(value -> value.containsKey("bearerAuth")));
        assertEquals(List.of("scope"), operation.getSecurity().get(1).get("operatorAuth"));
        var before = io.swagger.v3.core.util.Json.pretty(api);
        customizer.customise(api);
        assertEquals(before, io.swagger.v3.core.util.Json.pretty(api));
    }

    @Test void conflictingBearerDefinitionIsRefusedRatherThanOverwritten() {
        var api = new OpenAPI().components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme().type(SecurityScheme.Type.APIKEY)));
        var error = assertThrows(IllegalStateException.class, () -> new MasterDataOpenApiMetadataConfiguration()
                .masterDataBearerOpenApiCustomizer().customise(api));
        assertEquals("MASTER_DATA_OPENAPI_BEARER_CONFLICT", error.getMessage());
    }

    @Test void theRealTwoInstantFieldsReferenceTheSharedScalarContract() {
        var schemas = new ModelConverters().readAll(SubmitPublication.class);
        var draft = schemas.get("PublicationDraft");
        for (var name : List.of("effectiveFrom", "effectiveUntil")) {
            var property = (io.swagger.v3.oas.models.media.Schema<?>) draft.getProperties().get(name);
            assertEquals("#/components/schemas/FireflyInstant", property.get$ref());
        }
    }

    @Test void actualSharedCustomizerRegistersTheExactScalarReferencedByTheRealDraft() {
        var schemas = new ModelConverters().readAll(SubmitPublication.class);
        var api = new OpenAPI().components(new Components().schemas(new java.util.LinkedHashMap<>(schemas)));
        new org.fireflyframework.web.openapi.InstantScalarOpenApiCustomizer().customise(api);
        var scalar = api.getComponents().getSchemas().get("FireflyInstant");
        assertNotNull(scalar);
        assertEquals("string", scalar.getType());
        assertEquals("date-time", scalar.getFormat());
        assertNull(scalar.getProperties());
    }
}
