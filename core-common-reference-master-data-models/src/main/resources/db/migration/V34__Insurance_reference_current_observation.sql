-- Technical CURRENT scope/timing only. Verified shared caller and exact current PDP remain application obligations.
DO $$
BEGIN
    IF NOT pg_catalog.pg_has_role(current_user,'masters_ins_ref_guard','SET')
            OR NOT pg_catalog.pg_has_role(current_user,'masters_ins_ref_owner','SET')
            OR NOT pg_catalog.has_schema_privilege(current_user,'public','CREATE WITH GRANT OPTION') THEN
        RAISE EXCEPTION 'Authorized masters reference migrator required' USING ERRCODE='42501';
    END IF;
    IF EXISTS(SELECT 1 FROM pg_catalog.pg_roles r WHERE r.rolname IN('masters_ins_ref_owner','masters_ins_ref_guard')
        AND (r.rolcanlogin OR r.rolsuper OR r.rolbypassrls OR r.rolinherit OR r.rolcreatedb OR r.rolcreaterole OR r.rolreplication)) THEN
        RAISE EXCEPTION 'Existing guarded masters reference roles required' USING ERRCODE='42501';
    END IF;
END $$;
GRANT CREATE ON SCHEMA public TO masters_ins_ref_guard;
SET LOCAL ROLE masters_ins_ref_guard;
GRANT CREATE ON SCHEMA masters_ins_ref_private TO masters_ins_ref_guard;

CREATE TABLE masters_ins_ref_private.current_read_context (
    nonce UUID PRIMARY KEY CHECK(nonce<>'00000000-0000-0000-0000-000000000000'::uuid),
    xid XID8 NOT NULL,
    backend_pid INTEGER NOT NULL CHECK(backend_pid>0),
    db_session_user NAME NOT NULL CHECK(db_session_user='masters_ins_ref_reader'),
    record_id UUID NOT NULL,
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    publication_id UUID NOT NULL,
    revision BIGINT NOT NULL CHECK(revision>0),
    definition_digest VARCHAR(64) NOT NULL CHECK(length(definition_digest)=64 AND definition_digest ~ '^[0-9a-f]{64}$'),
    control_generation BIGINT NOT NULL CHECK(control_generation>=0),
    actor_issuer VARCHAR(500) NOT NULL CHECK(length(actor_issuer)>0),
    actor_subject VARCHAR(255) NOT NULL CHECK(length(actor_subject)>0),
    installed_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    authority_until TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ NOT NULL,
    captured_identity JSONB NOT NULL CHECK(jsonb_typeof(captured_identity)='object'),
    captured_revision JSONB NOT NULL CHECK(jsonb_typeof(captured_revision)='object'),
    captured_control JSONB NOT NULL CHECK(jsonb_typeof(captured_control)='object'),
    UNIQUE(xid,backend_pid,db_session_user),
    CHECK(record_id<>'00000000-0000-0000-0000-000000000000'::uuid
        AND publication_id<>'00000000-0000-0000-0000-000000000000'::uuid),
    CHECK(isfinite(installed_at) AND isfinite(recorded_at) AND isfinite(authority_until) AND isfinite(effective_until)
        AND recorded_at>=installed_at AND effective_until>recorded_at
        AND effective_until<=installed_at+INTERVAL '30 seconds' AND effective_until<=authority_until)
);
ALTER TABLE masters_ins_ref_private.current_read_context ENABLE ROW LEVEL SECURITY;
ALTER TABLE masters_ins_ref_private.current_read_context FORCE ROW LEVEL SECURITY;
REVOKE ALL ON masters_ins_ref_private.current_read_context FROM PUBLIC,masters_ins_ref_reader,masters_ins_ref_writer,masters_ins_ref_owner;
CREATE POLICY insurance_ref_current_context_integrity ON masters_ins_ref_private.current_read_context TO masters_ins_ref_guard USING(true) WITH CHECK(true);
CREATE TRIGGER insurance_ref_current_context_immutable BEFORE UPDATE OR DELETE ON masters_ins_ref_private.current_read_context
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_guard_context_immutable();

CREATE FUNCTION masters_ins_ref_private.v34_require_reader() RETURNS VOID
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
    IF masters_ins_ref_private.v32_require_runtime()<>'masters_ins_ref_reader' THEN
        RAISE EXCEPTION 'Restricted masters CURRENT reader required' USING ERRCODE='42501';
    END IF;
