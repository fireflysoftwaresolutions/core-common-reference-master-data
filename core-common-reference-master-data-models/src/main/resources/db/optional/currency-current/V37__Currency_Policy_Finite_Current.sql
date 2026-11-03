-- Opt-in technical CURRENT scope. This location is deliberately outside default Flyway scanning.
-- Shared verified principal/current PDP remain application obligations; this is not financial authority.
DO $$ BEGIN
    IF NOT pg_catalog.pg_has_role(current_user,'masters_currency_current_guard','SET') THEN
        RAISE EXCEPTION 'Authorized currency CURRENT migrator required' USING ERRCODE='42501';
    END IF;
    IF pg_catalog.has_schema_privilege('masters_currency_current_guard','public','CREATE')
        OR EXISTS(SELECT 1 FROM pg_catalog.pg_namespace n JOIN pg_catalog.pg_roles r ON r.oid=n.nspowner
            WHERE n.nspname='public' AND r.rolname='masters_currency_current_guard')
        OR EXISTS(SELECT 1 FROM pg_catalog.pg_database d JOIN pg_catalog.pg_roles r ON r.oid=d.datdba
            WHERE d.datname=current_database() AND r.rolname='masters_currency_current_guard') THEN
        RAISE EXCEPTION 'Temporary integrity CREATE must initially be absent' USING ERRCODE='42501';
    END IF;
    IF NOT pg_catalog.has_schema_privilege(current_user,'public','USAGE WITH GRANT OPTION')
        OR NOT pg_catalog.has_schema_privilege(current_user,'public','CREATE WITH GRANT OPTION') THEN
        RAISE EXCEPTION 'Separately delegated migration schema grant authority required' USING ERRCODE='42501';
    END IF;
    IF NOT EXISTS(SELECT 1 FROM pg_catalog.pg_roles r WHERE r.rolname='masters_currency_current_guard'
        AND NOT r.rolcanlogin AND NOT r.rolinherit AND NOT r.rolsuper AND NOT r.rolbypassrls
        AND NOT r.rolcreatedb AND NOT r.rolcreaterole AND NOT r.rolreplication)
      OR NOT EXISTS(SELECT 1 FROM pg_catalog.pg_roles r WHERE r.rolname='masters_currency_current_reader'
        AND r.rolcanlogin AND NOT r.rolinherit AND NOT r.rolsuper AND NOT r.rolbypassrls
        AND NOT r.rolcreatedb AND NOT r.rolcreaterole AND NOT r.rolreplication)
      OR pg_catalog.pg_has_role('masters_currency_current_reader','masters_currency_current_guard','MEMBER') THEN
        RAISE EXCEPTION 'Exact preprovisioned currency CURRENT roles required' USING ERRCODE='42501';
    END IF;
END $$;
CREATE SCHEMA masters_currency_current_private AUTHORIZATION masters_currency_current_guard;
SET LOCAL ROLE masters_currency_current_guard;
REVOKE ALL ON SCHEMA masters_currency_current_private FROM PUBLIC,masters_currency_current_reader;
RESET ROLE;
-- Installation ledger: preflight proves no existing CREATE/ownership; this transaction introduces and removes only this temporary grant.
GRANT USAGE,CREATE ON SCHEMA public TO masters_currency_current_guard;
GRANT SELECT ON public.currency_policy_identity,public.currency_execution_policy,public.currency_policy_control TO masters_currency_current_guard;
CREATE POLICY currency_current_guard_read ON public.currency_policy_identity FOR SELECT TO masters_currency_current_guard USING(true);
CREATE POLICY currency_current_guard_read ON public.currency_execution_policy FOR SELECT TO masters_currency_current_guard USING(true);
CREATE POLICY currency_current_guard_read ON public.currency_policy_control FOR SELECT TO masters_currency_current_guard USING(true);
SET LOCAL ROLE masters_currency_current_guard;

