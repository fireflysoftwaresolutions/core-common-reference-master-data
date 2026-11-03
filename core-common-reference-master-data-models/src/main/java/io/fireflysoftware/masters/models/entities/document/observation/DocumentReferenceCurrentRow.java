/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.models.entities.document.observation;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(value="document_reference_current_observation",schema="public")
public record DocumentReferenceCurrentRow(@Id UUID layoutRevisionId,UUID sourceRevisionId,String version,String localeCode,String canonicalJson,String definitionDigest,String sourceDigest,String documentType){@Override public String toString(){return "DocumentReferenceCurrentRow";}}