END $$;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v34_require_reader() FROM PUBLIC,masters_ins_ref_reader,masters_ins_ref_writer,masters_ins_ref_owner;

CREATE FUNCTION masters_ins_ref_private.v34_reject_mixed_context() RETURNS TRIGGER
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
    IF EXISTS(SELECT 1 FROM masters_ins_ref_private.current_read_context c
            WHERE c.xid=NEW.xid AND c.backend_pid=NEW.backend_pid AND c.db_session_user=NEW.db_session_user) THEN
        RAISE EXCEPTION 'Separate masters CURRENT transaction required' USING ERRCODE='42501';
    END IF;
    RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v34_reject_mixed_context() FROM PUBLIC,masters_ins_ref_reader,masters_ins_ref_writer,masters_ins_ref_owner;
CREATE TRIGGER insurance_ref_reject_current_command_mix BEFORE INSERT ON masters_ins_ref_private.context
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v34_reject_mixed_context();

CREATE FUNCTION masters_ins_ref_private.v34_current_context() RETURNS masters_ins_ref_private.current_read_context
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_ins_ref_private.current_read_context%ROWTYPE; observed TIMESTAMPTZ;
BEGIN
    PERFORM masters_ins_ref_private.v34_require_reader();
    SELECT * INTO c FROM masters_ins_ref_private.current_read_context s
        WHERE s.xid=pg_catalog.pg_current_xact_id_if_assigned() AND s.backend_pid=pg_catalog.pg_backend_pid() AND s.db_session_user=session_user;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'Exact current reference context required' USING ERRCODE='42501';
    END IF;
    observed:=pg_catalog.clock_timestamp();
    IF observed<c.recorded_at OR observed<c.installed_at OR observed>=c.effective_until OR observed>=c.authority_until
            OR c.recorded_at<c.installed_at OR c.effective_until>c.installed_at+INTERVAL '30 seconds'
            OR c.effective_until>c.authority_until
            OR EXISTS(SELECT 1 FROM masters_ins_ref_private.context old_context
                WHERE old_context.xid=c.xid AND old_context.backend_pid=c.backend_pid AND old_context.db_session_user=c.db_session_user) THEN
        RAISE EXCEPTION 'Current finite reference read context required' USING ERRCODE='42501';
    END IF;
    RETURN c;
END $$;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v34_current_context() FROM PUBLIC,masters_ins_ref_reader,masters_ins_ref_writer,masters_ins_ref_owner;

CREATE FUNCTION public.masters_ins_ref_current_begin(
    p_nonce UUID,p_record UUID,p_kind TEXT,p_publication UUID,p_revision BIGINT,p_digest TEXT,p_generation BIGINT,
    p_issuer TEXT,p_subject TEXT,p_authority_until TIMESTAMPTZ)
RETURNS TABLE(context_nonce UUID,record_id UUID,kind TEXT,publication_id UUID,revision BIGINT,definition_digest TEXT,
    control_generation BIGINT,db_now_ms BIGINT,recorded_at TIMESTAMPTZ,effective_until TIMESTAMPTZ)
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE tx XID8; started TIMESTAMPTZ; observed TIMESTAMPTZ; bound TIMESTAMPTZ;
    identity_value JSONB; revision_value JSONB; control_value JSONB;
    canonical TEXT; source_from TIMESTAMPTZ; source_until TIMESTAMPTZ; body JSONB;
