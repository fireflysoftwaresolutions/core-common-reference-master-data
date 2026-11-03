-- Layout metadata authority only; no renderer, assets or output bytes are installed by this migration.
-- MASTERS_JSON_V1 retains Java UTF-16 key order and Jackson's control-escape spelling.
CREATE FUNCTION document_layout_utf16_key(value text) RETURNS bytea LANGUAGE sql IMMUTABLE STRICT AS $$
 SELECT decode(coalesce(string_agg(CASE WHEN ascii(ch)<=65535 THEN lpad(to_hex(ascii(ch)),4,'0') ELSE lpad(to_hex(55296+((ascii(ch)-65536)>>10)),4,'0')||lpad(to_hex(56320+((ascii(ch)-65536)&1023)),4,'0') END,'' ORDER BY ordinal),''),'hex') FROM regexp_split_to_table(value,'') WITH ORDINALITY c(ch,ordinal) WHERE ch<>''
$$;
CREATE FUNCTION document_layout_quote(value text) RETURNS text LANGUAGE plpgsql IMMUTABLE STRICT AS $$
DECLARE result text:=to_jsonb(value)::text;cp integer;hex text;
BEGIN
 FOREACH cp IN ARRAY ARRAY[11,14,15,26,27,28,29,30,31] LOOP
  hex:=lpad(to_hex(cp),4,'0');
  result:=regexp_replace(result,'(?<!'||repeat(chr(92),2)||')((?:'||repeat(chr(92),4)||')*)'||repeat(chr(92),2)||'u'||hex,chr(92)||'1'||repeat(chr(92),2)||'u'||upper(hex),'g');
 END LOOP;RETURN result;