CREATE TABLE masters_currency_current_private.context (
    nonce UUID PRIMARY KEY CHECK(nonce<>'00000000-0000-0000-0000-000000000000'::uuid),
    xid XID8 NOT NULL,backend_pid INTEGER NOT NULL CHECK(backend_pid>0),db_session_user NAME NOT NULL,
    policy_id UUID NOT NULL,currency_id UUID NOT NULL,iso_code TEXT NOT NULL CHECK(iso_code ~ '^[A-Z]{3}$'),
    version BIGINT NOT NULL CHECK(version>0),definition_digest TEXT NOT NULL CHECK(definition_digest ~ '^[0-9a-f]{64}$'),
    encoding_version TEXT NOT NULL CHECK(encoding_version='MASTERS_CURRENCY_POLICY_V1'),posted_scale INTEGER NOT NULL CHECK(posted_scale BETWEEN 0 AND 10),
    actor_issuer VARCHAR(500) NOT NULL,actor_subject VARCHAR(255) NOT NULL,actor_tenant VARCHAR(255) NOT NULL,
    installed_at TIMESTAMPTZ NOT NULL,recorded_at TIMESTAMPTZ NOT NULL,authority_until TIMESTAMPTZ NOT NULL,effective_until TIMESTAMPTZ NOT NULL,
    captured_identity JSONB NOT NULL,captured_policy JSONB NOT NULL,captured_control JSONB NOT NULL,
    UNIQUE(xid,backend_pid,db_session_user),
    CHECK(db_session_user='masters_currency_current_reader'
        AND policy_id<>'00000000-0000-0000-0000-000000000000'::uuid AND currency_id<>'00000000-0000-0000-0000-000000000000'::uuid
        AND length(actor_issuer)>0 AND length(actor_subject)>0 AND length(actor_tenant)>0
        AND isfinite(installed_at) AND isfinite(recorded_at) AND isfinite(authority_until) AND isfinite(effective_until)
        AND recorded_at>=installed_at AND effective_until>recorded_at AND effective_until<=installed_at+INTERVAL '5 seconds'
        AND effective_until<=authority_until)
);
ALTER TABLE masters_currency_current_private.context ENABLE ROW LEVEL SECURITY;
ALTER TABLE masters_currency_current_private.context FORCE ROW LEVEL SECURITY;
REVOKE ALL ON masters_currency_current_private.context FROM PUBLIC,masters_currency_current_reader;
CREATE POLICY currency_current_context_integrity ON masters_currency_current_private.context TO masters_currency_current_guard USING(true) WITH CHECK(true);
CREATE FUNCTION masters_currency_current_private.reject_context_change() RETURNS TRIGGER
LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN RAISE EXCEPTION 'Immutable currency CURRENT context' USING ERRCODE='42501'; END $$;
CREATE TRIGGER currency_current_context_immutable BEFORE UPDATE OR DELETE ON masters_currency_current_private.context
    FOR EACH ROW EXECUTE FUNCTION masters_currency_current_private.reject_context_change();

