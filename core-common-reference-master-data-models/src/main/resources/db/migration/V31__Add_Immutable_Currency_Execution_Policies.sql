CREATE TABLE currency_policy_identity (
 iso_code VARCHAR(3) PRIMARY KEY CHECK(iso_code ~ '^[A-Z]{3}$'),
 currency_id UUID NOT NULL UNIQUE REFERENCES currencies(currency_id) ON DELETE RESTRICT,
 head_version BIGINT NOT NULL CHECK(head_version >= 0), UNIQUE(currency_id,iso_code)
);
CREATE TABLE currency_execution_policy (
 policy_id UUID PRIMARY KEY, currency_id UUID NOT NULL, iso_code VARCHAR(3) NOT NULL,
 version BIGINT NOT NULL CHECK(version > 0), encoding_version VARCHAR(50) NOT NULL CHECK(encoding_version='MASTERS_CURRENCY_POLICY_V1'),
 canonical_encoding TEXT NOT NULL, definition_digest VARCHAR(64) NOT NULL CHECK(definition_digest ~ '^[0-9a-f]{64}$'),
 source_encoding TEXT NOT NULL, source_digest VARCHAR(64) NOT NULL CHECK(source_digest ~ '^[0-9a-f]{64}$'),
 posted_scale INTEGER NOT NULL CHECK(posted_scale BETWEEN 0 AND 10),
 effective_from BIGINT NOT NULL CHECK(effective_from BETWEEN -2208988800000 AND 253402300799999),
 effective_to BIGINT CHECK(effective_to BETWEEN -2208988800000 AND 253402300799999 AND effective_to>effective_from),
 provenance_reference VARCHAR(500) NOT NULL CHECK(length(provenance_reference)>0),
 publication_action_id UUID NOT NULL UNIQUE, published_at BIGINT NOT NULL,
 published_by VARCHAR(255) NOT NULL, published_issuer VARCHAR(500) NOT NULL,
 UNIQUE(currency_id,version), UNIQUE(policy_id,currency_id),
 FOREIGN KEY(currency_id,iso_code) REFERENCES currency_policy_identity(currency_id,iso_code) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE currency_policy_action (
 action_id UUID PRIMARY KEY, currency_id UUID NOT NULL, policy_id UUID NOT NULL,
 kind VARCHAR(10) NOT NULL CHECK(kind IN('PUBLISH','RETIRE')),
 canonical_intent TEXT NOT NULL, intent_digest VARCHAR(64) NOT NULL CHECK(intent_digest ~ '^[0-9a-f]{64}$'),
 FOREIGN KEY(policy_id,currency_id) REFERENCES currency_execution_policy(policy_id,currency_id) DEFERRABLE INITIALLY DEFERRED
);
ALTER TABLE currency_execution_policy ADD FOREIGN KEY(publication_action_id) REFERENCES currency_policy_action(action_id) DEFERRABLE INITIALLY DEFERRED;
CREATE TABLE currency_policy_control (
 policy_id UUID PRIMARY KEY, currency_id UUID NOT NULL,
 lifecycle VARCHAR(10) NOT NULL CHECK(lifecycle IN('PUBLISHED','RETIRED')),
 state_version BIGINT NOT NULL CHECK(state_version IN(0,1)),
 retired_action_id UUID UNIQUE REFERENCES currency_policy_action(action_id) DEFERRABLE INITIALLY DEFERRED,
 retired_at BIGINT, retired_by VARCHAR(255), reason VARCHAR(500),
 CHECK((lifecycle='PUBLISHED' AND state_version=0 AND retired_action_id IS NULL AND retired_at IS NULL AND retired_by IS NULL AND reason IS NULL)
 OR(lifecycle='RETIRED' AND state_version=1 AND retired_action_id IS NOT NULL AND retired_at IS NOT NULL AND retired_by IS NOT NULL AND reason IS NOT NULL AND length(reason)>0)),
 FOREIGN KEY(policy_id,currency_id) REFERENCES currency_execution_policy(policy_id,currency_id) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE currency_policy_audit (
 event_id UUID PRIMARY KEY, currency_id UUID NOT NULL, policy_id UUID NOT NULL,
 action_id UUID NOT NULL UNIQUE REFERENCES currency_policy_action(action_id) DEFERRABLE INITIALLY DEFERRED,
 kind VARCHAR(10) NOT NULL CHECK(kind IN('PUBLISH','RETIRE')), observed_at BIGINT NOT NULL,
 actor VARCHAR(255) NOT NULL, issuer VARCHAR(500) NOT NULL,
 FOREIGN KEY(policy_id,currency_id) REFERENCES currency_execution_policy(policy_id,currency_id) DEFERRABLE INITIALLY DEFERRED
);
CREATE TABLE currency_policy_outbox (
 event_id UUID PRIMARY KEY REFERENCES currency_policy_audit(event_id) DEFERRABLE INITIALLY DEFERRED,
 currency_id UUID NOT NULL, policy_id UUID NOT NULL, action_id UUID NOT NULL UNIQUE,
 payload TEXT NOT NULL, observed_at BIGINT NOT NULL,
 FOREIGN KEY(policy_id,currency_id) REFERENCES currency_execution_policy(policy_id,currency_id) DEFERRABLE INITIALLY DEFERRED,
 FOREIGN KEY(action_id) REFERENCES currency_policy_action(action_id) DEFERRABLE INITIALLY DEFERRED
);
CREATE FUNCTION public.currency_policy_scope_matches(id UUID, purposes TEXT[]) RETURNS BOOLEAN
 LANGUAGE SQL STABLE SET search_path=pg_catalog AS $$
 SELECT COALESCE(current_setting('firefly.currency.currency_id',true)=id::text
  AND current_setting('firefly.currency.purpose',true)=ANY(purposes),FALSE)
$$;
REVOKE ALL ON FUNCTION public.currency_policy_scope_matches(UUID,TEXT[]) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.currency_policy_scope_matches(UUID,TEXT[]) TO PUBLIC;
DO $$ DECLARE t TEXT; BEGIN
 FOREACH t IN ARRAY ARRAY['currency_policy_identity','currency_execution_policy','currency_policy_action','currency_policy_control','currency_policy_audit','currency_policy_outbox'] LOOP
  EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY',t);
  EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY',t);
  EXECUTE format('CREATE POLICY currency_read ON public.%I FOR SELECT USING(public.currency_policy_scope_matches(currency_id,ARRAY[''READ'',''PUBLISH'',''RETIRE'']))',t);
  IF t IN('currency_policy_identity','currency_execution_policy','currency_policy_control') THEN
   EXECUTE format('CREATE POLICY currency_insert ON public.%I FOR INSERT WITH CHECK(public.currency_policy_scope_matches(currency_id,ARRAY[''PUBLISH'']))',t);
  ELSE
   EXECUTE format('CREATE POLICY currency_insert ON public.%I FOR INSERT WITH CHECK(public.currency_policy_scope_matches(currency_id,ARRAY[''PUBLISH'',''RETIRE'']))',t);
  END IF;
 END LOOP;
END $$;
CREATE POLICY currency_head_update ON currency_policy_identity FOR UPDATE
 USING(public.currency_policy_scope_matches(currency_id,ARRAY['PUBLISH'])) WITH CHECK(public.currency_policy_scope_matches(currency_id,ARRAY['PUBLISH']));
CREATE POLICY currency_control_update ON currency_policy_control FOR UPDATE
 USING(public.currency_policy_scope_matches(currency_id,ARRAY['RETIRE'])) WITH CHECK(public.currency_policy_scope_matches(currency_id,ARRAY['RETIRE']));
CREATE FUNCTION public.guard_currency_policy_append_only() RETURNS TRIGGER LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN RAISE EXCEPTION 'IMMUTABLE_CURRENCY_POLICY_RECORD' USING ERRCODE='23514'; END $$;
REVOKE ALL ON FUNCTION public.guard_currency_policy_append_only() FROM PUBLIC;
DO $$ DECLARE t TEXT; BEGIN
 FOREACH t IN ARRAY ARRAY['currency_execution_policy','currency_policy_action','currency_policy_audit','currency_policy_outbox'] LOOP
  EXECUTE format('CREATE TRIGGER currency_immutable BEFORE UPDATE OR DELETE ON public.%I FOR EACH ROW EXECUTE FUNCTION public.guard_currency_policy_append_only()',t);
 END LOOP;
END $$;
CREATE FUNCTION public.guard_currency_policy_initial_identity() RETURNS TRIGGER LANGUAGE plpgsql SET search_path=pg_catalog AS $$
DECLARE k TEXT;
BEGIN
 IF NEW.head_version<>0 THEN RAISE EXCEPTION 'CURRENCY_POLICY_INITIAL_HEAD_ZERO_REQUIRED' USING ERRCODE='23514'; END IF;
 FOR k IN SELECT value FROM unnest(ARRAY['CURRENCY:'||NEW.currency_id::text,'ISO:'||NEW.iso_code]) value ORDER BY value LOOP
  IF NOT pg_try_advisory_xact_lock(hashtextextended('MASTERS_CURRENCY:'||k,0)) THEN
   RAISE EXCEPTION 'CURRENCY_POLICY_CONCURRENT_SOURCE_CHANGE' USING ERRCODE='23514';
  END IF;
 END LOOP;
 PERFORM c.currency_id FROM public.currencies c WHERE c.currency_id=NEW.currency_id AND c.iso_code=NEW.iso_code FOR NO KEY UPDATE;
 IF NOT FOUND THEN RAISE EXCEPTION 'EXACT_CANONICAL_CURRENCY_IDENTITY_REQUIRED' USING ERRCODE='23514'; END IF;
 RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION public.guard_currency_policy_initial_identity() FROM PUBLIC;
CREATE TRIGGER currency_initial_identity_guard BEFORE INSERT ON currency_policy_identity FOR EACH ROW EXECUTE FUNCTION public.guard_currency_policy_initial_identity();
CREATE FUNCTION public.guard_currency_policy_head() RETURNS TRIGGER LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP='DELETE' OR NEW.currency_id<>OLD.currency_id OR NEW.iso_code<>OLD.iso_code OR OLD.head_version=9223372036854775807 OR NEW.head_version<>OLD.head_version+1 THEN
  RAISE EXCEPTION 'IMMUTABLE_CURRENCY_IDENTITY_AND_MONOTONIC_HEAD_REQUIRED' USING ERRCODE='23514';
 END IF; RETURN NEW;
END $$;
CREATE TRIGGER currency_head_guard BEFORE UPDATE OR DELETE ON currency_policy_identity FOR EACH ROW EXECUTE FUNCTION public.guard_currency_policy_head();
REVOKE ALL ON FUNCTION public.guard_currency_policy_head() FROM PUBLIC;
CREATE FUNCTION public.guard_currency_policy_control() RETURNS TRIGGER LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP='INSERT' THEN
  IF NEW.lifecycle<>'PUBLISHED' OR NEW.state_version<>0 THEN RAISE EXCEPTION 'CURRENCY_POLICY_INITIAL_CONTROL_REQUIRED' USING ERRCODE='23514'; END IF;
  RETURN NEW;
 END IF;
 IF TG_OP='DELETE' OR NEW.policy_id<>OLD.policy_id OR NEW.currency_id<>OLD.currency_id OR OLD.lifecycle<>'PUBLISHED' OR OLD.state_version<>0 OR NEW.lifecycle<>'RETIRED' OR NEW.state_version<>1 THEN
  RAISE EXCEPTION 'SINGLE_CURRENCY_RETIREMENT_REQUIRED' USING ERRCODE='23514';
 END IF; RETURN NEW;
END $$;
CREATE TRIGGER currency_control_guard BEFORE INSERT OR UPDATE OR DELETE ON currency_policy_control FOR EACH ROW EXECUTE FUNCTION public.guard_currency_policy_control();
REVOKE ALL ON FUNCTION public.guard_currency_policy_control() FROM PUBLIC;
CREATE FUNCTION public.guard_currency_catalog_identity() RETURNS TRIGGER LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE k TEXT; bound BOOLEAN;
BEGIN
 FOR k IN SELECT DISTINCT value FROM unnest(ARRAY['CURRENCY:'||OLD.currency_id::text,'ISO:'||OLD.iso_code,
  CASE WHEN TG_OP<>'DELETE' THEN 'CURRENCY:'||NEW.currency_id::text ELSE 'CURRENCY:'||OLD.currency_id::text END,
  CASE WHEN TG_OP<>'DELETE' THEN 'ISO:'||NEW.iso_code ELSE 'ISO:'||OLD.iso_code END]) value ORDER BY value LOOP
  IF NOT pg_try_advisory_xact_lock(hashtextextended('MASTERS_CURRENCY:'||k,0)) THEN
   RAISE EXCEPTION 'CURRENCY_POLICY_CONCURRENT_SOURCE_CHANGE' USING ERRCODE='23514';
  END IF;
 END LOOP;
 SELECT EXISTS(SELECT 1 FROM public.currency_policy_identity i WHERE i.currency_id=OLD.currency_id) INTO bound;
 IF bound AND (TG_OP='DELETE' OR NEW.currency_id<>OLD.currency_id OR NEW.iso_code<>OLD.iso_code) THEN
  RAISE EXCEPTION 'PUBLISHED_CURRENCY_IDENTITY_CANNOT_CHANGE_OR_DELETE' USING ERRCODE='23514';
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF; RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION public.guard_currency_catalog_identity() FROM PUBLIC;
CREATE TRIGGER currency_catalog_identity_guard BEFORE UPDATE OR DELETE ON currencies FOR EACH ROW EXECUTE FUNCTION public.guard_currency_catalog_identity();
-- A preprovisioned read-only NOLOGIN role gives the trigger global binding visibility without RLS bypass.
-- Migrations remain compatible when that role is absent; native runtime deliberately refuses that configuration.
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM pg_catalog.pg_roles WHERE rolname='firefly_currency_policy_guard' AND NOT rolsuper AND NOT rolbypassrls AND NOT rolcanlogin) THEN
  GRANT USAGE ON SCHEMA public TO firefly_currency_policy_guard;
  GRANT SELECT ON public.currency_policy_identity TO firefly_currency_policy_guard;
  CREATE POLICY currency_guard_binding_read ON public.currency_policy_identity FOR SELECT TO firefly_currency_policy_guard USING(TRUE);
  GRANT CREATE ON SCHEMA public TO firefly_currency_policy_guard;
  ALTER FUNCTION public.guard_currency_catalog_identity() OWNER TO firefly_currency_policy_guard;
  REVOKE CREATE ON SCHEMA public FROM firefly_currency_policy_guard;
 END IF;
END $$;
CREATE FUNCTION public.check_currency_policy_atomicity() RETURNS TRIGGER LANGUAGE plpgsql SET search_path=pg_catalog AS $$
DECLARE id UUID; a public.currency_policy_action%ROWTYPE; h BIGINT; n BIGINT; v BIGINT;
BEGIN
 id:=NEW.currency_id;
 SELECT head_version INTO h FROM public.currency_policy_identity WHERE currency_id=id;
 SELECT count(*),COALESCE(max(version),0) INTO n,v FROM public.currency_execution_policy WHERE currency_id=id;
 IF h IS NULL OR h<=0 OR h<>n OR h<>v THEN RAISE EXCEPTION 'CURRENCY_POLICY_HEAD_REQUIRES_COMPLETE_VERSION_CHAIN' USING ERRCODE='23514'; END IF;
 IF TG_TABLE_NAME='currency_execution_policy' THEN
  IF NOT EXISTS(SELECT 1 FROM public.currency_policy_action x WHERE x.action_id=NEW.publication_action_id AND x.policy_id=NEW.policy_id AND x.currency_id=NEW.currency_id AND x.kind='PUBLISH') THEN
   RAISE EXCEPTION 'CURRENCY_PUBLICATION_REQUIRES_MATCHING_PUBLICATION_ACTION' USING ERRCODE='23514';
  END IF;
 END IF;
 IF TG_TABLE_NAME='currency_policy_control' THEN
  IF NEW.lifecycle='RETIRED' THEN
   IF NOT EXISTS(SELECT 1 FROM public.currency_policy_action x JOIN public.currency_policy_audit audit_row ON audit_row.action_id=x.action_id AND audit_row.currency_id=x.currency_id AND audit_row.policy_id=x.policy_id AND audit_row.kind=x.kind JOIN public.currency_policy_outbox o ON o.event_id=audit_row.event_id AND o.action_id=audit_row.action_id AND o.currency_id=audit_row.currency_id AND o.policy_id=audit_row.policy_id WHERE x.action_id=NEW.retired_action_id AND x.policy_id=NEW.policy_id AND x.currency_id=NEW.currency_id AND x.kind='RETIRE') THEN
    RAISE EXCEPTION 'CURRENCY_RETIREMENT_REQUIRES_MATCHING_ACTION_AUDIT_OUTBOX' USING ERRCODE='23514';
   END IF;
  END IF;
 END IF;
 IF TG_TABLE_NAME='currency_policy_action' THEN
  a:=NEW;
  IF NOT EXISTS(SELECT 1 FROM public.currency_policy_audit x JOIN public.currency_policy_outbox o ON o.event_id=x.event_id AND o.currency_id=x.currency_id AND o.policy_id=x.policy_id AND o.action_id=x.action_id WHERE x.action_id=a.action_id AND x.policy_id=a.policy_id AND x.currency_id=a.currency_id AND x.kind=a.kind)
  OR NOT EXISTS(SELECT 1 FROM public.currency_policy_control c WHERE c.policy_id=a.policy_id AND c.currency_id=a.currency_id)
  OR (a.kind='PUBLISH' AND NOT EXISTS(SELECT 1 FROM public.currency_execution_policy p WHERE p.policy_id=a.policy_id AND p.currency_id=a.currency_id AND p.publication_action_id=a.action_id))
  OR (a.kind='RETIRE' AND NOT EXISTS(SELECT 1 FROM public.currency_policy_control c WHERE c.policy_id=a.policy_id AND c.currency_id=a.currency_id AND c.retired_action_id=a.action_id AND c.lifecycle='RETIRED')) THEN
   RAISE EXCEPTION 'CURRENCY_POLICY_ACTION_REQUIRES_ATOMIC_RECEIPT_AUDIT_OUTBOX' USING ERRCODE='23514';
  END IF;
 END IF; RETURN NULL;
END $$;
REVOKE ALL ON FUNCTION public.check_currency_policy_atomicity() FROM PUBLIC;
CREATE CONSTRAINT TRIGGER currency_identity_complete AFTER INSERT OR UPDATE ON currency_policy_identity DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.check_currency_policy_atomicity();
CREATE CONSTRAINT TRIGGER currency_policy_complete AFTER INSERT ON currency_execution_policy DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.check_currency_policy_atomicity();
CREATE CONSTRAINT TRIGGER currency_action_complete AFTER INSERT ON currency_policy_action DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.check_currency_policy_atomicity();

ALTER TABLE currency_execution_policy ADD CONSTRAINT currency_definition_digest_exact CHECK(definition_digest=encode(sha256(convert_to(canonical_encoding,'UTF8')),'hex'));
ALTER TABLE currency_execution_policy ADD CONSTRAINT currency_source_digest_exact CHECK(source_digest=encode(sha256(convert_to(source_encoding,'UTF8')),'hex'));
ALTER TABLE currency_policy_action ADD CONSTRAINT currency_action_digest_exact CHECK(intent_digest=encode(sha256(convert_to(canonical_intent,'UTF8')),'hex'));

CREATE CONSTRAINT TRIGGER currency_control_complete AFTER INSERT OR UPDATE ON currency_policy_control DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.check_currency_policy_atomicity();

ALTER TABLE currency_policy_outbox ADD CONSTRAINT currency_event_closed_shape CHECK(COALESCE(
 jsonb_typeof(payload::jsonb)='object'
 AND payload::jsonb ?& ARRAY['payloadVersion','actionKind','actionId','policyReference','intentEncoding','intentDigest','observedAtEpochMillis']
 AND payload::jsonb - ARRAY['payloadVersion','actionKind','actionId','policyReference','intentEncoding','intentDigest','observedAtEpochMillis']='{}'::jsonb
 AND payload::jsonb->>'payloadVersion'='MASTERS_CURRENCY_POLICY_EVENT_V1'
 AND payload::jsonb->>'actionKind' IN('PUBLISH','RETIRE')
 AND payload::jsonb->'actionId'=to_jsonb(action_id::text)
 AND jsonb_typeof(payload::jsonb->'policyReference')='object'
 AND jsonb_typeof(payload::jsonb->'intentEncoding')='string'
 AND jsonb_typeof(payload::jsonb->'intentDigest')='string'
 AND payload::jsonb->>'intentDigest' ~ '^[0-9a-f]{64}$'
 AND jsonb_typeof(payload::jsonb->'observedAtEpochMillis')='number'
 AND payload::jsonb->'observedAtEpochMillis'=to_jsonb(observed_at),FALSE));
CREATE FUNCTION public.check_currency_policy_event() RETURNS TRIGGER LANGUAGE plpgsql SET search_path=pg_catalog AS $$
BEGIN
 IF NOT EXISTS(SELECT 1 FROM public.currency_execution_policy p
  JOIN public.currency_policy_action a ON a.policy_id=p.policy_id AND a.currency_id=p.currency_id AND a.action_id=NEW.action_id
  JOIN public.currency_policy_audit audit_row ON audit_row.event_id=NEW.event_id AND audit_row.policy_id=p.policy_id AND audit_row.currency_id=p.currency_id AND audit_row.action_id=a.action_id AND audit_row.kind=a.kind AND audit_row.observed_at=NEW.observed_at
  WHERE p.policy_id=NEW.policy_id AND p.currency_id=NEW.currency_id
  AND NEW.payload::jsonb->'policyReference'=jsonb_build_object('policyId',p.policy_id::text,'currencyId',p.currency_id::text,'isoCode',p.iso_code,'version',p.version,'digest',p.definition_digest,'encodingVersion',p.encoding_version)
  AND NEW.payload::jsonb->'actionKind'=to_jsonb(a.kind)
  AND NEW.payload::jsonb->'intentEncoding'=to_jsonb(a.canonical_intent)
  AND NEW.payload::jsonb->'intentDigest'=to_jsonb(a.intent_digest)) THEN
  RAISE EXCEPTION 'EXACT_CURRENCY_EVENT_REFERENCE_AND_INTENT_REQUIRED' USING ERRCODE='23514';
 END IF; RETURN NULL;
END $$;
REVOKE ALL ON FUNCTION public.check_currency_policy_event() FROM PUBLIC;
CREATE CONSTRAINT TRIGGER currency_event_complete AFTER INSERT ON currency_policy_outbox DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.check_currency_policy_event();