END $$;
CREATE FUNCTION document_layout_canonical(j jsonb) RETURNS text LANGUAGE plpgsql IMMUTABLE STRICT AS $$
DECLARE result text;
BEGIN
 IF jsonb_typeof(j)='object' THEN SELECT '{'||coalesce(string_agg(document_layout_quote(key)||':'||document_layout_canonical(value),',' ORDER BY document_layout_utf16_key(key)),'')||'}' INTO result FROM jsonb_each(j);
 ELSIF jsonb_typeof(j)='array' THEN SELECT '['||coalesce(string_agg(document_layout_canonical(value),',' ORDER BY ordinal),'')||']' INTO result FROM jsonb_array_elements(j) WITH ORDINALITY a(value,ordinal);
 ELSIF jsonb_typeof(j)='string' THEN result:=document_layout_quote(j#>>'{}');
 ELSE result:=j::text; IF jsonb_typeof(j)='number' AND position('.' IN result)>0 THEN result:=rtrim(rtrim(result,'0'),'.'); END IF; END IF;
 RETURN result;
END $$;
CREATE FUNCTION document_layout_shape(j jsonb,keys text[]) RETURNS boolean LANGUAGE sql IMMUTABLE AS $$ SELECT coalesce(jsonb_typeof(j)='object' AND j ?& keys AND (j-keys)='{}'::jsonb,false) $$;
CREATE FUNCTION document_layout_text(j jsonb,maximum integer) RETURNS boolean LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE v text; boundary text:=chr(32)||chr(160)||chr(5760)||chr(8192)||chr(8193)||chr(8194)||chr(8195)||chr(8196)||chr(8197)||chr(8198)||chr(8199)||chr(8200)||chr(8201)||chr(8202)||chr(8232)||chr(8233)||chr(8239)||chr(8287)||chr(12288);
BEGIN IF j IS NULL OR jsonb_typeof(j) IS DISTINCT FROM 'string' THEN RETURN false; END IF;v:=j#>>'{}';RETURN char_length(v) BETWEEN 1 AND maximum AND position(left(v,1) IN boundary)=0 AND position(right(v,1) IN boundary)=0 AND v !~ ('['||chr(1)||'-'||chr(31)||chr(127)||'-'||chr(159)||']');END $$;
CREATE FUNCTION document_layout_uuid(j jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE AS $$ SELECT document_layout_text(j,36) AND (j#>>'{}') COLLATE "C" ~ '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$' $$;
CREATE FUNCTION document_layout_sha(j jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE AS $$ SELECT document_layout_text(j,64) AND (j#>>'{}') COLLATE "C" ~ '^[0-9a-f]{64}$' $$;
CREATE FUNCTION document_layout_master_reference_valid(j jsonb) RETURNS boolean LANGUAGE plpgsql IMMUTABLE AS $$
BEGIN
 RETURN document_layout_shape(j,ARRAY['revisionId','templateId','templateCode','version','localeCode','mediaType','canonicalizationVersion','sourceDigest','contentDigest','schemaDigest','styleDigest'])
 AND document_layout_uuid(j->'revisionId') AND document_layout_uuid(j->'templateId') AND document_layout_text(j->'templateCode',100)
 AND document_layout_text(j->'version',20) AND (j->>'version') COLLATE "C" ~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,19}$'
 AND document_layout_text(j->'localeCode',50) AND (j->>'localeCode') COLLATE "C" ~ '^[A-Za-z0-9][A-Za-z0-9_-]{0,49}$'
 AND j->'mediaType'='"text/html"'::jsonb AND j->'canonicalizationVersion'='"MASTERS_JSON_V1"'::jsonb
 AND document_layout_sha(j->'sourceDigest') AND document_layout_sha(j->'contentDigest') AND document_layout_sha(j->'schemaDigest') AND document_layout_sha(j->'styleDigest');
END $$;
CREATE FUNCTION document_layout_profile_valid(p jsonb) RETURNS boolean LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE r jsonb;token jsonb;
BEGIN
 IF NOT document_layout_shape(p,ARRAY['reference','sectionMode','pageSize','orientation','allowedClassTokens','maxSections','maxContentBytes']) THEN RETURN false;END IF;r:=p->'reference';
 IF NOT document_layout_shape(r,ARRAY['code','version','mediaType','manifestDigest']) OR NOT document_layout_text(r->'code',64) OR (r->>'code') COLLATE "C" !~ '^[A-Z][A-Z0-9_]{0,63}$' OR NOT document_layout_text(r->'version',64) OR (r->>'version') COLLATE "C" !~ '^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$' OR r->'mediaType' IS DISTINCT FROM '"text/html"'::jsonb OR NOT document_layout_sha(r->'manifestDigest') THEN RETURN false;END IF;
 IF p->'sectionMode' IS DISTINCT FROM '"AUTHORED_PAGES"'::jsonb OR p->'pageSize' IS DISTINCT FROM '"A4"'::jsonb OR p->'orientation' IS DISTINCT FROM '"PORTRAIT"'::jsonb OR jsonb_typeof(p->'allowedClassTokens') IS DISTINCT FROM 'array' OR jsonb_typeof(p->'maxSections') IS DISTINCT FROM 'number' OR jsonb_typeof(p->'maxContentBytes') IS DISTINCT FROM 'number' THEN RETURN false;END IF;
 IF (p->>'maxSections') !~ '^[1-9][0-9]?$' OR (p->>'maxContentBytes') !~ '^[1-9][0-9]{0,6}$' THEN RETURN false;END IF;
 IF (p->>'maxSections')::integer>32 OR (p->>'maxContentBytes')::integer>1048576 OR jsonb_array_length(p->'allowedClassTokens') NOT BETWEEN 1 AND 64 THEN RETURN false;END IF;
 FOR token IN SELECT value FROM jsonb_array_elements(p->'allowedClassTokens') LOOP IF NOT document_layout_text(token,64) OR (token#>>'{}') COLLATE "C" !~ '^[a-z][a-z0-9-]{0,63}$' THEN RETURN false;END IF;END LOOP;
 RETURN (SELECT count(DISTINCT value) FROM jsonb_array_elements(p->'allowedClassTokens'))=jsonb_array_length(p->'allowedClassTokens');
END $$;
CREATE FUNCTION document_layout_notice_schema_valid(s jsonb) RETURNS boolean LANGUAGE sql IMMUTABLE AS $$
 SELECT coalesce(jsonb_typeof(s)='object' AND s ?& ARRAY['$schema','type','additionalProperties'] AND (s-ARRAY['$schema','type','additionalProperties','properties','required'])='{}'::jsonb AND s->'type'='"object"'::jsonb AND s->'additionalProperties'='false'::jsonb AND s->'$schema'='"https://json-schema.org/draft/2020-12/schema"'::jsonb AND (NOT(s?'properties') OR s->'properties'='{}'::jsonb) AND (NOT(s?'required') OR s->'required'='[]'::jsonb),false)
$$;
CREATE FUNCTION document_layout_definition_valid(d jsonb) RETURNS boolean LANGUAGE plpgsql IMMUTABLE AS $$
DECLARE s jsonb;n jsonb;roles text[]:=ARRAY[]::text[];role text;
BEGIN
 IF NOT document_layout_shape(d,ARRAY['sourceReference','profileMetadata','semanticDefinition']) OR document_layout_master_reference_valid(d->'sourceReference') IS NOT TRUE OR document_layout_profile_valid(d->'profileMetadata') IS NOT TRUE OR d->'sourceReference'->>'styleDigest'<>'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855' THEN RETURN false;END IF;s:=d->'semanticDefinition';
 IF NOT document_layout_shape(s,ARRAY['sectionMode','pageSize','orientation','legalNotices']) OR s->'sectionMode' IS DISTINCT FROM '"AUTHORED_PAGES"'::jsonb OR s->'pageSize' IS DISTINCT FROM '"A4"'::jsonb OR s->'orientation' IS DISTINCT FROM '"PORTRAIT"'::jsonb OR jsonb_typeof(s->'legalNotices') IS DISTINCT FROM 'array' THEN RETURN false;END IF;
 IF jsonb_array_length(s->'legalNotices') NOT BETWEEN 1 AND 4 THEN RETURN false;END IF;
 FOR n IN SELECT value FROM jsonb_array_elements(s->'legalNotices') LOOP
  IF NOT document_layout_shape(n,ARRAY['role','placement','masterReference']) OR document_layout_master_reference_valid(n->'masterReference') IS NOT TRUE OR NOT document_layout_text(n->'role',30) OR NOT document_layout_text(n->'placement',20) THEN RETURN false;END IF;role:=n->>'role';
  IF role NOT IN ('GENERAL_DISCLOSURE','PRIVACY_NOTICE','COMPLAINTS_NOTICE','SIGNATURE_NOTICE') OR role=ANY(roles) OR n->'masterReference'->>'localeCode'<>d->'sourceReference'->>'localeCode' OR n->'masterReference'->>'styleDigest'<>'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855' OR n->'masterReference'->>'revisionId'=d->'sourceReference'->>'revisionId' THEN RETURN false;END IF;
  IF (role='GENERAL_DISCLOSURE' AND n->>'placement'<>'FOOTER') OR (role<>'GENERAL_DISCLOSURE' AND n->>'placement'<>'BODY_NOTE') THEN RETURN false;END IF;roles:=array_append(roles,role);
 END LOOP;
 RETURN 'GENERAL_DISCLOSURE'=ANY(roles);
END $$;
CREATE FUNCTION document_layout_reference_matches(j jsonb,r document_template_revision) RETURNS boolean LANGUAGE sql IMMUTABLE AS $$
 SELECT j->>'revisionId'=r.revision_id::text AND j->>'templateId'=r.template_id::text AND j->>'templateCode'=r.template_code AND j->>'version'=r.version AND j->>'localeCode'=r.locale_code AND j->>'mediaType'=r.media_type AND j->>'canonicalizationVersion'='MASTERS_JSON_V1' AND j->>'sourceDigest'=r.source_digest AND j->>'contentDigest'=r.content_digest AND j->>'schemaDigest'=r.schema_digest AND j->>'styleDigest'=r.style_digest
 AND r.content_digest=encode(sha256(convert_to(r.template_content,'UTF8')),'hex') AND r.schema_digest=encode(sha256(convert_to(document_layout_canonical(r.input_schema),'UTF8')),'hex') AND r.style_digest=encode(sha256(convert_to(r.style_content,'UTF8')),'hex') AND r.source_digest=encode(sha256(convert_to(document_layout_canonical(r.source_snapshot),'UTF8')),'hex')
$$;
CREATE TABLE document_layout_publication (
 layout_revision_id uuid PRIMARY KEY,source_revision_id uuid NOT NULL UNIQUE REFERENCES document_template_revision(revision_id) ON DELETE RESTRICT,
 template_id uuid NOT NULL REFERENCES document_template_catalog(template_id) ON DELETE RESTRICT,template_code varchar(100) NOT NULL,version varchar(20) NOT NULL,locale_code varchar(50) NOT NULL,
 canonical_json text NOT NULL CHECK(octet_length(canonical_json)<=32768),definition_digest char(64) NOT NULL,
 publication_intent_canonical text NOT NULL CHECK(octet_length(publication_intent_canonical)<=32768),publication_intent_digest char(64) NOT NULL,
 publication_action_id uuid NOT NULL UNIQUE,published_at_epoch_millis bigint NOT NULL CHECK(published_at_epoch_millis BETWEEN 0 AND 253402300799999),published_by varchar(255) NOT NULL CHECK(document_layout_text(to_jsonb(published_by),255)),
 lifecycle varchar(9) NOT NULL CHECK(lifecycle IN('PUBLISHED','RETIRED')),state_version integer NOT NULL,
 retirement_action_id uuid UNIQUE,retired_at_epoch_millis bigint,retired_by varchar(255),retirement_reason varchar(500),
 UNIQUE(template_id,version,locale_code),
 CHECK(canonical_json=document_layout_canonical(canonical_json::jsonb)),CHECK(document_layout_definition_valid(canonical_json::jsonb) IS TRUE),
 CHECK(definition_digest=encode(sha256(convert_to('MASTERS_DOCUMENT_LAYOUT_V1'||chr(10)||canonical_json,'UTF8')),'hex')),
 CHECK(publication_intent_canonical=document_layout_canonical(publication_intent_canonical::jsonb)),CHECK(publication_intent_digest=encode(sha256(convert_to('MASTERS_DOCUMENT_LAYOUT_ACTION_V1'||chr(10)||publication_intent_canonical,'UTF8')),'hex')),
 CHECK((lifecycle='PUBLISHED' AND state_version=0 AND retirement_action_id IS NULL AND retired_at_epoch_millis IS NULL AND retired_by IS NULL AND retirement_reason IS NULL) OR (lifecycle='RETIRED' AND state_version=1 AND retirement_action_id IS NOT NULL AND retired_at_epoch_millis BETWEEN published_at_epoch_millis AND 253402300799999 AND document_layout_text(to_jsonb(retired_by),255) AND document_layout_text(to_jsonb(retirement_reason),500)))
);
CREATE INDEX document_layout_publication_exact ON document_layout_publication(template_code,version,locale_code);
CREATE TABLE document_layout_notice (
 layout_revision_id uuid NOT NULL REFERENCES document_layout_publication(layout_revision_id) ON DELETE RESTRICT,
 notice_position smallint NOT NULL CHECK(notice_position BETWEEN 0 AND 3),
 role varchar(30) NOT NULL,placement varchar(20) NOT NULL,
 revision_id uuid NOT NULL REFERENCES document_template_revision(revision_id) ON DELETE RESTRICT,
 PRIMARY KEY(layout_revision_id,notice_position),UNIQUE(layout_revision_id,role),
 CHECK((role='GENERAL_DISCLOSURE' AND placement='FOOTER') OR (role IN('PRIVACY_NOTICE','COMPLAINTS_NOTICE','SIGNATURE_NOTICE') AND placement='BODY_NOTE'))
);
CREATE FUNCTION validate_document_layout_notice() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE notice jsonb;
BEGIN
 SELECT canonical_json::jsonb->'semanticDefinition'->'legalNotices'->NEW.notice_position::integer INTO notice FROM document_layout_publication WHERE layout_revision_id=NEW.layout_revision_id;
 IF NOT FOUND OR jsonb_typeof(notice) IS DISTINCT FROM 'object' OR ROW(NEW.role,NEW.placement,NEW.revision_id::text) IS DISTINCT FROM ROW(notice->>'role',notice->>'placement',notice->'masterReference'->>'revisionId') THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_NOTICE_DECLARATION' USING ERRCODE='23514';END IF;RETURN NEW;
END $$;
CREATE TRIGGER document_layout_notice_declaration BEFORE INSERT ON document_layout_notice FOR EACH ROW EXECUTE FUNCTION validate_document_layout_notice();
CREATE TABLE document_layout_action (
 action_id uuid PRIMARY KEY,layout_revision_id uuid NOT NULL REFERENCES document_layout_publication(layout_revision_id) ON DELETE RESTRICT,kind varchar(7) NOT NULL CHECK(kind IN('PUBLISH','RETIRE')),expected_state_version integer,request_canonical text NOT NULL,intent_digest char(64) NOT NULL,acted_at_epoch_millis bigint NOT NULL,acted_by varchar(255) NOT NULL,
 CHECK((kind='PUBLISH' AND expected_state_version IS NULL) OR (kind='RETIRE' AND expected_state_version=0)),CHECK(request_canonical=document_layout_canonical(request_canonical::jsonb)),CHECK(intent_digest=encode(sha256(convert_to('MASTERS_DOCUMENT_LAYOUT_ACTION_V1'||chr(10)||request_canonical,'UTF8')),'hex'))
);
CREATE TABLE document_layout_history (action_id uuid PRIMARY KEY REFERENCES document_layout_action(action_id) ON DELETE RESTRICT,layout_revision_id uuid NOT NULL REFERENCES document_layout_publication(layout_revision_id) ON DELETE RESTRICT,lifecycle varchar(9) NOT NULL,state_version integer NOT NULL,definition_digest char(64) NOT NULL,occurred_at_epoch_millis bigint NOT NULL,actor varchar(255) NOT NULL);
CREATE TABLE document_layout_outbox (event_id uuid PRIMARY KEY DEFAULT gen_random_uuid(),action_id uuid NOT NULL UNIQUE REFERENCES document_layout_action(action_id) ON DELETE RESTRICT,layout_revision_id uuid NOT NULL REFERENCES document_layout_publication(layout_revision_id) ON DELETE RESTRICT,payload_json jsonb NOT NULL);
CREATE FUNCTION protect_document_layout_publication() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE d jsonb;r document_template_revision%ROWTYPE;j jsonb;n jsonb;refs jsonb;expected text;
BEGIN
 IF TG_OP='DELETE' THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_RETAINED' USING ERRCODE='23514';END IF;
 IF TG_OP='UPDATE' THEN
  IF OLD.lifecycle<>'PUBLISHED' OR NEW.lifecycle<>'RETIRED' OR NEW.state_version<>1 OR (to_jsonb(NEW)-ARRAY['lifecycle','state_version','retirement_action_id','retired_at_epoch_millis','retired_by','retirement_reason']) IS DISTINCT FROM (to_jsonb(OLD)-ARRAY['lifecycle','state_version','retirement_action_id','retired_at_epoch_millis','retired_by','retirement_reason']) THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_IMMUTABLE' USING ERRCODE='23514';END IF;
  RETURN NEW;
 END IF;
 IF NEW.lifecycle<>'PUBLISHED' OR NEW.state_version<>0 OR document_layout_definition_valid(NEW.canonical_json::jsonb) IS NOT TRUE THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_INITIAL_SHAPE' USING ERRCODE='23514';END IF;
 -- Existing committed V1 publications cannot acquire a layout association retroactively.
 IF NOT EXISTS(SELECT 1 FROM document_template_revision WHERE revision_id=NEW.source_revision_id AND xmin=pg_current_xact_id()::xid AND lifecycle='PUBLISHED') THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_NEW_SOURCE_REQUIRED' USING ERRCODE='23514';END IF;
 d:=NEW.canonical_json::jsonb;
 IF d->'sourceReference'->>'revisionId'<>NEW.source_revision_id::text OR d->'sourceReference'->>'templateId'<>NEW.template_id::text OR d->'sourceReference'->>'templateCode'<>NEW.template_code OR d->'sourceReference'->>'version'<>NEW.version OR d->'sourceReference'->>'localeCode'<>NEW.locale_code THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_SOURCE_IDENTITY' USING ERRCODE='23514';END IF;
 refs:=jsonb_build_array(d->'sourceReference');FOR n IN SELECT value FROM jsonb_array_elements(d->'semanticDefinition'->'legalNotices') LOOP refs:=refs||jsonb_build_array(n->'masterReference');END LOOP;
 FOR j IN SELECT value FROM jsonb_array_elements(refs) ORDER BY value->>'revisionId' LOOP
  PERFORM r0.revision_id FROM document_template_revision r0 JOIN document_template_catalog c ON c.template_id=r0.template_id AND c.template_code=r0.template_code JOIN document_template_localization l ON l.localization_id=r0.localization_id AND l.template_id=r0.template_id AND l.locale_id=r0.locale_id JOIN language_locale loc ON loc.locale_id=r0.locale_id AND loc.locale_code=r0.locale_code JOIN document_template_type_catalog t ON t.type_id=c.type_id WHERE r0.revision_id=(j->>'revisionId')::uuid AND r0.lifecycle='PUBLISHED' AND c.status='ACTIVE' AND l.status='ACTIVE' AND loc.status='ACTIVE' AND t.status='ACTIVE' FOR SHARE OF r0,c,l,loc,t;
  IF NOT FOUND THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_SOURCE_NOT_USABLE' USING ERRCODE='23514';END IF;
  SELECT * INTO r FROM document_template_revision WHERE revision_id=(j->>'revisionId')::uuid;
  IF document_layout_reference_matches(j,r) IS NOT TRUE OR r.media_type<>'text/html' OR r.style_content<>'' THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_EXACT_PINS_REQUIRED' USING ERRCODE='23514';END IF;
  IF r.revision_id<>NEW.source_revision_id AND (r.source_snapshot->>'category' IS DISTINCT FROM 'LEGAL_NOTICE' OR document_layout_notice_schema_valid(r.input_schema) IS NOT TRUE) THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_NOTICE_REQUIRED' USING ERRCODE='23514';END IF;
 END LOOP;
 SELECT * INTO r FROM document_template_revision WHERE revision_id=NEW.source_revision_id;
 expected:=document_layout_canonical(jsonb_build_object('kind','PUBLISH','targetTemplateId',NEW.template_id::text,'targetLayoutRevisionId',NULL,'expectedDraftReference',jsonb_build_object('templateId',r.template_id::text,'localizationId',r.localization_id::text,'localeId',r.locale_id::text,'templateCode',r.template_code,'version',r.version,'localeCode',r.locale_code,'mediaType',r.media_type,'canonicalizationVersion','MASTERS_JSON_V1','sourceDigest',r.source_digest,'contentDigest',r.content_digest,'schemaDigest',r.schema_digest,'styleDigest',r.style_digest),'outputProfile',d->'profileMetadata'->'reference','semanticDefinition',d->'semanticDefinition','expectedStateVersion',NULL,'reason',NULL));
 IF NEW.publication_intent_canonical<>expected THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_PUBLICATION_INTENT' USING ERRCODE='23514';END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER document_layout_publication_guard BEFORE INSERT OR UPDATE OR DELETE ON document_layout_publication FOR EACH ROW EXECUTE FUNCTION protect_document_layout_publication();
CREATE FUNCTION document_layout_retirement_intent(layout_id uuid,reason text) RETURNS text LANGUAGE sql IMMUTABLE AS $$ SELECT document_layout_canonical(jsonb_build_object('kind','RETIRE','targetTemplateId',NULL,'targetLayoutRevisionId',layout_id::text,'expectedDraftReference',NULL,'outputProfile',NULL,'semanticDefinition',NULL,'expectedStateVersion',0,'reason',reason)) $$;
CREATE FUNCTION validate_document_layout_action() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE r document_layout_publication%ROWTYPE;action uuid;expected integer;actor text;at_millis bigint;intent text;
BEGIN
 SELECT * INTO r FROM document_layout_publication WHERE layout_revision_id=NEW.layout_revision_id;
 IF NOT FOUND THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_ACTION_PARENT' USING ERRCODE='23514';END IF;
 IF NEW.kind='PUBLISH' THEN action:=r.publication_action_id;actor:=r.published_by;at_millis:=r.published_at_epoch_millis;intent:=r.publication_intent_canonical;
 ELSIF NEW.kind='RETIRE' THEN action:=r.retirement_action_id;expected:=0;actor:=r.retired_by;at_millis:=r.retired_at_epoch_millis;intent:=document_layout_retirement_intent(r.layout_revision_id,r.retirement_reason);
 ELSE RAISE EXCEPTION 'DOCUMENT_LAYOUT_ACTION_KIND' USING ERRCODE='23514';END IF;
 IF action IS NULL OR ROW(NEW.action_id,NEW.expected_state_version,NEW.acted_by,NEW.acted_at_epoch_millis,NEW.request_canonical) IS DISTINCT FROM ROW(action,expected,actor,at_millis,intent) THEN RAISE EXCEPTION 'DOCUMENT_LAYOUT_ACTION_DECLARATION' USING ERRCODE='23514';END IF;RETURN NEW;
END $$;
CREATE TRIGGER document_layout_action_declaration BEFORE INSERT ON document_layout_action FOR EACH ROW EXECUTE FUNCTION validate_document_layout_action();
CREATE FUNCTION append_document_layout_evidence() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE action uuid;kind text;expected integer;actor text;at_millis bigint;intent text;
BEGIN
 IF TG_OP='INSERT' THEN
  INSERT INTO document_layout_notice(layout_revision_id,notice_position,role,placement,revision_id) SELECT NEW.layout_revision_id,(ordinal-1)::smallint,value->>'role',value->>'placement',(value->'masterReference'->>'revisionId')::uuid FROM jsonb_array_elements(NEW.canonical_json::jsonb->'semanticDefinition'->'legalNotices') WITH ORDINALITY n(value,ordinal);
  action:=NEW.publication_action_id;kind:='PUBLISH';actor:=NEW.published_by;at_millis:=NEW.published_at_epoch_millis;intent:=NEW.publication_intent_canonical;
 ELSE action:=NEW.retirement_action_id;kind:='RETIRE';expected:=0;actor:=NEW.retired_by;at_millis:=NEW.retired_at_epoch_millis;intent:=document_layout_retirement_intent(NEW.layout_revision_id,NEW.retirement_reason);END IF;
 INSERT INTO document_layout_action VALUES(action,NEW.layout_revision_id,kind,expected,intent,encode(sha256(convert_to('MASTERS_DOCUMENT_LAYOUT_ACTION_V1'||chr(10)||intent,'UTF8')),'hex'),at_millis,actor);
 INSERT INTO document_layout_history VALUES(action,NEW.layout_revision_id,NEW.lifecycle,NEW.state_version,NEW.definition_digest,at_millis,actor);
 INSERT INTO document_layout_outbox(action_id,layout_revision_id,payload_json) VALUES(action,NEW.layout_revision_id,jsonb_build_object('actionId',action,'layoutRevisionId',NEW.layout_revision_id,'sourceRevisionId',NEW.source_revision_id,'definitionDigest',NEW.definition_digest,'lifecycle',NEW.lifecycle,'stateVersion',NEW.state_version,'occurredAtEpochMillis',at_millis,'actor',actor));RETURN NEW;
END $$;
CREATE TRIGGER document_layout_publication_evidence AFTER INSERT OR UPDATE ON document_layout_publication FOR EACH ROW EXECUTE FUNCTION append_document_layout_evidence();
CREATE FUNCTION protect_document_layout_evidence() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'DOCUMENT_LAYOUT_EVIDENCE_RETAINED' USING ERRCODE='23514';END $$;
CREATE TRIGGER document_layout_action_immutable BEFORE UPDATE OR DELETE ON document_layout_action FOR EACH ROW EXECUTE FUNCTION protect_document_layout_evidence();
CREATE TRIGGER document_layout_history_immutable BEFORE UPDATE OR DELETE ON document_layout_history FOR EACH ROW EXECUTE FUNCTION protect_document_layout_evidence();
CREATE TRIGGER document_layout_outbox_immutable BEFORE UPDATE OR DELETE ON document_layout_outbox FOR EACH ROW EXECUTE FUNCTION protect_document_layout_evidence();

CREATE TRIGGER document_layout_notice_immutable BEFORE UPDATE OR DELETE ON document_layout_notice FOR EACH ROW EXECUTE FUNCTION protect_document_layout_evidence();
