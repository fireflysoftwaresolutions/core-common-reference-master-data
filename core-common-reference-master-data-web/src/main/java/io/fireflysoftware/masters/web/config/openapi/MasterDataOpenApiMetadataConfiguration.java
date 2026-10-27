/* Copyright 2026 Firefly Software Foundation. Licensed under Apache License, Version 2.0. */
package io.fireflysoftware.masters.web.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.ArrayList;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documents the owner's global bearer boundary without creating security or business beans. */
@Configuration(proxyBeanMethods = false)
public class MasterDataOpenApiMetadataConfiguration {
    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenApiCustomizer masterDataBearerOpenApiCustomizer() {
        return api -> {
            if (api.getComponents() == null) api.setComponents(new Components());
            var schemes = api.getComponents().getSecuritySchemes();
            var existing = schemes == null ? null : schemes.get(BEARER);
            if (existing != null && (existing.getType() != SecurityScheme.Type.HTTP
                    || !"bearer".equals(existing.getScheme()) || existing.get$ref() != null
                    || existing.getBearerFormat() != null && !"JWT".equals(existing.getBearerFormat())))
                throw new IllegalStateException("MASTER_DATA_OPENAPI_BEARER_CONFLICT");
            if (existing == null) {
                api.getComponents().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"));
            } else {
                existing.setBearerFormat("JWT");
            }
            api.setSecurity(requireBearer(api.getSecurity()));
            if (api.getPaths() != null) api.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                if (operation.getSecurity() != null) operation.setSecurity(requireBearer(operation.getSecurity()));
            }));
        };
    }

    private static List<SecurityRequirement> requireBearer(List<SecurityRequirement> original) {
        if (original == null || original.isEmpty()) return List.of(new SecurityRequirement().addList(BEARER));
        var result = new ArrayList<SecurityRequirement>();
        for (var requirement : original) {
            var protectedRequirement = new SecurityRequirement();
            protectedRequirement.putAll(requirement);
            protectedRequirement.put(BEARER, List.of());
            result.add(protectedRequirement);
        }
        return result;
    }
}