BEGIN
    PERFORM masters_ins_ref_private.v34_require_reader();
    started:=pg_catalog.clock_timestamp();tx:=pg_catalog.pg_current_xact_id();
    IF p_nonce IS NULL OR p_nonce='00000000-0000-0000-0000-000000000000'::uuid
            OR p_record IS NULL OR p_record='00000000-0000-0000-0000-000000000000'::uuid
            OR p_publication IS NULL OR p_publication='00000000-0000-0000-0000-000000000000'::uuid
            OR p_kind IS NULL OR p_kind NOT IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')
            OR p_revision IS NULL OR p_revision<1 OR p_generation IS NULL OR p_generation<0
            OR p_digest IS NULL OR length(p_digest)<>64 OR p_digest !~ '^[0-9a-f]{64}$'
            OR p_issuer IS NULL OR length(p_issuer)<1 OR length(p_issuer)>500
            OR p_subject IS NULL OR length(p_subject)<1 OR length(p_subject)>255
            OR p_authority_until IS NULL OR NOT pg_catalog.isfinite(p_authority_until) OR p_authority_until<=started
            OR p_authority_until>started+INTERVAL '30 seconds'
            OR EXISTS(SELECT 1 FROM masters_ins_ref_private.context s WHERE s.xid=tx AND s.backend_pid=pg_catalog.pg_backend_pid() AND s.db_session_user=session_user)
            OR EXISTS(SELECT 1 FROM masters_ins_ref_private.current_read_context s WHERE s.xid=tx AND s.backend_pid=pg_catalog.pg_backend_pid() AND s.db_session_user=session_user) THEN
        RAISE EXCEPTION 'Exact separate finite reference read required' USING ERRCODE='42501';
    END IF;
    SELECT to_jsonb(i),to_jsonb(r),to_jsonb(c),r.canonical_definition,r.effective_from,r.effective_until
        INTO identity_value,revision_value,control_value,canonical,source_from,source_until
    FROM public.insurance_reference_identity i
    JOIN public.insurance_reference_revision r ON r.catalog_scope=i.catalog_scope AND r.record_id=i.record_id AND r.kind=i.kind
    JOIN public.insurance_reference_control c ON c.catalog_scope=r.catalog_scope AND c.record_id=r.record_id AND c.kind=r.kind
        AND c.publication_id=r.publication_id AND c.revision=r.revision
    WHERE i.catalog_scope='GLOBAL_MASTER' AND i.record_id=p_record AND i.kind=p_kind
        AND r.publication_id=p_publication AND r.revision=p_revision AND r.definition_digest=p_digest
        AND c.generation=p_generation AND c.lifecycle='ACTIVE';
    IF NOT FOUND THEN RETURN; END IF;
    IF masters_ins_ref_private.v32_definition_valid(canonical) IS NOT TRUE THEN
        RAISE EXCEPTION 'Exact immutable reference snapshot required' USING ERRCODE='42501';
    END IF;
    body:=canonical::jsonb;
    IF body->>'recordId' IS DISTINCT FROM p_record::text OR body->>'publicationId' IS DISTINCT FROM p_publication::text
            OR body->>'kind' IS DISTINCT FROM p_kind OR (body->>'revision')::bigint IS DISTINCT FROM p_revision
            OR body->>'code' IS DISTINCT FROM identity_value->>'code'
            OR (body->>'effectiveFrom')::timestamptz IS DISTINCT FROM source_from
            OR (body->>'effectiveUntil')::timestamptz IS DISTINCT FROM source_until
            OR encode(sha256(convert_to('masters-insurance-reference-v1'||E'\n'||canonical,'UTF8')),'hex') IS DISTINCT FROM p_digest THEN
        RAISE EXCEPTION 'Exact immutable reference facts required' USING ERRCODE='42501';
    END IF;
    observed:=pg_catalog.clock_timestamp();bound:=LEAST(p_authority_until,started+INTERVAL '30 seconds',source_until);
    IF observed<started OR observed>=p_authority_until OR observed>=started+INTERVAL '30 seconds' THEN
        RAISE EXCEPTION 'Current reference snapshot window exhausted' USING ERRCODE='42501';
    END IF;
    IF observed<source_from OR (source_until IS NOT NULL AND observed>=source_until) THEN RETURN; END IF;
    INSERT INTO masters_ins_ref_private.current_read_context(nonce,xid,backend_pid,db_session_user,record_id,kind,
        publication_id,revision,definition_digest,control_generation,actor_issuer,actor_subject,installed_at,recorded_at,
        authority_until,effective_until,captured_identity,captured_revision,captured_control)
    VALUES(p_nonce,tx,pg_catalog.pg_backend_pid(),session_user,p_record,p_kind,p_publication,p_revision,p_digest,p_generation,
        p_issuer,p_subject,started,observed,p_authority_until,bound,identity_value,revision_value,control_value);
    RETURN QUERY SELECT p_nonce,p_record,p_kind,p_publication,p_revision,p_digest,p_generation,
        pg_catalog.floor(extract(epoch FROM observed)*1000)::BIGINT,observed,bound;
END $$;
REVOKE ALL ON FUNCTION public.masters_ins_ref_current_begin(UUID,UUID,TEXT,UUID,BIGINT,TEXT,BIGINT,TEXT,TEXT,TIMESTAMPTZ)
    FROM PUBLIC,masters_ins_ref_reader,masters_ins_ref_writer,masters_ins_ref_owner;

