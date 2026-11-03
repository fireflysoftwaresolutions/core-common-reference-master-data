package io.fireflysoftware.masters.core.services.insurance.reference.impl;

import java.time.*;
import java.util.*;
import java.util.function.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.spi.*;
import reactor.core.publisher.Mono;
import io.fireflysoftware.masters.core.services.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCommands.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;
import io.fireflysoftware.masters.models.entities.insurance.reference.InsuranceReferenceRows.*;

/** Opt-in service only: no bean or native/source-owner success implementation is registered. */
public final class InsuranceReferencePublicationServiceImpl implements InsuranceReferencePublicationService {
    private static final String SCOPE=InsuranceReferenceContracts.SCOPE;
    private static final Duration OPERATION_LIMIT=Duration.ofSeconds(5),INTENT_LIFETIME=Duration.ofDays(1);
    private final boolean enabled;
    private final SecurityContextPort principals;
    private final PolicyDecisionPort policies;
    private final Set<String> issuers;
    private final InsuranceReferencePersistence persistence;
    private final InsuranceReferenceSourceVerifier sources;
    private final Clock clock;
    private final LongSupplier nanos;
    private final Supplier<UUID> ids;
    private record Access(SecurityPrincipal actor,Instant observedAt,Instant until,long beforeNanos,R2dbcOperationBudget budget) { @Override public String toString(){return "InsuranceReferenceAccess";} }
    private record Heads(Optional<Identity> identity,Optional<ReferenceControl> control) {}

