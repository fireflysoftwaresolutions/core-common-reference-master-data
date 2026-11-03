package io.fireflysoftware.masters.core.services.insurance.source.impl;

import java.time.*;
import java.util.*;
import java.util.function.*;
import io.fireflysoftware.masters.core.services.insurance.source.*;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import io.fireflysoftware.masters.models.entities.insurance.source.InsuranceSandboxFactRows.*;
import io.fireflysoftware.masters.models.repositories.insurance.source.InsuranceSandboxFactRepository.Head;
import org.springframework.dao.*;
import reactor.core.publisher.*;

/** Real durable sandbox administration, independent from the publication maker/checker decision. */
public final class InsuranceSandboxFactRegistrationServiceImpl implements InsuranceSandboxFactRegistrationService {
    private final InsuranceSandboxFactAuthority authority;
    private final InsuranceSandboxFactPersistence persistence;
    private final InsuranceReferencePublicationService publications;
    private final Supplier<UUID> ids;
    private record Created(UUID versionId,Reference reference,Mono<Void> append) {}
    public InsuranceSandboxFactRegistrationServiceImpl(InsuranceSandboxFactAuthority authority,InsuranceSandboxFactPersistence persistence,
            InsuranceReferencePublicationService publications,Supplier<UUID> ids){this.authority=Objects.requireNonNull(authority);this.persistence=Objects.requireNonNull(persistence);this.publications=Objects.requireNonNull(publications);this.ids=Objects.requireNonNull(ids);}

