package io.fireflysoftware.masters.models.repositories.insurance.reference;

import java.time.OffsetDateTime;
import java.util.UUID;
import reactor.core.publisher.Mono;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.relational.core.mapping.Column;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;
import io.fireflysoftware.masters.models.entities.insurance.reference.InsuranceReferenceRows.Identity;

/** Explicit factory construction only; native context calls are not business CRUD or authority. */
@NoRepositoryBean
public interface InsuranceReferenceContextRepository extends Repository<Identity,UUID> {
    record Observation(@Column("context_nonce") UUID nonce,@Column("record_id") UUID recordId,Kind kind,
            @Column("db_now_ms") long dbNowMillis,@Column("recorded_at") OffsetDateTime recordedAt,
            @Column("effective_until") OffsetDateTime effectiveUntil) {
        @Override public String toString(){return "InsuranceReferenceNativeObservation";}
    }
    @Query("SELECT context_nonce,record_id,kind,db_now_ms,recorded_at,effective_until FROM public.masters_ins_ref_begin(:nonce,:record,:kind,:action,:intent,:mode,:issuer,:subject,:authority_until)")
    Mono<Observation> begin(@Param("nonce") UUID nonce,@Param("record") UUID recordId,@Param("kind") Kind kind,
            @Param("action") UUID actionId,@Param("intent") UUID intentId,@Param("mode") String mode,
            @Param("issuer") String issuer,@Param("subject") String subject,@Param("authority_until") OffsetDateTime authorityUntil);
    @Query("SELECT public.masters_ins_ref_complete(:nonce) AS complete")
    Mono<Boolean> complete(@Param("nonce") UUID nonce);
}
