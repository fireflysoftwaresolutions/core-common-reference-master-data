/*
 * Copyright 2025 Firefly Software Foundation
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


package io.fireflysoftware.masters.core.mappers.document.v1;

import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplateCatalogDTO;
import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplateTypeCatalogDTO;
import io.fireflysoftware.masters.models.entities.document.v1.DocumentTemplateCatalog;
import io.fireflysoftware.masters.models.entities.document.v1.DocumentTemplateTypeCatalog;
import io.r2dbc.postgresql.codec.Json;
import io.fireflysoftware.masters.core.services.document.v1.DocumentTemplateCanonicalJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.UUID;

/**
 * Mapper for converting between DocumentTemplateCatalog entities and DTOs.
 */
@Mapper(componentModel = "spring", imports = {Json.class, DocumentTemplateCanonicalJson.class})
public abstract class DocumentTemplateCatalogMapper {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Convert a DocumentTemplateCatalog entity to a DTO.
     *
     * @param entity the entity to convert
     * @return the converted DTO
     */
    @Mapping(target = "inputSchemaJson", expression = "java(entity.getInputSchema() == null ? null : DocumentTemplateCanonicalJson.canonicalize(entity.getInputSchema().asString()))")
    @Mapping(source = "inputSchema", target = "inputSchema", qualifiedByName = "templateVariablesToMap")
    @Mapping(source = "templateVariables", target = "templateVariables", qualifiedByName = "templateVariablesToMap")
    @Mapping(source = "typeId", target = "templateType", qualifiedByName = "typeIdToTemplateType")
    public abstract DocumentTemplateCatalogDTO toDTO(DocumentTemplateCatalog entity);

    /**
     * Convert a DocumentTemplateCatalogDTO to an entity.
     *
     * @param dto the DTO to convert
     * @return the converted entity
     */
    @Mapping(target = "inputSchema", expression = "java(dto.getInputSchemaJson() != null ? Json.of(dto.getInputSchemaJson()) : mapToTemplateVariables(dto.getInputSchema()))")
    @Mapping(source = "templateVariables", target = "templateVariables", qualifiedByName = "mapToTemplateVariables")
    @Mapping(target = "typeId", expression = "java(dto.getTypeId() != null ? dto.getTypeId() : templateTypeToTypeId(dto.getTemplateType()))")
    public abstract DocumentTemplateCatalog toEntity(DocumentTemplateCatalogDTO dto);

    /**
     * Convert a JSON string to a Map.
     *
     * @param templateVariables the JSON string to convert
     * @return the converted Map
     */
    @Named("templateVariablesToMap")
    protected Map<String, Object> templateVariablesToMap(Json templateVariables) {
        if (templateVariables == null) {
            return null;
        }
        try {
            return objectMapper.readValue(templateVariables.asString(), Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting template variables JSON to Map", e);
        }
    }

    /**
     * Convert a Map to a JSON string.
     *
     * @param templateVariables the Map to convert
     * @return the converted JSON string
     */
    @Named("mapToTemplateVariables")
    protected Json mapToTemplateVariables(Map<String, Object> templateVariables) {
        if (templateVariables == null) {
            return null;
        }
        try {
            return Json.of(objectMapper.writeValueAsString(templateVariables));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error converting template variables Map to JSON", e);
        }
    }

    /**
     * Convert a type ID to a DocumentTemplateTypeCatalogDTO.
     *
     * @param typeId the type ID to convert
     * @return the converted DocumentTemplateTypeCatalogDTO
     */
    @Named("typeIdToTemplateType")
    protected DocumentTemplateTypeCatalogDTO typeIdToTemplateType(UUID typeId) {
        if (typeId == null) {
            return null;
        }
        return DocumentTemplateTypeCatalogDTO.builder().typeId(typeId).build();
    }

    /**
     * Convert a DocumentTemplateTypeCatalogDTO to a type ID.
     *
     * @param templateType the DocumentTemplateTypeCatalogDTO to convert
     * @return the converted type ID
     */
    @Named("templateTypeToTypeId")
    protected UUID templateTypeToTypeId(DocumentTemplateTypeCatalogDTO templateType) {
        if (templateType == null) {
            return null;
        }
        return templateType.getTypeId();
    }
}
