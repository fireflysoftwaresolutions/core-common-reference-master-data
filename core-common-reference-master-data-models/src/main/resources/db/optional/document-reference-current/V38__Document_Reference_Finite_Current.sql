-- Opt-in finite metadata observation; default Flyway does not scan this location.
-- Product, asset, renderer and legal/financial admission remain outside Masters.
DO $$ BEGIN
 IF NOT pg_catalog.pg_has_role(current_user,'masters_document_current_guard','SET')
  OR pg_catalog.has_schema_privilege('masters_document_current_guard','public','CREATE')
  OR NOT pg_catalog.has_schema_privilege(current_user,'public','USAGE WITH GRANT OPTION')
  OR NOT pg_catalog.has_schema_privilege(current_user,'public','CREATE WITH GRANT OPTION') THEN
  RAISE EXCEPTION 'Separately delegated document CURRENT migrator required' USING ERRCODE='42501';END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_catalog.pg_roles WHERE rolname='masters_document_current_guard' AND NOT rolcanlogin AND NOT rolinherit AND NOT rolsuper AND NOT rolbypassrls AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolreplication)
  OR NOT EXISTS(SELECT 1 FROM pg_catalog.pg_roles WHERE rolname='masters_document_current_reader' AND rolcanlogin AND NOT rolinherit AND NOT rolsuper AND NOT rolbypassrls AND NOT rolcreatedb AND NOT rolcreaterole AND NOT rolreplication)
  OR EXISTS(SELECT 1 FROM pg_catalog.pg_auth_members m JOIN pg_catalog.pg_roles member_role ON member_role.oid=m.member WHERE member_role.rolname IN('masters_document_current_guard','masters_document_current_reader'))
  OR pg_catalog.pg_has_role('masters_document_current_reader','masters_document_current_guard','MEMBER')
  OR EXISTS(SELECT 1 FROM pg_catalog.pg_namespace n JOIN pg_catalog.pg_roles r ON r.oid=n.nspowner WHERE n.nspname='public' AND r.rolname='masters_document_current_guard')
  OR EXISTS(SELECT 1 FROM pg_catalog.pg_database d JOIN pg_catalog.pg_roles r ON r.oid=d.datdba WHERE d.datname=current_database() AND r.rolname='masters_document_current_guard') THEN
  RAISE EXCEPTION 'Exact separate nonowner document CURRENT roles required' USING ERRCODE='42501';END IF;
END $$;
CREATE SCHEMA masters_document_current_private AUTHORIZATION masters_document_current_guard;
SET LOCAL ROLE masters_document_current_guard;
REVOKE ALL ON SCHEMA masters_document_current_private FROM PUBLIC,masters_document_current_reader;
RESET ROLE;
GRANT USAGE,CREATE ON SCHEMA public TO masters_document_current_guard;
GRANT SELECT ON public.document_layout_publication,public.document_layout_notice,public.document_template_revision,public.document_template_catalog,public.document_template_localization,public.document_template_type_catalog,public.language_locale TO masters_document_current_guard;
SET LOCAL ROLE masters_document_current_guard;

