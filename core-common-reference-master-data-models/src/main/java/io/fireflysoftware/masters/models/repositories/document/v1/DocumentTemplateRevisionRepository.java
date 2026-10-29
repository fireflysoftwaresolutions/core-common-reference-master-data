package io.fireflysoftware.masters.models.repositories.document.v1;

import io.fireflysoftware.masters.models.entities.document.v1.DocumentTemplateRevision;
import io.r2dbc.spi.Row;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Repository
public class DocumentTemplateRevisionRepository {
    private final DatabaseClient database;

    public DocumentTemplateRevisionRepository(DatabaseClient database) {
        this.database = database;
    }

    /** The catalog lock serializes publication intents for all versions and locales of one identity. */
    public Mono<String> observeSource(UUID templateId, String localeCode) {
        return source(templateId, localeCode, false);
    }

    public Mono<String> lockSource(UUID templateId, String localeCode) {
        return source(templateId, localeCode, true);
    }

    private Mono<String> source(UUID templateId, String localeCode, boolean lock) {
        return database.sql("""
                SELECT jsonb_build_object(
                    'canonicalizationVersion', 'MASTERS_JSON_V1', 'templateId', c.template_id, 'localizationId', l.localization_id, 'localeId', loc.locale_id,
                    'templateCode', c.template_code, 'version', c.version, 'localeCode', loc.locale_code,
                    'templateName', l.template_name, 'mediaType', c.media_type, 'templateContent', l.template_content,
                    'inputSchema', c.input_schema, 'styleContent', coalesce(c.style_content, ''),
                    'templateVariables', coalesce(c.template_variables, '{}'::jsonb),
                    'catalogName', c.template_name, 'catalogContent', c.template_content,
                    'category', c.category, 'description', c.description, 'typeId', t.type_id,
                    'typeCode', t.type_code, 'catalogStatus', c.status, 'localizationStatus', l.status,
                    'localeStatus', loc.status, 'typeStatus', t.status) ::text AS source
                FROM document_template_catalog c
                JOIN document_template_localization l ON l.template_id=c.template_id
                JOIN language_locale loc ON loc.locale_id=l.locale_id
                JOIN document_template_type_catalog t ON t.type_id=c.type_id
                WHERE c.template_id=:templateId AND loc.locale_code=:localeCode
                """ + (lock ? " FOR UPDATE OF c,l,loc,t" : "")).bind("templateId", templateId).bind("localeCode", localeCode)
                .map((row, metadata) -> row.get("source", String.class)).one();
    }

    public Mono<DocumentTemplateRevision> findById(UUID revisionId) {
        return database.sql("SELECT " + columns() + " FROM document_template_revision WHERE revision_id=:id")
                .bind("id", revisionId).map((row, metadata) -> map(row)).one();
    }

    public Mono<DocumentTemplateRevision> findExact(String templateCode, String version, String localeCode) {
        return database.sql("SELECT " + columns() + " FROM document_template_revision WHERE template_code=:code AND version=:version AND locale_code=:locale")
                .bind("code", templateCode).bind("version", version).bind("locale", localeCode)
                .map((row, metadata) -> map(row)).one();
    }

    public Mono<DocumentTemplateRevision> insert(DocumentTemplateRevision revision) {
        return database.sql("""
                INSERT INTO document_template_revision(revision_id,template_id,localization_id,locale_id,
                    template_code,version,locale_code,template_name,media_type,template_content,input_schema,
                    style_content,template_variables,source_snapshot,content_digest,schema_digest,style_digest,
                    source_digest,published_at,published_by,lifecycle)
                VALUES (:id,:templateId,:localizationId,:localeId,:code,:version,:locale,:name,:media,:content,
                    CAST(:schema AS jsonb),:style,CAST(:variables AS jsonb),CAST(:source AS jsonb),
                    :contentDigest,:schemaDigest,:styleDigest,:sourceDigest,:publishedAt,:actor,'PUBLISHED')
                """)
                .bind("id", revision.revisionId()).bind("templateId", revision.templateId())
                .bind("localizationId", revision.localizationId()).bind("localeId", revision.localeId())
                .bind("code", revision.templateCode()).bind("version", revision.version()).bind("locale", revision.localeCode())
                .bind("name", revision.templateName()).bind("media", revision.mediaType()).bind("content", revision.templateContent())
                .bind("schema", revision.inputSchema()).bind("style", revision.styleContent())
                .bind("variables", revision.templateVariables()).bind("source", revision.sourceSnapshot())
                .bind("contentDigest", revision.contentDigest()).bind("schemaDigest", revision.schemaDigest())
                .bind("styleDigest", revision.styleDigest()).bind("sourceDigest", revision.sourceDigest())
                .bind("publishedAt", OffsetDateTime.ofInstant(revision.publishedAt(), ZoneOffset.UTC)).bind("actor", revision.publishedBy())
                .fetch().rowsUpdated().then(findById(revision.revisionId()));
    }

    public Mono<DocumentTemplateRevision> retire(UUID revisionId, String actor) {
        return database.sql("""
                UPDATE document_template_revision SET lifecycle='RETIRED',retired_at=CURRENT_TIMESTAMP,retired_by=:actor
                WHERE revision_id=:id AND lifecycle='PUBLISHED'
                """).bind("id", revisionId).bind("actor", actor).fetch().rowsUpdated().then(findById(revisionId));
    }

    public Mono<Boolean> isUsable(UUID revisionId) {
        return database.sql("""
                SELECT EXISTS(SELECT 1 FROM document_template_revision r
                JOIN document_template_catalog c ON c.template_id=r.template_id AND c.template_code=r.template_code
                JOIN document_template_localization l ON l.localization_id=r.localization_id AND l.template_id=r.template_id AND l.locale_id=r.locale_id
                JOIN language_locale loc ON loc.locale_id=r.locale_id AND loc.locale_code=r.locale_code
                JOIN document_template_type_catalog t ON t.type_id=c.type_id
                WHERE r.revision_id=:id AND r.lifecycle='PUBLISHED' AND c.status='ACTIVE'
                    AND l.status='ACTIVE' AND loc.status='ACTIVE' AND t.status='ACTIVE') AS usable
                """).bind("id", revisionId).map((row, metadata) -> row.get("usable", Boolean.class)).one();
    }

    private static String columns() {
        return "revision_id,template_id,localization_id,locale_id,template_code,version,locale_code,template_name,"
                + "media_type,template_content,input_schema::text,style_content,template_variables::text,source_snapshot::text,"
                + "content_digest,schema_digest,style_digest,source_digest,published_at,published_by,lifecycle,retired_at,retired_by";
    }

    private static DocumentTemplateRevision map(Row row) {
        var retiredAt = row.get("retired_at", OffsetDateTime.class);
        return new DocumentTemplateRevision(row.get("revision_id", UUID.class), row.get("template_id", UUID.class),
                row.get("localization_id", UUID.class), row.get("locale_id", UUID.class), row.get("template_code", String.class),
                row.get("version", String.class), row.get("locale_code", String.class), row.get("template_name", String.class),
                row.get("media_type", String.class), row.get("template_content", String.class), row.get("input_schema", String.class),
                row.get("style_content", String.class), row.get("template_variables", String.class), row.get("source_snapshot", String.class),
                row.get("content_digest", String.class), row.get("schema_digest", String.class), row.get("style_digest", String.class),
                row.get("source_digest", String.class), row.get("published_at", OffsetDateTime.class).toInstant(),
                row.get("published_by", String.class), row.get("lifecycle", String.class),
                retiredAt == null ? null : retiredAt.toInstant(), row.get("retired_by", String.class));
    }
}