CREATE FUNCTION public.currency_policy_current_runtime_allowed() RETURNS BOOLEAN
LANGUAGE SQL STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT session_user='masters_currency_current_reader' AND NOT r.rolsuper AND NOT r.rolbypassrls
    AND NOT r.rolcreatedb AND NOT r.rolcreaterole AND NOT r.rolreplication AND NOT r.rolinherit
    AND NOT pg_catalog.pg_has_role(session_user,'masters_currency_current_guard','MEMBER')
    AND NOT pg_catalog.has_schema_privilege(session_user,'public','CREATE')
    AND NOT pg_catalog.has_schema_privilege(session_user,'masters_currency_current_private','USAGE')
    AND EXISTS(SELECT 1 FROM pg_catalog.pg_roles g WHERE g.rolname='masters_currency_current_guard'
        AND NOT g.rolcanlogin AND NOT g.rolinherit AND NOT g.rolsuper AND NOT g.rolbypassrls
        AND NOT g.rolcreatedb AND NOT g.rolcreaterole AND NOT g.rolreplication)
    AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
        WHERE n.nspname='public' AND c.relname IN('currency_policy_identity','currency_execution_policy','currency_policy_control')
        AND (NOT c.relrowsecurity OR NOT c.relforcerowsecurity OR pg_catalog.pg_has_role(session_user,c.relowner,'MEMBER')
             OR pg_catalog.has_table_privilege(session_user,c.oid,'INSERT,UPDATE,DELETE')))
    AND (SELECT count(*) FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
        WHERE n.nspname='public' AND c.relname IN('currency_policy_identity','currency_execution_policy','currency_policy_control'))=3
    AND EXISTS(SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
        WHERE n.nspname='public' AND c.relname='currency_policy_observation' AND c.relkind='v' AND c.reloptions @> ARRAY['security_invoker=true'])
    AND pg_catalog.has_table_privilege(session_user,'public.currency_policy_observation','SELECT')
    AND (SELECT count(*) FROM pg_catalog.pg_policy policy JOIN pg_catalog.pg_class table_value ON table_value.oid=policy.polrelid
        JOIN pg_catalog.pg_namespace namespace ON namespace.oid=table_value.relnamespace
        WHERE namespace.nspname='public' AND table_value.relname IN('currency_policy_identity','currency_execution_policy','currency_policy_control')
            AND policy.polname='currency_current_required' AND NOT policy.polpermissive AND policy.polcmd='r'
            AND policy.polroles @> ARRAY[r.oid])=3
    AND EXISTS(SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
        JOIN pg_catalog.pg_roles owner_value ON owner_value.oid=c.relowner
        WHERE n.nspname='masters_currency_current_private' AND c.relname='context' AND c.relrowsecurity AND c.relforcerowsecurity
            AND owner_value.rolname='masters_currency_current_guard')
    AND (SELECT count(*) FROM pg_catalog.pg_proc f JOIN pg_catalog.pg_namespace n ON n.oid=f.pronamespace
        JOIN pg_catalog.pg_roles owner_value ON owner_value.oid=f.proowner
        WHERE n.nspname='public' AND f.proname IN('currency_policy_current_runtime_allowed','currency_policy_current_begin','currency_policy_current_scope_matches','currency_policy_current_complete')
            AND f.prosecdef AND f.proconfig @> ARRAY['search_path=pg_catalog'] AND owner_value.rolname='masters_currency_current_guard')=4
 FROM pg_catalog.pg_roles r WHERE r.rolname=session_user
$$;
CREATE FUNCTION masters_currency_current_private.current_context() RETURNS masters_currency_current_private.context
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_currency_current_private.context%ROWTYPE; observed TIMESTAMPTZ;
BEGIN
    IF public.currency_policy_current_runtime_allowed() IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Restricted currency CURRENT runtime required' USING ERRCODE='42501';
    END IF;
    SELECT * INTO c FROM masters_currency_current_private.context t
        WHERE t.xid=pg_catalog.pg_current_xact_id_if_assigned() AND t.backend_pid=pg_catalog.pg_backend_pid() AND t.db_session_user=session_user;
    IF NOT FOUND THEN RAISE EXCEPTION 'Protected currency CURRENT context required' USING ERRCODE='42501'; END IF;
    observed:=pg_catalog.clock_timestamp();
    IF observed<c.recorded_at OR observed<c.installed_at OR observed>=c.effective_until OR observed>=c.authority_until
        OR c.effective_until>c.installed_at+INTERVAL '5 seconds' OR c.effective_until>c.authority_until THEN
        RAISE EXCEPTION 'Finite currency CURRENT context required' USING ERRCODE='42501';
    END IF;
    RETURN c;
END $$;

CREATE FUNCTION public.currency_policy_current_begin(p_nonce UUID,p_policy UUID,p_currency UUID,p_iso TEXT,p_version BIGINT,
    p_digest TEXT,p_encoding TEXT,p_scale INTEGER,p_issuer TEXT,p_subject TEXT,p_tenant TEXT,p_until TIMESTAMPTZ)
RETURNS TABLE(context_nonce UUID,policy_id UUID,currency_id UUID,iso_code TEXT,version BIGINT,definition_digest TEXT,encoding_version TEXT,
    posted_scale INTEGER,db_now_millis BIGINT,recorded_at TIMESTAMPTZ,effective_until TIMESTAMPTZ)
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE started TIMESTAMPTZ; observed TIMESTAMPTZ; bound TIMESTAMPTZ; identity_value JSONB; policy_value JSONB; control_value JSONB;
    first_effective BIGINT; last_effective BIGINT; tx XID8;