    public InsuranceReferencePublicationServiceImpl(boolean enabled,SecurityContextPort principals,PolicyDecisionPort policies,
            Set<String> issuers,InsuranceReferencePersistence persistence,InsuranceReferenceSourceVerifier sources,
            Clock clock,LongSupplier nanos,Supplier<UUID> ids) {
        this.enabled=enabled;this.principals=Objects.requireNonNull(principals);this.policies=Objects.requireNonNull(policies);this.issuers=Set.copyOf(issuers);
        this.persistence=Objects.requireNonNull(persistence);this.sources=Objects.requireNonNull(sources);this.clock=Objects.requireNonNull(clock);this.nanos=Objects.requireNonNull(nanos);if(!persistence.usesTimeSources(clock,nanos))throw new IllegalArgumentException("One original time source graph required");this.ids=Objects.requireNonNull(ids);
    }
    @Override public Mono<Receipt> submitPublication(SubmitPublication command) {
        return Mono.defer(()->{
            Objects.requireNonNull(command);var draft=command.draft();
            return authorize("submit",draft.recordId(),draft.kind()).flatMap(access->{
                var request=request(access,command.actionId(),draft.recordId(),draft.kind(),null,"SUBMIT_PUBLICATION");
                return persistence.execute(access.budget(),request,InsuranceReferencePersistence.Purpose.WRITE,null,work->
                    work.session().repository().action(draft.recordId(),draft.kind(),command.actionId())
                      .flatMap(action->replayPublication(work,access,command,action))
                      .switchIfEmpty(Mono.defer(()->heads(work,draft.recordId(),draft.kind()).flatMap(heads->{
                          requireHead(draft.expected(),heads.control());
                          if(heads.identity().isPresent()&&!heads.identity().get().code().equals(draft.code()))throw unavailable();
                          long revision=draft.expected().publicationId()==null?1:Math.addExact(draft.expected().revision(),1);
                          UUID intentId=nextId(),publicationId=nextId();
                          var candidate=new InsuranceReferenceContracts.Revision(draft.kind(),draft.recordId(),publicationId,revision,draft.code(),draft.effectiveFrom(),draft.effectiveUntil(),draft.definition(),draft.provenance());
                          String canonical=InsuranceReferenceIntentCodec.encode(new InsuranceReferenceIntentCodec.Intent("PUBLISH",candidate,draft.expected()));
                          String digest=InsuranceReferenceIntentCodec.digest(canonical);Instant validUntil=work.stamp().recordedAt().plus(INTENT_LIFETIME);
                          if(candidate.effectiveUntil()!=null&&candidate.effectiveUntil().isBefore(validUntil))validUntil=candidate.effectiveUntil();
                          if(!validUntil.isAfter(work.stamp().recordedAt()))throw unavailable();
                          var repository=work.session().repository();
                          Mono<?> identity=heads.identity().isPresent()?Mono.just(heads.identity().get()):repository.appendIdentity(new Identity(SCOPE,draft.recordId(),draft.kind(),draft.code()));
                          var intent=new MakerIntent(SCOPE,intentId,draft.recordId(),draft.kind(),"PUBLISH",canonical,digest,draft.expected().publicationId(),draft.expected().revision(),draft.expected().generation(),access.actor().issuer(),access.actor().subject(),validUntil,null);
                          var control=new IntentControl(SCOPE,intentId,draft.recordId(),draft.kind(),0,"OPEN",null,null);
                          var action=new Action(SCOPE,command.actionId(),draft.recordId(),draft.kind(),intentId,"SUBMIT_PUBLICATION",digest,publicationId,revision,0,null);
                          return identity.then(repository.appendMakerIntent(intent)).then(repository.appendIntentControl(control)).then(journal(work,access,action));
                      })))).flatMap(done->deliver(access,"submit",draft.recordId(),draft.kind(),done));
            });
        }).onErrorMap(InsuranceReferencePublicationServiceImpl::publicFailure);
    }
    private record ExecutionPrepared(Receipt replay,Checked checked) {}
    @Override public Mono<Receipt> submitWithdrawal(SubmitWithdrawal command) {
        return Mono.defer(()->authorize("submit",command.recordId(),command.kind()).flatMap(access->{
            var request=request(access,command.actionId(),command.recordId(),command.kind(),null,"SUBMIT_WITHDRAWAL");
            return persistence.execute(access.budget(),request,InsuranceReferencePersistence.Purpose.WRITE,null,work->work.session().repository().action(command.recordId(),command.kind(),command.actionId())
                .flatMap(action->{if(!action.operation().equals("SUBMIT_WITHDRAWAL"))throw unavailable();return work.session().repository().intent(command.recordId(),command.kind(),action.intentId()).switchIfEmpty(Mono.error(unavailable())).flatMap(row->{var value=stored(row);if(!value.operation().equals("WITHDRAW")||!value.expected().equals(command.expected()))throw unavailable();return retained(work,access,action);});})
                .switchIfEmpty(Mono.defer(()->heads(work,command.recordId(),command.kind()).flatMap(heads->{
                    requireHead(command.expected(),heads.control());var control=heads.control().orElseThrow();if(!control.lifecycle().equals("ACTIVE"))throw unavailable();
                    return work.session().repository().revision(command.recordId(),command.kind(),control.publicationId(),control.revision()).switchIfEmpty(Mono.error(unavailable())).flatMap(row->{
                        var definition=storedRevision(row);if(heads.identity().isEmpty()||!heads.identity().get().code().equals(definition.code()))throw unavailable();
                        UUID intent=nextId();var value=new InsuranceReferenceIntentCodec.Intent("WITHDRAW",definition,command.expected());String canonical=InsuranceReferenceIntentCodec.encode(value),digest=InsuranceReferenceIntentCodec.digest(canonical);var repository=work.session().repository();
                        var stored=new MakerIntent(SCOPE,intent,command.recordId(),command.kind(),"WITHDRAW",canonical,digest,control.publicationId(),control.revision(),control.generation(),access.actor().issuer(),access.actor().subject(),work.stamp().recordedAt().plus(INTENT_LIFETIME),null);
                        var pointer=new IntentControl(SCOPE,intent,command.recordId(),command.kind(),0,"OPEN",null,null);var action=new Action(SCOPE,command.actionId(),command.recordId(),command.kind(),intent,"SUBMIT_WITHDRAWAL",digest,definition.publicationId(),definition.revision(),0,null);
                        return repository.appendMakerIntent(stored).then(repository.appendIntentControl(pointer)).then(journal(work,access,action));
                    });
                })))).flatMap(done->deliver(access,"submit",command.recordId(),command.kind(),done));
        })).onErrorMap(InsuranceReferencePublicationServiceImpl::publicFailure);
    }
    @Override public Mono<Receipt> recover(Recovery command) {
        return Mono.defer(()->authorize("recover",command.recordId(),command.kind()).flatMap(access->{
            var request=request(access,command.actionId(),command.recordId(),command.kind(),null,"RECOVER");
            return persistence.execute(access.budget(),request,InsuranceReferencePersistence.Purpose.READ,null,work->work.session().repository().action(command.recordId(),command.kind(),command.actionId()).switchIfEmpty(Mono.error(unavailable())).flatMap(action->{
                if(!action.actionId().equals(command.actionId())||!action.intentDigest().equals(command.intentDigest()))throw unavailable();return retained(work,access,action);
            })).flatMap(done->deliver(access,"recover",command.recordId(),command.kind(),done));
        })).onErrorMap(InsuranceReferencePublicationServiceImpl::publicFailure);
    }
    private record Governed(MakerIntent row,InsuranceReferenceIntentCodec.Intent value,IntentControl control,CheckerApproval approval) {}
    private record Checked(Governed governed,Heads heads,Optional<InsuranceReferenceContracts.Revision> prior) {}
    private record SourceChecked(Checked checked,InsuranceReferencePersistence.Window window,InsuranceReferenceSourceVerifier.Evidence evidence) {}

