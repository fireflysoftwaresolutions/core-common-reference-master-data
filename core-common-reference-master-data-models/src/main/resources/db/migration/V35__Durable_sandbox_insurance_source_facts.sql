-- Durable synthetic provenance only; database rows are never current caller/PDP or financial authority.
DO $$
BEGIN
 IF NOT pg_catalog.pg_has_role(current_user,'masters_ins_ref_owner','SET') OR NOT pg_catalog.pg_has_role(current_user,'masters_ins_ref_guard','SET')
  OR NOT pg_catalog.has_schema_privilege(current_user,'public','CREATE WITH GRANT OPTION') THEN
  RAISE EXCEPTION 'Authorized sandbox fact migrator required' USING ERRCODE='42501';
 END IF;
 IF (SELECT count(*) FROM pg_catalog.pg_roles r WHERE r.rolname IN('masters_ins_ref_source_registrar','masters_ins_ref_source_reader')
  AND r.rolcanlogin AND NOT r.rolsuper AND NOT r.rolbypassrls AND NOT r.rolinherit AND NOT r.rolcreatedb AND NOT r.rolcreaterole AND NOT r.rolreplication
  AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_auth_members m WHERE m.member=r.oid))<>2 THEN
  RAISE EXCEPTION 'Prebootstrapped restricted sandbox roles required' USING ERRCODE='42501';
 END IF;
END $$;
GRANT USAGE ON SCHEMA public TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
GRANT CREATE ON SCHEMA public TO masters_ins_ref_owner,masters_ins_ref_guard;
SET LOCAL ROLE masters_ins_ref_owner;

CREATE TABLE public.insurance_sandbox_locale_version(
 version_id UUID NOT NULL PRIMARY KEY,
 catalog_scope TEXT NOT NULL CHECK(catalog_scope='SANDBOX'),
 fact_id UUID NOT NULL,
 version BIGINT NOT NULL CHECK(version>0),
 admitted_generation BIGINT NOT NULL CHECK(admitted_generation>=0 AND admitted_generation<9223372036854775807),
 locale_id UUID NOT NULL,
 locale_code TEXT NOT NULL,
 source_epoch BIGINT NOT NULL CHECK(source_epoch>=0),
 source_encoding TEXT NOT NULL,
 source_digest TEXT NOT NULL CHECK(length(source_digest)=64 AND source_digest ~ '^[0-9a-f]{64}$'),
 canonical_fact TEXT NOT NULL CHECK(octet_length(canonical_fact)<=524288),
 digest TEXT NOT NULL CHECK(length(digest)=64 AND digest ~ '^[0-9a-f]{64}$'),
 effective_until TIMESTAMPTZ NOT NULL CHECK(pg_catalog.isfinite(effective_until)),
 registration_action_id UUID NOT NULL,
 UNIQUE(fact_id,version),
 UNIQUE(version_id,fact_id,version,digest),
 UNIQUE(fact_id,version,admitted_generation,digest),
 CHECK(digest=encode(sha256(convert_to('masters-insurance-sandbox-fact-v1'||E'\n'||canonical_fact,'UTF8')),'hex'))
);

