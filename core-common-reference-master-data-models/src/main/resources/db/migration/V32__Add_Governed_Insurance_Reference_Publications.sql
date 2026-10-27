-- BEGIN 00-role-preflight.sql
-- Exact preprovisioned roles; administrative creation is a separate reviewed preparation step.
DO $$
DECLARE role_name TEXT; actual_role RECORD;
BEGIN
    FOREACH role_name IN ARRAY ARRAY[
        'masters_ins_ref_owner','masters_ins_ref_guard',
        'masters_ins_ref_writer','masters_ins_ref_reader'
    ] LOOP
        SELECT * INTO actual_role FROM pg_catalog.pg_roles WHERE rolname=role_name;
        IF NOT FOUND OR actual_role.rolsuper OR actual_role.rolbypassrls
                OR actual_role.rolinherit OR actual_role.rolcreatedb
                OR actual_role.rolcreaterole OR actual_role.rolreplication
                OR actual_role.rolcanlogin IS DISTINCT FROM
                    (role_name IN('masters_ins_ref_writer','masters_ins_ref_reader'))
                OR EXISTS(SELECT 1 FROM pg_catalog.pg_auth_members m WHERE m.member=actual_role.oid) THEN
            RAISE EXCEPTION 'Exact restricted masters insurance reference roles required' USING ERRCODE='42501';
        END IF;
    END LOOP;
    IF pg_catalog.has_schema_privilege('masters_ins_ref_writer','public','CREATE')
            OR pg_catalog.has_schema_privilege('masters_ins_ref_reader','public','CREATE')
            OR pg_catalog.pg_has_role('masters_ins_ref_writer','masters_ins_ref_owner','MEMBER')
            OR pg_catalog.pg_has_role('masters_ins_ref_writer','masters_ins_ref_guard','MEMBER')
            OR pg_catalog.pg_has_role('masters_ins_ref_reader','masters_ins_ref_owner','MEMBER')
            OR pg_catalog.pg_has_role('masters_ins_ref_reader','masters_ins_ref_guard','MEMBER') THEN
        RAISE EXCEPTION 'Nonowner masters insurance reference runtimes required' USING ERRCODE='42501';
    END IF;
    IF pg_catalog.has_schema_privilege('masters_ins_ref_owner','public','CREATE')
            OR pg_catalog.has_schema_privilege('masters_ins_ref_guard','public','CREATE')
            OR EXISTS(SELECT 1 FROM pg_catalog.pg_namespace n JOIN pg_catalog.pg_roles r ON r.oid=n.nspowner
                WHERE n.nspname='public' AND r.rolname IN('masters_ins_ref_owner','masters_ins_ref_guard'))
            OR EXISTS(SELECT 1 FROM pg_catalog.pg_database d JOIN pg_catalog.pg_roles r ON r.oid=d.datdba
                WHERE d.datname=current_database() AND r.rolname IN('masters_ins_ref_owner','masters_ins_ref_guard')) THEN
        RAISE EXCEPTION 'Integrity schema privileges must initially be absent' USING ERRCODE='42501';
    END IF;
    IF NOT pg_catalog.has_schema_privilege(current_user,'public','USAGE WITH GRANT OPTION')
            OR NOT pg_catalog.has_schema_privilege(current_user,'public','CREATE WITH GRANT OPTION') THEN
        RAISE EXCEPTION 'Separately delegated migration schema privileges required' USING ERRCODE='42501';
    END IF;
    IF current_user IN('masters_ins_ref_writer','masters_ins_ref_reader')
            OR NOT pg_catalog.pg_has_role(current_user,'masters_ins_ref_owner','SET')
            OR NOT pg_catalog.pg_has_role(current_user,'masters_ins_ref_guard','SET') THEN
        RAISE EXCEPTION 'Separately authorized masters insurance reference migrator required' USING ERRCODE='42501';
    END IF;
END $$;

