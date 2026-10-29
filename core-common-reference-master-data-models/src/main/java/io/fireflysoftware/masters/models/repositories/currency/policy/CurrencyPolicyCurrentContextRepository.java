package io.fireflysoftware.masters.models.repositories.currency.policy;

import java.time.OffsetDateTime;
import java.util.UUID;
import io.fireflysoftware.masters.models.entities.currency.policy.CurrencyPolicyRows.Identity;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;

/** Technical native admission calls; business content is read through mapped projection repositories. */
@NoRepositoryBean
public interface CurrencyPolicyCurrentContextRepository extends Repository<Identity,String> {
    @Query("SELECT public.currency_policy_current_runtime_allowed() AS allowed") Mono<Boolean> runtimeAllowed();
    @Query("SELECT * FROM public.currency_policy_current_begin(:nonce,:policy,:currency,:iso,:version,:digest,:encoding,:scale,:issuer,:subject,:tenant,:until)")
    Mono<Stamp> begin(@Param("nonce") UUID nonce,@Param("policy") UUID policy,@Param("currency") UUID currency,
            @Param("iso") String iso,@Param("version") long version,@Param("digest") String digest,@Param("encoding") String encoding,
            @Param("scale") int scale,@Param("issuer") String issuer,@Param("subject") String subject,@Param("tenant") String tenant,@Param("until") OffsetDateTime until);
    @Query("SELECT public.currency_policy_current_complete(:nonce) AS completed") Mono<Boolean> complete(@Param("nonce") UUID nonce);
    record Stamp(UUID contextNonce,UUID policyId,UUID currencyId,String isoCode,Long version,String definitionDigest,String encodingVersion,
            Integer postedScale,Long dbNowMillis,OffsetDateTime recordedAt,OffsetDateTime effectiveUntil) {}
}