BEGIN
    IF public.currency_policy_current_runtime_allowed() IS DISTINCT FROM true THEN
        RAISE EXCEPTION 'Restricted currency CURRENT runtime required' USING ERRCODE='42501';
    END IF;
    started:=pg_catalog.clock_timestamp();tx:=pg_catalog.pg_current_xact_id();
    IF p_nonce IS NULL OR p_nonce='00000000-0000-0000-0000-000000000000'::uuid
        OR p_policy IS NULL OR p_policy='00000000-0000-0000-0000-000000000000'::uuid
        OR p_currency IS NULL OR p_currency='00000000-0000-0000-0000-000000000000'::uuid
        OR p_iso IS NULL OR p_iso !~ '^[A-Z]{3}$' OR p_version IS NULL OR p_version<1
        OR p_digest IS NULL OR length(p_digest)<>64 OR p_digest !~ '^[0-9a-f]{64}$'
        OR p_encoding IS DISTINCT FROM 'MASTERS_CURRENCY_POLICY_V1' OR p_scale IS NULL OR p_scale NOT BETWEEN 0 AND 10
        OR p_issuer IS NULL OR length(p_issuer) NOT BETWEEN 1 AND 500 OR p_subject IS NULL OR length(p_subject) NOT BETWEEN 1 AND 255
        OR p_tenant IS NULL OR length(p_tenant) NOT BETWEEN 1 AND 255 OR p_until IS NULL OR NOT isfinite(p_until) OR p_until<=started THEN
        RAISE EXCEPTION 'Exact currency CURRENT scope required' USING ERRCODE='42501';
    END IF;
    IF EXISTS(SELECT 1 FROM masters_currency_current_private.context c WHERE c.xid=tx AND c.backend_pid=pg_catalog.pg_backend_pid() AND c.db_session_user=session_user)
        OR COALESCE(pg_catalog.current_setting('firefly.currency.currency_id',true),'')<>''
        OR COALESCE(pg_catalog.current_setting('firefly.currency.purpose',true),'')<>'' THEN
        RAISE EXCEPTION 'Separate currency CURRENT transaction required' USING ERRCODE='42501';
    END IF;
    SELECT pg_catalog.jsonb_build_object('currency_id',i.currency_id,'iso_code',i.iso_code),to_jsonb(p),to_jsonb(c),p.effective_from,p.effective_to
      INTO identity_value,policy_value,control_value,first_effective,last_effective
      FROM public.currency_execution_policy p JOIN public.currency_policy_control c ON c.policy_id=p.policy_id AND c.currency_id=p.currency_id
      JOIN public.currency_policy_identity i ON i.currency_id=p.currency_id AND i.iso_code=p.iso_code
      WHERE p.policy_id=p_policy AND p.currency_id=p_currency AND p.iso_code=p_iso AND p.version=p_version AND i.head_version>=p_version
        AND p.definition_digest=p_digest AND p.encoding_version=p_encoding AND p.posted_scale=p_scale AND c.lifecycle='PUBLISHED' AND c.state_version=0;
    IF NOT FOUND THEN RETURN; END IF;
    observed:=pg_catalog.clock_timestamp();
    IF observed<started OR observed<pg_catalog.to_timestamp(first_effective/1000.0)
        OR (last_effective IS NOT NULL AND observed>=pg_catalog.to_timestamp(last_effective/1000.0)) THEN RETURN; END IF;
    bound:=LEAST(started+INTERVAL '5 seconds',p_until,COALESCE(pg_catalog.to_timestamp(last_effective/1000.0),'infinity'::timestamptz));
    IF bound<=observed THEN RAISE EXCEPTION 'Currency CURRENT window exhausted' USING ERRCODE='42501'; END IF;
    INSERT INTO masters_currency_current_private.context VALUES(p_nonce,tx,pg_catalog.pg_backend_pid(),session_user,p_policy,p_currency,p_iso,p_version,p_digest,p_encoding,p_scale,
        p_issuer,p_subject,p_tenant,started,observed,p_until,bound,identity_value,policy_value,control_value);
    PERFORM pg_catalog.set_config('firefly.currency.currency_id',p_currency::text,true);
    PERFORM pg_catalog.set_config('firefly.currency.purpose','READ',true);
    RETURN QUERY SELECT p_nonce,p_policy,p_currency,p_iso,p_version,p_digest,p_encoding,p_scale,
        pg_catalog.floor(extract(epoch FROM observed)*1000)::bigint,observed,bound;
