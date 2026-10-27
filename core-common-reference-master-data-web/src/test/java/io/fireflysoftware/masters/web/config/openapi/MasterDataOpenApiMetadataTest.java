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

    private static void verifySourceRoutes(OpenAPI api) {
        int legacy=api.getPaths().entrySet().stream().filter(entry->!entry.getKey().startsWith("/api/v1/insurance-references"))
                .mapToInt(entry->entry.getValue().readOperations().size()).sum();
        assertEquals(201,legacy);
        int declared=0;
        var expectedRoutes=new java.util.HashSet<String>();
        for(var controller:List.of(io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceReferenceController.class,
                io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceReferenceCurrentController.class,
                io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceSandboxFactController.class)) {
            var prefix=controller.getAnnotation(org.springframework.web.bind.annotation.RequestMapping.class).value()[0];
            for(var method:controller.getDeclaredMethods()) {
                var post=method.getAnnotation(org.springframework.web.bind.annotation.PostMapping.class);
                var get=method.getAnnotation(org.springframework.web.bind.annotation.GetMapping.class);
                if(post==null&&get==null)continue;
                String route=prefix+(post==null?get.value()[0]:post.value()[0]);
                var verb=post==null?PathItem.HttpMethod.GET:PathItem.HttpMethod.POST;
                var id=method.getAnnotation(io.swagger.v3.oas.annotations.Operation.class).operationId();
                var path=api.getPaths().computeIfAbsent(route,ignored->new PathItem());
                var existing=path.readOperationsMap().get(verb);
                if(existing==null)path.operation(verb,new Operation().operationId(id));else assertEquals(id,existing.getOperationId());
                expectedRoutes.add(verb+" "+route);declared++;
            }
        }
        assertEquals(11,declared);
        for(var entry:api.getPaths().entrySet())if(entry.getKey().startsWith("/api/v1/insurance-references"))
            for(var verb:entry.getValue().readOperationsMap().keySet())assertTrue(expectedRoutes.contains(verb+" "+entry.getKey()));
        new MasterDataOpenApiMetadataConfiguration().masterDataBearerOpenApiCustomizer().customise(api);
        assertEquals(SecurityScheme.Type.HTTP,api.getComponents().getSecuritySchemes().get("bearerAuth").getType());
        assertEquals("bearer",api.getComponents().getSecuritySchemes().get("bearerAuth").getScheme());
        int protectedOperations=0;
        for(var path:api.getPaths().values())for(var operation:path.readOperations()){
            var effective=operation.getSecurity()==null?api.getSecurity():operation.getSecurity();
            assertFalse(effective.isEmpty());assertTrue(effective.stream().allMatch(requirement->requirement.containsKey("bearerAuth")));protectedOperations++;
        }
        assertEquals(212,protectedOperations);
        var before=io.swagger.v3.core.util.Json.pretty(api);
        new MasterDataOpenApiMetadataConfiguration().masterDataBearerOpenApiCustomizer().customise(api);
        assertEquals(before,io.swagger.v3.core.util.Json.pretty(api));
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
