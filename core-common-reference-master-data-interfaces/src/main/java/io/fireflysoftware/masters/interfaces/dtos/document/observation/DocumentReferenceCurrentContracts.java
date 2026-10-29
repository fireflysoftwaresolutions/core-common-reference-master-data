/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.interfaces.dtos.document.observation;

import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.DocumentLayoutDefinition;

/** Exact owner references and caller intent; no product, financial or asset admission. */
public final class DocumentReferenceCurrentContracts {
    private DocumentReferenceCurrentContracts() {}
    @Schema(name="DocumentReferenceProductScope",additionalProperties=Schema.AdditionalPropertiesValue.FALSE,requiredProperties={"tenantId","rootId","productId","revisionId","publicationId","definitionDigest"})
    public record ProductScope(UUID tenantId,UUID rootId,UUID productId,UUID revisionId,UUID publicationId,String definitionDigest) {
        public ProductScope {for(UUID id:List.of(Objects.requireNonNull(tenantId),Objects.requireNonNull(rootId),Objects.requireNonNull(productId),Objects.requireNonNull(revisionId),Objects.requireNonNull(publicationId)))uuid(id);digest(definitionDigest);}
    }
    @Schema(name="DocumentReferenceExact",additionalProperties=Schema.AdditionalPropertiesValue.FALSE,requiredProperties={"owner","kind","id","versionFormat","versionText","digest"})
    public record ExactReference(@Schema(allowableValues={"core-common-reference-master-data"}) String owner,@Schema(allowableValues={"DOCUMENT_TEMPLATE","DOCUMENT_LAYOUT"}) String kind,UUID id,@Schema(allowableValues={"OPAQUE"}) String versionFormat,@Schema(maxLength=20,pattern="[A-Za-z0-9][A-Za-z0-9._-]{0,19}") String versionText,@Schema(pattern="[0-9a-f]{64}") String digest) {
        public ExactReference {if(!"core-common-reference-master-data".equals(owner)||!Set.of("DOCUMENT_TEMPLATE","DOCUMENT_LAYOUT").contains(kind)||!"OPAQUE".equals(versionFormat))throw invalid();uuid(id);if(versionText==null||!versionText.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,19}"))throw invalid();DocumentReferenceCurrentContracts.digest(digest);}
    }
    @Schema(name="DocumentReferenceCurrentTarget",additionalProperties=Schema.AdditionalPropertiesValue.FALSE,requiredProperties={"product","purpose","documentType","locale","template","layout"})
    public record Target(ProductScope product,@Schema(allowableValues={"DEPOSIT_ACCOUNT_ENROLLMENT"}) String purpose,@Schema(maxLength=100,pattern="[A-Z][A-Z0-9_]{0,99}") String documentType,@Schema(maxLength=50,pattern="[A-Za-z0-9][A-Za-z0-9_-]{0,49}") String locale,ExactReference template,ExactReference layout) {
        public Target {if(product==null||!"DEPOSIT_ACCOUNT_ENROLLMENT".equals(purpose)||documentType==null||!documentType.matches("[A-Z][A-Z0-9_]{0,99}")||locale==null||!locale.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,49}")||template==null||layout==null||!"DOCUMENT_TEMPLATE".equals(template.kind())||!"DOCUMENT_LAYOUT".equals(layout.kind())||template.id().equals(layout.id())||!template.versionText().equals(layout.versionText()))throw invalid();}
    }
    @Schema(name="DocumentReferenceCurrent",additionalProperties=Schema.AdditionalPropertiesValue.FALSE,requiredProperties={"target","definition","observedAtEpochMillis","effectiveUntilEpochMillis"})
    public record Current(Target target,DocumentLayoutDefinition definition,Long observedAtEpochMillis,Long effectiveUntilEpochMillis) {
        public Current {if(target==null||definition==null||observedAtEpochMillis==null||effectiveUntilEpochMillis==null||observedAtEpochMillis<0||effectiveUntilEpochMillis<=observedAtEpochMillis||effectiveUntilEpochMillis-observedAtEpochMillis>5000)throw invalid();}
    }
    private static void uuid(UUID value){if(value==null||value.equals(new UUID(0,0)))throw invalid();}
    private static void digest(String value){if(value==null||!value.matches("[0-9a-f]{64}"))throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("DOCUMENT_REFERENCE_CURRENT_TARGET_INVALID");}
}