    @Override public Mono<Receipt> approve(IntentAction command) {
        return Mono.defer(()->authorize("approve",command.recordId(),command.kind()).flatMap(access->{
            var request=request(access,command.actionId(),command.recordId(),command.kind(),command.intentId(),"APPROVE");
            return persistence.execute(access.budget(),request,InsuranceReferencePersistence.Purpose.WRITE,null,work->existingIntentAction(work,access,command,"APPROVE")
                .switchIfEmpty(Mono.defer(()->load(work,command,false).flatMap(governed->{
                    if(!governed.control().lifecycle().equals("OPEN")||governed.control().generation()!=command.expectedIntentGeneration()||sameActor(access.actor(),governed.row().makerIssuer(),governed.row().makerSubject()))throw unavailable();
                    return heads(work,command.recordId(),command.kind()).flatMap(heads->{requireHead(governed.value().expected(),heads.control());requireIdentity(heads,governed.value());
                        UUID approvalId=nextId();long next=Math.addExact(governed.control().generation(),1L);Instant until=Collections.min(List.of(governed.row().validUntil(),work.stamp().recordedAt().plus(INTENT_LIFETIME)));
                        var approval=new CheckerApproval(SCOPE,approvalId,command.recordId(),command.kind(),command.intentId(),command.intentDigest(),governed.control().generation(),access.actor().issuer(),access.actor().subject(),until,null);
                        var replacement=new IntentControl(SCOPE,command.intentId(),command.recordId(),command.kind(),next,"APPROVED",approvalId,null);
                        var definition=governed.value().definition();var action=new Action(SCOPE,command.actionId(),command.recordId(),command.kind(),command.intentId(),"APPROVE",command.intentDigest(),definition.publicationId(),definition.revision(),next,null);
                        return work.session().repository().appendCheckerApproval(approval).then(work.session().repository().compareAndSetIntent(governed.control(),replacement)).then(journal(work,access,action));
                    });
                })))).flatMap(done->deliver(access,"approve",command.recordId(),command.kind(),done));
        })).onErrorMap(InsuranceReferencePublicationServiceImpl::publicFailure);
    }
    @Override public Mono<Receipt> withdrawIntent(IntentAction command) {
        return Mono.defer(()->authorize("withdraw-intent",command.recordId(),command.kind()).flatMap(access->{
            var request=request(access,command.actionId(),command.recordId(),command.kind(),command.intentId(),"WITHDRAW_INTENT");
            return persistence.execute(access.budget(),request,InsuranceReferencePersistence.Purpose.WRITE,null,work->existingIntentAction(work,access,command,"WITHDRAW_INTENT")
                .switchIfEmpty(Mono.defer(()->load(work,command,false).flatMap(governed->{
                    if(!Set.of("OPEN","APPROVED").contains(governed.control().lifecycle())||governed.control().generation()!=command.expectedIntentGeneration()||!sameActor(access.actor(),governed.row().makerIssuer(),governed.row().makerSubject()))throw unavailable();
                    long next=Math.addExact(governed.control().generation(),1L);var replacement=new IntentControl(SCOPE,command.intentId(),command.recordId(),command.kind(),next,"WITHDRAWN",governed.control().approvalId(),null);var definition=governed.value().definition();
                    var action=new Action(SCOPE,command.actionId(),command.recordId(),command.kind(),command.intentId(),"WITHDRAW_INTENT",command.intentDigest(),definition.publicationId(),definition.revision(),next,null);
                    return work.session().repository().compareAndSetIntent(governed.control(),replacement).then(journal(work,access,action));
                })))).flatMap(done->deliver(access,"withdraw-intent",command.recordId(),command.kind(),done));
        })).onErrorMap(InsuranceReferencePublicationServiceImpl::publicFailure);
    }
    @Override public Mono<Receipt> execute(IntentAction command) {
        return Mono.defer(()->authorize("execute",command.recordId(),command.kind()).flatMap(access->{
            var prepare=request(access,command.actionId(),command.recordId(),command.kind(),command.intentId(),"PREPARE_EXECUTION");
            return persistence.execute(access.budget(),prepare,InsuranceReferencePersistence.Purpose.READ,null,work->existingIntentAction(work,access,command,"EXECUTE").map(value->new ExecutionPrepared(value,null)).switchIfEmpty(Mono.defer(()->checked(work,command).map(value->new ExecutionPrepared(null,value)))))
                .flatMap(prepared->{
                    if(prepared.value().replay()!=null)return deliver(access,"execute",command.recordId(),command.kind(),new InsuranceReferencePersistence.Completed<>(prepared.value().replay(),prepared.window(),prepared.facts()));
                    var checked=prepared.value().checked();var proposed=checked.governed().value().definition();
                    Mono<SourceChecked> source=checked.governed().value().operation().equals("WITHDRAW")?Mono.just(new SourceChecked(checked,prepared.window(),null)):verifySource(proposed,checked,prepared.window());
                    return source.flatMap(sourceChecked->current(access,"execute",command.recordId(),command.kind()).timeout(sourceChecked.window().remaining()).then(Mono.defer(()->{
                        var request=request(access,command.actionId(),command.recordId(),command.kind(),command.intentId(),"EXECUTE");
                        return persistence.execute(access.budget(),request,InsuranceReferencePersistence.Purpose.WRITE,sourceChecked.window(),work->existingIntentAction(work,access,command,"EXECUTE")
                            .switchIfEmpty(Mono.defer(()->checked(work,command).flatMap(fresh->{
                                if(!fresh.equals(checked))throw unavailable();var governed=fresh.governed();var repository=work.session().repository();var definition=governed.value().definition();
                                InsuranceReferenceContracts.Control prior=fresh.heads().control().map(InsuranceReferencePublicationServiceImpl::publicControl).orElse(null);
                                InsuranceReferenceContracts.Control next=governed.value().operation().equals("PUBLISH")?InsuranceReferenceLifecycle.publish(prior,fresh.prior().orElse(null),definition,governed.value().expected().generation(),work.stamp().recordedAt()):InsuranceReferenceLifecycle.withdraw(Objects.requireNonNull(prior),governed.value().expected().generation());
                                var control=new ReferenceControl(SCOPE,next.recordId(),next.kind(),next.publicationId(),next.revision(),next.generation(),next.state().name());
                                Mono<?> mutation;
                                if(governed.value().operation().equals("PUBLISH")) {
                                    String canonical=InsuranceReferenceCodec.encode(definition);var revision=new Revision(SCOPE,definition.publicationId(),definition.recordId(),definition.kind(),definition.revision(),canonical,InsuranceReferenceCodec.digest(definition),definition.effectiveFrom(),definition.effectiveUntil(),command.actionId(),null);
                                    mutation=repository.appendRevision(revision).then(fresh.heads().control().isEmpty()?repository.appendReferenceControl(control).then():repository.compareAndSetControl(fresh.heads().control().get(),control).then());
                                } else mutation=repository.compareAndSetControl(fresh.heads().control().orElseThrow(),control);
                                long generation=Math.addExact(governed.control().generation(),1L);var executed=new IntentControl(SCOPE,command.intentId(),command.recordId(),command.kind(),generation,"EXECUTED",governed.control().approvalId(),command.actionId());
                                var action=new Action(SCOPE,command.actionId(),command.recordId(),command.kind(),command.intentId(),"EXECUTE",command.intentDigest(),definition.publicationId(),definition.revision(),generation,null);
                                return mutation.then(repository.compareAndSetIntent(governed.control(),executed)).then(journal(work,access,action));
                            })))).flatMap(done->{
                                Mono<Receipt> result=sourceChecked.evidence()==null?deliver(access,"execute",command.recordId(),command.kind(),done):verifySource(proposed,checked,done.window()).flatMap(finalSource->deliver(access,"execute",command.recordId(),command.kind(),new InsuranceReferencePersistence.Completed<>(done.value(),finalSource.window(),done.facts())));
                                return result.timeout(done.window().remaining())
                                    .onErrorMap(error->error instanceof InsuranceReferenceOutcome?error:new InsuranceReferenceOutcome(command.actionId(),done.facts().commitAcknowledged(),done.facts().boundaryCompleted(),done.facts().rollbackAcknowledged()));
                            });
                    })));
                });
        })).onErrorMap(InsuranceReferencePublicationServiceImpl::publicFailure);
    }
    private Mono<SourceChecked> verifySource(InsuranceReferenceContracts.Revision definition,Checked checked,InsuranceReferencePersistence.Window inherited) {
        return Mono.defer(()->{
            inherited.remaining();long before=nanos.getAsLong();Instant observed=clock.instant();
            return sources.verify(definition).switchIfEmpty(Mono.error(unavailable())).timeout(inherited.remaining()).map(evidence->{
                if(!Objects.equals(evidence.definitionDigest(),InsuranceReferenceCodec.digest(definition)))throw unavailable();
                if(definition.definition() instanceof InsuranceReferenceContracts.PremiumTax tax) {
                    if(!Objects.equals(evidence.currencyPolicy(),tax.currencyPolicy())||evidence.postedScale()==null||evidence.postedScale()!=tax.postedScale())throw unavailable();
                } else if(evidence.currencyPolicy()!=null||evidence.postedScale()!=null)throw unavailable();
                return new SourceChecked(checked,persistence.observeSource(inherited,evidence,before,observed),evidence);
            });
        });
    }
    private Mono<Checked> checked(InsuranceReferencePersistence.Work work,IntentAction command) {
        return load(work,command,true).flatMap(governed->{
            if(!governed.control().lifecycle().equals("APPROVED")||governed.control().generation()!=command.expectedIntentGeneration())throw unavailable();
            return heads(work,command.recordId(),command.kind()).flatMap(heads->{requireHead(governed.value().expected(),heads.control());requireIdentity(heads,governed.value());
                if(heads.control().isEmpty())return Mono.just(new Checked(governed,heads,Optional.empty()));
                var current=heads.control().get();return work.session().repository().revision(command.recordId(),command.kind(),current.publicationId(),current.revision()).switchIfEmpty(Mono.error(unavailable())).map(row->new Checked(governed,heads,Optional.of(storedRevision(row))));
            });
        });
    }
    private Mono<Governed> load(InsuranceReferencePersistence.Work work,IntentAction command,boolean approvalRequired) {
        var repository=work.session().repository();
        return repository.intent(command.recordId(),command.kind(),command.intentId()).switchIfEmpty(Mono.error(unavailable())).flatMap(row->{
            var value=stored(row);if(!row.intentId().equals(command.intentId())||!row.intentDigest().equals(command.intentDigest())||row.recordedAt()==null||row.recordedAt().isAfter(work.stamp().recordedAt())||!row.validUntil().isAfter(work.stamp().recordedAt()))throw unavailable();
            return repository.intentControl(command.recordId(),command.kind(),command.intentId()).switchIfEmpty(Mono.error(unavailable())).flatMap(control->{
                requireIntentControl(control);
                if(!control.intentId().equals(row.intentId())||!control.recordId().equals(row.recordId())||control.kind()!=row.kind())throw unavailable();
                if(!approvalRequired)return Mono.just(new Governed(row,value,control,null));
                if(!control.lifecycle().equals("APPROVED")||control.approvalId()==null)throw unavailable();
                return repository.approval(command.recordId(),command.kind(),command.intentId(),control.approvalId(),row.intentDigest()).switchIfEmpty(Mono.error(unavailable())).map(approval->{
                    if(!approval.approvalId().equals(control.approvalId())||!approval.intentId().equals(row.intentId())||!approval.recordId().equals(row.recordId())||approval.kind()!=row.kind()||!approval.intentDigest().equals(row.intentDigest())||approval.expectedIntentGeneration()==Long.MAX_VALUE||control.generation()!=approval.expectedIntentGeneration()+1||sameIdentity(row.makerIssuer(),row.makerSubject(),approval.checkerIssuer(),approval.checkerSubject())||approval.recordedAt()==null||approval.recordedAt().isBefore(row.recordedAt())||approval.recordedAt().isAfter(work.stamp().recordedAt())||!approval.validUntil().isAfter(work.stamp().recordedAt())||approval.validUntil().isAfter(row.validUntil()))throw unavailable();
                    return new Governed(row,value,control,approval);
                });
            });
        });
    }
    private static void requireIdentity(Heads heads,InsuranceReferenceIntentCodec.Intent value) {if(heads.identity().isEmpty()||!heads.identity().get().recordId().equals(value.definition().recordId())||heads.identity().get().kind()!=value.definition().kind()||!heads.identity().get().code().equals(value.definition().code()))throw unavailable();}
    private static InsuranceReferenceContracts.Control publicControl(ReferenceControl row){return new InsuranceReferenceContracts.Control(row.recordId(),row.kind(),row.publicationId(),row.revision(),row.generation(),InsuranceReferenceContracts.State.valueOf(row.lifecycle()));}
    private static InsuranceReferenceContracts.Revision storedRevision(Revision row){var value=InsuranceReferenceCodec.decode(row.canonicalDefinition());if(!InsuranceReferenceCodec.digest(value).equals(row.definitionDigest())||!value.publicationId().equals(row.publicationId())||!value.recordId().equals(row.recordId())||value.kind()!=row.kind()||value.revision()!=row.revision()||!value.effectiveFrom().equals(row.effectiveFrom())||!Objects.equals(value.effectiveUntil(),row.effectiveUntil())||row.recordedAt()==null)throw unavailable();return value;}
    private Mono<Receipt> existingIntentAction(InsuranceReferencePersistence.Work work,Access access,IntentAction command,String operation) {
        return work.session().repository().action(command.recordId(),command.kind(),command.actionId()).flatMap(action->{
            if(!action.operation().equals(operation)||!action.intentId().equals(command.intentId())||!action.intentDigest().equals(command.intentDigest())||command.expectedIntentGeneration()==Long.MAX_VALUE||action.resultGeneration()!=command.expectedIntentGeneration()+1)throw unavailable();
            return retained(work,access,action);
        });
    }
    private Mono<Receipt> retained(InsuranceReferencePersistence.Work work,Access access,Action action) {
        if(!action.recordId().equals(work.stamp().recordId())||action.kind()!=work.stamp().kind()||action.recordedAt()==null||action.recordedAt().isAfter(work.stamp().recordedAt())||!Set.of("SUBMIT_PUBLICATION","SUBMIT_WITHDRAWAL","APPROVE","EXECUTE","WITHDRAW_INTENT").contains(action.operation()))return Mono.error(unavailable());
        var repository=work.session().repository();return repository.intent(action.recordId(),action.kind(),action.intentId()).switchIfEmpty(Mono.error(unavailable())).flatMap(intent->{
            var value=stored(intent);if(intent.recordedAt()==null||intent.recordedAt().isAfter(action.recordedAt())||!action.intentDigest().equals(intent.intentDigest())||!action.publicationId().equals(value.definition().publicationId())||action.revision()!=value.definition().revision())throw unavailable();
            return repository.intentControl(action.recordId(),action.kind(),action.intentId()).switchIfEmpty(Mono.error(unavailable())).flatMap(control->{
                requireIntentControl(control);
                if(action.operation().equals("SUBMIT_PUBLICATION")&&(!value.operation().equals("PUBLISH")||action.resultGeneration()!=0)||action.operation().equals("SUBMIT_WITHDRAWAL")&&(!value.operation().equals("WITHDRAW")||action.resultGeneration()!=0)||action.operation().equals("APPROVE")&&(action.resultGeneration()!=1||control.approvalId()==null)||action.operation().equals("EXECUTE")&&(action.resultGeneration()!=2||control.approvalId()==null)||action.operation().equals("WITHDRAW_INTENT")&&(!control.lifecycle().equals("WITHDRAWN")||action.resultGeneration()!=control.generation()))throw unavailable();
                if(control.generation()<action.resultGeneration()||!control.intentId().equals(intent.intentId())||!control.recordId().equals(intent.recordId())||control.kind()!=intent.kind()||action.operation().equals("EXECUTE")&&(!control.lifecycle().equals("EXECUTED")||!action.actionId().equals(control.executionActionId())))throw unavailable();
                Mono<?> approval=control.approvalId()==null?Mono.just(Boolean.TRUE):repository.approval(action.recordId(),action.kind(),action.intentId(),control.approvalId(),intent.intentDigest()).switchIfEmpty(Mono.error(unavailable())).map(row->{if(!row.approvalId().equals(control.approvalId())||!row.intentId().equals(intent.intentId())||row.kind()!=intent.kind()||!row.recordId().equals(intent.recordId())||!row.intentDigest().equals(intent.intentDigest())||sameIdentity(row.checkerIssuer(),row.checkerSubject(),intent.makerIssuer(),intent.makerSubject())||row.recordedAt()==null||row.recordedAt().isBefore(intent.recordedAt()))throw unavailable();return row;});
                return approval.then(repository.audit(action.recordId(),action.kind(),action.actionId()).switchIfEmpty(Mono.error(unavailable()))).flatMap(audit->{
                    if(!audit.actionId().equals(action.actionId())||!audit.recordId().equals(action.recordId())||audit.kind()!=action.kind()||!audit.eventKind().equals(action.operation())||audit.recordedAt()==null||audit.recordedAt().isBefore(action.recordedAt())||audit.recordedAt().isAfter(work.stamp().recordedAt())||!sameActor(access.actor(),audit.actorIssuer(),audit.actorSubject())||Set.of("SUBMIT_PUBLICATION","SUBMIT_WITHDRAWAL","WITHDRAW_INTENT").contains(action.operation())&&!sameIdentity(audit.actorIssuer(),audit.actorSubject(),intent.makerIssuer(),intent.makerSubject()))throw unavailable();
                    return repository.outbox(action.recordId(),action.kind(),action.actionId()).switchIfEmpty(Mono.error(unavailable())).map(outbox->{if(!outbox.eventId().equals(audit.eventId())||!outbox.actionId().equals(action.actionId())||!outbox.recordId().equals(action.recordId())||outbox.kind()!=action.kind()||!outbox.eventKind().equals(action.operation())||outbox.recordedAt()==null||outbox.recordedAt().isBefore(audit.recordedAt())||outbox.recordedAt().isAfter(work.stamp().recordedAt()))throw unavailable();return receipt(action);});
                });
            });
        }).flatMap(receipt->retainedIdentityAndPublication(work,action).thenReturn(receipt));
    }
    private Mono<Void> retainedIdentityAndPublication(InsuranceReferencePersistence.Work work,Action action) {
        var repository=work.session().repository();return repository.intent(action.recordId(),action.kind(),action.intentId()).switchIfEmpty(Mono.error(unavailable())).flatMap(intent->{var value=stored(intent);
            return heads(work,action.recordId(),action.kind()).flatMap(heads->{requireIdentity(heads,value);
                if(!action.operation().equals("EXECUTE"))return Mono.just(Boolean.TRUE);
                var control=heads.control().orElseThrow();long effectGeneration=value.expected().publicationId()==null?0:Math.addExact(value.expected().generation(),1L);
                if(control.generation()<effectGeneration||control.revision()<action.revision()||value.operation().equals("WITHDRAW")&&control.publicationId().equals(action.publicationId())&&!control.lifecycle().equals("WITHDRAWN"))throw unavailable();
                return repository.revision(action.recordId(),action.kind(),action.publicationId(),action.revision()).switchIfEmpty(Mono.error(unavailable())).map(row->{
                    if(!storedRevision(row).equals(value.definition())||value.operation().equals("PUBLISH")&&!row.publicationActionId().equals(action.actionId()))throw unavailable();return Boolean.TRUE;
                });
            });
        }).then();
    }
    private static void requireIntentControl(IntentControl control) {
        boolean valid=switch(control.lifecycle()){
            case "OPEN"->control.generation()==0&&control.approvalId()==null&&control.executionActionId()==null;
            case "APPROVED"->control.generation()==1&&control.approvalId()!=null&&control.executionActionId()==null;
            case "EXECUTED"->control.generation()==2&&control.approvalId()!=null&&control.executionActionId()!=null;
            case "WITHDRAWN"->control.generation()==(control.approvalId()==null?1:2)&&control.executionActionId()==null;
            default->false;
        };if(!valid)throw unavailable();
    }
    private static boolean sameActor(SecurityPrincipal actor,String issuer,String subject){return sameIdentity(actor.issuer(),actor.subject(),issuer,subject);}
    private static boolean sameIdentity(String issuer,String subject,String otherIssuer,String otherSubject){return Objects.equals(issuer,otherIssuer)&&Objects.equals(subject,otherSubject);}
    private Mono<Receipt> replayPublication(InsuranceReferencePersistence.Work work,Access access,SubmitPublication request,Action action) {
        var d=request.draft();if(!action.operation().equals("SUBMIT_PUBLICATION"))return Mono.error(unavailable());
        return work.session().repository().intent(d.recordId(),d.kind(),action.intentId()).switchIfEmpty(Mono.error(unavailable())).map(intent->{
            var value=stored(intent);var candidate=value.definition();
            if(!intent.makerIssuer().equals(access.actor().issuer())||!intent.makerSubject().equals(access.actor().subject())||!value.operation().equals("PUBLISH")||!value.expected().equals(d.expected())||!candidate.recordId().equals(d.recordId())||candidate.kind()!=d.kind()||!candidate.code().equals(d.code())||!candidate.effectiveFrom().equals(d.effectiveFrom())||!Objects.equals(candidate.effectiveUntil(),d.effectiveUntil())||!candidate.definition().equals(d.definition())||!candidate.provenance().equals(d.provenance())||!action.intentDigest().equals(intent.intentDigest())||!action.publicationId().equals(candidate.publicationId())||action.revision()!=candidate.revision())throw unavailable();
            return receipt(action);
        }).flatMap(value->retained(work,access,action));
    }
    private Mono<Heads> heads(InsuranceReferencePersistence.Work work,UUID record,Kind kind) {
        var repository=work.session().repository();
        return repository.identity(record,kind).map(Optional::of).defaultIfEmpty(Optional.empty()).flatMap(identity->
            repository.control(record,kind).map(Optional::of).defaultIfEmpty(Optional.empty()).map(control->new Heads(identity,control)));
    }
    private static void requireHead(Head expected,Optional<ReferenceControl> current) {
        if(current.isEmpty()){if(expected.publicationId()!=null)throw unavailable();return;}
        var value=current.get();if(!Objects.equals(expected.publicationId(),value.publicationId())||expected.revision()!=value.revision()||expected.generation()!=value.generation())throw unavailable();
    }
    private Mono<Receipt> journal(InsuranceReferencePersistence.Work work,Access access,Action action) {
        var repository=work.session().repository();UUID event=nextId();
        return repository.appendAction(action).then(repository.appendAudit(new Audit(SCOPE,event,action.recordId(),action.kind(),action.actionId(),action.operation(),access.actor().issuer(),access.actor().subject(),null)))
            .then(repository.appendOutbox(new Outbox(SCOPE,event,action.recordId(),action.kind(),action.actionId(),action.operation(),null)))
            .then(current(access,policyOperation(action.operation()),action.recordId(),action.kind()).timeout(work.window().remaining())).thenReturn(receipt(action));
    }
    private static String policyOperation(String operation){return switch(operation){case "SUBMIT_PUBLICATION","SUBMIT_WITHDRAWAL"->"submit";case "APPROVE"->"approve";case "EXECUTE"->"execute";case "WITHDRAW_INTENT"->"withdraw-intent";default->throw unavailable();};}
    private static Receipt receipt(Action action){return new Receipt(action.actionId(),action.recordId(),action.kind(),action.intentId(),action.intentDigest(),action.publicationId(),action.revision(),action.resultGeneration(),action.operation());}
    private static InsuranceReferenceIntentCodec.Intent stored(MakerIntent intent) {
        var value=InsuranceReferenceIntentCodec.decode(intent.canonicalIntent());
        if(!InsuranceReferenceIntentCodec.digest(intent.canonicalIntent()).equals(intent.intentDigest())||!value.operation().equals(intent.operation())||!value.definition().recordId().equals(intent.recordId())||value.definition().kind()!=intent.kind()||!Objects.equals(value.expected().publicationId(),intent.expectedPublicationId())||value.expected().revision()!=intent.expectedRevision()||value.expected().generation()!=intent.expectedGeneration())throw unavailable();
        return value;
    }
    private Mono<Access> authorize(String operation,UUID record,Kind kind) {
        return Mono.defer(()->{
            if(!enabled)return Mono.error(unavailable());long before=nanos.getAsLong();Instant observed=clock.instant();
            return principals.currentPrincipal().switchIfEmpty(Mono.error(unavailable())).flatMap(actor->{
                Instant expiry=trusted(actor);Instant until=Collections.min(List.of(observed.plus(OPERATION_LIMIT),expiry));
                var access=new Access(actor,observed,until,before,R2dbcOperationBudget.observed(observed,until,before,OPERATION_LIMIT,clock,nanos));
                return policy(actor,operation,record,kind).then(Mono.fromSupplier(()->{access.budget().remaining();return access;}));
            }).timeout(OPERATION_LIMIT).onErrorMap(error->unavailable());
        });
    }
    private Mono<Void> current(Access access,String operation,UUID record,Kind kind) {
        return Mono.defer(()->{
            access.budget().remaining();return principals.currentPrincipal().switchIfEmpty(Mono.error(unavailable())).flatMap(actor->{
                trusted(actor);if(!actor.issuer().equals(access.actor().issuer())||!actor.subject().equals(access.actor().subject())||!Objects.equals(actor.tenantId(),access.actor().tenantId()))return Mono.error(unavailable());
                return policy(actor,operation,record,kind).then(Mono.<Void>fromRunnable(()->access.budget().remaining()));
            }).timeout(access.budget().remaining());
        });
    }
    private Mono<Void> policy(SecurityPrincipal actor,String operation,UUID record,Kind kind) {
        return policies.authorize(actor,"masters.insurance-reference."+operation,"insurance-reference/"+kind+"/"+record,Map.of("scope",SCOPE,"recordId",record.toString(),"kind",kind.name()))
            .filter(decision->decision.granted()&&decision.obligations().isEmpty()).switchIfEmpty(Mono.error(unavailable())).then();
    }
    private Instant trusted(SecurityPrincipal actor) {
        if(actor==null||actor.subject()==null||actor.subject().isBlank()||actor.subject().length()>255||actor.issuer()==null||!issuers.contains(actor.issuer()))throw unavailable();
        var expiry=actor.claim("exp",Instant.class);if(expiry==null||!expiry.isAfter(clock.instant()))throw unavailable();return expiry;
    }
    private <T> Mono<T> deliver(Access access,String operation,UUID record,Kind kind,InsuranceReferencePersistence.Completed<T> done) {
        return Mono.defer(()->current(access,operation,record,kind).timeout(done.window().remaining()).then(Mono.fromSupplier(()->{done.window().remaining();return done.value();})))
            .onErrorMap(error->{var facts=done.facts();return new InsuranceReferenceOutcome(done.value() instanceof Receipt value?value.actionId():null,facts.commitAcknowledged(),facts.boundaryCompleted(),facts.rollbackAcknowledged());});
    }
    private InsuranceReferenceNativeContext.Request request(Access access,UUID action,UUID record,Kind kind,UUID intent,String mode){return new InsuranceReferenceNativeContext.Request(nextId(),record,kind,action,intent,mode,access.actor().issuer(),access.actor().subject(),access.until());}
    private UUID nextId(){var value=Objects.requireNonNull(ids.get());if(value.equals(new UUID(0,0)))throw unavailable();return value;}
    private static Throwable publicFailure(Throwable error){return error instanceof InsuranceReferenceOutcome?error:unavailable();}
    private static IllegalStateException unavailable(){return new IllegalStateException("INSURANCE_REFERENCE_CURRENT_OPERATION_UNAVAILABLE");}
}
