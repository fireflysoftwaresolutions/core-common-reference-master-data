/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.masters.models.repositories.currency.policy;

import java.util.UUID;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;
import io.fireflysoftware.masters.models.entities.currency.policy.CurrencyPolicyRows.Identity;

@NoRepositoryBean
public interface CurrencyPolicyTechnicalRepository extends Repository<Identity,String> {
    @Query("SELECT pg_catalog.floor(extract(epoch from pg_catalog.clock_timestamp())*1000)::bigint AS now")
    Mono<Long> now();
    @Query("SELECT 1 AS acquired FROM pg_catalog.pg_advisory_xact_lock(pg_catalog.hashtextextended(:key,0))")
    Mono<Integer> gate(@Param("key") String key);
    @Query("SELECT public.currency_policy_lock_source(:currency) AS locked")
    Mono<Boolean> lockSource(@Param("currency") UUID currency);
    @Query("""
SELECT NOT r.rolsuper AND NOT r.rolbypassrls
 AND EXISTS(SELECT 1 FROM pg_catalog.pg_class v JOIN pg_catalog.pg_namespace vn ON vn.oid=v.relnamespace
  WHERE vn.nspname='public' AND v.relname='currency_policy_observation' AND v.relkind='v'
  AND v.reloptions @> ARRAY['security_invoker=true'] AND NOT pg_catalog.pg_has_role(current_user,v.relowner,'MEMBER'))
 AND pg_catalog.has_table_privilege(current_user,'public.currency_policy_observation','SELECT')
 AND EXISTS(SELECT 1 FROM pg_catalog.pg_proc lock_fn JOIN pg_catalog.pg_namespace lock_ns ON lock_ns.oid=lock_fn.pronamespace
  WHERE lock_ns.nspname='public' AND lock_fn.proname='currency_policy_lock_source'
  AND pg_catalog.pg_get_function_identity_arguments(lock_fn.oid)='p_currency uuid'
  AND NOT lock_fn.prosecdef AND lock_fn.proconfig @> ARRAY['search_path=pg_catalog']
  AND NOT pg_catalog.pg_has_role(current_user,lock_fn.proowner,'MEMBER'))
 AND pg_catalog.has_function_privilege(current_user,'public.currency_policy_lock_source(uuid)','EXECUTE')
                                                     AND NOT pg_catalog.has_schema_privilege(current_user,'public','CREATE')
                                                     AND NOT EXISTS(SELECT 1 FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace
                                                      WHERE n.nspname='public' AND c.relname IN('currency_policy_identity','currency_execution_policy','currency_policy_action','currency_policy_control','currency_policy_audit','currency_policy_outbox')
                                                      AND (pg_catalog.pg_get_userbyid(c.relowner)=current_user OR NOT c.relrowsecurity OR NOT c.relforcerowsecurity))
                                                     AND (SELECT count(*) FROM pg_catalog.pg_class c JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public'
                                                      AND c.relname IN('currency_policy_identity','currency_execution_policy','currency_policy_action','currency_policy_control','currency_policy_audit','currency_policy_outbox'))=6
                                                     AND EXISTS(SELECT 1 FROM pg_catalog.pg_proc p JOIN pg_catalog.pg_namespace n ON n.oid=p.pronamespace JOIN pg_catalog.pg_roles o ON o.oid=p.proowner
                                                      WHERE n.nspname='public' AND p.proname='guard_currency_catalog_identity' AND p.prosecdef AND NOT o.rolsuper AND NOT o.rolbypassrls AND NOT o.rolcanlogin
                                                      AND NOT pg_catalog.pg_has_role(current_user,o.oid,'MEMBER') AND p.proconfig @> ARRAY['search_path=pg_catalog']) AS allowed
                                                    FROM pg_catalog.pg_roles r WHERE r.rolname=current_user
        """)
    Mono<Boolean> runtimeAllowed();
    @Query("SELECT pg_catalog.set_config('firefly.currency.currency_id',:currency,true) AS currency_scope,pg_catalog.set_config('firefly.currency.purpose',:purpose,true) AS purpose_scope")
    Mono<Scope> establish(@Param("currency") String currency, @Param("purpose") String purpose);
    record Scope(String currencyScope, String purposeScope) {}
}
