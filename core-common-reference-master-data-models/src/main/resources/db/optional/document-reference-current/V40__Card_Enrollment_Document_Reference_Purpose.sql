-- Opt-in forward extension of V39; an exact card document observation is not card issuance authority.
-- Run with the same separately delegated migrator; no caller or reader grants change.
DO $$ BEGIN
 IF current_user<>session_user OR NOT pg_catalog.pg_has_role(current_user,'masters_document_current_guard','SET')
  OR pg_catalog.has_schema_privilege('masters_document_current_guard','public','CREATE')
  OR NOT EXISTS(SELECT 1 FROM pg_catalog.pg_roles WHERE rolname='masters_document_current_guard' AND NOT rolcanlogin AND NOT rolinherit AND NOT rolsuper AND NOT rolbypassrls AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolreplication)
  OR NOT EXISTS(SELECT 1 FROM pg_catalog.pg_proc p JOIN pg_catalog.pg_namespace n ON n.oid=p.pronamespace JOIN pg_catalog.pg_roles r ON r.oid=p.proowner WHERE n.nspname='masters_document_current_private' AND p.proname='target_valid' AND pg_catalog.pg_get_function_identity_arguments(p.oid)='t jsonb' AND r.rolname='masters_document_current_guard' AND NOT p.prosecdef AND p.provolatile='i' AND p.proconfig=ARRAY['search_path=pg_catalog']) THEN
  RAISE EXCEPTION 'Established protected document CURRENT installation required' USING ERRCODE='42501';END IF;
END $$;
SET LOCAL ROLE masters_document_current_guard;
CREATE OR REPLACE FUNCTION masters_document_current_private.target_valid(t JSONB) RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE SET search_path=pg_catalog AS $$
DECLARE p JSONB;r JSONB;k TEXT;
BEGIN
 IF masters_document_current_private.document_layout_shape(t,ARRAY['product','purpose','documentType','locale','template','layout']) IS NOT TRUE OR COALESCE(t->>'purpose','') NOT IN ('DEPOSIT_ACCOUNT_ENROLLMENT','INSURANCE_TERM_PC_QUOTE','CARD_ENROLLMENT')
 OR masters_document_current_private.document_layout_text(t->'documentType',100) IS NOT TRUE OR masters_document_current_private.document_layout_text(t->'locale',50) IS NOT TRUE OR (t->>'documentType') COLLATE "C" !~ '^[A-Z][A-Z0-9_]{0,99}$' OR (t->>'locale') COLLATE "C" !~ '^[A-Za-z0-9][A-Za-z0-9_-]{0,49}$' THEN RETURN false;END IF;
 p:=t->'product';IF masters_document_current_private.document_layout_shape(p,ARRAY['tenantId','rootId','productId','revisionId','publicationId','definitionDigest']) IS NOT TRUE OR masters_document_current_private.document_layout_sha(p->'definitionDigest') IS NOT TRUE THEN RETURN false;END IF;
 FOREACH k IN ARRAY ARRAY['tenantId','rootId','productId','revisionId','publicationId'] LOOP IF masters_document_current_private.document_layout_uuid(p->k) IS NOT TRUE OR p->>k='00000000-0000-0000-0000-000000000000' THEN RETURN false;END IF;END LOOP;
 FOREACH k IN ARRAY ARRAY['template','layout'] LOOP
  r:=t->k;IF masters_document_current_private.document_layout_shape(r,ARRAY['owner','kind','id','versionFormat','versionText','digest']) IS NOT TRUE OR r->>'owner' IS DISTINCT FROM 'core-common-reference-master-data' OR r->>'kind' IS DISTINCT FROM (CASE k WHEN 'template' THEN 'DOCUMENT_TEMPLATE' ELSE 'DOCUMENT_LAYOUT' END)
   OR masters_document_current_private.document_layout_uuid(r->'id') IS NOT TRUE OR r->>'id'='00000000-0000-0000-0000-000000000000' OR r->>'versionFormat' IS DISTINCT FROM 'OPAQUE'
   OR masters_document_current_private.document_layout_text(r->'versionText',20) IS NOT TRUE OR (r->>'versionText') COLLATE "C" !~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,19}$' OR masters_document_current_private.document_layout_sha(r->'digest') IS NOT TRUE THEN RETURN false;END IF;
 END LOOP;
 RETURN t->'template'->>'id'<>t->'layout'->>'id' AND t->'template'->>'versionText'=t->'layout'->>'versionText';
END $$;
RESET ROLE;
