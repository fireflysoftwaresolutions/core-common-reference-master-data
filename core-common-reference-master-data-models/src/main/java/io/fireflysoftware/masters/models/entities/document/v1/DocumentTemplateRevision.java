package io.fireflysoftware.masters.models.entities.document.v1;

import java.time.Instant;
import java.util.UUID;

/** Stored publication snapshot. Mutation is limited by the database retention trigger. */
public record DocumentTemplateRevision(
        UUID revisionId, UUID templateId, UUID localizationId, UUID localeId,
        String templateCode, String version, String localeCode, String templateName,
        String mediaType, String templateContent, String inputSchema,
        String styleContent, String templateVariables, String sourceSnapshot,
        String contentDigest, String schemaDigest, String styleDigest, String sourceDigest,
        Instant publishedAt, String publishedBy, String lifecycle, Instant retiredAt, String retiredBy) {
}