CREATE FUNCTION public.masters_ins_ref_current_matches(p_relation TEXT,p_row JSONB) RETURNS BOOLEAN
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_ins_ref_private.current_read_context%ROWTYPE;
BEGIN
    c:=masters_ins_ref_private.v34_current_context();
    RETURN CASE p_relation
        WHEN 'insurance_reference_identity' THEN p_row=c.captured_identity
        WHEN 'insurance_reference_revision' THEN p_row=c.captured_revision
        WHEN 'insurance_reference_control' THEN p_row=c.captured_control
        ELSE false END;
EXCEPTION WHEN insufficient_privilege THEN RETURN false;
END $$;
REVOKE ALL ON FUNCTION public.masters_ins_ref_current_matches(TEXT,JSONB) FROM PUBLIC,masters_ins_ref_reader,masters_ins_ref_writer,masters_ins_ref_owner;

CREATE FUNCTION public.masters_ins_ref_current_complete(p_nonce UUID) RETURNS BOOLEAN
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_ins_ref_private.current_read_context%ROWTYPE; identity_value JSONB; revision_value JSONB; control_value JSONB;
BEGIN
    c:=masters_ins_ref_private.v34_current_context();
    IF c.nonce IS DISTINCT FROM p_nonce THEN
        RAISE EXCEPTION 'Exact current reference completion required' USING ERRCODE='42501';
    END IF;
    SELECT to_jsonb(i),to_jsonb(r),to_jsonb(head) INTO identity_value,revision_value,control_value
    FROM public.insurance_reference_identity i
    JOIN public.insurance_reference_revision r ON r.catalog_scope=i.catalog_scope AND r.record_id=i.record_id AND r.kind=i.kind
    JOIN public.insurance_reference_control head ON head.catalog_scope=r.catalog_scope AND head.record_id=r.record_id AND head.kind=r.kind
        AND head.publication_id=r.publication_id AND head.revision=r.revision
    WHERE i.catalog_scope='GLOBAL_MASTER' AND i.record_id=c.record_id AND i.kind=c.kind
        AND r.publication_id=c.publication_id AND r.revision=c.revision AND r.definition_digest=c.definition_digest
        AND head.generation=c.control_generation AND head.lifecycle='ACTIVE';
    IF NOT FOUND OR identity_value IS DISTINCT FROM c.captured_identity OR revision_value IS DISTINCT FROM c.captured_revision
            OR control_value IS DISTINCT FROM c.captured_control THEN
        RAISE EXCEPTION 'Exact current reference graph changed' USING ERRCODE='42501';
    END IF;
    PERFORM masters_ins_ref_private.v34_current_context();
    RETURN true;
END $$;
REVOKE ALL ON FUNCTION public.masters_ins_ref_current_complete(UUID) FROM PUBLIC,masters_ins_ref_reader,masters_ins_ref_writer,masters_ins_ref_owner;
GRANT EXECUTE ON FUNCTION public.masters_ins_ref_current_begin(UUID,UUID,TEXT,UUID,BIGINT,TEXT,BIGINT,TEXT,TEXT,TIMESTAMPTZ) TO masters_ins_ref_reader;
GRANT EXECUTE ON FUNCTION public.masters_ins_ref_current_matches(TEXT,JSONB) TO masters_ins_ref_reader;
GRANT EXECUTE ON FUNCTION public.masters_ins_ref_current_complete(UUID) TO masters_ins_ref_reader;
REVOKE CREATE ON SCHEMA masters_ins_ref_private FROM masters_ins_ref_guard;
RESET ROLE;
SET LOCAL ROLE masters_ins_ref_owner;
CREATE POLICY insurance_ref_current_read_identity ON public.insurance_reference_identity FOR SELECT TO masters_ins_ref_reader
    USING(public.masters_ins_ref_current_matches('insurance_reference_identity',to_jsonb(insurance_reference_identity)));
CREATE POLICY insurance_ref_current_read_revision ON public.insurance_reference_revision FOR SELECT TO masters_ins_ref_reader
    USING(public.masters_ins_ref_current_matches('insurance_reference_revision',to_jsonb(insurance_reference_revision)));
CREATE POLICY insurance_ref_current_read_control ON public.insurance_reference_control FOR SELECT TO masters_ins_ref_reader
    USING(public.masters_ins_ref_current_matches('insurance_reference_control',to_jsonb(insurance_reference_control)));
RESET ROLE;
REVOKE CREATE ON SCHEMA public FROM masters_ins_ref_guard;