    @Override public Mono<Receipt> registerLocale(RegisterLocale command){
        return register(Type.LOCALE,command.actionId(),command.factId(),command.expectedVersion(),command.expectedGeneration(),command,
            Map.of("localeId",command.localeId().toString(),"localeCode",command.localeCode(),"effectiveUntil",command.effectiveUntil().toString()),
            work->Mono.just(command),(work,input,version,generation)->work.repository().localeSource(command.localeId()).switchIfEmpty(Mono.error(unavailable())).map(source->{
                if(!"ACTIVE".equals(source.status())||!command.localeCode().equals(source.localeCode())||source.sourceEpoch()<0)throw unavailable();
                InsuranceSandboxSourceRules.requireWindow(work.nativeNow(),command.effectiveUntil());
                work.narrow(command.effectiveUntil());
                String encoding=InsuranceSandboxFactCodec.localeEncoding(source),sourceDigest=InsuranceSandboxSourceRules.sha(encoding);
                String canonical=InsuranceSandboxFactCodec.fact(Type.LOCALE,command.factId(),version,generation,
                    InsuranceSandboxFactCodec.localePayload(source.localeId(),source.localeCode(),source.sourceEpoch(),encoding,sourceDigest),command.effectiveUntil());
                String digest=InsuranceSandboxFactCodec.digest(canonical);UUID id=nextId();
                var row=new LocaleVersion(id,"SANDBOX",command.factId(),version,generation,source.localeId(),source.localeCode(),source.sourceEpoch(),encoding,sourceDigest,canonical,digest,command.effectiveUntil(),command.actionId());
                return new Created(id,new Reference(Type.LOCALE,command.factId(),version,generation,digest),work.repository().insertLocale(row));
            }));
    }
    @Override public Mono<Receipt> registerRights(RegisterRights command){
        return register(Type.RIGHTS,command.actionId(),command.factId(),command.expectedVersion(),command.expectedGeneration(),command,
            Map.of("sourceId",command.sourceId().toString(),"insuranceKind",command.insuranceKind().name(),"sourceDigest",InsuranceSandboxSourceRules.sha(command.sourceText()),"effectiveUntil",command.effectiveUntil().toString()),
            work->Mono.just(command),(work,input,version,generation)->locale(work,command.locale()).map(locale->{
                InsuranceSandboxSourceRules.requireWindow(work.nativeNow(),command.effectiveUntil(),locale.map(LocaleVersion::effectiveUntil).orElse(null));
                work.narrow(command.effectiveUntil());
                String sourceDigest=InsuranceSandboxSourceRules.sha(command.sourceText());
                String canonical=InsuranceSandboxFactCodec.fact(Type.RIGHTS,command.factId(),version,generation,
                    InsuranceSandboxFactCodec.rightsPayload(command.sourceId(),command.insuranceKind().name(),sourceDigest,command.locale()),command.effectiveUntil());
                String digest=InsuranceSandboxFactCodec.digest(canonical);UUID id=nextId();var ref=command.locale();
                var row=new RightsVersion(id,"SANDBOX",command.factId(),version,generation,command.sourceId(),command.insuranceKind().name(),command.sourceText(),sourceDigest,
                    ref==null?null:ref.factId(),ref==null?null:ref.version(),ref==null?null:ref.generation(),ref==null?null:ref.digest(),canonical,digest,command.effectiveUntil(),command.actionId());
                return new Created(id,new Reference(Type.RIGHTS,command.factId(),version,generation,digest),work.repository().insertRights(row));
            }));
    }
    @Override public Mono<Receipt> registerSource(RegisterSource command){
        return register(Type.SOURCE,command.actionId(),command.factId(),command.expectedVersion(),command.expectedGeneration(),command,
            Map.of("recordId",command.draft().recordId().toString(),"insuranceKind",command.draft().kind().name(),"originalActionId",command.submitted().actionId().toString(),"effectiveUntil",command.effectiveUntil().toString()),
            access->publications.recover(new InsuranceReferenceCommands.Recovery(command.submitted().actionId(),command.draft().recordId(),command.draft().kind(),command.submitted().intentDigest()))
                .timeout(access.budget().remaining()).map(retained->InsuranceSandboxSourceRules.submitted(command,retained)),
            (work,definition,version,generation)->rights(work,command.rights()).flatMap(rights->locale(work,command.locale()).map(locale->{
                var boundLocale=InsuranceSandboxFactCodec.locale(rights.localeFactId(),rights.localeVersion(),rights.localeGeneration(),rights.localeDigest());
                if(!rights.sourceId().equals(command.factId())||!rights.insuranceKind().equals(definition.kind().name())
                        ||!rights.sourceText().equals(command.sourceText())||!rights.sourceDigest().equals(definition.provenance().sourceDigest())
                        ||!Objects.equals(boundLocale,command.locale()))throw unavailable();
                if(definition.definition() instanceof InsuranceReferenceContracts.Wording wording){
                    var actual=locale.orElseThrow(InsuranceSandboxFactRegistrationServiceImpl::unavailable);
                    if(!wording.localeId().equals(actual.localeId())||!wording.localeCode().equals(actual.localeCode()))throw unavailable();
                }
                InsuranceSandboxSourceRules.requireWindow(work.nativeNow(),command.effectiveUntil(),rights.effectiveUntil(),locale.map(LocaleVersion::effectiveUntil).orElse(null),definition.effectiveUntil());
                work.narrow(command.effectiveUntil());
                String canonicalDefinition=InsuranceReferenceCodec.encode(definition),definitionDigest=InsuranceReferenceCodec.digest(definition);
                String canonical=InsuranceSandboxFactCodec.fact(Type.SOURCE,command.factId(),version,generation,
                    InsuranceSandboxFactCodec.sourcePayload(canonicalDefinition,definitionDigest,definition.provenance().sourceDigest(),command.rights(),command.locale()),command.effectiveUntil());
                String digest=InsuranceSandboxFactCodec.digest(canonical);UUID id=nextId();var ref=command.locale();
                var row=new SourceVersion(id,"SANDBOX",command.factId(),version,generation,definition.recordId(),definition.kind().name(),definition.publicationId(),definition.revision(),command.submitted().actionId(),command.submitted().intentDigest(),canonicalDefinition,
                    definitionDigest,command.sourceText(),definition.provenance().sourceDigest(),command.rights().factId(),command.rights().version(),command.rights().generation(),command.rights().digest(),
                    ref==null?null:ref.factId(),ref==null?null:ref.version(),ref==null?null:ref.generation(),ref==null?null:ref.digest(),canonical,digest,command.effectiveUntil(),command.actionId());
                return new Created(id,new Reference(Type.SOURCE,command.factId(),version,generation,digest),work.repository().insertSource(row));
            })));
    }
    @FunctionalInterface private interface Factory<T>{Mono<Created> create(InsuranceSandboxFactPersistence.Work work,T input,long version,long generation);}
    private <T> Mono<Receipt> register(Type type,UUID actionId,UUID factId,long expectedVersion,long expectedGeneration,Object command,
            Map<String,Object> context,Function<InsuranceSandboxFactAuthority.Access,Mono<T>> prepare,Factory<T> factory){
        return Mono.defer(()->{
            String digest=InsuranceSandboxFactCodec.command("REGISTER_"+type,command);var facts=new HashMap<String,Object>(context);facts.put("commandDigest",digest);facts.put("expectedVersion",expectedVersion);facts.put("expectedGeneration",expectedGeneration);
            return authority.authorize(type,"register",factId,facts).flatMap(access->prepare.apply(access).flatMap(input->
                persistence.run(actionId,true,access.budget(),access.until(),work->gates(work,type,actionId,factId).then(work.repository().action(actionId))
                    .flatMap(action->replay(work,access,type,"REGISTER",factId,digest,action))
                    .switchIfEmpty(Mono.defer(()->work.repository().head(type,factId).map(Optional::of).defaultIfEmpty(Optional.empty()).flatMap(prior->{
                        requireExpected(prior,expectedVersion,expectedGeneration);long version=Math.addExact(expectedVersion,1);long generation=prior.isEmpty()?0:Math.addExact(expectedGeneration,1);
                        if(generation==Long.MAX_VALUE)throw conflict();
                        return factory.create(work,input,version,generation).flatMap(created->{
                            var next=new Head(created.versionId(),version,generation,created.reference().digest(),"ACTIVE");
                            Mono<Void> head=prior.isEmpty()?work.repository().insertHead(type,factId,next):work.repository().compareAndSetHead(type,factId,prior.get(),next);
                            var actor=access.actor();var action=new Action(actionId,"SANDBOX",type.name(),factId,created.versionId(),version,generation,created.reference().digest(),"ACTIVE","REGISTER",digest,actor.issuer(),actor.subject(),actor.tenantId(),null,null);
                            return created.append().then(head).then(work.repository().insertAction(action))
                                .then(authority.current(access,type,"register",factId,facts,work.budget())).thenReturn(new Receipt(actionId,created.reference(),State.ACTIVE));
                        });
                    })))).flatMap(done->deliver(access,type,"register",factId,facts,done))
            ));
        }).onErrorMap(InsuranceSandboxFactRegistrationServiceImpl::failure);
    }
    @Override public Mono<Receipt> withdraw(Withdraw command){return Mono.defer(()->{
        String digest=InsuranceSandboxFactCodec.command("WITHDRAW",command);var facts=Map.<String,Object>of("commandDigest",digest,"expectedVersion",command.expectedVersion(),"expectedGeneration",command.expectedGeneration());
        return authority.authorize(command.type(),"withdraw",command.factId(),facts).flatMap(access->persistence.run(command.actionId(),true,access.budget(),access.until(),work->
            gates(work,command.type(),command.actionId(),command.factId()).then(work.repository().action(command.actionId()))
                .flatMap(action->replay(work,access,command.type(),"WITHDRAW",command.factId(),digest,action))
                .switchIfEmpty(Mono.defer(()->work.repository().head(command.type(),command.factId()).switchIfEmpty(Mono.error(conflict())).flatMap(before->{
                    if(before.version()!=command.expectedVersion()||before.generation()!=command.expectedGeneration()||!"ACTIVE".equals(before.lifecycle())||before.generation()==Long.MAX_VALUE)throw conflict();
                    long generation=Math.addExact(before.generation(),1);var next=new Head(before.versionId(),before.version(),generation,before.digest(),"WITHDRAWN");var actor=access.actor();
                    var action=new Action(command.actionId(),"SANDBOX",command.type().name(),command.factId(),before.versionId(),before.version(),generation,before.digest(),"WITHDRAWN","WITHDRAW",digest,actor.issuer(),actor.subject(),actor.tenantId(),command.reasonCode(),null);
                    return work.repository().compareAndSetHead(command.type(),command.factId(),before,next).then(work.repository().insertAction(action))
                        .then(authority.current(access,command.type(),"withdraw",command.factId(),facts,work.budget())).thenReturn(new Receipt(command.actionId(),new Reference(command.type(),command.factId(),before.version(),generation,before.digest()),State.WITHDRAWN));
                })))) .flatMap(done->deliver(access,command.type(),"withdraw",command.factId(),facts,done)));
    }).onErrorMap(InsuranceSandboxFactRegistrationServiceImpl::failure);}
    private Mono<Optional<LocaleVersion>> locale(InsuranceSandboxFactPersistence.Work work,Reference expected){
        if(expected==null)return Mono.just(Optional.empty());
        return work.repository().locale(expected.factId(),expected.version()).switchIfEmpty(Mono.error(unavailable())).flatMap(row->{
            InsuranceSandboxFactCodec.validate(row);if(!InsuranceSandboxFactCodec.reference(row).equals(expected))throw unavailable();
            return active(work,expected,row.versionId(),row.effectiveUntil()).then(work.repository().localeSource(row.localeId())).switchIfEmpty(Mono.error(unavailable())).map(source->{
                if(!"ACTIVE".equals(source.status())||source.sourceEpoch()!=row.sourceEpoch()||!source.localeCode().equals(row.localeCode())||!InsuranceSandboxFactCodec.localeEncoding(source).equals(row.sourceEncoding()))throw unavailable();return Optional.of(row);
            });
        });
    }
    private Mono<RightsVersion> rights(InsuranceSandboxFactPersistence.Work work,Reference expected){return work.repository().rights(expected.factId(),expected.version()).switchIfEmpty(Mono.error(unavailable())).flatMap(row->{InsuranceSandboxFactCodec.validate(row);if(!InsuranceSandboxFactCodec.reference(row).equals(expected))throw unavailable();return active(work,expected,row.versionId(),row.effectiveUntil()).thenReturn(row);});}
    private Mono<Void> active(InsuranceSandboxFactPersistence.Work work,Reference expected,UUID versionId,Instant until){
        return work.repository().head(expected.type(),expected.factId()).filter(head->head.versionId().equals(versionId)&&head.version()==expected.version()&&head.generation()==expected.generation()&&head.digest().equals(expected.digest())&&head.lifecycle().equals("ACTIVE"))
            .switchIfEmpty(Mono.error(unavailable())).then(Mono.fromRunnable(()->{if(!work.nativeNow().isBefore(until))throw unavailable();work.narrow(until);}));
    }
    private Mono<Receipt> replay(InsuranceSandboxFactPersistence.Work work,InsuranceSandboxFactAuthority.Access access,Type type,String operation,UUID factId,String digest,Action action){
        var actor=access.actor();if(!type.name().equals(action.factType())||!factId.equals(action.factId())||!operation.equals(action.operation())||!digest.equals(action.commandDigest())
                ||!actor.issuer().equals(action.actorIssuer())||!actor.subject().equals(action.actorSubject())||!actor.tenantId().equals(action.actorTenant())||action.recordedAt()==null)throw conflict();
        return Mono.just(new Receipt(action.actionId(),new Reference(type,factId,action.version(),action.generation(),action.factDigest()),State.valueOf(action.resultState())));
    }
    private static void requireExpected(Optional<Head> prior,long version,long generation){if(prior.isEmpty()?version!=0||generation!=0:prior.get().version()!=version||prior.get().generation()!=generation)throw conflict();}
    private Mono<Void> gates(InsuranceSandboxFactPersistence.Work work,Type type,UUID action,UUID fact){var keys=new TreeSet<String>(List.of("masters-insurance-sandbox:action:"+action,"masters-insurance-sandbox:fact:"+type+":"+fact));return Flux.fromIterable(keys).concatMap(work.repository()::gate).then();}
    private Mono<Receipt> deliver(InsuranceSandboxFactAuthority.Access access,Type type,String operation,UUID id,Map<String,Object> facts,InsuranceSandboxFactPersistence.Completed<Receipt> done){
        return authority.current(access,type,operation,id,facts,done.budget()).then(Mono.fromSupplier(()->{done.budget().remaining();return done.value();}))
            .onErrorMap(error->{var f=done.facts();return new InsuranceReferenceOutcome(done.value().actionId(),f.commitAcknowledged(),f.boundaryCompleted(),f.rollbackAcknowledged());});
    }
    private UUID nextId(){UUID id=Objects.requireNonNull(ids.get());if(id.equals(new UUID(0,0)))throw unavailable();return id;}
    private static Throwable failure(Throwable error){if(error instanceof InsuranceReferenceOutcome||error instanceof InsuranceSandboxFactFailure)return error;if(error instanceof DuplicateKeyException||error instanceof OptimisticLockingFailureException)return conflict();return unavailable();}
    private static InsuranceSandboxFactFailure conflict(){return new InsuranceSandboxFactFailure(InsuranceSandboxFactFailure.Kind.CONFLICT);}
    private static InsuranceSandboxFactFailure unavailable(){return new InsuranceSandboxFactFailure(InsuranceSandboxFactFailure.Kind.UNAVAILABLE);}
}
