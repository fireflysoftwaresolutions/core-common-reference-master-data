-- Correct record-shape dispatch only; original completion, authority, graph and clock guards remain intact.
DO $$
BEGIN
    IF NOT pg_catalog.pg_has_role(current_user,'masters_ins_ref_guard','SET')
        OR NOT EXISTS(SELECT 1 FROM pg_catalog.pg_proc p
            JOIN pg_catalog.pg_namespace n ON n.oid=p.pronamespace
            JOIN pg_catalog.pg_roles r ON r.oid=p.proowner
            WHERE n.nspname='masters_ins_ref_private' AND p.proname='v32_deferred_complete'
                AND pg_catalog.pg_get_function_identity_arguments(p.oid)=''
                AND p.prorettype='pg_catalog.trigger'::regtype AND p.prosecdef
                AND p.proconfig @> ARRAY['search_path=pg_catalog']
                AND r.rolname='masters_ins_ref_guard' AND NOT r.rolcanlogin
                AND NOT r.rolsuper AND NOT r.rolbypassrls) THEN
        RAISE EXCEPTION 'Existing exact guarded deferred completion required' USING ERRCODE='42501';
    END IF;
END $$;
SET LOCAL ROLE masters_ins_ref_guard;
GRANT CREATE ON SCHEMA masters_ins_ref_private TO masters_ins_ref_guard;
CREATE OR REPLACE FUNCTION masters_ins_ref_private.v32_deferred_complete() RETURNS TRIGGER
LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
    IF TG_TABLE_SCHEMA<>'masters_ins_ref_private' THEN
        RAISE EXCEPTION 'Exact private deferred relation required' USING ERRCODE='42501';
    END IF;
    IF TG_TABLE_NAME='context' THEN
        PERFORM public.masters_ins_ref_complete(NEW.nonce);
    ELSIF TG_TABLE_NAME='row_witness' THEN
        PERFORM public.masters_ins_ref_complete(NEW.context_nonce);
    ELSE
        RAISE EXCEPTION 'Exact private deferred relation required' USING ERRCODE='42501';
    END IF;
    RETURN NULL;
END $$;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v32_deferred_complete() FROM PUBLIC,masters_ins_ref_writer,masters_ins_ref_reader;
REVOKE CREATE ON SCHEMA masters_ins_ref_private FROM masters_ins_ref_guard;
RESET ROLE;
