package io.fireflysoftware.masters.models.entities.insurance.source;

import java.time.*;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.*;

/** Persisted synthetic facts only; no row is a current principal or publication approval. */
public final class InsuranceSandboxFactRows {
    private InsuranceSandboxFactRows() {}
    @Table("insurance_sandbox_locale_version")
    public record LocaleVersion(@Id UUID versionId,String catalogScope,UUID factId,long version,long admittedGeneration,
            UUID localeId,String localeCode,long sourceEpoch,String sourceEncoding,String sourceDigest,String canonicalFact,String digest,
            Instant effectiveUntil,UUID registrationActionId) {}
    @Table("insurance_sandbox_rights_version")
    public record RightsVersion(@Id UUID versionId,String catalogScope,UUID factId,long version,long admittedGeneration,
            UUID sourceId,String insuranceKind,String sourceText,String sourceDigest,
            UUID localeFactId,Long localeVersion,Long localeGeneration,String localeDigest,String canonicalFact,String digest,
            Instant effectiveUntil,UUID registrationActionId) {}
    @Table("insurance_sandbox_source_version")
    public record SourceVersion(@Id UUID versionId,String catalogScope,UUID factId,long version,long admittedGeneration,
            UUID recordId,String insuranceKind,UUID publicationId,long publicationRevision,UUID submittedActionId,String submittedIntentDigest,String canonicalDefinition,
            String definitionDigest,String sourceText,String sourceDigest,UUID rightsFactId,long rightsVersion,long rightsGeneration,String rightsDigest,
            UUID localeFactId,Long localeVersion,Long localeGeneration,String localeDigest,String canonicalFact,String digest,
            Instant effectiveUntil,UUID registrationActionId) {}
    @Table("insurance_sandbox_locale_control")
    public record LocaleControl(@Id UUID factId,String catalogScope,UUID versionId,long version,long generation,String digest,String lifecycle) {}
    @Table("insurance_sandbox_rights_control")
    public record RightsControl(@Id UUID factId,String catalogScope,UUID versionId,long version,long generation,String digest,String lifecycle) {}
    @Table("insurance_sandbox_source_control")
    public record SourceControl(@Id UUID factId,String catalogScope,UUID versionId,long version,long generation,String digest,String lifecycle) {}
    @Table("insurance_sandbox_fact_action")
    public record Action(@Id UUID actionId,String catalogScope,String factType,UUID factId,UUID versionId,long version,long generation,
            String factDigest,String resultState,String operation,String commandDigest,String actorIssuer,String actorSubject,String actorTenant,
            String reasonCode,@ReadOnlyProperty Instant recordedAt) {}
    @Table("insurance_sandbox_locale_source")
    public record LocaleSource(@Id UUID localeId,String languageCode,String countryCode,String localeCode,String languageName,
            String nativeName,String regionName,Boolean rtl,Integer sortOrder,String status,LocalDateTime dateCreated,LocalDateTime dateUpdated,
            long sourceEpoch) {}
}
