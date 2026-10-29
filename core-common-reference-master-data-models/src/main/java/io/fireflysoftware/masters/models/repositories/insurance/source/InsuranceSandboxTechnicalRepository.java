package io.fireflysoftware.masters.models.repositories.insurance.source;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.r2dbc.repository.Query;
import io.fireflysoftware.masters.models.entities.insurance.source.InsuranceSandboxFactRows.Action;
import reactor.core.publisher.Mono;

/** Technical clock and transaction gate only; no business CRUD function. */
@NoRepositoryBean
public interface InsuranceSandboxTechnicalRepository extends Repository<Action,UUID> {
    @Query("SELECT pg_catalog.clock_timestamp() AS observed_at") Mono<OffsetDateTime> now();
    @Query("SELECT 1 AS acquired FROM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(:key,736135))") Mono<Integer> gate(@Param("key") String key);
    @Query("""
SELECT session_user=current_user AND r.rolname=:expected_role AND r.rolcanlogin AND NOT r.rolsuper AND NOT r.rolbypassrls
 AND NOT r.rolinherit AND NOT r.rolcreatedb AND NOT r.rolcreaterole AND NOT r.rolreplication
 AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_auth_members m WHERE m.member=r.oid)
 AND NOT pg_catalog.has_schema_privilege(current_user,'public','CREATE')
 AND NOT pg_catalog.pg_has_role(current_user,'masters_ins_ref_owner','MEMBER')
 AND NOT pg_catalog.pg_has_role(current_user,'masters_ins_ref_guard','MEMBER')
 AND (SELECT count(*) FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
      JOIN pg_catalog.pg_roles o ON o.oid=c.relowner WHERE n.nspname='public'
      AND c.relname IN('insurance_sandbox_locale_version','insurance_sandbox_rights_version','insurance_sandbox_source_version',
       'insurance_sandbox_locale_control','insurance_sandbox_rights_control','insurance_sandbox_source_control','insurance_sandbox_fact_action')
      AND o.rolname='masters_ins_ref_owner' AND c.relrowsecurity AND c.relforcerowsecurity)=7
 AND EXISTS(SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
      WHERE n.nspname='public' AND c.relname='insurance_sandbox_locale_source' AND c.relkind='v'
      AND c.reloptions @> ARRAY['security_invoker=true']) AS allowed
FROM pg_catalog.pg_roles r WHERE r.rolname=current_user
        """)
    Mono<Boolean> runtimeAllowed(@Param("expected_role") String expectedRole);

}