END $$;

CREATE FUNCTION public.currency_policy_current_scope_matches(p_currency UUID,p_policy UUID) RETURNS BOOLEAN
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_currency_current_private.context%ROWTYPE;
BEGIN
    c:=masters_currency_current_private.current_context();
    RETURN c.currency_id=p_currency AND (p_policy IS NULL OR c.policy_id=p_policy);
END $$;
CREATE FUNCTION public.currency_policy_current_complete(p_nonce UUID) RETURNS BOOLEAN
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_currency_current_private.context%ROWTYPE; identity_value JSONB; policy_value JSONB; control_value JSONB;
BEGIN
    c:=masters_currency_current_private.current_context();
    IF p_nonce IS DISTINCT FROM c.nonce THEN RAISE EXCEPTION 'Exact currency CURRENT nonce required' USING ERRCODE='42501'; END IF;
    SELECT pg_catalog.jsonb_build_object('currency_id',i.currency_id,'iso_code',i.iso_code),to_jsonb(p),to_jsonb(control)
      INTO identity_value,policy_value,control_value FROM public.currency_execution_policy p
      JOIN public.currency_policy_control control ON control.policy_id=p.policy_id AND control.currency_id=p.currency_id
      JOIN public.currency_policy_identity i ON i.currency_id=p.currency_id AND i.iso_code=p.iso_code
      WHERE p.policy_id=c.policy_id AND p.currency_id=c.currency_id AND i.head_version>=c.version AND control.lifecycle='PUBLISHED' AND control.state_version=0;
    IF NOT FOUND OR identity_value IS DISTINCT FROM c.captured_identity OR policy_value IS DISTINCT FROM c.captured_policy OR control_value IS DISTINCT FROM c.captured_control THEN
        RAISE EXCEPTION 'Currency CURRENT snapshot changed' USING ERRCODE='42501';
    END IF;
    PERFORM masters_currency_current_private.current_context();RETURN true;
END $$;

REVOKE ALL ON FUNCTION masters_currency_current_private.reject_context_change(),masters_currency_current_private.current_context() FROM PUBLIC,masters_currency_current_reader;
REVOKE ALL ON FUNCTION public.currency_policy_current_runtime_allowed(),public.currency_policy_current_begin(UUID,UUID,UUID,TEXT,BIGINT,TEXT,TEXT,INTEGER,TEXT,TEXT,TEXT,TIMESTAMPTZ),
    public.currency_policy_current_scope_matches(UUID,UUID),public.currency_policy_current_complete(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.currency_policy_current_runtime_allowed(),public.currency_policy_current_begin(UUID,UUID,UUID,TEXT,BIGINT,TEXT,TEXT,INTEGER,TEXT,TEXT,TEXT,TIMESTAMPTZ),
    public.currency_policy_current_scope_matches(UUID,UUID),public.currency_policy_current_complete(UUID) TO masters_currency_current_reader;
RESET ROLE;
REVOKE CREATE ON SCHEMA public FROM masters_currency_current_guard;
DO $$ BEGIN
    IF pg_catalog.has_schema_privilege('masters_currency_current_guard','public','CREATE') THEN
        RAISE EXCEPTION 'Temporary currency CURRENT CREATE cleanup incomplete' USING ERRCODE='42501';
    END IF;
END $$;
CREATE POLICY currency_current_required ON public.currency_policy_identity AS RESTRICTIVE FOR SELECT TO masters_currency_current_reader
    USING(public.currency_policy_current_scope_matches(currency_id,NULL::uuid));
CREATE POLICY currency_current_required ON public.currency_execution_policy AS RESTRICTIVE FOR SELECT TO masters_currency_current_reader
    USING(public.currency_policy_current_scope_matches(currency_id,policy_id));
CREATE POLICY currency_current_required ON public.currency_policy_control AS RESTRICTIVE FOR SELECT TO masters_currency_current_reader
    USING(public.currency_policy_current_scope_matches(currency_id,policy_id));
GRANT USAGE ON SCHEMA public TO masters_currency_current_reader;
GRANT SELECT ON public.currency_policy_identity,public.currency_execution_policy,public.currency_policy_control,public.currency_policy_observation TO masters_currency_current_reader;