-- END 00-role-preflight.sql
-- BEGIN 01-records.sql
-- Source fragment only. Role/context/RLS/integrity sections must precede any runnable V32 gate.
CREATE TABLE public.insurance_reference_identity (
    catalog_scope TEXT NOT NULL CHECK(catalog_scope='GLOBAL_MASTER'),
    record_id UUID NOT NULL PRIMARY KEY CHECK(record_id<>'00000000-0000-0000-0000-000000000000'::uuid),
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    code VARCHAR(64) NOT NULL CHECK(code ~ '^[A-Z][A-Z0-9_]{0,63}$'),
    UNIQUE(catalog_scope,record_id,kind),
    UNIQUE(catalog_scope,kind,code)
);
CREATE TABLE public.insurance_reference_revision (
    catalog_scope TEXT NOT NULL CHECK(catalog_scope='GLOBAL_MASTER'),
    publication_id UUID NOT NULL CHECK(publication_id<>'00000000-0000-0000-0000-000000000000'::uuid),
    record_id UUID NOT NULL,
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    revision BIGINT NOT NULL CHECK(revision>0),
    canonical_definition TEXT NOT NULL CHECK(octet_length(canonical_definition) BETWEEN 1 AND 131072),
    definition_digest VARCHAR(64) NOT NULL CHECK(definition_digest ~ '^[0-9a-f]{64}$'),
    effective_from TIMESTAMPTZ NOT NULL,
    effective_until TIMESTAMPTZ,
    publication_action_id UUID NOT NULL UNIQUE,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT pg_catalog.clock_timestamp(),
    PRIMARY KEY(publication_id),
    UNIQUE(catalog_scope,record_id,kind,revision),
    UNIQUE(catalog_scope,record_id,kind,publication_id,revision),
    CHECK(effective_until IS NULL OR effective_until>effective_from),
    CHECK(effective_from BETWEEN '1900-01-01T00:00:00Z'::timestamptz AND '9999-12-31T23:59:59.999999Z'::timestamptz),
    CHECK(effective_until IS NULL OR effective_until<='9999-12-31T23:59:59.999999Z'::timestamptz),
    CHECK(canonical_definition IS JSON OBJECT WITH UNIQUE KEYS),
    CHECK(definition_digest=encode(sha256(convert_to('masters-insurance-reference-v1'||E'\n'||canonical_definition,'UTF8')),'hex')),
    FOREIGN KEY(catalog_scope,record_id,kind) REFERENCES public.insurance_reference_identity(catalog_scope,record_id,kind) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE public.insurance_reference_control (
    catalog_scope TEXT NOT NULL CHECK(catalog_scope='GLOBAL_MASTER'),
    record_id UUID NOT NULL PRIMARY KEY,
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    publication_id UUID NOT NULL,
    revision BIGINT NOT NULL CHECK(revision>0),
    generation BIGINT NOT NULL CHECK(generation>=0),
    lifecycle TEXT NOT NULL CHECK(lifecycle IN('ACTIVE','WITHDRAWN')),
    FOREIGN KEY(catalog_scope,record_id,kind) REFERENCES public.insurance_reference_identity(catalog_scope,record_id,kind) DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY(catalog_scope,record_id,kind,publication_id,revision) REFERENCES public.insurance_reference_revision(catalog_scope,record_id,kind,publication_id,revision) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE public.insurance_reference_maker_intent (
    catalog_scope TEXT NOT NULL CHECK(catalog_scope='GLOBAL_MASTER'),
    intent_id UUID NOT NULL PRIMARY KEY CHECK(intent_id<>'00000000-0000-0000-0000-000000000000'::uuid),
    record_id UUID NOT NULL,
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    operation TEXT NOT NULL CHECK(operation IN('PUBLISH','WITHDRAW')),
    canonical_intent TEXT NOT NULL CHECK(octet_length(canonical_intent) BETWEEN 1 AND 150000),
    intent_digest VARCHAR(64) NOT NULL CHECK(intent_digest ~ '^[0-9a-f]{64}$'),
    expected_publication_id UUID,
    expected_revision BIGINT NOT NULL CHECK(expected_revision>=0),
    expected_generation BIGINT NOT NULL CHECK(expected_generation>=0),
    maker_issuer VARCHAR(500) NOT NULL CHECK(length(maker_issuer)>0),
    maker_subject VARCHAR(255) NOT NULL CHECK(length(maker_subject)>0),
    valid_until TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT pg_catalog.clock_timestamp(),
    UNIQUE(catalog_scope,record_id,kind,intent_id),
    UNIQUE(catalog_scope,record_id,kind,intent_id,intent_digest),
    CHECK((expected_publication_id IS NULL AND expected_revision=0 AND expected_generation=0)
       OR (expected_publication_id IS NOT NULL AND expected_revision>0)),
    CHECK(valid_until>recorded_at AND valid_until<=recorded_at+INTERVAL '1 day'),
    CHECK(canonical_intent IS JSON OBJECT WITH UNIQUE KEYS),
    CHECK(intent_digest=encode(sha256(convert_to('masters-insurance-governed-intent-v1'||E'\n'||canonical_intent,'UTF8')),'hex')),
    FOREIGN KEY(catalog_scope,record_id,kind) REFERENCES public.insurance_reference_identity(catalog_scope,record_id,kind) DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY(catalog_scope,record_id,kind,expected_publication_id,expected_revision) REFERENCES public.insurance_reference_revision(catalog_scope,record_id,kind,publication_id,revision) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE public.insurance_reference_intent_control (
    catalog_scope TEXT NOT NULL CHECK(catalog_scope='GLOBAL_MASTER'),
    intent_id UUID NOT NULL PRIMARY KEY,
    record_id UUID NOT NULL,
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    generation BIGINT NOT NULL CHECK(generation>=0),
    lifecycle TEXT NOT NULL CHECK(lifecycle IN('OPEN','APPROVED','EXECUTED','WITHDRAWN')),
    approval_id UUID,
    execution_action_id UUID,
    CHECK((lifecycle='OPEN' AND generation=0 AND approval_id IS NULL AND execution_action_id IS NULL)
       OR (lifecycle='APPROVED' AND generation=1 AND approval_id IS NOT NULL AND execution_action_id IS NULL)
       OR (lifecycle='EXECUTED' AND generation=2 AND approval_id IS NOT NULL AND execution_action_id IS NOT NULL)
       OR (lifecycle='WITHDRAWN' AND execution_action_id IS NULL AND ((approval_id IS NULL AND generation=1) OR (approval_id IS NOT NULL AND generation=2)))),
    FOREIGN KEY(catalog_scope,record_id,kind,intent_id) REFERENCES public.insurance_reference_maker_intent(catalog_scope,record_id,kind,intent_id) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE public.insurance_reference_checker_approval (
    catalog_scope TEXT NOT NULL CHECK(catalog_scope='GLOBAL_MASTER'),
    approval_id UUID NOT NULL PRIMARY KEY CHECK(approval_id<>'00000000-0000-0000-0000-000000000000'::uuid),
    record_id UUID NOT NULL,
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    intent_id UUID NOT NULL,
    intent_digest VARCHAR(64) NOT NULL CHECK(intent_digest ~ '^[0-9a-f]{64}$'),
    expected_intent_generation BIGINT NOT NULL CHECK(expected_intent_generation=0),
    checker_issuer VARCHAR(500) NOT NULL CHECK(length(checker_issuer)>0),
    checker_subject VARCHAR(255) NOT NULL CHECK(length(checker_subject)>0),
    valid_until TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT pg_catalog.clock_timestamp(),
    UNIQUE(catalog_scope,record_id,kind,intent_id,approval_id),
    UNIQUE(catalog_scope,record_id,kind,intent_id),
    CHECK(valid_until>recorded_at AND valid_until<=recorded_at+INTERVAL '1 day'),
    FOREIGN KEY(catalog_scope,record_id,kind,intent_id,intent_digest) REFERENCES public.insurance_reference_maker_intent(catalog_scope,record_id,kind,intent_id,intent_digest) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE public.insurance_reference_action (
    catalog_scope TEXT NOT NULL CHECK(catalog_scope='GLOBAL_MASTER'),
    action_id UUID NOT NULL PRIMARY KEY CHECK(action_id<>'00000000-0000-0000-0000-000000000000'::uuid),
    record_id UUID NOT NULL,
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    intent_id UUID NOT NULL,
    operation TEXT NOT NULL CHECK(operation IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL','APPROVE','EXECUTE','WITHDRAW_INTENT')),
    intent_digest VARCHAR(64) NOT NULL CHECK(intent_digest ~ '^[0-9a-f]{64}$'),
    publication_id UUID NOT NULL,
    revision BIGINT NOT NULL CHECK(revision>0),
    result_generation BIGINT NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT pg_catalog.clock_timestamp(),
    UNIQUE(catalog_scope,record_id,kind,action_id),
    UNIQUE(catalog_scope,record_id,kind,action_id,operation),
    UNIQUE(catalog_scope,record_id,kind,intent_id,action_id),
    UNIQUE(catalog_scope,record_id,kind,intent_id,operation),
    CHECK((operation IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL') AND result_generation=0)
       OR (operation='APPROVE' AND result_generation=1)
       OR (operation='EXECUTE' AND result_generation=2)
       OR (operation='WITHDRAW_INTENT' AND result_generation IN(1,2))),
    FOREIGN KEY(catalog_scope,record_id,kind,intent_id,intent_digest) REFERENCES public.insurance_reference_maker_intent(catalog_scope,record_id,kind,intent_id,intent_digest) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE public.insurance_reference_audit (
    catalog_scope TEXT NOT NULL CHECK(catalog_scope='GLOBAL_MASTER'),
    event_id UUID NOT NULL PRIMARY KEY CHECK(event_id<>'00000000-0000-0000-0000-000000000000'::uuid),
    record_id UUID NOT NULL,
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    action_id UUID NOT NULL UNIQUE,
    event_kind TEXT NOT NULL CHECK(event_kind IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL','APPROVE','EXECUTE','WITHDRAW_INTENT')),
    actor_issuer VARCHAR(500) NOT NULL CHECK(length(actor_issuer)>0),
    actor_subject VARCHAR(255) NOT NULL CHECK(length(actor_subject)>0),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT pg_catalog.clock_timestamp(),
    UNIQUE(catalog_scope,record_id,kind,action_id,event_id,event_kind),
    FOREIGN KEY(catalog_scope,record_id,kind,action_id,event_kind) REFERENCES public.insurance_reference_action(catalog_scope,record_id,kind,action_id,operation) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE public.insurance_reference_outbox (
    catalog_scope TEXT NOT NULL CHECK(catalog_scope='GLOBAL_MASTER'),
    event_id UUID NOT NULL PRIMARY KEY,
    record_id UUID NOT NULL,
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    action_id UUID NOT NULL UNIQUE,
    event_kind TEXT NOT NULL CHECK(event_kind IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL','APPROVE','EXECUTE','WITHDRAW_INTENT')),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT pg_catalog.clock_timestamp(),
    FOREIGN KEY(catalog_scope,record_id,kind,action_id,event_id,event_kind) REFERENCES public.insurance_reference_audit(catalog_scope,record_id,kind,action_id,event_id,event_kind) DEFERRABLE INITIALLY DEFERRED,
    FOREIGN KEY(catalog_scope,record_id,kind,action_id,event_kind) REFERENCES public.insurance_reference_action(catalog_scope,record_id,kind,action_id,operation) DEFERRABLE INITIALLY DEFERRED
);
ALTER TABLE public.insurance_reference_intent_control ADD CONSTRAINT insurance_reference_approval_pointer
    FOREIGN KEY(catalog_scope,record_id,kind,intent_id,approval_id) REFERENCES public.insurance_reference_checker_approval(catalog_scope,record_id,kind,intent_id,approval_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_reference_intent_control ADD CONSTRAINT insurance_reference_execution_pointer
    FOREIGN KEY(catalog_scope,record_id,kind,intent_id,execution_action_id) REFERENCES public.insurance_reference_action(catalog_scope,record_id,kind,intent_id,action_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_reference_revision ADD CONSTRAINT insurance_reference_publication_action
    FOREIGN KEY(catalog_scope,record_id,kind,publication_action_id) REFERENCES public.insurance_reference_action(catalog_scope,record_id,kind,action_id) DEFERRABLE INITIALLY DEFERRED;

-- Guard function/trigger construction is migration-only; final table ACLs revoke TRIGGER.
GRANT USAGE ON SCHEMA public TO masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT CREATE ON SCHEMA public TO masters_ins_ref_guard;
GRANT SELECT,TRIGGER ON public.insurance_reference_identity TO masters_ins_ref_guard;
GRANT SELECT,TRIGGER ON public.insurance_reference_revision TO masters_ins_ref_guard;
GRANT SELECT,TRIGGER ON public.insurance_reference_control TO masters_ins_ref_guard;
GRANT SELECT,TRIGGER ON public.insurance_reference_maker_intent TO masters_ins_ref_guard;
GRANT SELECT,TRIGGER ON public.insurance_reference_intent_control TO masters_ins_ref_guard;
GRANT SELECT,TRIGGER ON public.insurance_reference_checker_approval TO masters_ins_ref_guard;
GRANT SELECT,TRIGGER ON public.insurance_reference_action TO masters_ins_ref_guard;
GRANT SELECT,TRIGGER ON public.insurance_reference_audit TO masters_ins_ref_guard;
GRANT SELECT,TRIGGER ON public.insurance_reference_outbox TO masters_ins_ref_guard;

-- END 01-records.sql
-- BEGIN 02-private-state.sql
-- Technical context/witness state only; application verified principal/current PDP remains mandatory.
CREATE SCHEMA masters_ins_ref_private AUTHORIZATION masters_ins_ref_guard;
SET LOCAL ROLE masters_ins_ref_guard;
REVOKE ALL ON SCHEMA masters_ins_ref_private FROM PUBLIC;
CREATE TABLE masters_ins_ref_private.context (
    nonce UUID PRIMARY KEY CHECK(nonce<>'00000000-0000-0000-0000-000000000000'::uuid),
    xid XID8 NOT NULL,
    backend_pid INTEGER NOT NULL CHECK(backend_pid>0),
    db_session_user NAME NOT NULL,
    record_id UUID NOT NULL CHECK(record_id<>'00000000-0000-0000-0000-000000000000'::uuid),
    kind TEXT NOT NULL CHECK(kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
    action_id UUID NOT NULL CHECK(action_id<>'00000000-0000-0000-0000-000000000000'::uuid),
    request_intent_id UUID,
    resolved_intent_id UUID,
    mode TEXT NOT NULL CHECK(mode IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL','APPROVE','PREPARE_EXECUTION','EXECUTE','WITHDRAW_INTENT','RECOVER')),
    actor_issuer VARCHAR(500) NOT NULL CHECK(length(actor_issuer)>0),
    actor_subject VARCHAR(255) NOT NULL CHECK(length(actor_subject)>0),
    authority_until TIMESTAMPTZ NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL DEFAULT pg_catalog.clock_timestamp(),
    effective_until TIMESTAMPTZ NOT NULL,
    request_digest VARCHAR(64) NOT NULL CHECK(request_digest ~ '^[0-9a-f]{64}$'),
    locked BOOLEAN NOT NULL DEFAULT false,
    recorded_at TIMESTAMPTZ,
    captured_identity JSONB,
    captured_control JSONB,
    captured_intent JSONB,
    captured_intent_control JSONB,
    captured_approval JSONB,
    captured_action JSONB,
    UNIQUE(xid,backend_pid,db_session_user),
    CHECK(effective_until>installed_at AND effective_until<=installed_at+INTERVAL '30 seconds' AND effective_until<=authority_until),
    CHECK((NOT locked AND recorded_at IS NULL)
       OR (locked AND recorded_at IS NOT NULL AND recorded_at>=installed_at AND recorded_at<effective_until)),
    CHECK((mode IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL','RECOVER') AND request_intent_id IS NULL)
       OR (mode IN('APPROVE','PREPARE_EXECUTION','EXECUTE','WITHDRAW_INTENT') AND request_intent_id IS NOT NULL))
);
CREATE TABLE masters_ins_ref_private.row_witness (
    context_nonce UUID NOT NULL REFERENCES masters_ins_ref_private.context(nonce) DEFERRABLE INITIALLY DEFERRED,
    relation_name TEXT NOT NULL CHECK(relation_name IN('insurance_reference_identity','insurance_reference_revision','insurance_reference_control',
        'insurance_reference_maker_intent','insurance_reference_intent_control','insurance_reference_checker_approval',
        'insurance_reference_action','insurance_reference_audit','insurance_reference_outbox')),
    operation TEXT NOT NULL CHECK(operation IN('INSERT','UPDATE')),
    old_row JSONB,
    new_row JSONB NOT NULL CHECK(jsonb_typeof(new_row)='object'),
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT pg_catalog.clock_timestamp(),
    PRIMARY KEY(context_nonce,relation_name),
    CHECK((operation='INSERT' AND old_row IS NULL)
       OR (operation='UPDATE' AND old_row IS NOT NULL AND jsonb_typeof(old_row)='object'))
);
ALTER TABLE masters_ins_ref_private.context ENABLE ROW LEVEL SECURITY;
ALTER TABLE masters_ins_ref_private.context FORCE ROW LEVEL SECURITY;
ALTER TABLE masters_ins_ref_private.row_witness ENABLE ROW LEVEL SECURITY;
ALTER TABLE masters_ins_ref_private.row_witness FORCE ROW LEVEL SECURITY;
REVOKE ALL ON masters_ins_ref_private.context FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
REVOKE ALL ON masters_ins_ref_private.row_witness FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
CREATE POLICY integrity_context ON masters_ins_ref_private.context TO masters_ins_ref_guard USING(true) WITH CHECK(true);
CREATE POLICY integrity_witness ON masters_ins_ref_private.row_witness TO masters_ins_ref_guard USING(true) WITH CHECK(true);

-- END 02-private-state.sql
-- BEGIN 03-context-primitives.sql
CREATE FUNCTION masters_ins_ref_private.v32_require_runtime() RETURNS NAME
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE runtime_role RECORD; owner_oid OID; relation_count INTEGER; integrity_role_count INTEGER;
BEGIN
    SELECT * INTO runtime_role FROM pg_catalog.pg_roles WHERE rolname=session_user;
    IF NOT FOUND OR runtime_role.rolname NOT IN('masters_ins_ref_writer','masters_ins_ref_reader')
            OR runtime_role.rolsuper OR runtime_role.rolbypassrls OR runtime_role.rolinherit
            OR runtime_role.rolcreatedb OR runtime_role.rolcreaterole OR runtime_role.rolreplication
            OR NOT runtime_role.rolcanlogin
            OR EXISTS(SELECT 1 FROM pg_catalog.pg_auth_members WHERE member=runtime_role.oid)
            OR pg_catalog.has_schema_privilege(runtime_role.oid,'public','CREATE')
            OR pg_catalog.current_setting('transaction_isolation')<>'repeatable read'
            OR pg_catalog.current_setting('transaction_read_only')<>'off' THEN
        RAISE EXCEPTION 'Restricted repeatable-read masters reference runtime required' USING ERRCODE='42501';
    END IF;
    SELECT count(*) INTO integrity_role_count FROM pg_catalog.pg_roles r
        WHERE rolname IN('masters_ins_ref_owner','masters_ins_ref_guard')
        AND NOT rolsuper AND NOT rolbypassrls AND NOT rolcanlogin AND NOT rolinherit
        AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolreplication
        AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_auth_members m WHERE m.member=r.oid);
    SELECT oid INTO owner_oid FROM pg_catalog.pg_roles WHERE rolname='masters_ins_ref_owner'
        AND NOT rolsuper AND NOT rolbypassrls AND NOT rolcanlogin AND NOT rolinherit;
    SELECT count(*) INTO relation_count FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
        WHERE n.nspname='public' AND c.relname IN('insurance_reference_identity','insurance_reference_revision','insurance_reference_control',
            'insurance_reference_maker_intent','insurance_reference_intent_control','insurance_reference_checker_approval',
            'insurance_reference_action','insurance_reference_audit','insurance_reference_outbox')
        AND c.relowner=owner_oid AND c.relrowsecurity AND c.relforcerowsecurity;
    IF owner_oid IS NULL OR relation_count<>9 OR integrity_role_count<>2
            OR pg_catalog.pg_has_role(runtime_role.oid,'masters_ins_ref_owner','MEMBER')
            OR pg_catalog.pg_has_role(runtime_role.oid,'masters_ins_ref_guard','MEMBER') THEN
        RAISE EXCEPTION 'Exact guarded masters reference storage required' USING ERRCODE='42501';
    END IF;
    RETURN runtime_role.rolname;
END $$;

CREATE FUNCTION masters_ins_ref_private.v32_current_context() RETURNS masters_ins_ref_private.context
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_ins_ref_private.context%ROWTYPE; observed TIMESTAMPTZ;
BEGIN
    PERFORM masters_ins_ref_private.v32_require_runtime();
    SELECT * INTO c FROM masters_ins_ref_private.context s WHERE s.xid=pg_catalog.pg_current_xact_id_if_assigned()
        AND s.backend_pid=pg_catalog.pg_backend_pid() AND s.db_session_user=session_user;
    observed:=pg_catalog.clock_timestamp();
    IF NOT FOUND OR NOT c.locked OR c.recorded_at IS NULL
            OR observed<c.recorded_at OR observed<c.installed_at OR observed>=c.effective_until OR observed>=c.authority_until
            OR c.effective_until>c.installed_at+INTERVAL '30 seconds' OR c.effective_until>c.authority_until
            OR c.recorded_at<c.installed_at THEN
        RAISE EXCEPTION 'Current finite masters reference context required' USING ERRCODE='42501';
    END IF;
    RETURN c;
END $$;

CREATE FUNCTION public.masters_ins_ref_context_current(p_nonce UUID) RETURNS BOOLEAN
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_ins_ref_private.context%ROWTYPE;
BEGIN
    c:=masters_ins_ref_private.v32_current_context();
    IF c.nonce IS DISTINCT FROM p_nonce THEN
        RAISE EXCEPTION 'Exact masters reference context required' USING ERRCODE='42501';
    END IF;
    RETURN true;
END $$;

CREATE FUNCTION public.masters_ins_ref_scope_matches(p_record UUID,p_kind TEXT) RETURNS BOOLEAN
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_ins_ref_private.context%ROWTYPE;
BEGIN
    c:=masters_ins_ref_private.v32_current_context();
    RETURN c.record_id=p_record AND c.kind=p_kind;
EXCEPTION WHEN insufficient_privilege THEN
    RETURN false;
END $$;

CREATE FUNCTION masters_ins_ref_private.v32_guard_context_immutable() RETURNS TRIGGER
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
    RAISE EXCEPTION 'Installed masters reference context is immutable' USING ERRCODE='42501';
END $$;
CREATE TRIGGER masters_ins_ref_context_immutable BEFORE UPDATE OR DELETE ON masters_ins_ref_private.context
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_guard_context_immutable();
CREATE TRIGGER masters_ins_ref_witness_immutable BEFORE UPDATE OR DELETE ON masters_ins_ref_private.row_witness
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_guard_context_immutable();

-- END 03-context-primitives.sql
-- BEGIN 04-begin-context.sql
CREATE FUNCTION public.masters_ins_ref_begin(
    p_nonce UUID,p_record UUID,p_kind TEXT,p_action UUID,p_intent UUID,p_mode TEXT,
    p_issuer TEXT,p_subject TEXT,p_authority_until TIMESTAMPTZ)
RETURNS TABLE(context_nonce UUID,record_id UUID,kind TEXT,db_now_ms BIGINT,recorded_at TIMESTAMPTZ,effective_until TIMESTAMPTZ)
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE runtime_name NAME; started TIMESTAMPTZ; observed TIMESTAMPTZ; bound TIMESTAMPTZ;
    identity_row public.insurance_reference_identity%ROWTYPE;
    control_row public.insurance_reference_control%ROWTYPE;
    intent_row public.insurance_reference_maker_intent%ROWTYPE;
    intent_control_row public.insurance_reference_intent_control%ROWTYPE;
    approval_row public.insurance_reference_checker_approval%ROWTYPE;
    action_row public.insurance_reference_action%ROWTYPE;
    audit_row public.insurance_reference_audit%ROWTYPE;
    identity_value JSONB; control_value JSONB; intent_value JSONB; intent_control_value JSONB; approval_value JSONB; action_value JSONB;
    resolved_intent UUID; body JSONB; request_value JSONB; replay BOOLEAN:=false;
BEGIN
    started:=pg_catalog.clock_timestamp();
    runtime_name:=masters_ins_ref_private.v32_require_runtime();
    IF p_nonce IS NULL OR p_record IS NULL OR p_action IS NULL
            OR p_nonce='00000000-0000-0000-0000-000000000000'::uuid
            OR p_record='00000000-0000-0000-0000-000000000000'::uuid
            OR p_action='00000000-0000-0000-0000-000000000000'::uuid
            OR p_kind IS NULL OR p_kind NOT IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')
            OR p_mode IS NULL OR p_mode NOT IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL','APPROVE','PREPARE_EXECUTION','EXECUTE','WITHDRAW_INTENT','RECOVER')
            OR p_issuer IS NULL OR length(p_issuer) NOT BETWEEN 1 AND 500 OR btrim(p_issuer)=''
            OR p_subject IS NULL OR length(p_subject) NOT BETWEEN 1 AND 255 OR btrim(p_subject)=''
            OR p_issuer ~ '[[:cntrl:]]' OR p_subject ~ '[[:cntrl:]]'
            OR p_authority_until IS NULL OR NOT isfinite(p_authority_until) OR p_authority_until<=started
            OR (p_mode IN('PREPARE_EXECUTION','RECOVER')) IS DISTINCT FROM (runtime_name='masters_ins_ref_reader')
            OR (p_mode IN('APPROVE','PREPARE_EXECUTION','EXECUTE','WITHDRAW_INTENT')) IS DISTINCT FROM (p_intent IS NOT NULL)
            OR p_intent='00000000-0000-0000-0000-000000000000'::uuid
            OR EXISTS(SELECT 1 FROM masters_ins_ref_private.context c WHERE c.nonce=p_nonce
                OR (c.xid=pg_catalog.pg_current_xact_id() AND c.backend_pid=pg_catalog.pg_backend_pid() AND c.db_session_user=session_user)) THEN
        RAISE EXCEPTION 'Exact fresh masters reference command context required' USING ERRCODE='42501';
    END IF;
    -- Fixed order: record gate, optional intent gate, action gate, then scoped row captures.
    PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('MASTERS_INS_REF_RECORD:'||p_record::text,0));
    IF p_intent IS NOT NULL THEN
        PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('MASTERS_INS_REF_INTENT:'||p_intent::text,0));
    END IF;
    PERFORM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended('MASTERS_INS_REF_ACTION:'||p_action::text,0));
    SELECT * INTO identity_row FROM public.insurance_reference_identity i WHERE i.record_id=p_record FOR NO KEY UPDATE;
    IF FOUND THEN
        IF identity_row.kind<>p_kind THEN RAISE EXCEPTION 'Exact masters reference identity required' USING ERRCODE='42501'; END IF;
        identity_value:=to_jsonb(identity_row);
    END IF;
    SELECT * INTO control_row FROM public.insurance_reference_control c WHERE c.record_id=p_record FOR NO KEY UPDATE;
    IF FOUND THEN control_value:=to_jsonb(control_row); END IF;
    SELECT * INTO action_row FROM public.insurance_reference_action a WHERE a.action_id=p_action FOR NO KEY UPDATE;
    IF FOUND THEN
        IF action_row.record_id<>p_record OR action_row.kind<>p_kind
                OR (p_intent IS NOT NULL AND action_row.intent_id<>p_intent)
                OR (p_mode='SUBMIT_PUBLICATION' AND action_row.operation<>'SUBMIT_PUBLICATION')
                OR (p_mode='SUBMIT_WITHDRAWAL' AND action_row.operation<>'SUBMIT_WITHDRAWAL')
                OR (p_mode='APPROVE' AND action_row.operation<>'APPROVE')
                OR (p_mode IN('PREPARE_EXECUTION','EXECUTE') AND action_row.operation<>'EXECUTE')
                OR (p_mode='WITHDRAW_INTENT' AND action_row.operation<>'WITHDRAW_INTENT') THEN
            RAISE EXCEPTION 'Exact masters reference retained action required' USING ERRCODE='42501';
        END IF;
        SELECT * INTO audit_row FROM public.insurance_reference_audit a WHERE a.action_id=p_action;
        IF NOT FOUND OR audit_row.record_id<>p_record OR audit_row.kind<>p_kind
                OR audit_row.actor_issuer<>p_issuer OR audit_row.actor_subject<>p_subject THEN
            RAISE EXCEPTION 'Current original masters reference action actor required' USING ERRCODE='42501';
        END IF;
        replay:=true;resolved_intent:=action_row.intent_id;action_value:=to_jsonb(action_row);
    ELSE resolved_intent:=p_intent;
    END IF;
    IF p_mode='RECOVER' AND NOT replay THEN
        RAISE EXCEPTION 'Retained masters reference action is not observed' USING ERRCODE='42501';
    END IF;
    IF resolved_intent IS NOT NULL THEN
        SELECT * INTO intent_row FROM public.insurance_reference_maker_intent i
            WHERE i.intent_id=resolved_intent AND i.record_id=p_record AND i.kind=p_kind FOR NO KEY UPDATE;
        IF NOT FOUND THEN RAISE EXCEPTION 'Stored masters reference intent required' USING ERRCODE='42501'; END IF;
        intent_value:=to_jsonb(intent_row);
        SELECT * INTO intent_control_row FROM public.insurance_reference_intent_control c
            WHERE c.intent_id=resolved_intent AND c.record_id=p_record AND c.kind=p_kind FOR NO KEY UPDATE;
        IF NOT FOUND THEN RAISE EXCEPTION 'Stored masters reference intent control required' USING ERRCODE='42501'; END IF;
        intent_control_value:=to_jsonb(intent_control_row);
        IF intent_control_row.approval_id IS NOT NULL THEN
            SELECT * INTO approval_row FROM public.insurance_reference_checker_approval a
                WHERE a.approval_id=intent_control_row.approval_id AND a.intent_id=resolved_intent
                AND a.record_id=p_record AND a.kind=p_kind FOR NO KEY UPDATE;
            IF NOT FOUND THEN RAISE EXCEPTION 'Stored masters reference checker approval required' USING ERRCODE='42501'; END IF;
            approval_value:=to_jsonb(approval_row);
        END IF;
    END IF;
    observed:=pg_catalog.clock_timestamp();bound:=least(p_authority_until,started+INTERVAL '30 seconds');
    IF NOT replay THEN
        IF p_mode IN('APPROVE','PREPARE_EXECUTION','EXECUTE','WITHDRAW_INTENT') THEN
            IF intent_row.valid_until<=observed OR intent_row.recorded_at>observed THEN
                RAISE EXCEPTION 'Unexpired stored masters reference intent required' USING ERRCODE='42501';
            END IF;
            bound:=least(bound,intent_row.valid_until);
            IF p_mode='APPROVE' AND (intent_control_row.lifecycle<>'OPEN' OR intent_control_row.generation<>0
                    OR (intent_row.maker_issuer=p_issuer AND intent_row.maker_subject=p_subject)) THEN
                RAISE EXCEPTION 'Distinct current masters reference checker required' USING ERRCODE='42501';
            END IF;
            IF p_mode='WITHDRAW_INTENT' AND (intent_control_row.lifecycle NOT IN('OPEN','APPROVED')
                    OR intent_row.maker_issuer<>p_issuer OR intent_row.maker_subject<>p_subject) THEN
                RAISE EXCEPTION 'Current original masters reference maker required' USING ERRCODE='42501';
            END IF;
        END IF;
        IF p_mode IN('PREPARE_EXECUTION','EXECUTE') THEN
            IF approval_value IS NULL OR intent_control_row.lifecycle<>'APPROVED' OR intent_control_row.generation<>1
                    OR approval_row.valid_until<=observed OR approval_row.recorded_at>observed
                    OR approval_row.intent_digest<>intent_row.intent_digest
                    OR (approval_row.checker_issuer=intent_row.maker_issuer AND approval_row.checker_subject=intent_row.maker_subject) THEN
                RAISE EXCEPTION 'Current stored masters reference approval required' USING ERRCODE='42501';
            END IF;
            bound:=least(bound,approval_row.valid_until);
            body:=intent_row.canonical_intent::jsonb->'definition';
            IF intent_row.operation='PUBLISH' AND jsonb_typeof(body->'effectiveUntil')<>'null' THEN
                bound:=least(bound,(body->>'effectiveUntil')::timestamptz);
            END IF;
        END IF;
        IF p_mode='SUBMIT_WITHDRAWAL' AND (control_value IS NULL OR control_row.lifecycle<>'ACTIVE') THEN
            RAISE EXCEPTION 'Current published masters reference required for withdrawal draft' USING ERRCODE='42501';
        END IF;
    END IF;
    IF observed<started OR bound<=observed THEN
        RAISE EXCEPTION 'Finite masters reference native context exhausted' USING ERRCODE='42501';
    END IF;
    request_value:=jsonb_build_object('nonce',p_nonce,'recordId',p_record,'kind',p_kind,'actionId',p_action,
        'intentId',p_intent,'mode',p_mode,'issuer',p_issuer,'subject',p_subject,'authorityUntil',p_authority_until);
    INSERT INTO masters_ins_ref_private.context(nonce,xid,backend_pid,db_session_user,record_id,kind,action_id,
        request_intent_id,resolved_intent_id,mode,actor_issuer,actor_subject,authority_until,installed_at,effective_until,
        request_digest,locked,recorded_at,captured_identity,captured_control,captured_intent,captured_intent_control,captured_approval,captured_action)
    VALUES(p_nonce,pg_catalog.pg_current_xact_id(),pg_catalog.pg_backend_pid(),session_user,p_record,p_kind,p_action,
        p_intent,resolved_intent,p_mode,p_issuer,p_subject,p_authority_until,started,bound,
        encode(sha256(convert_to(request_value::text,'UTF8')),'hex'),true,observed,
        identity_value,control_value,intent_value,intent_control_value,approval_value,action_value);
    RETURN QUERY SELECT c.nonce,c.record_id,c.kind,floor(extract(epoch FROM c.recorded_at)*1000)::bigint,c.recorded_at,c.effective_until
        FROM masters_ins_ref_private.context c WHERE c.nonce=p_nonce AND c.xid=pg_catalog.pg_current_xact_id_if_assigned()
        AND c.backend_pid=pg_catalog.pg_backend_pid() AND c.db_session_user=session_user;
END $$;

-- END 04-begin-context.sql
-- BEGIN 05-wire-invariants.sql
CREATE FUNCTION masters_ins_ref_private.v32_closed(p_value JSONB,p_keys TEXT[]) RETURNS BOOLEAN
LANGUAGE sql IMMUTABLE SET search_path=pg_catalog AS $$
SELECT COALESCE(jsonb_typeof(p_value)='object'
    AND ARRAY(SELECT jsonb_object_keys(p_value) ORDER BY 1)=ARRAY(SELECT unnest(p_keys) ORDER BY 1),false) $$;
CREATE FUNCTION masters_ins_ref_private.v32_uuid(p_value JSON) RETURNS BOOLEAN
LANGUAGE sql IMMUTABLE SET search_path=pg_catalog AS $$
SELECT COALESCE(json_typeof(p_value)='string' AND (p_value#>>'{}') ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'
    AND (p_value#>>'{}')<>'00000000-0000-0000-0000-000000000000',false) $$;
CREATE FUNCTION masters_ins_ref_private.v32_integer(p_value JSON,p_min BIGINT,p_max BIGINT) RETURNS BOOLEAN
LANGUAGE plpgsql IMMUTABLE SET search_path=pg_catalog AS $$
DECLARE token TEXT;
BEGIN
    IF p_value IS NULL OR json_typeof(p_value)<>'number' THEN RETURN false; END IF;
    token:=p_value::text;
    IF token !~ '^(0|[1-9][0-9]{0,18})$' THEN RETURN false; END IF;
    RETURN token::numeric BETWEEN p_min AND p_max;
END $$;
CREATE FUNCTION masters_ins_ref_private.v32_text(p_value JSON,p_max INTEGER,p_multiline BOOLEAN) RETURNS BOOLEAN
LANGUAGE plpgsql IMMUTABLE SET search_path=pg_catalog AS $$
DECLARE value TEXT; item TEXT; point INTEGER;
BEGIN
    IF p_value IS NULL OR json_typeof(p_value)<>'string' OR p_max<1 THEN RETURN false; END IF;
    value:=p_value#>>'{}';
    IF value IS NULL OR octet_length(value)>p_max OR value !~ '[^[:space:]]' THEN RETURN false; END IF;
    FOR item IN SELECT unnest(string_to_array(value,NULL)) LOOP
        point:=ascii(item);
        IF point=127 OR (point<32 AND NOT(p_multiline AND point IN(9,10,13)))
                OR (NOT p_multiline AND point IN(133,8232,8233)) THEN RETURN false; END IF;
    END LOOP;
    RETURN true;
END $$;
CREATE FUNCTION masters_ins_ref_private.v32_time(p_value JSON,p_nullable BOOLEAN) RETURNS BOOLEAN
LANGUAGE plpgsql IMMUTABLE SET search_path=pg_catalog AS $$
DECLARE value TEXT; parsed TIMESTAMPTZ;
BEGIN
    IF p_value IS NULL THEN RETURN false; END IF;
    IF json_typeof(p_value)='null' THEN RETURN p_nullable; END IF;
    IF json_typeof(p_value)<>'string' THEN RETURN false; END IF;
    value:=p_value#>>'{}';
    IF value !~ '^[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{6}Z$' THEN RETURN false; END IF;
    parsed:=value::timestamptz;
    RETURN parsed BETWEEN '1900-01-01T00:00:00Z'::timestamptz AND '9999-12-31T23:59:59.999999Z'::timestamptz;
EXCEPTION WHEN invalid_text_representation OR datetime_field_overflow THEN RETURN false;
END $$;
CREATE FUNCTION masters_ins_ref_private.v32_definition_valid(p_original TEXT) RETURNS BOOLEAN
LANGUAGE plpgsql IMMUTABLE SET search_path=pg_catalog AS $$
DECLARE raw JSON; doc JSONB; body JSONB; source JSONB; application JSONB; element JSONB;
BEGIN
    IF p_original IS NULL OR octet_length(p_original) NOT BETWEEN 1 AND 131072
            OR NOT(p_original IS JSON OBJECT WITH UNIQUE KEYS) THEN RETURN false; END IF;
    raw:=p_original::json;doc:=p_original::jsonb;
    IF NOT masters_ins_ref_private.v32_closed(doc,ARRAY['encodingVersion','scope','kind','recordId','publicationId','revision','code','effectiveFrom','effectiveUntil','definition','provenance'])
            OR doc->>'encodingVersion' IS DISTINCT FROM 'MASTERS_INSURANCE_REFERENCE_V1'
            OR doc->>'scope' IS DISTINCT FROM 'GLOBAL_MASTER'
            OR NOT COALESCE(doc->>'kind' IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING'),false)
            OR NOT masters_ins_ref_private.v32_uuid(raw->'recordId') OR NOT masters_ins_ref_private.v32_uuid(raw->'publicationId')
            OR NOT masters_ins_ref_private.v32_integer(raw->'revision',1,9223372036854775807)
            OR jsonb_typeof(doc->'code') IS DISTINCT FROM 'string' OR NOT COALESCE(doc->>'code' ~ '^[A-Z][A-Z0-9_]{0,63}$',false)
            OR NOT masters_ins_ref_private.v32_time(raw->'effectiveFrom',false)
            OR NOT masters_ins_ref_private.v32_time(raw->'effectiveUntil',true) THEN RETURN false; END IF;
    IF jsonb_typeof(doc->'effectiveUntil')<>'null' AND (doc->>'effectiveUntil')::timestamptz<=(doc->>'effectiveFrom')::timestamptz THEN RETURN false; END IF;
    source:=doc->'provenance';body:=doc->'definition';
    IF NOT masters_ins_ref_private.v32_closed(source,ARRAY['origin','sourceReference','versionText','sourceDigest','rightsReference'])
            OR NOT COALESCE(source->>'origin' IN('SANDBOX','CATALOG_SOURCE'),false)
            OR NOT masters_ins_ref_private.v32_text(raw#>'{provenance,sourceReference}',500,false)
            OR NOT masters_ins_ref_private.v32_text(raw#>'{provenance,versionText}',128,false)
            OR NOT masters_ins_ref_private.v32_text(raw#>'{provenance,rightsReference}',500,false)
            OR jsonb_typeof(source->'sourceDigest') IS DISTINCT FROM 'string'
            OR NOT COALESCE(source->>'sourceDigest' ~ '^[0-9a-f]{64}$',false) THEN RETURN false; END IF;
    IF doc->>'kind'='PREMIUM_TAX' THEN
        IF NOT masters_ins_ref_private.v32_closed(body,ARRAY['basis','ratePartsPerMillion','roundingMode','postedScale','currencyPolicy','applicability'])
                OR body->>'basis' IS DISTINCT FROM 'NET_PREMIUM_EXCLUSIVE'
                OR NOT COALESCE(body->>'roundingMode' IN('HALF_UP','HALF_EVEN','DOWN','UP'),false)
                OR NOT masters_ins_ref_private.v32_integer(raw#>'{definition,ratePartsPerMillion}',0,1000000)
                OR NOT masters_ins_ref_private.v32_integer(raw#>'{definition,postedScale}',0,10)
                OR NOT masters_ins_ref_private.v32_closed(body->'currencyPolicy',ARRAY['policyId','currencyId','isoCode','version','digest','encodingVersion'])
                OR NOT masters_ins_ref_private.v32_uuid(raw#>'{definition,currencyPolicy,policyId}')
                OR NOT masters_ins_ref_private.v32_uuid(raw#>'{definition,currencyPolicy,currencyId}')
                OR NOT masters_ins_ref_private.v32_integer(raw#>'{definition,currencyPolicy,version}',1,9223372036854775807)
                OR jsonb_typeof(body#>'{currencyPolicy,isoCode}') IS DISTINCT FROM 'string'
                OR NOT COALESCE(body#>>'{currencyPolicy,isoCode}' ~ '^[A-Z]{3}$',false)
                OR jsonb_typeof(body#>'{currencyPolicy,digest}') IS DISTINCT FROM 'string'
                OR NOT COALESCE(body#>>'{currencyPolicy,digest}' ~ '^[0-9a-f]{64}$',false)
                OR body#>>'{currencyPolicy,encodingVersion}' IS DISTINCT FROM 'MASTERS_CURRENCY_POLICY_V1' THEN RETURN false; END IF;
    ELSE
        IF NOT masters_ins_ref_private.v32_closed(body,ARRAY['localeId','localeCode','mediaType','content','applicability'])
                OR NOT masters_ins_ref_private.v32_uuid(raw#>'{definition,localeId}')
                OR jsonb_typeof(body->'localeCode') IS DISTINCT FROM 'string'
                OR NOT COALESCE(body->>'localeCode' ~ '^[A-Za-z0-9][A-Za-z0-9_-]{0,49}$',false)
                OR body->>'mediaType' IS DISTINCT FROM 'text/plain'
                OR NOT masters_ins_ref_private.v32_text(raw#>'{definition,content}',65536,true) THEN RETURN false; END IF;
    END IF;
    application:=body->'applicability';
    IF NOT masters_ins_ref_private.v32_closed(application,ARRAY['jurisdictions','productProfiles','events'])
            OR jsonb_typeof(application->'jurisdictions') IS DISTINCT FROM 'array'
            OR jsonb_array_length(application->'jurisdictions') NOT BETWEEN 1 AND 64
            OR application->'productProfiles' IS DISTINCT FROM '["TERM_PC_INSURANCE_V1"]'::jsonb
            OR jsonb_typeof(application->'events') IS DISTINCT FROM 'array'
            OR jsonb_array_length(application->'events') NOT BETWEEN 1 AND 4 THEN RETURN false; END IF;
    FOR element IN SELECT jsonb_array_elements(application->'jurisdictions') LOOP
        IF jsonb_typeof(element) IS DISTINCT FROM 'string' OR NOT COALESCE(element#>>'{}' ~ '^[A-Z]{2}(-[A-Z0-9]{1,8})?$',false) THEN RETURN false; END IF;
    END LOOP;
    FOR element IN SELECT jsonb_array_elements(application->'events') LOOP
        IF jsonb_typeof(element) IS DISTINCT FROM 'string' OR NOT COALESCE(element#>>'{}' IN('NEW_BUSINESS','RENEWAL','ENDORSEMENT','CANCELLATION_REFUND'),false) THEN RETURN false; END IF;
    END LOOP;
    IF (SELECT count(DISTINCT value) FROM jsonb_array_elements(application->'jurisdictions'))<>jsonb_array_length(application->'jurisdictions')
            OR (SELECT count(DISTINCT value) FROM jsonb_array_elements(application->'events'))<>jsonb_array_length(application->'events')
            OR (doc->>'kind'='PREMIUM_TAX' AND application->'events' IS DISTINCT FROM '["NEW_BUSINESS"]'::jsonb) THEN RETURN false; END IF;
    RETURN true;
EXCEPTION WHEN invalid_text_representation OR datetime_field_overflow OR numeric_value_out_of_range OR invalid_parameter_value THEN RETURN false;
END $$;
CREATE FUNCTION masters_ins_ref_private.v32_intent_valid(p_original TEXT) RETURNS BOOLEAN
LANGUAGE plpgsql IMMUTABLE SET search_path=pg_catalog AS $$
DECLARE raw JSON; doc JSONB; expected JSONB;
BEGIN
    IF p_original IS NULL OR octet_length(p_original) NOT BETWEEN 1 AND 150000
            OR NOT(p_original IS JSON OBJECT WITH UNIQUE KEYS) THEN RETURN false; END IF;
    raw:=p_original::json;doc:=p_original::jsonb;expected:=doc->'expected';
    IF NOT masters_ins_ref_private.v32_closed(doc,ARRAY['encodingVersion','operation','definition','expected'])
            OR doc->>'encodingVersion' IS DISTINCT FROM 'MASTERS_INSURANCE_REFERENCE_INTENT_V1'
            OR NOT COALESCE(doc->>'operation' IN('PUBLISH','WITHDRAW'),false)
            OR NOT masters_ins_ref_private.v32_closed(expected,ARRAY['publicationId','revision','generation'])
            OR NOT masters_ins_ref_private.v32_integer(raw#>'{expected,revision}',0,9223372036854775807)
            OR NOT masters_ins_ref_private.v32_integer(raw#>'{expected,generation}',0,9223372036854775807)
            OR NOT masters_ins_ref_private.v32_definition_valid((raw->'definition')::text) THEN RETURN false; END IF;
    IF jsonb_typeof(expected->'publicationId')='null' THEN
        RETURN expected->'revision'='0'::jsonb AND expected->'generation'='0'::jsonb AND doc->>'operation'='PUBLISH';
    END IF;
    RETURN masters_ins_ref_private.v32_uuid(raw#>'{expected,publicationId}') AND (expected->>'revision')::bigint>0;
EXCEPTION WHEN invalid_text_representation OR numeric_value_out_of_range OR invalid_parameter_value THEN RETURN false;
END $$;

-- END 05-wire-invariants.sql
-- BEGIN 06-mutation-witness.sql
CREATE FUNCTION masters_ins_ref_private.v32_before_row() RETURNS TRIGGER
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_ins_ref_private.context%ROWTYPE; new_value JSONB; old_value JSONB; body JSONB; allowed BOOLEAN:=false;
BEGIN
    c:=masters_ins_ref_private.v32_current_context();
    IF TG_TABLE_SCHEMA<>'public' OR session_user<>'masters_ins_ref_writer' OR TG_OP='DELETE'
            OR c.captured_action IS NOT NULL OR c.mode IN('PREPARE_EXECUTION','RECOVER') THEN
        RAISE EXCEPTION 'New scoped masters reference mutation required' USING ERRCODE='42501';
    END IF;
    new_value:=to_jsonb(NEW);
    IF new_value->>'catalog_scope' IS DISTINCT FROM 'GLOBAL_MASTER'
            OR new_value->>'record_id' IS DISTINCT FROM c.record_id::text
            OR new_value->>'kind' IS DISTINCT FROM c.kind THEN
        RAISE EXCEPTION 'Exact masters reference mutation scope required' USING ERRCODE='42501';
    END IF;
    IF TG_OP='UPDATE' THEN old_value:=to_jsonb(OLD); END IF;
    IF TG_TABLE_NAME NOT IN('insurance_reference_control','insurance_reference_intent_control') AND TG_OP<>'INSERT' THEN
        RAISE EXCEPTION 'Masters reference journal and definitions are append-only' USING ERRCODE='42501';
    END IF;
    CASE TG_TABLE_NAME
    WHEN 'insurance_reference_identity' THEN
        allowed:=c.mode='SUBMIT_PUBLICATION' AND TG_OP='INSERT' AND c.captured_identity IS NULL;
    WHEN 'insurance_reference_maker_intent' THEN
        allowed:=c.mode IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL') AND TG_OP='INSERT'
            AND NEW.maker_issuer=c.actor_issuer AND NEW.maker_subject=c.actor_subject
            AND masters_ins_ref_private.v32_intent_valid(NEW.canonical_intent);
        IF allowed THEN
            body:=NEW.canonical_intent::jsonb;
            allowed:=body->>'operation'=NEW.operation AND body#>>'{definition,recordId}'=c.record_id::text
                AND body#>>'{definition,kind}'=c.kind
                AND (body#>>'{expected,publicationId}')::uuid IS NOT DISTINCT FROM NEW.expected_publication_id
                AND (body#>>'{expected,revision}')::bigint=NEW.expected_revision
                AND (body#>>'{expected,generation}')::bigint=NEW.expected_generation
                AND ((c.mode='SUBMIT_PUBLICATION' AND NEW.operation='PUBLISH') OR (c.mode='SUBMIT_WITHDRAWAL' AND NEW.operation='WITHDRAW'));
        END IF;
    WHEN 'insurance_reference_checker_approval' THEN
        allowed:=c.mode='APPROVE' AND TG_OP='INSERT' AND NEW.intent_id=c.resolved_intent_id
            AND NEW.checker_issuer=c.actor_issuer AND NEW.checker_subject=c.actor_subject
            AND NEW.intent_digest=c.captured_intent->>'intent_digest'
            AND NEW.expected_intent_generation=0
            AND NEW.valid_until<=(c.captured_intent->>'valid_until')::timestamptz
            AND NOT(NEW.checker_issuer=c.captured_intent->>'maker_issuer' AND NEW.checker_subject=c.captured_intent->>'maker_subject');
    WHEN 'insurance_reference_revision' THEN
        allowed:=c.mode='EXECUTE' AND TG_OP='INSERT' AND c.captured_intent->>'operation'='PUBLISH'
            AND NEW.publication_action_id=c.action_id AND masters_ins_ref_private.v32_definition_valid(NEW.canonical_definition);
        IF allowed THEN
            body:=NEW.canonical_definition::jsonb;
            allowed:=body=(c.captured_intent->>'canonical_intent')::jsonb->'definition'
                AND body->>'publicationId'=NEW.publication_id::text AND (body->>'revision')::bigint=NEW.revision
                AND body->>'recordId'=c.record_id::text AND body->>'kind'=c.kind
                AND (body->>'effectiveFrom')::timestamptz=NEW.effective_from
                AND (body->>'effectiveUntil')::timestamptz IS NOT DISTINCT FROM NEW.effective_until;
        END IF;
    WHEN 'insurance_reference_control' THEN
        allowed:=c.mode='EXECUTE';
        IF TG_OP='INSERT' THEN
            allowed:=allowed AND c.captured_control IS NULL AND NEW.generation=0 AND NEW.lifecycle='ACTIVE'
                AND c.captured_intent->>'operation'='PUBLISH';
        ELSE
            IF old_value IS DISTINCT FROM c.captured_control THEN
                RAISE EXCEPTION 'Exact masters reference head before-image required' USING ERRCODE='42501';
            END IF;
            allowed:=allowed AND (new_value-'publication_id'-'revision'-'generation'-'lifecycle')=(old_value-'publication_id'-'revision'-'generation'-'lifecycle')
                AND OLD.generation<9223372036854775807 AND NEW.generation=OLD.generation+1;
            IF c.captured_intent->>'operation'='PUBLISH' THEN
                allowed:=allowed AND OLD.revision<9223372036854775807 AND NEW.revision=OLD.revision+1
                    AND NEW.publication_id<>OLD.publication_id AND NEW.lifecycle='ACTIVE';
            ELSE
                allowed:=allowed AND c.captured_intent->>'operation'='WITHDRAW' AND OLD.lifecycle='ACTIVE'
                    AND NEW.publication_id=OLD.publication_id AND NEW.revision=OLD.revision AND NEW.lifecycle='WITHDRAWN';
            END IF;
        END IF;
    WHEN 'insurance_reference_intent_control' THEN
        IF TG_OP='INSERT' THEN
            allowed:=c.mode IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL') AND NEW.generation=0 AND NEW.lifecycle='OPEN'
                AND NEW.approval_id IS NULL AND NEW.execution_action_id IS NULL;
        ELSE
            IF old_value IS DISTINCT FROM c.captured_intent_control THEN
                RAISE EXCEPTION 'Exact masters intent before-image required' USING ERRCODE='42501';
            END IF;
            allowed:=NEW.intent_id=c.resolved_intent_id AND OLD.generation<9223372036854775807 AND NEW.generation=OLD.generation+1
                AND (new_value-'generation'-'lifecycle'-'approval_id'-'execution_action_id')=(old_value-'generation'-'lifecycle'-'approval_id'-'execution_action_id');
            CASE c.mode
            WHEN 'APPROVE' THEN allowed:=allowed AND OLD.lifecycle='OPEN' AND NEW.lifecycle='APPROVED'
                AND OLD.approval_id IS NULL AND NEW.approval_id IS NOT NULL AND NEW.execution_action_id IS NULL;
            WHEN 'EXECUTE' THEN allowed:=allowed AND OLD.lifecycle='APPROVED' AND NEW.lifecycle='EXECUTED'
                AND NEW.approval_id=OLD.approval_id AND NEW.execution_action_id=c.action_id;
            WHEN 'WITHDRAW_INTENT' THEN allowed:=allowed AND OLD.lifecycle IN('OPEN','APPROVED') AND NEW.lifecycle='WITHDRAWN'
                AND NEW.approval_id IS NOT DISTINCT FROM OLD.approval_id AND NEW.execution_action_id IS NULL;
            ELSE allowed:=false;
            END CASE;
        END IF;
    WHEN 'insurance_reference_action' THEN
        allowed:=TG_OP='INSERT' AND NEW.action_id=c.action_id AND NEW.operation=c.mode
            AND (c.resolved_intent_id IS NULL OR NEW.intent_id=c.resolved_intent_id);
    WHEN 'insurance_reference_audit' THEN
        allowed:=TG_OP='INSERT' AND NEW.action_id=c.action_id AND NEW.event_kind=c.mode
            AND NEW.actor_issuer=c.actor_issuer AND NEW.actor_subject=c.actor_subject;
    WHEN 'insurance_reference_outbox' THEN
        allowed:=TG_OP='INSERT' AND NEW.action_id=c.action_id AND NEW.event_kind=c.mode;
    ELSE allowed:=false;
    END CASE;
    IF allowed IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Exact masters reference transition required' USING ERRCODE='42501';
    END IF;
    IF TG_TABLE_NAME IN('insurance_reference_revision','insurance_reference_maker_intent','insurance_reference_checker_approval',
            'insurance_reference_action','insurance_reference_audit','insurance_reference_outbox') THEN
        NEW.recorded_at:=pg_catalog.clock_timestamp();
        IF NEW.recorded_at<c.recorded_at OR NEW.recorded_at>=c.effective_until THEN
            RAISE EXCEPTION 'Native masters reference mutation horizon exhausted' USING ERRCODE='42501';
        END IF;
    END IF;
    RETURN NEW;
END $$;

CREATE FUNCTION masters_ins_ref_private.v32_after_row() RETURNS TRIGGER
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_ins_ref_private.context%ROWTYPE;
BEGIN
    c:=masters_ins_ref_private.v32_current_context();
    IF TG_TABLE_SCHEMA<>'public' OR TG_OP NOT IN('INSERT','UPDATE') OR session_user<>'masters_ins_ref_writer' THEN
        RAISE EXCEPTION 'Exact masters reference affected row required' USING ERRCODE='42501';
    END IF;
    INSERT INTO masters_ins_ref_private.row_witness(context_nonce,relation_name,operation,old_row,new_row)
    VALUES(c.nonce,TG_TABLE_NAME,TG_OP,CASE WHEN TG_OP='UPDATE' THEN to_jsonb(OLD) ELSE NULL END,to_jsonb(NEW));
    RETURN NULL;
END $$;
CREATE TRIGGER insurance_ref_before_identity BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_reference_identity
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_before_row();
CREATE TRIGGER insurance_ref_after_identity AFTER INSERT OR UPDATE ON public.insurance_reference_identity
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_after_row();
CREATE TRIGGER insurance_ref_before_revision BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_reference_revision
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_before_row();
CREATE TRIGGER insurance_ref_after_revision AFTER INSERT OR UPDATE ON public.insurance_reference_revision
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_after_row();
CREATE TRIGGER insurance_ref_before_control BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_reference_control
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_before_row();
CREATE TRIGGER insurance_ref_after_control AFTER INSERT OR UPDATE ON public.insurance_reference_control
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_after_row();
CREATE TRIGGER insurance_ref_before_maker_intent BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_reference_maker_intent
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_before_row();
CREATE TRIGGER insurance_ref_after_maker_intent AFTER INSERT OR UPDATE ON public.insurance_reference_maker_intent
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_after_row();
CREATE TRIGGER insurance_ref_before_intent_control BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_reference_intent_control
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_before_row();
CREATE TRIGGER insurance_ref_after_intent_control AFTER INSERT OR UPDATE ON public.insurance_reference_intent_control
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_after_row();
CREATE TRIGGER insurance_ref_before_checker_approval BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_reference_checker_approval
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_before_row();
CREATE TRIGGER insurance_ref_after_checker_approval AFTER INSERT OR UPDATE ON public.insurance_reference_checker_approval
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_after_row();
CREATE TRIGGER insurance_ref_before_action BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_reference_action
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_before_row();
CREATE TRIGGER insurance_ref_after_action AFTER INSERT OR UPDATE ON public.insurance_reference_action
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_after_row();
CREATE TRIGGER insurance_ref_before_audit BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_reference_audit
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_before_row();
CREATE TRIGGER insurance_ref_after_audit AFTER INSERT OR UPDATE ON public.insurance_reference_audit
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_after_row();
CREATE TRIGGER insurance_ref_before_outbox BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_reference_outbox
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_before_row();
CREATE TRIGGER insurance_ref_after_outbox AFTER INSERT OR UPDATE ON public.insurance_reference_outbox
    FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_after_row();

-- END 06-mutation-witness.sql
-- BEGIN 07-completion.sql
CREATE FUNCTION masters_ins_ref_private.v32_head_matches(p_control JSONB,p_expected JSONB) RETURNS BOOLEAN
LANGUAGE sql IMMUTABLE SET search_path=pg_catalog AS $$
SELECT CASE WHEN p_control IS NULL THEN
    p_expected->'publicationId'='null'::jsonb AND p_expected->'revision'='0'::jsonb AND p_expected->'generation'='0'::jsonb
ELSE p_control->>'publication_id'=p_expected->>'publicationId'
    AND p_control->'revision'=p_expected->'revision' AND p_control->'generation'=p_expected->'generation' END $$;

CREATE FUNCTION public.masters_ins_ref_complete(p_nonce UUID) RETURNS BOOLEAN
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_ins_ref_private.context%ROWTYPE; actual_relations TEXT[]; expected_relations TEXT[];
    identity_row public.insurance_reference_identity%ROWTYPE; intent_row public.insurance_reference_maker_intent%ROWTYPE;
    intent_control_row public.insurance_reference_intent_control%ROWTYPE; approval_row public.insurance_reference_checker_approval%ROWTYPE;
    action_row public.insurance_reference_action%ROWTYPE; audit_row public.insurance_reference_audit%ROWTYPE;
    outbox_row public.insurance_reference_outbox%ROWTYPE; revision_row public.insurance_reference_revision%ROWTYPE;
    control_row public.insurance_reference_control%ROWTYPE; body JSONB; definition JSONB; chosen_intent UUID;
    read_or_replay BOOLEAN; expected_generation BIGINT; native_now TIMESTAMPTZ;
BEGIN
    c:=masters_ins_ref_private.v32_current_context();
    IF c.nonce IS DISTINCT FROM p_nonce THEN RAISE EXCEPTION 'Exact masters reference completion context required' USING ERRCODE='42501'; END IF;
    SELECT COALESCE(array_agg(w.relation_name ORDER BY w.relation_name),ARRAY[]::text[]) INTO actual_relations
        FROM masters_ins_ref_private.row_witness w WHERE w.context_nonce=c.nonce;
    read_or_replay:=c.mode IN('PREPARE_EXECUTION','RECOVER') OR c.captured_action IS NOT NULL;
    IF read_or_replay AND cardinality(actual_relations)<>0 THEN
        RAISE EXCEPTION 'Masters reference read/recovery cannot create a mutation' USING ERRCODE='42501';
    END IF;
    SELECT * INTO action_row FROM public.insurance_reference_action a WHERE a.action_id=c.action_id AND a.record_id=c.record_id AND a.kind=c.kind;
    IF NOT FOUND AND (c.mode<>'PREPARE_EXECUTION' OR c.captured_action IS NOT NULL) THEN
        RAISE EXCEPTION 'Stored masters reference action required' USING ERRCODE='42501';
    END IF;
    chosen_intent:=COALESCE(action_row.intent_id,c.resolved_intent_id);
    SELECT * INTO intent_row FROM public.insurance_reference_maker_intent i WHERE i.intent_id=chosen_intent AND i.record_id=c.record_id AND i.kind=c.kind;
    IF NOT FOUND OR NOT masters_ins_ref_private.v32_intent_valid(intent_row.canonical_intent) THEN
        RAISE EXCEPTION 'Exact stored masters reference intent required' USING ERRCODE='42501';
    END IF;
    body:=intent_row.canonical_intent::jsonb;definition:=body->'definition';
    SELECT * INTO identity_row FROM public.insurance_reference_identity i WHERE i.record_id=c.record_id AND i.kind=c.kind;
    IF NOT FOUND OR identity_row.code<>definition->>'code' OR definition->>'recordId'<>c.record_id::text
            OR definition->>'kind'<>c.kind OR body->>'operation'<>intent_row.operation
            OR (body#>>'{expected,publicationId}')::uuid IS DISTINCT FROM intent_row.expected_publication_id
            OR (body#>>'{expected,revision}')::bigint<>intent_row.expected_revision
            OR (body#>>'{expected,generation}')::bigint<>intent_row.expected_generation THEN
        RAISE EXCEPTION 'Exact immutable masters reference identity/definition required' USING ERRCODE='42501';
    END IF;
    SELECT * INTO intent_control_row FROM public.insurance_reference_intent_control i
        WHERE i.intent_id=chosen_intent AND i.record_id=c.record_id AND i.kind=c.kind;
    IF NOT FOUND THEN RAISE EXCEPTION 'Stored masters intent control required' USING ERRCODE='42501'; END IF;
    IF c.captured_intent IS NOT NULL AND to_jsonb(intent_row) IS DISTINCT FROM c.captured_intent THEN
        RAISE EXCEPTION 'Stored masters reference intent is immutable' USING ERRCODE='42501';
    END IF;
    IF intent_control_row.approval_id IS NOT NULL THEN
        SELECT * INTO approval_row FROM public.insurance_reference_checker_approval a
            WHERE a.approval_id=intent_control_row.approval_id AND a.intent_id=chosen_intent AND a.record_id=c.record_id AND a.kind=c.kind;
        IF NOT FOUND OR approval_row.intent_digest<>intent_row.intent_digest OR approval_row.expected_intent_generation<>0
                OR approval_row.valid_until>intent_row.valid_until OR approval_row.recorded_at<intent_row.recorded_at
                OR (approval_row.checker_issuer=intent_row.maker_issuer AND approval_row.checker_subject=intent_row.maker_subject) THEN
            RAISE EXCEPTION 'Exact stored distinct masters checker approval required' USING ERRCODE='42501';
        END IF;
        IF c.captured_approval IS NOT NULL AND to_jsonb(approval_row) IS DISTINCT FROM c.captured_approval THEN
            RAISE EXCEPTION 'Stored masters checker approval is immutable' USING ERRCODE='42501';
        END IF;
    END IF;
    IF c.mode='PREPARE_EXECUTION' AND c.captured_action IS NULL THEN
        IF intent_control_row.lifecycle<>'APPROVED' OR intent_control_row.generation<>1
                OR NOT COALESCE(masters_ins_ref_private.v32_head_matches(c.captured_control,body->'expected'),false)
                OR approval_row.approval_id IS NULL THEN
            RAISE EXCEPTION 'Current approved masters reference preparation required' USING ERRCODE='42501';
        END IF;
        PERFORM masters_ins_ref_private.v32_current_context();
        RETURN true;
    END IF;
    SELECT * INTO audit_row FROM public.insurance_reference_audit a WHERE a.action_id=c.action_id AND a.record_id=c.record_id AND a.kind=c.kind;
    IF NOT FOUND THEN RAISE EXCEPTION 'Stored masters reference audit required' USING ERRCODE='42501'; END IF;
    SELECT * INTO outbox_row FROM public.insurance_reference_outbox o WHERE o.action_id=c.action_id AND o.record_id=c.record_id AND o.kind=c.kind;
    IF NOT FOUND OR action_row.intent_digest<>intent_row.intent_digest
            OR action_row.publication_id::text<>definition->>'publicationId' OR action_row.revision<>(definition->>'revision')::bigint
            OR action_row.recorded_at<intent_row.recorded_at OR audit_row.recorded_at<action_row.recorded_at
            OR outbox_row.recorded_at<audit_row.recorded_at OR outbox_row.event_id<>audit_row.event_id
            OR outbox_row.event_kind<>action_row.operation OR audit_row.event_kind<>action_row.operation
            OR audit_row.actor_issuer<>c.actor_issuer OR audit_row.actor_subject<>c.actor_subject
            OR (action_row.operation IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL','WITHDRAW_INTENT')
                AND (audit_row.actor_issuer<>intent_row.maker_issuer OR audit_row.actor_subject<>intent_row.maker_subject))
            OR (action_row.operation='APPROVE' AND (approval_row.approval_id IS NULL
                OR audit_row.actor_issuer<>approval_row.checker_issuer OR audit_row.actor_subject<>approval_row.checker_subject))
            OR intent_control_row.generation<action_row.result_generation
            OR (action_row.operation='SUBMIT_PUBLICATION' AND intent_row.operation<>'PUBLISH')
            OR (action_row.operation='SUBMIT_WITHDRAWAL' AND intent_row.operation<>'WITHDRAW')
            OR (action_row.operation='EXECUTE' AND (intent_control_row.lifecycle<>'EXECUTED'
                OR intent_control_row.execution_action_id IS DISTINCT FROM action_row.action_id OR approval_row.approval_id IS NULL))
            OR (action_row.operation='WITHDRAW_INTENT' AND (intent_control_row.lifecycle<>'WITHDRAWN'
                OR intent_control_row.generation<>action_row.result_generation)) THEN
        RAISE EXCEPTION 'Complete scoped masters reference receipt graph required' USING ERRCODE='42501';
    END IF;
    SELECT * INTO control_row FROM public.insurance_reference_control r WHERE r.record_id=c.record_id AND r.kind=c.kind;
    IF action_row.operation='EXECUTE' THEN
        expected_generation:=CASE WHEN intent_row.expected_publication_id IS NULL THEN 0 ELSE intent_row.expected_generation+1 END;
        IF control_row.record_id IS NULL OR control_row.generation<expected_generation OR control_row.revision<action_row.revision THEN
            RAISE EXCEPTION 'Stored masters reference execution head required' USING ERRCODE='42501';
        END IF;
        SELECT * INTO revision_row FROM public.insurance_reference_revision r WHERE r.record_id=c.record_id AND r.kind=c.kind
            AND r.publication_id=action_row.publication_id AND r.revision=action_row.revision;
        IF NOT FOUND OR revision_row.canonical_definition::jsonb IS DISTINCT FROM definition
                OR (intent_row.operation='PUBLISH' AND revision_row.publication_action_id<>action_row.action_id)
                OR (intent_row.operation='WITHDRAW' AND control_row.publication_id=action_row.publication_id AND control_row.lifecycle<>'WITHDRAWN') THEN
            RAISE EXCEPTION 'Immutable masters reference execution publication required' USING ERRCODE='42501';
        END IF;
    END IF;
    IF read_or_replay THEN
        PERFORM masters_ins_ref_private.v32_current_context();
        RETURN true;
    END IF;
    IF action_row.operation<>c.mode OR NOT COALESCE(masters_ins_ref_private.v32_head_matches(c.captured_control,body->'expected'),false) THEN
        RAISE EXCEPTION 'Current exact masters reference head required' USING ERRCODE='42501';
    END IF;
    expected_relations:=ARRAY['insurance_reference_action','insurance_reference_audit','insurance_reference_outbox'];
    CASE c.mode
    WHEN 'SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL' THEN
        expected_relations:=expected_relations||ARRAY['insurance_reference_maker_intent','insurance_reference_intent_control'];
        IF c.captured_identity IS NULL THEN
            IF c.mode<>'SUBMIT_PUBLICATION' THEN RAISE EXCEPTION 'Existing masters reference withdrawal identity required' USING ERRCODE='42501'; END IF;
            expected_relations:=array_append(expected_relations,'insurance_reference_identity');
        END IF;
        IF intent_control_row.lifecycle<>'OPEN' OR intent_control_row.generation<>0 OR action_row.result_generation<>0 THEN
            RAISE EXCEPTION 'New masters reference maker intent required' USING ERRCODE='42501';
        END IF;
        IF intent_row.operation='PUBLISH' THEN
            IF action_row.revision<>intent_row.expected_revision+1 OR action_row.publication_id IS NOT DISTINCT FROM intent_row.expected_publication_id THEN
                RAISE EXCEPTION 'Next immutable masters publication identity required' USING ERRCODE='42501';
            END IF;
        ELSE
            SELECT * INTO revision_row FROM public.insurance_reference_revision r WHERE r.publication_id=intent_row.expected_publication_id
                AND r.record_id=c.record_id AND r.kind=c.kind AND r.revision=intent_row.expected_revision;
            IF NOT FOUND OR c.captured_control->>'lifecycle'<>'ACTIVE' OR revision_row.canonical_definition::jsonb IS DISTINCT FROM definition THEN
                RAISE EXCEPTION 'Exact published masters definition for withdrawal required' USING ERRCODE='42501';
            END IF;
        END IF;
    WHEN 'APPROVE' THEN
        expected_relations:=expected_relations||ARRAY['insurance_reference_checker_approval','insurance_reference_intent_control'];
        IF intent_control_row.lifecycle<>'APPROVED' OR intent_control_row.generation<>1 OR action_row.result_generation<>1
                OR approval_row.checker_issuer<>c.actor_issuer OR approval_row.checker_subject<>c.actor_subject THEN
            RAISE EXCEPTION 'Stored masters approval transition required' USING ERRCODE='42501';
        END IF;
    WHEN 'EXECUTE' THEN
        expected_relations:=expected_relations||ARRAY['insurance_reference_control','insurance_reference_intent_control'];
        IF intent_row.operation='PUBLISH' THEN expected_relations:=array_append(expected_relations,'insurance_reference_revision'); END IF;
        IF control_row.generation<>expected_generation OR control_row.publication_id<>action_row.publication_id OR control_row.revision<>action_row.revision
                OR control_row.lifecycle<>(CASE WHEN intent_row.operation='PUBLISH' THEN 'ACTIVE' ELSE 'WITHDRAWN' END) THEN
            RAISE EXCEPTION 'Exact executed masters reference head required' USING ERRCODE='42501';
        END IF;
    WHEN 'WITHDRAW_INTENT' THEN expected_relations:=array_append(expected_relations,'insurance_reference_intent_control');
    ELSE RAISE EXCEPTION 'Masters reference mutation mode unavailable' USING ERRCODE='42501';
    END CASE;
    SELECT array_agg(x ORDER BY x) INTO expected_relations FROM unnest(expected_relations) x;
    IF actual_relations IS DISTINCT FROM expected_relations THEN
        RAISE EXCEPTION 'Exact masters reference affected-row graph required' USING ERRCODE='42501';
    END IF;
    IF EXISTS(SELECT 1 FROM masters_ins_ref_private.row_witness w WHERE w.context_nonce=c.nonce
            AND ((w.new_row->>'record_id')::uuid<>c.record_id OR w.new_row->>'kind'<>c.kind
                OR w.recorded_at<c.recorded_at OR w.recorded_at>=c.effective_until
                OR (w.relation_name NOT IN('insurance_reference_control','insurance_reference_intent_control') AND w.operation<>'INSERT')
                OR (w.relation_name='insurance_reference_intent_control' AND w.operation<>CASE WHEN c.mode IN('SUBMIT_PUBLICATION','SUBMIT_WITHDRAWAL') THEN 'INSERT' ELSE 'UPDATE' END)
                OR (w.relation_name='insurance_reference_control' AND w.operation<>CASE WHEN c.captured_control IS NULL THEN 'INSERT' ELSE 'UPDATE' END))) THEN
        RAISE EXCEPTION 'Exact native masters affected-row witnesses required' USING ERRCODE='42501';
    END IF;
    native_now:=pg_catalog.clock_timestamp();
    IF native_now<c.recorded_at OR native_now>=c.effective_until OR native_now>=intent_row.valid_until
            OR (c.mode='EXECUTE' AND native_now>=approval_row.valid_until) THEN
        RAISE EXCEPTION 'Masters reference native completion horizon exhausted' USING ERRCODE='42501';
    END IF;
    RETURN true;
END $$;

CREATE FUNCTION masters_ins_ref_private.v32_deferred_complete() RETURNS TRIGGER
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
    PERFORM public.masters_ins_ref_complete(CASE WHEN TG_TABLE_NAME='context' THEN NEW.nonce ELSE NEW.context_nonce END);
    RETURN NULL;
END $$;
CREATE CONSTRAINT TRIGGER insurance_ref_context_complete AFTER INSERT ON masters_ins_ref_private.context
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_deferred_complete();
CREATE CONSTRAINT TRIGGER insurance_ref_witness_complete AFTER INSERT ON masters_ins_ref_private.row_witness
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v32_deferred_complete();

-- END 07-completion.sql
-- BEGIN 08-ownership-rls-acl.sql
-- Table DDL/ACLs run as their creator before ownership transfer; function ACLs run as guard.
RESET ROLE;
GRANT CREATE ON SCHEMA public TO masters_ins_ref_owner;
ALTER TABLE public.insurance_reference_identity ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_reference_identity FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_reference_identity FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT SELECT ON public.insurance_reference_identity TO masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT INSERT ON public.insurance_reference_identity TO masters_ins_ref_writer;
GRANT UPDATE(record_id) ON public.insurance_reference_identity TO masters_ins_ref_guard;
CREATE POLICY insurance_ref_read_identity ON public.insurance_reference_identity FOR SELECT TO masters_ins_ref_writer,masters_ins_ref_reader
    USING(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_append_identity ON public.insurance_reference_identity FOR INSERT TO masters_ins_ref_writer
    WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_integrity_identity ON public.insurance_reference_identity TO masters_ins_ref_guard USING(true) WITH CHECK(true);
ALTER TABLE public.insurance_reference_revision ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_reference_revision FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_reference_revision FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT SELECT ON public.insurance_reference_revision TO masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT INSERT ON public.insurance_reference_revision TO masters_ins_ref_writer;
GRANT UPDATE(record_id) ON public.insurance_reference_revision TO masters_ins_ref_guard;
CREATE POLICY insurance_ref_read_revision ON public.insurance_reference_revision FOR SELECT TO masters_ins_ref_writer,masters_ins_ref_reader
    USING(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_append_revision ON public.insurance_reference_revision FOR INSERT TO masters_ins_ref_writer
    WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_integrity_revision ON public.insurance_reference_revision TO masters_ins_ref_guard USING(true) WITH CHECK(true);
ALTER TABLE public.insurance_reference_control ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_reference_control FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_reference_control FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT SELECT ON public.insurance_reference_control TO masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT INSERT ON public.insurance_reference_control TO masters_ins_ref_writer;
GRANT UPDATE(record_id) ON public.insurance_reference_control TO masters_ins_ref_guard;
CREATE POLICY insurance_ref_read_control ON public.insurance_reference_control FOR SELECT TO masters_ins_ref_writer,masters_ins_ref_reader
    USING(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_append_control ON public.insurance_reference_control FOR INSERT TO masters_ins_ref_writer
    WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_integrity_control ON public.insurance_reference_control TO masters_ins_ref_guard USING(true) WITH CHECK(true);
ALTER TABLE public.insurance_reference_maker_intent ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_reference_maker_intent FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_reference_maker_intent FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT SELECT ON public.insurance_reference_maker_intent TO masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT INSERT ON public.insurance_reference_maker_intent TO masters_ins_ref_writer;
GRANT UPDATE(record_id) ON public.insurance_reference_maker_intent TO masters_ins_ref_guard;
CREATE POLICY insurance_ref_read_maker_intent ON public.insurance_reference_maker_intent FOR SELECT TO masters_ins_ref_writer,masters_ins_ref_reader
    USING(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_append_maker_intent ON public.insurance_reference_maker_intent FOR INSERT TO masters_ins_ref_writer
    WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_integrity_maker_intent ON public.insurance_reference_maker_intent TO masters_ins_ref_guard USING(true) WITH CHECK(true);
ALTER TABLE public.insurance_reference_intent_control ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_reference_intent_control FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_reference_intent_control FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT SELECT ON public.insurance_reference_intent_control TO masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT INSERT ON public.insurance_reference_intent_control TO masters_ins_ref_writer;
GRANT UPDATE(record_id) ON public.insurance_reference_intent_control TO masters_ins_ref_guard;
CREATE POLICY insurance_ref_read_intent_control ON public.insurance_reference_intent_control FOR SELECT TO masters_ins_ref_writer,masters_ins_ref_reader
    USING(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_append_intent_control ON public.insurance_reference_intent_control FOR INSERT TO masters_ins_ref_writer
    WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_integrity_intent_control ON public.insurance_reference_intent_control TO masters_ins_ref_guard USING(true) WITH CHECK(true);
ALTER TABLE public.insurance_reference_checker_approval ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_reference_checker_approval FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_reference_checker_approval FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT SELECT ON public.insurance_reference_checker_approval TO masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT INSERT ON public.insurance_reference_checker_approval TO masters_ins_ref_writer;
GRANT UPDATE(record_id) ON public.insurance_reference_checker_approval TO masters_ins_ref_guard;
CREATE POLICY insurance_ref_read_checker_approval ON public.insurance_reference_checker_approval FOR SELECT TO masters_ins_ref_writer,masters_ins_ref_reader
    USING(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_append_checker_approval ON public.insurance_reference_checker_approval FOR INSERT TO masters_ins_ref_writer
    WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_integrity_checker_approval ON public.insurance_reference_checker_approval TO masters_ins_ref_guard USING(true) WITH CHECK(true);
ALTER TABLE public.insurance_reference_action ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_reference_action FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_reference_action FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT SELECT ON public.insurance_reference_action TO masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT INSERT ON public.insurance_reference_action TO masters_ins_ref_writer;
GRANT UPDATE(record_id) ON public.insurance_reference_action TO masters_ins_ref_guard;
CREATE POLICY insurance_ref_read_action ON public.insurance_reference_action FOR SELECT TO masters_ins_ref_writer,masters_ins_ref_reader
    USING(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_append_action ON public.insurance_reference_action FOR INSERT TO masters_ins_ref_writer
    WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_integrity_action ON public.insurance_reference_action TO masters_ins_ref_guard USING(true) WITH CHECK(true);
ALTER TABLE public.insurance_reference_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_reference_audit FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_reference_audit FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT SELECT ON public.insurance_reference_audit TO masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT INSERT ON public.insurance_reference_audit TO masters_ins_ref_writer;
GRANT UPDATE(record_id) ON public.insurance_reference_audit TO masters_ins_ref_guard;
CREATE POLICY insurance_ref_read_audit ON public.insurance_reference_audit FOR SELECT TO masters_ins_ref_writer,masters_ins_ref_reader
    USING(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_append_audit ON public.insurance_reference_audit FOR INSERT TO masters_ins_ref_writer
    WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_integrity_audit ON public.insurance_reference_audit TO masters_ins_ref_guard USING(true) WITH CHECK(true);
ALTER TABLE public.insurance_reference_outbox ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_reference_outbox FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_reference_outbox FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT SELECT ON public.insurance_reference_outbox TO masters_ins_ref_writer,masters_ins_ref_reader,masters_ins_ref_guard;
GRANT INSERT ON public.insurance_reference_outbox TO masters_ins_ref_writer;
GRANT UPDATE(record_id) ON public.insurance_reference_outbox TO masters_ins_ref_guard;
CREATE POLICY insurance_ref_read_outbox ON public.insurance_reference_outbox FOR SELECT TO masters_ins_ref_writer,masters_ins_ref_reader
    USING(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_append_outbox ON public.insurance_reference_outbox FOR INSERT TO masters_ins_ref_writer
    WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
CREATE POLICY insurance_ref_integrity_outbox ON public.insurance_reference_outbox TO masters_ins_ref_guard USING(true) WITH CHECK(true);
GRANT UPDATE(publication_id,revision,generation,lifecycle) ON public.insurance_reference_control TO masters_ins_ref_writer;
CREATE POLICY insurance_ref_cas_control ON public.insurance_reference_control FOR UPDATE TO masters_ins_ref_writer
    USING(public.masters_ins_ref_scope_matches(record_id,kind)) WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
GRANT UPDATE(generation,lifecycle,approval_id,execution_action_id) ON public.insurance_reference_intent_control TO masters_ins_ref_writer;
CREATE POLICY insurance_ref_cas_intent_control ON public.insurance_reference_intent_control FOR UPDATE TO masters_ins_ref_writer
    USING(public.masters_ins_ref_scope_matches(record_id,kind)) WITH CHECK(public.masters_ins_ref_scope_matches(record_id,kind));
ALTER TABLE public.insurance_reference_identity OWNER TO masters_ins_ref_owner;
ALTER TABLE public.insurance_reference_revision OWNER TO masters_ins_ref_owner;
ALTER TABLE public.insurance_reference_control OWNER TO masters_ins_ref_owner;
ALTER TABLE public.insurance_reference_maker_intent OWNER TO masters_ins_ref_owner;
ALTER TABLE public.insurance_reference_intent_control OWNER TO masters_ins_ref_owner;
ALTER TABLE public.insurance_reference_checker_approval OWNER TO masters_ins_ref_owner;
ALTER TABLE public.insurance_reference_action OWNER TO masters_ins_ref_owner;
ALTER TABLE public.insurance_reference_audit OWNER TO masters_ins_ref_owner;
ALTER TABLE public.insurance_reference_outbox OWNER TO masters_ins_ref_owner;
SET LOCAL ROLE masters_ins_ref_guard;
ALTER FUNCTION masters_ins_ref_private.v32_require_runtime() OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_require_runtime() FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_current_context() OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_current_context() FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION public.masters_ins_ref_context_current(UUID) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION public.masters_ins_ref_context_current(UUID) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION public.masters_ins_ref_scope_matches(UUID,TEXT) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION public.masters_ins_ref_scope_matches(UUID,TEXT) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_guard_context_immutable() OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_guard_context_immutable() FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_closed(JSONB,TEXT[]) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_closed(JSONB,TEXT[]) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_uuid(JSON) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_uuid(JSON) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_integer(JSON,BIGINT,BIGINT) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_integer(JSON,BIGINT,BIGINT) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_text(JSON,INTEGER,BOOLEAN) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_text(JSON,INTEGER,BOOLEAN) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_time(JSON,BOOLEAN) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_time(JSON,BOOLEAN) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_definition_valid(TEXT) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_definition_valid(TEXT) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_intent_valid(TEXT) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_intent_valid(TEXT) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_before_row() OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_before_row() FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_after_row() OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_after_row() FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_head_matches(JSONB,JSONB) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_head_matches(JSONB,JSONB) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION public.masters_ins_ref_complete(UUID) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION public.masters_ins_ref_complete(UUID) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION masters_ins_ref_private.v32_deferred_complete() OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_deferred_complete() FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
GRANT EXECUTE ON FUNCTION public.masters_ins_ref_context_current(UUID) TO masters_ins_ref_writer,masters_ins_ref_reader;
GRANT EXECUTE ON FUNCTION public.masters_ins_ref_scope_matches(UUID,TEXT) TO masters_ins_ref_writer,masters_ins_ref_reader;
GRANT EXECUTE ON FUNCTION public.masters_ins_ref_complete(UUID) TO masters_ins_ref_writer,masters_ins_ref_reader;
ALTER FUNCTION public.masters_ins_ref_begin(UUID,UUID,TEXT,UUID,UUID,TEXT,TEXT,TEXT,TIMESTAMPTZ) OWNER TO masters_ins_ref_guard;
REVOKE ALL ON FUNCTION public.masters_ins_ref_begin(UUID,UUID,TEXT,UUID,UUID,TEXT,TEXT,TEXT,TIMESTAMPTZ) FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
GRANT EXECUTE ON FUNCTION public.masters_ins_ref_begin(UUID,UUID,TEXT,UUID,UUID,TEXT,TEXT,TEXT,TIMESTAMPTZ) TO masters_ins_ref_writer,masters_ins_ref_reader;
REVOKE CREATE ON SCHEMA masters_ins_ref_private FROM masters_ins_ref_guard;
RESET ROLE;
REVOKE CREATE ON SCHEMA public FROM masters_ins_ref_owner,masters_ins_ref_guard;

-- END 08-ownership-rls-acl.sql
