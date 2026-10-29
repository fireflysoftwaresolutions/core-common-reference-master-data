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

/** Explicit technical CURRENT factory only; no business CRUD or authority is implemented here. */
@NoRepositoryBean
public interface InsuranceReferenceCurrentContextRepository extends Repository<Identity,UUID> {
    record Observation(@Column("context_nonce") UUID nonce,@Column("record_id") UUID recordId,Kind kind,
            @Column("publication_id") UUID publicationId,long revision,@Column("definition_digest") String definitionDigest,
            @Column("control_generation") long generation,@Column("db_now_ms") long dbNowMillis,
            @Column("recorded_at") OffsetDateTime recordedAt,@Column("effective_until") OffsetDateTime effectiveUntil) {
        @Override public String toString(){return "InsuranceReferenceCurrentNativeProjection";}
    }
    @Query("SELECT context_nonce,record_id,kind,publication_id,revision,definition_digest,control_generation,db_now_ms,recorded_at,effective_until FROM public.masters_ins_ref_current_begin(:nonce,:record,:kind,:publication,:revision,:digest,:generation,:issuer,:subject,:authority_until)")
    Mono<Observation> begin(@Param("nonce") UUID nonce,@Param("record") UUID recordId,@Param("kind") Kind kind,
            @Param("publication") UUID publicationId,@Param("revision") long revision,@Param("digest") String digest,
            @Param("generation") long generation,@Param("issuer") String issuer,@Param("subject") String subject,
            @Param("authority_until") OffsetDateTime authorityUntil);
    @Query("SELECT public.masters_ins_ref_current_complete(:nonce) AS complete")
    Mono<Boolean> complete(@Param("nonce") UUID nonce);
}
