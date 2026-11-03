package io.fireflysoftware.masters.core.services.insurance.source.impl;

import java.time.*;
import java.util.*;
import io.fireflysoftware.masters.core.services.insurance.source.*;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import io.fireflysoftware.masters.models.entities.insurance.source.InsuranceSandboxFactRows.*;
import reactor.core.publisher.Mono;

/** Current persisted exact versions, rights and locale source; never origin-label success. */
public final class InsuranceSandboxFactReaderImpl implements InsuranceSandboxFactReader {
    private final InsuranceSandboxFactAuthority authority;
    private final InsuranceSandboxFactPersistence persistence;
    private record Checked(Reference source,Reference rights,Reference locale,String definitionDigest) {}
    public InsuranceSandboxFactReaderImpl(InsuranceSandboxFactAuthority authority,InsuranceSandboxFactPersistence persistence){this.authority=Objects.requireNonNull(authority);this.persistence=Objects.requireNonNull(persistence);}
    @Override public Mono<Observation> observe(InsuranceReferenceContracts.Revision expected){return Mono.defer(()->{
        if(expected.provenance().origin()!=InsuranceReferenceContracts.Origin.SANDBOX)throw unavailable();
        UUID id=sourceId(expected.provenance().sourceReference());long version=version(expected.provenance().versionText());String definitionDigest=InsuranceReferenceCodec.digest(expected);
        var facts=Map.<String,Object>of("recordId",expected.recordId().toString(),"insuranceKind",expected.kind().name(),"publicationId",expected.publicationId().toString(),"revision",expected.revision(),"definitionDigest",definitionDigest,"version",version);
        return authority.authorize(Type.SOURCE,"read",id,facts).flatMap(access->persistence.run(null,false,access.budget(),access.until(),work->
            work.repository().source(id,version).switchIfEmpty(Mono.error(unavailable())).flatMap(source->{
                InsuranceSandboxFactCodec.validate(source);var sourceRef=InsuranceSandboxFactCodec.reference(source);
                var decoded=InsuranceReferenceCodec.decode(source.canonicalDefinition());
                if(!decoded.equals(expected)||!source.definitionDigest().equals(definitionDigest)||!source.recordId().equals(expected.recordId())
                        ||!source.insuranceKind().equals(expected.kind().name())||!source.publicationId().equals(expected.publicationId())||source.publicationRevision()!=expected.revision())throw unavailable();
                var rightsRef=new Reference(Type.RIGHTS,source.rightsFactId(),source.rightsVersion(),source.rightsGeneration(),source.rightsDigest());
                var localeRef=InsuranceSandboxFactCodec.locale(source.localeFactId(),source.localeVersion(),source.localeGeneration(),source.localeDigest());
                InsuranceSandboxSourceRules.sourceBinding(id,version,expected,source.sourceText(),rightsRef);
                return active(work,sourceRef,source.versionId(),source.effectiveUntil()).then(work.repository().rights(rightsRef.factId(),rightsRef.version()))
                    .switchIfEmpty(Mono.error(unavailable())).flatMap(rights->{
                        InsuranceSandboxFactCodec.validate(rights);if(!rightsRef.equals(InsuranceSandboxFactCodec.reference(rights))||!id.equals(rights.sourceId())
                                ||!rights.insuranceKind().equals(expected.kind().name())||!rights.sourceText().equals(source.sourceText())||!rights.sourceDigest().equals(source.sourceDigest())
                                ||!Objects.equals(localeRef,InsuranceSandboxFactCodec.locale(rights.localeFactId(),rights.localeVersion(),rights.localeGeneration(),rights.localeDigest())))throw unavailable();
                        return active(work,rightsRef,rights.versionId(),rights.effectiveUntil()).then(locale(work,expected,localeRef))
                            .then(authority.current(access,Type.SOURCE,"read",id,facts,work.budget())).thenReturn(new Checked(sourceRef,rightsRef,localeRef,definitionDigest));
                    });
            })).flatMap(done->authority.current(access,Type.SOURCE,"read",id,facts,done.budget()).then(Mono.fromSupplier(()->{
                Instant until=Collections.min(List.of(done.until(),done.observedAt().plus(done.budget().remaining()))).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
                done.budget().remaining();if(!done.observedAt().isBefore(until))throw unavailable();
                return new Observation(done.value().source(),done.value().rights(),done.value().locale(),done.value().definitionDigest(),done.observedAt(),until,done.budget(),access.actor());
            }))));
    }).onErrorMap(error->error instanceof InsuranceSandboxFactFailure?error:unavailable());}
    private Mono<Void> locale(InsuranceSandboxFactPersistence.Work work,InsuranceReferenceContracts.Revision expected,Reference ref){
        if(expected.definition() instanceof InsuranceReferenceContracts.PremiumTax){if(ref!=null)return Mono.error(unavailable());return Mono.empty();}
        if(ref==null)return Mono.error(unavailable());var wording=(InsuranceReferenceContracts.Wording)expected.definition();
        return work.repository().locale(ref.factId(),ref.version()).switchIfEmpty(Mono.error(unavailable())).flatMap(locale->{
            InsuranceSandboxFactCodec.validate(locale);
            if(!ref.equals(InsuranceSandboxFactCodec.reference(locale))||!wording.localeId().equals(locale.localeId())||!wording.localeCode().equals(locale.localeCode()))throw unavailable();
            return active(work,ref,locale.versionId(),locale.effectiveUntil()).then(work.repository().localeSource(locale.localeId())).switchIfEmpty(Mono.error(unavailable())).flatMap(source->{
                if(!"ACTIVE".equals(source.status())||source.sourceEpoch()!=locale.sourceEpoch()||!source.localeCode().equals(locale.localeCode())||!InsuranceSandboxFactCodec.localeEncoding(source).equals(locale.sourceEncoding()))throw unavailable();
                return Mono.<Void>empty();
            });
        });
    }
    private Mono<Void> active(InsuranceSandboxFactPersistence.Work work,Reference ref,UUID versionId,Instant until){
        return work.repository().head(ref.type(),ref.factId()).filter(head->head.versionId().equals(versionId)&&head.version()==ref.version()&&head.generation()==ref.generation()&&head.digest().equals(ref.digest())&&"ACTIVE".equals(head.lifecycle()))
            .switchIfEmpty(Mono.error(unavailable())).then(Mono.fromRunnable(()->{if(!work.nativeNow().isBefore(until))throw unavailable();work.narrow(until);}));
    }
    private static UUID sourceId(String value){String prefix="urn:firefly:sandbox:source:";if(value==null||!value.startsWith(prefix))throw unavailable();UUID id=UUID.fromString(value.substring(prefix.length()));if(!InsuranceSandboxFacts.sourceUri(id).equals(value))throw unavailable();return id;}
    private static long version(String value){if(value==null||!value.matches("v[1-9][0-9]{0,18}"))throw unavailable();return Long.parseLong(value.substring(1));}
    private static InsuranceSandboxFactFailure unavailable(){return new InsuranceSandboxFactFailure(InsuranceSandboxFactFailure.Kind.UNAVAILABLE);}
}
