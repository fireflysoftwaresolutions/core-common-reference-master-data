-- Draft additions do not reinterpret the legacy template_variables map as JSON Schema.
ALTER TABLE document_template_catalog ADD COLUMN input_schema JSONB;
ALTER TABLE document_template_catalog ADD COLUMN style_content TEXT;
ALTER TABLE document_template_catalog ADD COLUMN media_type VARCHAR(100);

CREATE TABLE document_template_revision (
    revision_id UUID PRIMARY KEY,
    template_id UUID NOT NULL REFERENCES document_template_catalog(template_id) ON DELETE RESTRICT,
    localization_id UUID NOT NULL REFERENCES document_template_localization(localization_id) ON DELETE RESTRICT,
    locale_id UUID NOT NULL REFERENCES language_locale(locale_id) ON DELETE RESTRICT,
    template_code VARCHAR(100) NOT NULL,
    version VARCHAR(20) NOT NULL,
    locale_code VARCHAR(50) NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    template_content TEXT NOT NULL,
    input_schema JSONB NOT NULL CHECK (jsonb_typeof(input_schema) = 'object'),
    style_content TEXT NOT NULL,
    template_variables JSONB NOT NULL,
    source_snapshot JSONB NOT NULL,
    content_digest VARCHAR(64) NOT NULL CHECK (content_digest ~ '^[0-9a-f]{64}$'),
    schema_digest VARCHAR(64) NOT NULL CHECK (schema_digest ~ '^[0-9a-f]{64}$'),
    style_digest VARCHAR(64) NOT NULL CHECK (style_digest ~ '^[0-9a-f]{64}$'),
    source_digest VARCHAR(64) NOT NULL CHECK (source_digest ~ '^[0-9a-f]{64}$'),
    published_at TIMESTAMPTZ NOT NULL,
    published_by VARCHAR(255) NOT NULL CHECK (length(trim(published_by)) > 0),
    lifecycle VARCHAR(20) NOT NULL CHECK (lifecycle IN ('PUBLISHED','RETIRED')),
    retired_at TIMESTAMPTZ,
    retired_by VARCHAR(255),
    CONSTRAINT uk_document_template_revision_identity UNIQUE (template_id,version,locale_code),
    CONSTRAINT ck_document_template_revision_retirement CHECK (
        (lifecycle = 'PUBLISHED' AND retired_at IS NULL AND retired_by IS NULL)
        OR (lifecycle = 'RETIRED' AND retired_at IS NOT NULL AND length(trim(retired_by)) > 0))
);
CREATE INDEX idx_document_template_revision_lookup ON document_template_revision(template_code,version,locale_code);

CREATE FUNCTION protect_document_template_revision() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'DOCUMENT_REVISION_IMMUTABLE' USING ERRCODE = '23514';
    END IF;
    IF OLD.lifecycle <> 'PUBLISHED' OR NEW.lifecycle <> 'RETIRED'
        OR NEW.retired_at IS NULL OR NEW.retired_by IS NULL
        OR (to_jsonb(OLD) - ARRAY['lifecycle','retired_at','retired_by'])
            IS DISTINCT FROM (to_jsonb(NEW) - ARRAY['lifecycle','retired_at','retired_by']) THEN
        RAISE EXCEPTION 'DOCUMENT_REVISION_IMMUTABLE' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER document_template_revision_immutable
BEFORE UPDATE OR DELETE ON document_template_revision
FOR EACH ROW EXECUTE FUNCTION protect_document_template_revision();

CREATE FUNCTION protect_published_document_template_identity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.template_code IS DISTINCT FROM OLD.template_code OR NEW.type_id IS DISTINCT FROM OLD.type_id)
        AND EXISTS (SELECT 1 FROM document_template_revision WHERE template_id = OLD.template_id) THEN
        RAISE EXCEPTION 'DOCUMENT_TEMPLATE_IDENTITY_IMMUTABLE' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER document_template_published_identity
BEFORE UPDATE ON document_template_catalog
FOR EACH ROW EXECUTE FUNCTION protect_published_document_template_identity();

CREATE FUNCTION protect_published_document_localization_identity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.template_id IS DISTINCT FROM OLD.template_id OR NEW.locale_id IS DISTINCT FROM OLD.locale_id)
        AND EXISTS (SELECT 1 FROM document_template_revision WHERE localization_id = OLD.localization_id) THEN
        RAISE EXCEPTION 'DOCUMENT_LOCALIZATION_IDENTITY_IMMUTABLE' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER document_template_localization_published_identity
BEFORE UPDATE ON document_template_localization
FOR EACH ROW EXECUTE FUNCTION protect_published_document_localization_identity();

CREATE FUNCTION protect_published_document_type_identity() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.type_code IS DISTINCT FROM OLD.type_code
        AND EXISTS (SELECT 1 FROM document_template_catalog c JOIN document_template_revision r
            ON r.template_id=c.template_id WHERE c.type_id=OLD.type_id) THEN
        RAISE EXCEPTION 'DOCUMENT_TYPE_IDENTITY_IMMUTABLE' USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER document_template_type_published_identity
BEFORE UPDATE ON document_template_type_catalog
FOR EACH ROW EXECUTE FUNCTION protect_published_document_type_identity();
