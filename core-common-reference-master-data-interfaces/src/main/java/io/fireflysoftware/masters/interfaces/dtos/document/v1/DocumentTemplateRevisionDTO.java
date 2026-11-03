package io.fireflysoftware.masters.interfaces.dtos.document.v1;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** An immutable published snapshot; retired revisions remain available for historical readback. */
public record DocumentTemplateRevisionDTO(
        UUID revisionId, UUID templateId, UUID localizationId, UUID localeId,
        String templateCode, String version, String localeCode, String templateName,
        String mediaType, String templateContent, Map<String, Object> inputSchema,
        String inputSchemaCanonicalJson, String styleContent, Map<String, Object> templateVariables, Map<String, Object> sourceSnapshot,
        String sourceSnapshotCanonicalJson, String canonicalizationVersion,
        String contentDigest, String schemaDigest, String styleDigest, String sourceDigest,
        Instant publishedAt, String publishedBy, String lifecycle, Instant retiredAt, String retiredBy) {
}
