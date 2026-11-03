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


package io.fireflysoftware.masters.models.entities.document.v1;

import io.fireflysoftware.masters.interfaces.enums.commons.v1.StatusEnum;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a document template catalog record.
 * This defines the templates that can be used for document generation.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table("document_template_catalog")
public class DocumentTemplateCatalog {

    @Id
    @Column("template_id")
    private UUID templateId;

    @Column("template_code")
    private String templateCode;

    @Column("type_id")
    private UUID typeId;

    @Column("category")
    private String category;

    @Column("description")
    private String description;

    @Column("template_name")
    private String templateName;

    @Column("template_content")
    private String templateContent;

    @Column("template_variables")
    private Json templateVariables;

    @Column("input_schema")
    private Json inputSchema;

    @Column("style_content")
    private String styleContent;

    @Column("media_type")
    private String mediaType;

    @Column("version")
    private String version;

    @Column("status")
    private StatusEnum status;

    @Column("date_created")
    private LocalDateTime dateCreated;

    @Column("date_updated")
    private LocalDateTime dateUpdated;
}