CREATE TABLE public.insurance_sandbox_rights_version(
 version_id UUID NOT NULL PRIMARY KEY,
 catalog_scope TEXT NOT NULL CHECK(catalog_scope='SANDBOX'),
 fact_id UUID NOT NULL,
 version BIGINT NOT NULL CHECK(version>0),
 admitted_generation BIGINT NOT NULL CHECK(admitted_generation>=0 AND admitted_generation<9223372036854775807),
 source_id UUID NOT NULL,
 insurance_kind TEXT NOT NULL CHECK(insurance_kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
 source_text TEXT NOT NULL CHECK(octet_length(source_text)<=131072),
 source_digest TEXT NOT NULL CHECK(length(source_digest)=64 AND source_digest ~ '^[0-9a-f]{64}$'),
 locale_fact_id UUID,
 locale_version BIGINT,
 locale_generation BIGINT,
 locale_digest TEXT,
 canonical_fact TEXT NOT NULL CHECK(octet_length(canonical_fact)<=524288),
 digest TEXT NOT NULL CHECK(length(digest)=64 AND digest ~ '^[0-9a-f]{64}$'),
 effective_until TIMESTAMPTZ NOT NULL CHECK(pg_catalog.isfinite(effective_until)),
 registration_action_id UUID NOT NULL,
 UNIQUE(fact_id,version),
 UNIQUE(version_id,fact_id,version,digest),
 UNIQUE(fact_id,version,admitted_generation,digest),
 CHECK(digest=encode(sha256(convert_to('masters-insurance-sandbox-fact-v1'||E'\n'||canonical_fact,'UTF8')),'hex')),
 CHECK((locale_fact_id IS NULL AND locale_version IS NULL AND locale_generation IS NULL AND locale_digest IS NULL AND insurance_kind='PREMIUM_TAX') OR (locale_fact_id IS NOT NULL AND locale_version IS NOT NULL AND locale_generation IS NOT NULL AND locale_digest IS NOT NULL AND insurance_kind IN('COVERAGE_WORDING','EXCLUSION_WORDING')))
);

CREATE TABLE public.insurance_sandbox_source_version(
 version_id UUID NOT NULL PRIMARY KEY,
 catalog_scope TEXT NOT NULL CHECK(catalog_scope='SANDBOX'),
 fact_id UUID NOT NULL,
 version BIGINT NOT NULL CHECK(version>0),
 admitted_generation BIGINT NOT NULL CHECK(admitted_generation>=0 AND admitted_generation<9223372036854775807),
 record_id UUID NOT NULL,
 insurance_kind TEXT NOT NULL CHECK(insurance_kind IN('PREMIUM_TAX','COVERAGE_WORDING','EXCLUSION_WORDING')),
 publication_id UUID NOT NULL,
 publication_revision BIGINT NOT NULL CHECK(publication_revision>0),
 submitted_action_id UUID NOT NULL,
 submitted_intent_digest TEXT NOT NULL CHECK(length(submitted_intent_digest)=64 AND submitted_intent_digest ~ '^[0-9a-f]{64}$'),
 canonical_definition TEXT NOT NULL CHECK(octet_length(canonical_definition)<=131072),
 definition_digest TEXT NOT NULL CHECK(length(definition_digest)=64 AND definition_digest ~ '^[0-9a-f]{64}$'),
 source_text TEXT NOT NULL CHECK(octet_length(source_text)<=131072),
 source_digest TEXT NOT NULL CHECK(length(source_digest)=64 AND source_digest ~ '^[0-9a-f]{64}$'),
 rights_fact_id UUID NOT NULL,
 rights_version BIGINT NOT NULL,
 rights_generation BIGINT NOT NULL,
 rights_digest TEXT NOT NULL,
 locale_fact_id UUID,
 locale_version BIGINT,
 locale_generation BIGINT,
 locale_digest TEXT,
 canonical_fact TEXT NOT NULL CHECK(octet_length(canonical_fact)<=524288),
 digest TEXT NOT NULL CHECK(length(digest)=64 AND digest ~ '^[0-9a-f]{64}$'),
 effective_until TIMESTAMPTZ NOT NULL CHECK(pg_catalog.isfinite(effective_until)),
 registration_action_id UUID NOT NULL,
 UNIQUE(fact_id,version),
 UNIQUE(version_id,fact_id,version,digest),
 UNIQUE(fact_id,version,admitted_generation,digest),
 CHECK(digest=encode(sha256(convert_to('masters-insurance-sandbox-fact-v1'||E'\n'||canonical_fact,'UTF8')),'hex')),
 CHECK((locale_fact_id IS NULL AND locale_version IS NULL AND locale_generation IS NULL AND locale_digest IS NULL AND insurance_kind='PREMIUM_TAX') OR (locale_fact_id IS NOT NULL AND locale_version IS NOT NULL AND locale_generation IS NOT NULL AND locale_digest IS NOT NULL AND insurance_kind IN('COVERAGE_WORDING','EXCLUSION_WORDING')))
);

CREATE TABLE public.insurance_sandbox_locale_control(
 fact_id UUID NOT NULL PRIMARY KEY,
 catalog_scope TEXT NOT NULL CHECK(catalog_scope='SANDBOX'),
 version_id UUID NOT NULL,
 version BIGINT NOT NULL CHECK(version>0),
 generation BIGINT NOT NULL CHECK(generation>=0),
 digest TEXT NOT NULL CHECK(length(digest)=64 AND digest ~ '^[0-9a-f]{64}$'),
 lifecycle TEXT NOT NULL CHECK(lifecycle IN('ACTIVE','WITHDRAWN') AND (lifecycle<>'ACTIVE' OR generation<9223372036854775807))
);

CREATE TABLE public.insurance_sandbox_rights_control(
 fact_id UUID NOT NULL PRIMARY KEY,
 catalog_scope TEXT NOT NULL CHECK(catalog_scope='SANDBOX'),
 version_id UUID NOT NULL,
 version BIGINT NOT NULL CHECK(version>0),
 generation BIGINT NOT NULL CHECK(generation>=0),
 digest TEXT NOT NULL CHECK(length(digest)=64 AND digest ~ '^[0-9a-f]{64}$'),
 lifecycle TEXT NOT NULL CHECK(lifecycle IN('ACTIVE','WITHDRAWN') AND (lifecycle<>'ACTIVE' OR generation<9223372036854775807))
);

CREATE TABLE public.insurance_sandbox_source_control(
 fact_id UUID NOT NULL PRIMARY KEY,
 catalog_scope TEXT NOT NULL CHECK(catalog_scope='SANDBOX'),
 version_id UUID NOT NULL,
 version BIGINT NOT NULL CHECK(version>0),
 generation BIGINT NOT NULL CHECK(generation>=0),
 digest TEXT NOT NULL CHECK(length(digest)=64 AND digest ~ '^[0-9a-f]{64}$'),
 lifecycle TEXT NOT NULL CHECK(lifecycle IN('ACTIVE','WITHDRAWN') AND (lifecycle<>'ACTIVE' OR generation<9223372036854775807))
);

CREATE TABLE public.insurance_sandbox_fact_action(
 action_id UUID NOT NULL PRIMARY KEY,
 catalog_scope TEXT NOT NULL CHECK(catalog_scope='SANDBOX'),
 fact_type TEXT NOT NULL CHECK(fact_type IN('SOURCE','RIGHTS','LOCALE')),
 fact_id UUID NOT NULL,
 version_id UUID NOT NULL,
 version BIGINT NOT NULL CHECK(version>0),
 generation BIGINT NOT NULL CHECK(generation>=0),
 fact_digest TEXT NOT NULL CHECK(length(fact_digest)=64 AND fact_digest ~ '^[0-9a-f]{64}$'),
 result_state TEXT NOT NULL CHECK(result_state IN('ACTIVE','WITHDRAWN')),
 operation TEXT NOT NULL CHECK(operation IN('REGISTER','WITHDRAW')),
 command_digest TEXT NOT NULL CHECK(length(command_digest)=64 AND command_digest ~ '^[0-9a-f]{64}$'),
 actor_issuer TEXT NOT NULL CHECK(length(actor_issuer) BETWEEN 1 AND 500),
 actor_subject TEXT NOT NULL CHECK(length(actor_subject) BETWEEN 1 AND 255),
 actor_tenant TEXT NOT NULL CHECK(length(actor_tenant) BETWEEN 1 AND 255),
 reason_code TEXT,
 recorded_at TIMESTAMPTZ NOT NULL DEFAULT pg_catalog.clock_timestamp()
);
ALTER TABLE public.insurance_sandbox_locale_control ADD CONSTRAINT insurance_sandbox_locale_control_version_fk FOREIGN KEY(version_id,fact_id,version,digest)
 REFERENCES public.insurance_sandbox_locale_version(version_id,fact_id,version,digest) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_locale_version ADD CONSTRAINT insurance_sandbox_locale_version_action_fk FOREIGN KEY(registration_action_id)
 REFERENCES public.insurance_sandbox_fact_action(action_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_rights_control ADD CONSTRAINT insurance_sandbox_rights_control_version_fk FOREIGN KEY(version_id,fact_id,version,digest)
 REFERENCES public.insurance_sandbox_rights_version(version_id,fact_id,version,digest) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_rights_version ADD CONSTRAINT insurance_sandbox_rights_version_action_fk FOREIGN KEY(registration_action_id)
 REFERENCES public.insurance_sandbox_fact_action(action_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_source_control ADD CONSTRAINT insurance_sandbox_source_control_version_fk FOREIGN KEY(version_id,fact_id,version,digest)
 REFERENCES public.insurance_sandbox_source_version(version_id,fact_id,version,digest) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_source_version ADD CONSTRAINT insurance_sandbox_source_version_action_fk FOREIGN KEY(registration_action_id)
 REFERENCES public.insurance_sandbox_fact_action(action_id) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_rights_version ADD CONSTRAINT insurance_sandbox_rights_version_locale_fk
 FOREIGN KEY(locale_fact_id,locale_version,locale_generation,locale_digest)
 REFERENCES public.insurance_sandbox_locale_version(fact_id,version,admitted_generation,digest) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_source_version ADD CONSTRAINT insurance_sandbox_source_version_locale_fk
 FOREIGN KEY(locale_fact_id,locale_version,locale_generation,locale_digest)
 REFERENCES public.insurance_sandbox_locale_version(fact_id,version,admitted_generation,digest) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_source_version ADD CONSTRAINT insurance_sandbox_source_rights_fk
 FOREIGN KEY(rights_fact_id,rights_version,rights_generation,rights_digest)
 REFERENCES public.insurance_sandbox_rights_version(fact_id,version,admitted_generation,digest) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_fact_action ADD CONSTRAINT insurance_sandbox_one_action_per_generation UNIQUE(fact_type,fact_id,generation);
ALTER TABLE public.insurance_sandbox_fact_action ADD COLUMN locale_version_id UUID
 GENERATED ALWAYS AS(CASE WHEN fact_type='LOCALE' THEN version_id ELSE NULL END) STORED;
ALTER TABLE public.insurance_sandbox_fact_action ADD CONSTRAINT insurance_sandbox_action_locale_fk
 FOREIGN KEY(locale_version_id,fact_id,version,fact_digest)
 REFERENCES public.insurance_sandbox_locale_version(version_id,fact_id,version,digest) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_fact_action ADD COLUMN rights_version_id UUID
 GENERATED ALWAYS AS(CASE WHEN fact_type='RIGHTS' THEN version_id ELSE NULL END) STORED;
ALTER TABLE public.insurance_sandbox_fact_action ADD CONSTRAINT insurance_sandbox_action_rights_fk
 FOREIGN KEY(rights_version_id,fact_id,version,fact_digest)
 REFERENCES public.insurance_sandbox_rights_version(version_id,fact_id,version,digest) DEFERRABLE INITIALLY DEFERRED;
ALTER TABLE public.insurance_sandbox_fact_action ADD COLUMN source_version_id UUID
 GENERATED ALWAYS AS(CASE WHEN fact_type='SOURCE' THEN version_id ELSE NULL END) STORED;
ALTER TABLE public.insurance_sandbox_fact_action ADD CONSTRAINT insurance_sandbox_action_source_fk
 FOREIGN KEY(source_version_id,fact_id,version,fact_digest)
 REFERENCES public.insurance_sandbox_source_version(version_id,fact_id,version,digest) DEFERRABLE INITIALLY DEFERRED;
RESET ROLE;
SET LOCAL ROLE masters_ins_ref_owner;
GRANT SELECT ON public.insurance_sandbox_locale_version,public.insurance_sandbox_rights_version,public.insurance_sandbox_source_version,
 public.insurance_sandbox_locale_control,public.insurance_sandbox_rights_control,public.insurance_sandbox_source_control,public.insurance_sandbox_fact_action TO masters_ins_ref_guard;
SET LOCAL ROLE masters_ins_ref_guard;
GRANT CREATE ON SCHEMA masters_ins_ref_private TO masters_ins_ref_guard;
CREATE TABLE public.insurance_sandbox_locale_epoch(
 catalog_scope TEXT NOT NULL DEFAULT 'SANDBOX' CHECK(catalog_scope='SANDBOX'),locale_id UUID PRIMARY KEY,
 source_epoch BIGINT NOT NULL CHECK(source_epoch>=0));
ALTER TABLE public.insurance_sandbox_locale_epoch ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_sandbox_locale_epoch FORCE ROW LEVEL SECURITY;
CREATE POLICY insurance_sandbox_epoch_guard ON public.insurance_sandbox_locale_epoch TO masters_ins_ref_guard USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_epoch_reader ON public.insurance_sandbox_locale_epoch FOR SELECT TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader USING(catalog_scope='SANDBOX');
REVOKE ALL ON public.insurance_sandbox_locale_epoch FROM PUBLIC,masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
GRANT SELECT ON public.insurance_sandbox_locale_epoch TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader,masters_ins_ref_owner;

CREATE FUNCTION masters_ins_ref_private.v35_advance_locale_epoch() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE identifier UUID;
BEGIN
 IF TG_TABLE_SCHEMA<>'public' OR TG_TABLE_NAME<>'language_locale' THEN RAISE EXCEPTION 'Exact locale source required' USING ERRCODE='42501';END IF;
 IF TG_OP='DELETE' THEN identifier:=OLD.locale_id;ELSE identifier:=NEW.locale_id;END IF;
 INSERT INTO public.insurance_sandbox_locale_epoch(catalog_scope,locale_id,source_epoch) VALUES('SANDBOX',identifier,0)
 ON CONFLICT(locale_id) DO UPDATE SET source_epoch=public.insurance_sandbox_locale_epoch.source_epoch+1;
 IF TG_OP='UPDATE' AND NEW.locale_id IS DISTINCT FROM OLD.locale_id THEN
  INSERT INTO public.insurance_sandbox_locale_epoch(catalog_scope,locale_id,source_epoch) VALUES('SANDBOX',OLD.locale_id,0)
  ON CONFLICT(locale_id) DO UPDATE SET source_epoch=public.insurance_sandbox_locale_epoch.source_epoch+1;
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD;ELSE RETURN NEW;END IF;
END $$;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v35_advance_locale_epoch() FROM PUBLIC;

CREATE FUNCTION masters_ins_ref_private.v35_fact_immutable() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN RAISE EXCEPTION 'Immutable sandbox source fact required' USING ERRCODE='42501';END $$;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v35_fact_immutable() FROM PUBLIC;

CREATE FUNCTION masters_ins_ref_private.v35_control_transition() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
BEGIN
 IF TG_OP='INSERT' THEN
  IF NEW.version<>1 OR NEW.generation<>0 OR NEW.lifecycle<>'ACTIVE' THEN RAISE EXCEPTION 'Virgin sandbox head required' USING ERRCODE='42501';END IF;
  RETURN NEW;
 END IF;
 IF TG_OP='DELETE' OR NEW.fact_id IS DISTINCT FROM OLD.fact_id OR NEW.catalog_scope IS DISTINCT FROM OLD.catalog_scope
  OR OLD.generation=9223372036854775807 OR NEW.generation<>OLD.generation+1 THEN
  RAISE EXCEPTION 'Exact sandbox control transition required' USING ERRCODE='42501';END IF;
 IF (NEW.version_id=OLD.version_id AND NEW.version=OLD.version AND NEW.digest=OLD.digest AND OLD.lifecycle='ACTIVE' AND NEW.lifecycle='WITHDRAWN')
  OR (OLD.version<9223372036854775807 AND NEW.version=OLD.version+1 AND NEW.version_id<>OLD.version_id AND NEW.lifecycle='ACTIVE' AND NEW.generation<9223372036854775807) THEN RETURN NEW;END IF;
 RAISE EXCEPTION 'New version or irreversible sandbox withdrawal required' USING ERRCODE='42501';
END $$;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v35_control_transition() FROM PUBLIC;
RESET ROLE;
GRANT TRIGGER ON public.language_locale TO masters_ins_ref_guard;
SET LOCAL ROLE masters_ins_ref_guard;
CREATE TRIGGER insurance_sandbox_locale_epoch_after AFTER INSERT OR UPDATE OR DELETE ON public.language_locale
 FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_advance_locale_epoch();
RESET ROLE;
REVOKE TRIGGER ON public.language_locale FROM masters_ins_ref_guard;
GRANT SELECT(locale_id,language_code,country_code,locale_code,language_name,native_name,region_name,rtl,sort_order,status,date_created,date_updated)
 ON public.language_locale TO masters_ins_ref_guard,masters_ins_ref_source_registrar,masters_ins_ref_source_reader,masters_ins_ref_owner;
SET LOCAL ROLE masters_ins_ref_guard;
INSERT INTO public.insurance_sandbox_locale_epoch(catalog_scope,locale_id,source_epoch) SELECT 'SANDBOX',locale_id,0 FROM public.language_locale;
RESET ROLE;
SET LOCAL ROLE masters_ins_ref_owner;
CREATE VIEW public.insurance_sandbox_locale_source WITH(security_invoker=true) AS
 SELECT l.locale_id,l.language_code,l.country_code,l.locale_code,l.language_name,l.native_name,l.region_name,l.rtl,l.sort_order,l.status,l.date_created,l.date_updated,e.source_epoch
 FROM public.language_locale l JOIN public.insurance_sandbox_locale_epoch e ON e.locale_id=l.locale_id AND e.catalog_scope='SANDBOX';
REVOKE ALL ON public.insurance_sandbox_locale_source FROM PUBLIC;
GRANT SELECT ON public.insurance_sandbox_locale_source TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
ALTER TABLE public.insurance_sandbox_locale_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_sandbox_locale_version FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_sandbox_locale_version FROM PUBLIC,masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
GRANT SELECT ON public.insurance_sandbox_locale_version TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
CREATE POLICY insurance_sandbox_locale_version_read ON public.insurance_sandbox_locale_version FOR SELECT TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader USING(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_locale_version_append ON public.insurance_sandbox_locale_version FOR INSERT TO masters_ins_ref_source_registrar WITH CHECK(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_locale_version_guard ON public.insurance_sandbox_locale_version TO masters_ins_ref_guard USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
GRANT INSERT(version_id,catalog_scope,fact_id,version,admitted_generation,locale_id,locale_code,source_epoch,source_encoding,source_digest,canonical_fact,digest,effective_until,registration_action_id) ON public.insurance_sandbox_locale_version TO masters_ins_ref_source_registrar;
ALTER TABLE public.insurance_sandbox_rights_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_sandbox_rights_version FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_sandbox_rights_version FROM PUBLIC,masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
GRANT SELECT ON public.insurance_sandbox_rights_version TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
CREATE POLICY insurance_sandbox_rights_version_read ON public.insurance_sandbox_rights_version FOR SELECT TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader USING(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_rights_version_append ON public.insurance_sandbox_rights_version FOR INSERT TO masters_ins_ref_source_registrar WITH CHECK(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_rights_version_guard ON public.insurance_sandbox_rights_version TO masters_ins_ref_guard USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
GRANT INSERT(version_id,catalog_scope,fact_id,version,admitted_generation,source_id,insurance_kind,source_text,source_digest,locale_fact_id,locale_version,locale_generation,locale_digest,canonical_fact,digest,effective_until,registration_action_id) ON public.insurance_sandbox_rights_version TO masters_ins_ref_source_registrar;
ALTER TABLE public.insurance_sandbox_source_version ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_sandbox_source_version FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_sandbox_source_version FROM PUBLIC,masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
GRANT SELECT ON public.insurance_sandbox_source_version TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
CREATE POLICY insurance_sandbox_source_version_read ON public.insurance_sandbox_source_version FOR SELECT TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader USING(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_source_version_append ON public.insurance_sandbox_source_version FOR INSERT TO masters_ins_ref_source_registrar WITH CHECK(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_source_version_guard ON public.insurance_sandbox_source_version TO masters_ins_ref_guard USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
GRANT INSERT(version_id,catalog_scope,fact_id,version,admitted_generation,record_id,insurance_kind,publication_id,publication_revision,submitted_action_id,submitted_intent_digest,canonical_definition,definition_digest,source_text,source_digest,rights_fact_id,rights_version,rights_generation,rights_digest,locale_fact_id,locale_version,locale_generation,locale_digest,canonical_fact,digest,effective_until,registration_action_id) ON public.insurance_sandbox_source_version TO masters_ins_ref_source_registrar;
ALTER TABLE public.insurance_sandbox_locale_control ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_sandbox_locale_control FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_sandbox_locale_control FROM PUBLIC,masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
GRANT SELECT ON public.insurance_sandbox_locale_control TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
CREATE POLICY insurance_sandbox_locale_control_read ON public.insurance_sandbox_locale_control FOR SELECT TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader USING(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_locale_control_append ON public.insurance_sandbox_locale_control FOR INSERT TO masters_ins_ref_source_registrar WITH CHECK(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_locale_control_guard ON public.insurance_sandbox_locale_control TO masters_ins_ref_guard USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
GRANT INSERT(fact_id,catalog_scope,version_id,version,generation,digest,lifecycle) ON public.insurance_sandbox_locale_control TO masters_ins_ref_source_registrar;
GRANT UPDATE(version_id,version,generation,digest,lifecycle) ON public.insurance_sandbox_locale_control TO masters_ins_ref_source_registrar;
CREATE POLICY insurance_sandbox_locale_control_cas ON public.insurance_sandbox_locale_control FOR UPDATE TO masters_ins_ref_source_registrar USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
ALTER TABLE public.insurance_sandbox_rights_control ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_sandbox_rights_control FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_sandbox_rights_control FROM PUBLIC,masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
GRANT SELECT ON public.insurance_sandbox_rights_control TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
CREATE POLICY insurance_sandbox_rights_control_read ON public.insurance_sandbox_rights_control FOR SELECT TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader USING(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_rights_control_append ON public.insurance_sandbox_rights_control FOR INSERT TO masters_ins_ref_source_registrar WITH CHECK(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_rights_control_guard ON public.insurance_sandbox_rights_control TO masters_ins_ref_guard USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
GRANT INSERT(fact_id,catalog_scope,version_id,version,generation,digest,lifecycle) ON public.insurance_sandbox_rights_control TO masters_ins_ref_source_registrar;
GRANT UPDATE(version_id,version,generation,digest,lifecycle) ON public.insurance_sandbox_rights_control TO masters_ins_ref_source_registrar;
CREATE POLICY insurance_sandbox_rights_control_cas ON public.insurance_sandbox_rights_control FOR UPDATE TO masters_ins_ref_source_registrar USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
ALTER TABLE public.insurance_sandbox_source_control ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_sandbox_source_control FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_sandbox_source_control FROM PUBLIC,masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
GRANT SELECT ON public.insurance_sandbox_source_control TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
CREATE POLICY insurance_sandbox_source_control_read ON public.insurance_sandbox_source_control FOR SELECT TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader USING(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_source_control_append ON public.insurance_sandbox_source_control FOR INSERT TO masters_ins_ref_source_registrar WITH CHECK(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_source_control_guard ON public.insurance_sandbox_source_control TO masters_ins_ref_guard USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
GRANT INSERT(fact_id,catalog_scope,version_id,version,generation,digest,lifecycle) ON public.insurance_sandbox_source_control TO masters_ins_ref_source_registrar;
GRANT UPDATE(version_id,version,generation,digest,lifecycle) ON public.insurance_sandbox_source_control TO masters_ins_ref_source_registrar;
CREATE POLICY insurance_sandbox_source_control_cas ON public.insurance_sandbox_source_control FOR UPDATE TO masters_ins_ref_source_registrar USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
ALTER TABLE public.insurance_sandbox_fact_action ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.insurance_sandbox_fact_action FORCE ROW LEVEL SECURITY;
REVOKE ALL ON public.insurance_sandbox_fact_action FROM PUBLIC,masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
GRANT SELECT ON public.insurance_sandbox_fact_action TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader;
CREATE POLICY insurance_sandbox_fact_action_read ON public.insurance_sandbox_fact_action FOR SELECT TO masters_ins_ref_source_registrar,masters_ins_ref_source_reader USING(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_fact_action_append ON public.insurance_sandbox_fact_action FOR INSERT TO masters_ins_ref_source_registrar WITH CHECK(catalog_scope='SANDBOX');
CREATE POLICY insurance_sandbox_fact_action_guard ON public.insurance_sandbox_fact_action TO masters_ins_ref_guard USING(catalog_scope='SANDBOX') WITH CHECK(catalog_scope='SANDBOX');
GRANT INSERT(action_id,catalog_scope,fact_type,fact_id,version_id,version,generation,fact_digest,result_state,operation,command_digest,actor_issuer,actor_subject,actor_tenant,reason_code) ON public.insurance_sandbox_fact_action TO masters_ins_ref_source_registrar;
GRANT TRIGGER ON public.insurance_sandbox_locale_version TO masters_ins_ref_guard;
GRANT TRIGGER ON public.insurance_sandbox_rights_version TO masters_ins_ref_guard;
GRANT TRIGGER ON public.insurance_sandbox_source_version TO masters_ins_ref_guard;
GRANT TRIGGER ON public.insurance_sandbox_locale_control TO masters_ins_ref_guard;
GRANT TRIGGER ON public.insurance_sandbox_rights_control TO masters_ins_ref_guard;
GRANT TRIGGER ON public.insurance_sandbox_source_control TO masters_ins_ref_guard;
GRANT TRIGGER ON public.insurance_sandbox_fact_action TO masters_ins_ref_guard;
RESET ROLE;
SET LOCAL ROLE masters_ins_ref_guard;

CREATE FUNCTION masters_ins_ref_private.v35_registered_version() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE a public.insurance_sandbox_fact_action%ROWTYPE; version_type TEXT; value JSONB; head JSONB;
 original public.insurance_reference_action%ROWTYPE; original_audit public.insurance_reference_audit%ROWTYPE;
 intent public.insurance_reference_maker_intent%ROWTYPE;
BEGIN
 version_type:=CASE TG_TABLE_NAME WHEN 'insurance_sandbox_locale_version' THEN 'LOCALE' WHEN 'insurance_sandbox_rights_version' THEN 'RIGHTS' WHEN 'insurance_sandbox_source_version' THEN 'SOURCE' ELSE NULL END;
 IF TG_TABLE_SCHEMA<>'public' OR version_type IS NULL THEN RAISE EXCEPTION 'Exact sandbox version relation required' USING ERRCODE='42501';END IF;
 SELECT * INTO a FROM public.insurance_sandbox_fact_action WHERE action_id=NEW.registration_action_id;
 IF NOT FOUND OR a.operation<>'REGISTER' OR a.fact_type<>version_type OR a.fact_id<>NEW.fact_id OR a.version_id<>NEW.version_id
  OR a.version<>NEW.version OR a.generation<>NEW.admitted_generation OR a.fact_digest<>NEW.digest OR a.result_state<>'ACTIVE'
  OR a.recorded_at>=NEW.effective_until OR NEW.effective_until>a.recorded_at+INTERVAL '1 day' THEN
  RAISE EXCEPTION 'Complete durable sandbox registration required' USING ERRCODE='42501';END IF;
 value:=NEW.canonical_fact::jsonb;
 IF masters_ins_ref_private.v32_closed(value,ARRAY['encodingVersion','scope','type','factId','version','generation','payload','effectiveUntil']) IS NOT TRUE
  OR value->>'encodingVersion'<>'MASTERS_INSURANCE_SANDBOX_FACT_V1' OR value->>'scope'<>'SANDBOX' OR value->>'type'<>version_type
  OR value->>'factId'<>NEW.fact_id::text OR (value->>'version')::bigint<>NEW.version OR (value->>'generation')::bigint<>NEW.admitted_generation
  OR (value->>'effectiveUntil')::timestamptz IS DISTINCT FROM NEW.effective_until THEN
  RAISE EXCEPTION 'Exact immutable sandbox encoding required' USING ERRCODE='42501';END IF;
 IF version_type='SOURCE' THEN
  SELECT * INTO original FROM public.insurance_reference_action WHERE catalog_scope='GLOBAL_MASTER' AND record_id=NEW.record_id
   AND kind=NEW.insurance_kind AND action_id=NEW.submitted_action_id AND operation='SUBMIT_PUBLICATION';
  IF NOT FOUND OR original.intent_digest<>NEW.submitted_intent_digest OR original.publication_id<>NEW.publication_id OR original.revision<>NEW.publication_revision THEN
   RAISE EXCEPTION 'Actual submitted publication reference required' USING ERRCODE='42501';END IF;
  SELECT * INTO intent FROM public.insurance_reference_maker_intent WHERE intent_id=original.intent_id AND record_id=original.record_id AND kind=original.kind;
  IF NOT FOUND OR intent.intent_digest<>original.intent_digest OR (intent.canonical_intent::jsonb)->'definition' IS DISTINCT FROM NEW.canonical_definition::jsonb THEN
   RAISE EXCEPTION 'Exact original submitted definition required' USING ERRCODE='42501';END IF;
  SELECT * INTO original_audit FROM public.insurance_reference_audit WHERE action_id=original.action_id AND record_id=original.record_id AND kind=original.kind AND event_kind='SUBMIT_PUBLICATION';
  IF NOT FOUND OR original_audit.actor_issuer<>a.actor_issuer OR original_audit.actor_subject<>a.actor_subject THEN
   RAISE EXCEPTION 'Original submitting caller registration required' USING ERRCODE='42501';END IF;
 END IF;
 RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v35_registered_version() FROM PUBLIC;

CREATE FUNCTION masters_ins_ref_private.v35_complete_control() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE a public.insurance_sandbox_fact_action%ROWTYPE; version_type TEXT; v JSONB;
BEGIN
 version_type:=CASE TG_TABLE_NAME WHEN 'insurance_sandbox_locale_control' THEN 'LOCALE' WHEN 'insurance_sandbox_rights_control' THEN 'RIGHTS' WHEN 'insurance_sandbox_source_control' THEN 'SOURCE' ELSE NULL END;
 IF version_type='LOCALE' THEN SELECT to_jsonb(r) INTO v FROM public.insurance_sandbox_locale_version r WHERE version_id=NEW.version_id;
 ELSIF version_type='RIGHTS' THEN SELECT to_jsonb(r) INTO v FROM public.insurance_sandbox_rights_version r WHERE version_id=NEW.version_id;
 ELSIF version_type='SOURCE' THEN SELECT to_jsonb(r) INTO v FROM public.insurance_sandbox_source_version r WHERE version_id=NEW.version_id;
 ELSE RAISE EXCEPTION 'Exact sandbox control relation required' USING ERRCODE='42501';END IF;
 IF v IS NULL OR v->>'fact_id'<>NEW.fact_id::text OR (v->>'version')::bigint<>NEW.version OR v->>'digest'<>NEW.digest THEN
  RAISE EXCEPTION 'Exact sandbox version pointer required' USING ERRCODE='42501';END IF;
 SELECT * INTO a FROM public.insurance_sandbox_fact_action WHERE fact_type=version_type AND fact_id=NEW.fact_id AND generation=NEW.generation;
 IF NOT FOUND OR a.version_id<>NEW.version_id OR a.version<>NEW.version OR a.fact_digest<>NEW.digest OR a.result_state<>NEW.lifecycle
  OR (NEW.lifecycle='ACTIVE' AND (a.operation<>'REGISTER' OR (v->>'admitted_generation')::bigint<>NEW.generation OR pg_catalog.clock_timestamp()>=(v->>'effective_until')::timestamptz))
  OR (NEW.lifecycle='WITHDRAWN' AND a.operation<>'WITHDRAW') THEN
  RAISE EXCEPTION 'Complete current sandbox control transition required' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v35_complete_control() FROM PUBLIC;

CREATE FUNCTION masters_ins_ref_private.v35_complete_action() RETURNS TRIGGER
LANGUAGE plpgsql SECURITY DEFINER SET search_path=pg_catalog AS $$
DECLARE head JSONB;
BEGIN
 IF NEW.fact_type='LOCALE' THEN SELECT to_jsonb(r) INTO head FROM public.insurance_sandbox_locale_control r WHERE fact_id=NEW.fact_id;
 ELSIF NEW.fact_type='RIGHTS' THEN SELECT to_jsonb(r) INTO head FROM public.insurance_sandbox_rights_control r WHERE fact_id=NEW.fact_id;
 ELSIF NEW.fact_type='SOURCE' THEN SELECT to_jsonb(r) INTO head FROM public.insurance_sandbox_source_control r WHERE fact_id=NEW.fact_id;
 ELSE RAISE EXCEPTION 'Exact sandbox action type required' USING ERRCODE='42501';END IF;
 IF head IS NULL OR head->>'version_id'<>NEW.version_id::text OR (head->>'version')::bigint<>NEW.version
  OR (head->>'generation')::bigint<>NEW.generation OR head->>'digest'<>NEW.fact_digest OR head->>'lifecycle'<>NEW.result_state THEN
  RAISE EXCEPTION 'Actual sandbox action/control graph required' USING ERRCODE='42501';END IF;
 RETURN NEW;
END $$;
REVOKE ALL ON FUNCTION masters_ins_ref_private.v35_complete_action() FROM PUBLIC;
CREATE TRIGGER insurance_sandbox_locale_version_immutable BEFORE UPDATE OR DELETE ON public.insurance_sandbox_locale_version FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_fact_immutable();
CREATE TRIGGER insurance_sandbox_rights_version_immutable BEFORE UPDATE OR DELETE ON public.insurance_sandbox_rights_version FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_fact_immutable();
CREATE TRIGGER insurance_sandbox_source_version_immutable BEFORE UPDATE OR DELETE ON public.insurance_sandbox_source_version FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_fact_immutable();
CREATE TRIGGER insurance_sandbox_locale_control_transition BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_sandbox_locale_control FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_control_transition();
CREATE TRIGGER insurance_sandbox_rights_control_transition BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_sandbox_rights_control FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_control_transition();
CREATE TRIGGER insurance_sandbox_source_control_transition BEFORE INSERT OR UPDATE OR DELETE ON public.insurance_sandbox_source_control FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_control_transition();
CREATE TRIGGER insurance_sandbox_fact_action_immutable BEFORE UPDATE OR DELETE ON public.insurance_sandbox_fact_action FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_fact_immutable();
CREATE CONSTRAINT TRIGGER insurance_sandbox_locale_registered AFTER INSERT ON public.insurance_sandbox_locale_version
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_registered_version();
CREATE CONSTRAINT TRIGGER insurance_sandbox_locale_complete AFTER INSERT OR UPDATE ON public.insurance_sandbox_locale_control
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_complete_control();
CREATE CONSTRAINT TRIGGER insurance_sandbox_rights_registered AFTER INSERT ON public.insurance_sandbox_rights_version
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_registered_version();
CREATE CONSTRAINT TRIGGER insurance_sandbox_rights_complete AFTER INSERT OR UPDATE ON public.insurance_sandbox_rights_control
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_complete_control();
CREATE CONSTRAINT TRIGGER insurance_sandbox_source_registered AFTER INSERT ON public.insurance_sandbox_source_version
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_registered_version();
CREATE CONSTRAINT TRIGGER insurance_sandbox_source_complete AFTER INSERT OR UPDATE ON public.insurance_sandbox_source_control
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_complete_control();
CREATE CONSTRAINT TRIGGER insurance_sandbox_action_complete AFTER INSERT ON public.insurance_sandbox_fact_action
 DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION masters_ins_ref_private.v35_complete_action();
RESET ROLE;
SET LOCAL ROLE masters_ins_ref_owner;
REVOKE TRIGGER ON public.insurance_sandbox_locale_version FROM masters_ins_ref_guard;
REVOKE TRIGGER ON public.insurance_sandbox_locale_control FROM masters_ins_ref_guard;
REVOKE TRIGGER ON public.insurance_sandbox_rights_version FROM masters_ins_ref_guard;
REVOKE TRIGGER ON public.insurance_sandbox_rights_control FROM masters_ins_ref_guard;
REVOKE TRIGGER ON public.insurance_sandbox_source_version FROM masters_ins_ref_guard;
REVOKE TRIGGER ON public.insurance_sandbox_source_control FROM masters_ins_ref_guard;
REVOKE TRIGGER ON public.insurance_sandbox_fact_action FROM masters_ins_ref_guard;
RESET ROLE;
SET LOCAL ROLE masters_ins_ref_guard;
REVOKE CREATE ON SCHEMA masters_ins_ref_private FROM masters_ins_ref_guard;
RESET ROLE;
REVOKE CREATE ON SCHEMA public FROM masters_ins_ref_owner,masters_ins_ref_guard;
