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
        int original = api.getPaths().values().stream().mapToInt(path -> path.readOperations().size()).sum();
        var controller = io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceReferenceController.class;
        var prefix = controller.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class).value()[0];
        int commands = 0;
        int existingCommands = 0;
        for (var method : controller.getDeclaredMethods()) {
            var mapping = method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
            if (mapping == null) continue;
            var path = api.getPaths().computeIfAbsent(prefix + mapping.value()[0], ignored -> new PathItem());
            if (path.getPost() == null) path.post(new Operation());
            else existingCommands++;
            commands++;
        }
        assertEquals(6, commands);
        assertEquals(201, original - existingCommands);
        new MasterDataOpenApiMetadataConfiguration().masterDataBearerOpenApiCustomizer().customise(api);
        assertEquals(SecurityScheme.Type.HTTP, api.getComponents().getSecuritySchemes().get("bearerAuth").getType());
        assertEquals("bearer", api.getComponents().getSecuritySchemes().get("bearerAuth").getScheme());
        int protectedOperations = 0;
        for (var path : api.getPaths().values()) for (var operation : path.readOperations()) {
            var effective = operation.getSecurity() == null ? api.getSecurity() : operation.getSecurity();
            assertFalse(effective.isEmpty());
            assertTrue(effective.stream().allMatch(requirement -> requirement.containsKey("bearerAuth")));
            protectedOperations++;
        }
        assertEquals(207, protectedOperations);
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