-- Exact pure V30 delegates. Private SET paths prevent SQL inlining and resolve legacy sibling calls.
CREATE FUNCTION masters_document_current_private.document_layout_utf16_key(text) RETURNS bytea LANGUAGE sql IMMUTABLE STRICT SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_utf16_key($1) $$;
CREATE FUNCTION masters_document_current_private.document_layout_quote(text) RETURNS text LANGUAGE sql IMMUTABLE STRICT SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_quote($1) $$;
CREATE FUNCTION masters_document_current_private.document_layout_canonical(jsonb) RETURNS text LANGUAGE sql IMMUTABLE STRICT SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_canonical($1) $$;
CREATE FUNCTION masters_document_current_private.document_layout_shape(jsonb,text[]) RETURNS boolean LANGUAGE sql IMMUTABLE SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_shape($1,$2) $$;
CREATE FUNCTION masters_document_current_private.document_layout_text(jsonb,integer) RETURNS boolean LANGUAGE sql IMMUTABLE SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_text($1,$2) $$;
CREATE FUNCTION masters_document_current_private.document_layout_uuid(jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_uuid($1) $$;
CREATE FUNCTION masters_document_current_private.document_layout_sha(jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_sha($1) $$;
CREATE FUNCTION masters_document_current_private.document_layout_master_reference_valid(jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_master_reference_valid($1) $$;
CREATE FUNCTION masters_document_current_private.document_layout_profile_valid(jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_profile_valid($1) $$;
CREATE FUNCTION masters_document_current_private.document_layout_notice_schema_valid(jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_notice_schema_valid($1) $$;
CREATE FUNCTION masters_document_current_private.document_layout_definition_valid(jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_definition_valid($1) $$;
CREATE FUNCTION masters_document_current_private.document_layout_reference_matches(jsonb,public.document_template_revision) RETURNS boolean LANGUAGE sql IMMUTABLE SECURITY INVOKER SET search_path=pg_catalog,masters_document_current_private AS $$ SELECT public.document_layout_reference_matches($1,$2) $$;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA masters_document_current_private FROM PUBLIC,masters_document_current_reader;
CREATE FUNCTION masters_document_current_private.helper_closure_allowed() RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY INVOKER SET search_path=pg_catalog AS $$
 WITH expected(signature,return_type,language_name,is_strict,legacy_sha,alias_sha) AS (VALUES
 ('public.document_layout_utf16_key(text)','bytea','sql',true,'3fa172d4996028049dc94c7f8fdb32a567461bd366e653f3b1ac29690cf4be80','30fb666652c0524971412ef8c51ddcd0fef3bdaa7b4691a15342ac70bad593e3'),
 ('public.document_layout_quote(text)','text','plpgsql',true,'4ae301bdf78c91d6d169f15de0b9d00cca4e8f2cd90ec1e7968702a40b2181aa','ec8e40fa33fad3b4b220d6c44e62e0df25a0ca995f4874ba537f425ac76b4fc6'),
 ('public.document_layout_canonical(jsonb)','text','plpgsql',true,'1d21e1d480bd40b62ffb7eb8ac904ed6c065bb7a6a7e54a2b1a936610963456c','1484a75cab46d2bb8e3db362dbb65546c3e1b7c96441ab0bf3bf2bb3642fa42e'),
 ('public.document_layout_shape(jsonb,text[])','boolean','sql',false,'8958f80c6bda19712058cf5ba66d90bf35eddf6a0266a9957342454c2c83a338','d8e7ba592c565775177f78d6e06f51a187d472a271dd17117499317f7fd647a3'),
 ('public.document_layout_text(jsonb,integer)','boolean','plpgsql',false,'b7822a3b9d1386ab23df77c8c9f0dd60941784fc3d65566f13defebec8d4f366','a1065a5badabbdb1ba8786c59eb05cd3924d90761e46c6719abb84f14d724446'),
 ('public.document_layout_uuid(jsonb)','boolean','sql',false,'d0f27bdedb63d64df90248234744edba98bb77fafdfe406fa09940e933c67e42','191091873d8c84786e91bcc8a8a0e432e6f129fe75f0fdfaa0c49ec5abc31903'),
 ('public.document_layout_sha(jsonb)','boolean','sql',false,'173abcf96f3adec20c2f0bdeaa88d0a64e210ccbd0e847696283f45434ac09cf','f59f5a1e3412dcdd2061518f0c70e382aab8e62d6de0fabc9c408e7285091c9e'),
 ('public.document_layout_master_reference_valid(jsonb)','boolean','plpgsql',false,'3584499bd410722afc4a772959fe2664110701fcd52e6e4fb53dc13189abe7df','8e4bcbade83633af3c232a161ac97bf09233a0e6c1248fbb849603cf7545a815'),
 ('public.document_layout_profile_valid(jsonb)','boolean','plpgsql',false,'6209736d352595941f1dd6d6b49683e16255355e1575527ab2d8dbb5d374e358','df40202f2bed249c3e7bcf03700b024a02a6d780d2bdb33e21098b0d1015b4dc'),
 ('public.document_layout_notice_schema_valid(jsonb)','boolean','sql',false,'9873da0152fd88e6a6534765bc9e36fd2c5c7f89d8e63aea7dc6c45950d130bc','3b06c5e99264318b5a0927bf433f97b5dc6f892685e1f310876087921985e6a7'),
 ('public.document_layout_definition_valid(jsonb)','boolean','plpgsql',false,'b3f081b0e966d842a24ea893e2e897f9878738697319932baa6a1510b4d5b3f2','60e4cec591eb8bb3c279180e7962132665a2ad56e1d105043dc741372a322f6a'),
 ('public.document_layout_reference_matches(jsonb,public.document_template_revision)','boolean','sql',false,'ed6771b83699b08499050f033db1a947b623865fb0cd0e7660b0daf9a14068cc','cf3259738462d68c4aeb613f220bc7b2367c018a8d231b4127b068513b1c00ce')
 )
 SELECT count(*)=12 AND coalesce(bool_and(
  legacy.oid IS NOT NULL AND alias.oid IS NOT NULL AND legacy.proowner=business.relowner
  AND legacy.prorettype=to_regtype(expected.return_type) AND alias.prorettype=legacy.prorettype
  AND legacy.prokind='f' AND alias.prokind='f' AND NOT legacy.proretset AND NOT alias.proretset
  AND legacy.pronargdefaults=0 AND alias.pronargdefaults=0 AND legacy.provariadic=0 AND alias.provariadic=0
  AND language_value.lanname=expected.language_name AND alias_language.lanname='sql'
  AND legacy.provolatile='i' AND alias.provolatile='i' AND legacy.proisstrict=expected.is_strict AND alias.proisstrict=expected.is_strict
  AND NOT legacy.prosecdef AND NOT alias.prosecdef AND NOT legacy.proleakproof AND NOT alias.proleakproof
  AND legacy.proconfig IS NULL AND alias.proconfig=ARRAY['search_path=pg_catalog, masters_document_current_private']
  AND encode(sha256(convert_to(legacy.prosrc,'UTF8')),'hex')=expected.legacy_sha
  AND encode(sha256(convert_to(alias.prosrc,'UTF8')),'hex')=expected.alias_sha
  AND alias.proowner=guard.oid AND has_function_privilege(guard.oid,legacy.oid,'EXECUTE')
  AND NOT EXISTS(SELECT 1 FROM aclexplode(coalesce(alias.proacl,acldefault('f',alias.proowner))) a WHERE a.grantee<>alias.proowner)
  AND NOT EXISTS(SELECT 1 FROM aclexplode(coalesce(legacy.proacl,acldefault('f',legacy.proowner))) a WHERE a.grantee<>legacy.proowner AND (a.grantee<>0 OR a.privilege_type<>'EXECUTE' OR a.is_grantable))
 ),false)
 FROM expected
 LEFT JOIN pg_proc legacy ON legacy.oid=to_regprocedure(expected.signature)
 LEFT JOIN pg_namespace alias_namespace ON alias_namespace.nspname='masters_document_current_private'
 LEFT JOIN pg_proc alias ON alias.pronamespace=alias_namespace.oid AND alias.proname=legacy.proname AND alias.proargtypes=legacy.proargtypes
 LEFT JOIN pg_language language_value ON language_value.oid=legacy.prolang
 LEFT JOIN pg_language alias_language ON alias_language.oid=alias.prolang
 LEFT JOIN pg_roles guard ON guard.rolname='masters_document_current_guard'
 LEFT JOIN pg_class business ON business.oid='public.document_layout_publication'::regclass
 WHERE alias_namespace.nspowner=guard.oid
 AND NOT EXISTS(SELECT 1 FROM aclexplode(coalesce(alias_namespace.nspacl,acldefault('n',alias_namespace.nspowner))) a WHERE a.grantee<>alias_namespace.nspowner)
$$;
REVOKE ALL ON FUNCTION masters_document_current_private.helper_closure_allowed() FROM PUBLIC,masters_document_current_reader;
DO $$ BEGIN IF masters_document_current_private.helper_closure_allowed() IS DISTINCT FROM true THEN RAISE EXCEPTION 'Exact trusted legacy document helper closure required' USING ERRCODE='42501';END IF;END $$;

CREATE TABLE masters_document_current_private.context (
 nonce UUID PRIMARY KEY CHECK(nonce<>'00000000-0000-0000-0000-000000000000'::uuid),xid XID8 NOT NULL,backend_pid INTEGER NOT NULL CHECK(backend_pid>0),db_session_user NAME NOT NULL,
 target_canonical TEXT NOT NULL CHECK(octet_length(target_canonical)<=8192),actor_issuer VARCHAR(500) NOT NULL,actor_subject VARCHAR(255) NOT NULL,actor_tenant VARCHAR(255) NOT NULL,
 installed_at TIMESTAMPTZ NOT NULL,recorded_at TIMESTAMPTZ NOT NULL,authority_until TIMESTAMPTZ NOT NULL,effective_until TIMESTAMPTZ NOT NULL,captured_graph JSONB NOT NULL,
 UNIQUE(xid,backend_pid,db_session_user),CHECK(db_session_user='masters_document_current_reader' AND length(actor_issuer)>0 AND length(actor_subject)>0 AND length(actor_tenant)>0
 AND isfinite(installed_at) AND isfinite(recorded_at) AND isfinite(authority_until) AND isfinite(effective_until) AND recorded_at>=installed_at AND effective_until>recorded_at AND effective_until<=installed_at+INTERVAL '5 seconds' AND effective_until<=authority_until)
);
ALTER TABLE masters_document_current_private.context ENABLE ROW LEVEL SECURITY;
ALTER TABLE masters_document_current_private.context FORCE ROW LEVEL SECURITY;
REVOKE ALL ON masters_document_current_private.context FROM PUBLIC,masters_document_current_reader;
CREATE POLICY document_current_context_integrity ON masters_document_current_private.context TO masters_document_current_guard USING(true) WITH CHECK(true);
CREATE FUNCTION masters_document_current_private.reject_change() RETURNS trigger LANGUAGE plpgsql SET search_path=pg_catalog AS $$ BEGIN RAISE EXCEPTION 'Immutable document CURRENT context' USING ERRCODE='42501';END $$;
CREATE TRIGGER document_current_context_immutable BEFORE UPDATE OR DELETE ON masters_document_current_private.context FOR EACH ROW EXECUTE FUNCTION masters_document_current_private.reject_change();

CREATE FUNCTION masters_document_current_private.target_valid(t JSONB) RETURNS BOOLEAN LANGUAGE plpgsql IMMUTABLE SET search_path=pg_catalog AS $$
DECLARE p JSONB;r JSONB;k TEXT;
BEGIN
 IF masters_document_current_private.document_layout_shape(t,ARRAY['product','purpose','documentType','locale','template','layout']) IS NOT TRUE OR t->>'purpose' IS DISTINCT FROM 'DEPOSIT_ACCOUNT_ENROLLMENT'
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

-- Fixed integrity capture preserves every retained source/notice and its usable catalog identities.
CREATE FUNCTION masters_document_current_private.capture_graph(t JSONB) RETURNS JSONB LANGUAGE plpgsql VOLATILE SET search_path=pg_catalog AS $$
DECLARE l public.document_layout_publication%ROWTYPE;r public.document_template_revision%ROWTYPE;d JSONB;ref JSONB;refs JSONB;captured JSONB:='[]'::jsonb;joined JSONB;links JSONB;
BEGIN
 SELECT * INTO l FROM public.document_layout_publication WHERE layout_revision_id=(t->'layout'->>'id')::uuid AND source_revision_id=(t->'template'->>'id')::uuid
  AND version=t->'layout'->>'versionText' AND locale_code=t->>'locale' AND definition_digest=t->'layout'->>'digest' AND lifecycle='PUBLISHED' AND state_version=0;
 IF NOT FOUND THEN RETURN NULL;END IF;d:=l.canonical_json::jsonb;
 IF masters_document_current_private.document_layout_definition_valid(d) IS NOT TRUE OR masters_document_current_private.document_layout_canonical(d)<>l.canonical_json
  OR l.definition_digest<>encode(sha256(convert_to('MASTERS_DOCUMENT_LAYOUT_V1'||chr(10)||l.canonical_json,'UTF8')),'hex')
  OR d->'sourceReference'->>'revisionId'<>l.source_revision_id::text OR d->'sourceReference'->>'templateId'<>l.template_id::text OR d->'sourceReference'->>'templateCode'<>l.template_code OR d->'sourceReference'->>'version'<>l.version OR d->'sourceReference'->>'localeCode'<>l.locale_code
  OR d->'sourceReference'->>'sourceDigest'<>t->'template'->>'digest' THEN RETURN NULL;END IF;
 refs:=jsonb_build_array(d->'sourceReference');FOR ref IN SELECT value FROM jsonb_array_elements(d->'semanticDefinition'->'legalNotices') LOOP refs:=refs||jsonb_build_array(ref->'masterReference');END LOOP;
 FOR ref IN SELECT value FROM jsonb_array_elements(refs) ORDER BY value->>'revisionId' LOOP
  SELECT rr.* INTO r FROM public.document_template_revision rr JOIN public.document_template_catalog c ON c.template_id=rr.template_id AND c.template_code=rr.template_code
   JOIN public.document_template_localization loc ON loc.localization_id=rr.localization_id AND loc.template_id=rr.template_id AND loc.locale_id=rr.locale_id
   JOIN public.language_locale lang ON lang.locale_id=rr.locale_id AND lang.locale_code=rr.locale_code JOIN public.document_template_type_catalog typ ON typ.type_id=c.type_id
   WHERE rr.revision_id=(ref->>'revisionId')::uuid AND rr.lifecycle='PUBLISHED' AND c.status='ACTIVE' AND loc.status='ACTIVE' AND lang.status='ACTIVE' AND typ.status='ACTIVE';
  IF NOT FOUND OR masters_document_current_private.document_layout_reference_matches(ref,r) IS NOT TRUE OR r.media_type<>'text/html' OR r.style_content<>'' THEN RETURN NULL;END IF;
  IF r.revision_id<>l.source_revision_id AND (r.source_snapshot->>'category' IS DISTINCT FROM 'LEGAL_NOTICE' OR masters_document_current_private.document_layout_notice_schema_valid(r.input_schema) IS NOT TRUE) THEN RETURN NULL;END IF;
  SELECT jsonb_build_object('revision',to_jsonb(rr),'catalog',to_jsonb(c),'localization',to_jsonb(loc),'locale',to_jsonb(lang),'type',to_jsonb(typ)) INTO joined
   FROM public.document_template_revision rr JOIN public.document_template_catalog c ON c.template_id=rr.template_id JOIN public.document_template_localization loc ON loc.localization_id=rr.localization_id
   JOIN public.language_locale lang ON lang.locale_id=rr.locale_id JOIN public.document_template_type_catalog typ ON typ.type_id=c.type_id WHERE rr.revision_id=r.revision_id;
  IF r.revision_id=l.source_revision_id AND joined->'type'->>'type_code'<>t->>'documentType' THEN RETURN NULL;END IF;captured:=captured||jsonb_build_array(joined);
 END LOOP;
 SELECT coalesce(jsonb_agg(to_jsonb(n) ORDER BY notice_position),'[]'::jsonb) INTO links FROM public.document_layout_notice n WHERE layout_revision_id=l.layout_revision_id;
 IF jsonb_array_length(links)<>jsonb_array_length(d->'semanticDefinition'->'legalNotices') OR EXISTS(SELECT 1 FROM jsonb_array_elements(d->'semanticDefinition'->'legalNotices') WITH ORDINALITY expected(value,ordinal)
  WHERE NOT EXISTS(SELECT 1 FROM public.document_layout_notice n WHERE n.layout_revision_id=l.layout_revision_id AND n.notice_position=expected.ordinal-1 AND n.role=expected.value->>'role' AND n.placement=expected.value->>'placement' AND n.revision_id=(expected.value->'masterReference'->>'revisionId')::uuid)) THEN RETURN NULL;END IF;
 RETURN jsonb_build_object('layout',to_jsonb(l),'references',captured,'noticeLinks',links);
END $$;

CREATE FUNCTION public.document_reference_current_runtime_allowed() RETURNS BOOLEAN LANGUAGE sql STABLE SECURITY DEFINER SET search_path=pg_catalog AS $$
 SELECT session_user='masters_document_current_reader' AND r.rolcanlogin AND NOT r.rolinherit AND NOT r.rolsuper AND NOT r.rolbypassrls AND NOT r.rolcreatedb AND NOT r.rolcreaterole AND NOT r.rolreplication
 AND masters_document_current_private.helper_closure_allowed()
 AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_auth_members m JOIN pg_catalog.pg_roles v ON v.oid=m.member WHERE v.rolname IN('masters_document_current_guard','masters_document_current_reader'))
 AND NOT pg_catalog.has_schema_privilege('masters_document_current_guard','public','CREATE') AND NOT pg_catalog.has_database_privilege('masters_document_current_guard',current_database(),'CREATE')
 AND NOT pg_catalog.pg_has_role(session_user,'masters_document_current_guard','MEMBER') AND NOT pg_catalog.has_schema_privilege(session_user,'public','CREATE')
 AND NOT pg_catalog.has_database_privilege(session_user,current_database(),'CREATE,TEMP') AND NOT pg_catalog.has_schema_privilege(session_user,'masters_document_current_private','USAGE')
 AND EXISTS(SELECT 1 FROM pg_catalog.pg_roles g WHERE g.rolname='masters_document_current_guard' AND NOT g.rolcanlogin AND NOT g.rolinherit AND NOT g.rolsuper AND NOT g.rolbypassrls AND NOT g.rolcreatedb AND NOT g.rolcreaterole AND NOT g.rolreplication)
 AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relname IN('document_layout_publication','document_layout_notice','document_template_revision','document_template_catalog','document_template_localization','document_template_type_catalog','language_locale') AND (pg_catalog.pg_has_role(session_user,c.relowner,'MEMBER') OR pg_catalog.has_table_privilege(session_user,c.oid,'INSERT,UPDATE,DELETE,TRUNCATE,TRIGGER,REFERENCES') OR pg_catalog.has_table_privilege(session_user,c.oid,'SELECT') OR pg_catalog.has_any_column_privilege(session_user,c.oid,'SELECT,INSERT,UPDATE,REFERENCES') OR pg_catalog.has_table_privilege('masters_document_current_guard',c.oid,'INSERT,UPDATE,DELETE,TRUNCATE,TRIGGER,REFERENCES') OR pg_catalog.has_any_column_privilege('masters_document_current_guard',c.oid,'INSERT,UPDATE,REFERENCES') OR pg_catalog.pg_has_role('masters_document_current_guard',c.relowner,'MEMBER')))
 AND (SELECT count(*) FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind='r' AND c.relname IN('document_layout_publication','document_layout_notice','document_template_revision','document_template_catalog','document_template_localization','document_template_type_catalog','language_locale'))=7
 AND EXISTS(SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace JOIN pg_catalog.pg_roles own ON own.oid=c.relowner WHERE n.nspname='public' AND c.relname='document_reference_current_observation' AND c.relkind='v' AND c.reloptions @> ARRAY['security_invoker=false','security_barrier=true'] AND own.rolname='masters_document_current_guard')
 AND pg_catalog.has_table_privilege(session_user,'public.document_reference_current_observation','SELECT')
 AND NOT pg_catalog.has_table_privilege(session_user,'public.document_reference_current_observation','INSERT,UPDATE,DELETE,TRUNCATE,TRIGGER,REFERENCES')
 AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace CROSS JOIN LATERAL pg_catalog.aclexplode(coalesce(c.relacl,pg_catalog.acldefault('r',c.relowner))) a WHERE n.nspname='public' AND c.relname='document_reference_current_observation' AND a.grantee<>c.relowner AND (a.grantee<>r.oid OR a.privilege_type<>'SELECT' OR a.is_grantable))
 AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_proc f JOIN pg_catalog.pg_namespace n ON n.oid=f.pronamespace CROSS JOIN LATERAL pg_catalog.aclexplode(coalesce(f.proacl,pg_catalog.acldefault('f',f.proowner))) a WHERE n.nspname='public' AND f.proname IN('document_reference_current_runtime_allowed','document_reference_current_begin','document_reference_current_scope_matches','document_reference_current_complete') AND a.grantee<>f.proowner AND (a.grantee<>r.oid OR a.privilege_type<>'EXECUTE' OR a.is_grantable))
 AND EXISTS(SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace JOIN pg_catalog.pg_roles own ON own.oid=c.relowner WHERE n.nspname='masters_document_current_private' AND c.relname='context' AND c.relrowsecurity AND c.relforcerowsecurity AND own.rolname='masters_document_current_guard')
 AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_proc f JOIN pg_catalog.pg_namespace n ON n.oid=f.pronamespace CROSS JOIN LATERAL pg_catalog.aclexplode(coalesce(f.proacl,pg_catalog.acldefault('f',f.proowner))) a WHERE n.nspname='masters_document_current_private' AND a.grantee<>f.proowner)
 AND (SELECT count(*) FROM pg_catalog.pg_proc f JOIN pg_catalog.pg_namespace n ON n.oid=f.pronamespace JOIN pg_catalog.pg_roles own ON own.oid=f.proowner WHERE n.nspname='masters_document_current_private' AND f.proname IN('reject_change','target_valid','capture_graph','current_context','helper_closure_allowed') AND f.proconfig @> ARRAY['search_path=pg_catalog'] AND own.rolname='masters_document_current_guard')=5
 AND (SELECT count(*) FROM pg_catalog.pg_proc f JOIN pg_catalog.pg_namespace n ON n.oid=f.pronamespace JOIN pg_catalog.pg_roles own ON own.oid=f.proowner WHERE n.nspname='public' AND f.proname IN('document_reference_current_runtime_allowed','document_reference_current_begin','document_reference_current_scope_matches','document_reference_current_complete') AND f.prosecdef AND f.proconfig @> ARRAY['search_path=pg_catalog'] AND own.rolname='masters_document_current_guard')=4
 FROM pg_catalog.pg_roles r WHERE r.rolname=session_user
$$;
CREATE FUNCTION masters_document_current_private.current_context() RETURNS masters_document_current_private.context LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_document_current_private.context%ROWTYPE;now_at TIMESTAMPTZ;
BEGIN
 IF public.document_reference_current_runtime_allowed() IS DISTINCT FROM true THEN RAISE EXCEPTION 'Restricted document CURRENT runtime required' USING ERRCODE='42501';END IF;
 SELECT * INTO c FROM masters_document_current_private.context WHERE xid=pg_catalog.pg_current_xact_id_if_assigned() AND backend_pid=pg_catalog.pg_backend_pid() AND db_session_user=session_user;
 IF NOT FOUND THEN RAISE EXCEPTION 'Protected document CURRENT context required' USING ERRCODE='42501';END IF;now_at:=pg_catalog.clock_timestamp();
 IF now_at<c.recorded_at OR now_at<c.installed_at OR now_at>=c.effective_until OR now_at>=c.authority_until OR c.effective_until>c.installed_at+INTERVAL '5 seconds' OR c.effective_until>c.authority_until THEN RAISE EXCEPTION 'Finite document CURRENT context required' USING ERRCODE='42501';END IF;RETURN c;
END $$;
CREATE FUNCTION public.document_reference_current_begin(p_nonce UUID,p_target TEXT,p_issuer TEXT,p_subject TEXT,p_tenant TEXT,p_until TIMESTAMPTZ)
 RETURNS TABLE(context_nonce UUID,target_canonical TEXT,db_now_millis BIGINT,recorded_at TIMESTAMPTZ,effective_until TIMESTAMPTZ) LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE started TIMESTAMPTZ;observed TIMESTAMPTZ;bound TIMESTAMPTZ;t JSONB;captured JSONB;tx XID8;
BEGIN
 started:=pg_catalog.clock_timestamp();
 IF public.document_reference_current_runtime_allowed() IS DISTINCT FROM true OR current_setting('transaction_isolation')<>'repeatable read' OR current_setting('transaction_read_only')<>'off' OR p_nonce IS NULL OR p_nonce='00000000-0000-0000-0000-000000000000'::uuid OR p_target IS NULL OR octet_length(p_target)>8192 OR p_issuer IS NULL OR length(trim(p_issuer)) NOT BETWEEN 1 AND 500 OR p_subject IS NULL OR length(trim(p_subject)) NOT BETWEEN 1 AND 255 OR p_tenant IS NULL OR length(trim(p_tenant)) NOT BETWEEN 1 AND 255 OR p_until IS NULL OR NOT isfinite(p_until) OR p_until<=started THEN RAISE EXCEPTION 'Exact finite document CURRENT admission required' USING ERRCODE='42501';END IF;
 t:=p_target::jsonb;IF masters_document_current_private.target_valid(t) IS DISTINCT FROM true OR masters_document_current_private.document_layout_canonical(t)<>p_target OR t->'product'->>'tenantId'<>p_tenant THEN RAISE EXCEPTION 'Exact document CURRENT target required' USING ERRCODE='42501';END IF;
 tx:=pg_catalog.pg_current_xact_id();IF EXISTS(SELECT 1 FROM masters_document_current_private.context WHERE xid=tx AND backend_pid=pg_catalog.pg_backend_pid() AND db_session_user=session_user) THEN RAISE EXCEPTION 'One original document CURRENT target required' USING ERRCODE='42501';END IF;
 captured:=masters_document_current_private.capture_graph(t);IF captured IS NULL THEN RETURN;END IF;observed:=pg_catalog.clock_timestamp();bound:=least(started+INTERVAL '5 seconds',p_until);IF observed<started OR observed>=bound THEN RAISE EXCEPTION 'Document CURRENT budget exhausted' USING ERRCODE='42501';END IF;
 INSERT INTO masters_document_current_private.context VALUES(p_nonce,tx,pg_catalog.pg_backend_pid(),session_user,p_target,p_issuer,p_subject,p_tenant,started,observed,p_until,bound,captured);
 RETURN QUERY SELECT p_nonce,p_target,floor(extract(epoch FROM observed)*1000)::bigint,observed,bound;
END $$;
CREATE FUNCTION public.document_reference_current_scope_matches(p_layout UUID,p_source UUID) RETURNS BOOLEAN LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_document_current_private.context%ROWTYPE;t JSONB;
BEGIN c:=masters_document_current_private.current_context();t:=c.target_canonical::jsonb;RETURN (t->'layout'->>'id')::uuid=p_layout AND (t->'template'->>'id')::uuid=p_source;END $$;
CREATE VIEW public.document_reference_current_observation WITH(security_invoker=false,security_barrier=true) AS
 SELECT l.layout_revision_id,l.source_revision_id,l.version,l.locale_code,l.canonical_json,l.definition_digest,r.source_digest,typ.type_code AS document_type
 FROM public.document_layout_publication l JOIN public.document_template_revision r ON r.revision_id=l.source_revision_id JOIN public.document_template_catalog c ON c.template_id=r.template_id JOIN public.document_template_type_catalog typ ON typ.type_id=c.type_id
 WHERE public.document_reference_current_scope_matches(l.layout_revision_id,l.source_revision_id);
CREATE FUNCTION public.document_reference_current_complete(p_nonce UUID) RETURNS BOOLEAN LANGUAGE plpgsql VOLATILE SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE c masters_document_current_private.context%ROWTYPE;captured JSONB;
BEGIN c:=masters_document_current_private.current_context();IF c.nonce IS DISTINCT FROM p_nonce THEN RAISE EXCEPTION 'Exact document CURRENT nonce required' USING ERRCODE='42501';END IF;captured:=masters_document_current_private.capture_graph(c.target_canonical::jsonb);IF captured IS NULL OR captured IS DISTINCT FROM c.captured_graph THEN RAISE EXCEPTION 'Document CURRENT snapshot changed' USING ERRCODE='42501';END IF;PERFORM masters_document_current_private.current_context();RETURN true;END $$;
REVOKE ALL ON ALL FUNCTIONS IN SCHEMA masters_document_current_private FROM PUBLIC,masters_document_current_reader;
REVOKE ALL ON FUNCTION public.document_reference_current_runtime_allowed(),public.document_reference_current_begin(UUID,TEXT,TEXT,TEXT,TEXT,TIMESTAMPTZ),public.document_reference_current_scope_matches(UUID,UUID),public.document_reference_current_complete(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.document_reference_current_runtime_allowed(),public.document_reference_current_begin(UUID,TEXT,TEXT,TEXT,TEXT,TIMESTAMPTZ),public.document_reference_current_scope_matches(UUID,UUID),public.document_reference_current_complete(UUID) TO masters_document_current_reader;
REVOKE ALL ON public.document_reference_current_observation FROM PUBLIC;
GRANT SELECT ON public.document_reference_current_observation TO masters_document_current_reader;
RESET ROLE;
REVOKE CREATE ON SCHEMA public FROM masters_document_current_guard;
GRANT USAGE ON SCHEMA public TO masters_document_current_reader;
DO $$ BEGIN IF pg_catalog.has_schema_privilege('masters_document_current_guard','public','CREATE') THEN RAISE EXCEPTION 'Temporary document CURRENT CREATE cleanup incomplete' USING ERRCODE='42501';END IF;END $$;
