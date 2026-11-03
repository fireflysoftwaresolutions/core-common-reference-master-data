package io.fireflysoftware.masters.models.repositories.insurance.reference.impl;

import java.util.Objects;
import java.util.UUID;
import reactor.core.publisher.Mono;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;
import io.fireflysoftware.masters.models.entities.insurance.reference.InsuranceReferenceRows.*;
import io.fireflysoftware.masters.models.repositories.insurance.reference.InsuranceReferenceRepository;

/** No automatic registration: the qualified owner transaction/context construction point is required. */
public final class InsuranceReferenceRepositoryImpl implements InsuranceReferenceRepository {
    private final R2dbcEntityTemplate template;
    public InsuranceReferenceRepositoryImpl(R2dbcEntityTemplate template){this.template=Objects.requireNonNull(template);}
    private static Criteria scope(UUID record,Kind kind){id(record);Objects.requireNonNull(kind);return Criteria.where("catalogScope").is(InsuranceReferenceContracts.SCOPE).and("recordId").is(record).and("kind").is(kind);}
    private <T> Mono<T> one(Class<T> type,Criteria criteria){return template.select(type).matching(Query.query(criteria)).one();}
    @Override public Mono<Identity> identity(UUID record,Kind kind){return Mono.defer(()->one(Identity.class,scope(record,kind)));}
    @Override public Mono<Revision> revision(UUID record,Kind kind,UUID publication,long revision){return Mono.defer(()->{id(publication);if(revision<1)throw invalid();return one(Revision.class,scope(record,kind).and("publicationId").is(publication).and("revision").is(revision));});}
    @Override public Mono<ReferenceControl> control(UUID record,Kind kind){return Mono.defer(()->one(ReferenceControl.class,scope(record,kind)));}
    @Override public Mono<MakerIntent> intent(UUID record,Kind kind,UUID intent){return Mono.defer(()->{id(intent);return one(MakerIntent.class,scope(record,kind).and("intentId").is(intent));});}
    @Override public Mono<IntentControl> intentControl(UUID record,Kind kind,UUID intent){return Mono.defer(()->{id(intent);return one(IntentControl.class,scope(record,kind).and("intentId").is(intent));});}
    @Override public Mono<CheckerApproval> approval(UUID record,Kind kind,UUID intent,UUID approval,String digest){return Mono.defer(()->{id(intent);id(approval);if(digest==null||!digest.matches("[0-9a-f]{64}"))throw invalid();return one(CheckerApproval.class,scope(record,kind).and("intentId").is(intent).and("approvalId").is(approval).and("intentDigest").is(digest));});}
    @Override public Mono<Action> action(UUID record,Kind kind,UUID action){return Mono.defer(()->{id(action);return one(Action.class,scope(record,kind).and("actionId").is(action));});}
    @Override public Mono<Audit> audit(UUID record,Kind kind,UUID action){return Mono.defer(()->{id(action);return one(Audit.class,scope(record,kind).and("actionId").is(action));});}
    @Override public Mono<Outbox> outbox(UUID record,Kind kind,UUID action){return Mono.defer(()->{id(action);return one(Outbox.class,scope(record,kind).and("actionId").is(action));});}
    @Override public Mono<Long> compareAndSetControl(ReferenceControl before,ReferenceControl after){return Mono.defer(()->{
        Objects.requireNonNull(before);Objects.requireNonNull(after);
        if(!before.recordId().equals(after.recordId())||before.kind()!=after.kind()||before.generation()==Long.MAX_VALUE||after.generation()!=before.generation()+1)throw invalid();
        boolean withdraw=before.lifecycle().equals("ACTIVE")&&after.lifecycle().equals("WITHDRAWN")&&before.publicationId().equals(after.publicationId())&&before.revision()==after.revision();
        boolean publish=after.lifecycle().equals("ACTIVE")&&before.revision()!=Long.MAX_VALUE&&after.revision()==before.revision()+1&&!before.publicationId().equals(after.publicationId());
        if(!withdraw&&!publish)throw invalid();
        return exact(template.update(ReferenceControl.class).matching(Query.query(scope(before.recordId(),before.kind()).and("publicationId").is(before.publicationId()).and("revision").is(before.revision()).and("generation").is(before.generation()).and("lifecycle").is(before.lifecycle())))
            .apply(Update.update("publicationId",after.publicationId()).set("revision",after.revision()).set("generation",after.generation()).set("lifecycle",after.lifecycle())));
    });}
    @Override public Mono<Long> compareAndSetIntent(IntentControl before,IntentControl after){return Mono.defer(()->{
        Objects.requireNonNull(before);Objects.requireNonNull(after);
        if(!before.recordId().equals(after.recordId())||before.kind()!=after.kind()||!before.intentId().equals(after.intentId())||before.generation()==Long.MAX_VALUE||after.generation()!=before.generation()+1)throw invalid();
        boolean approve=before.lifecycle().equals("OPEN")&&after.lifecycle().equals("APPROVED")&&after.approvalId()!=null;
        boolean execute=before.lifecycle().equals("APPROVED")&&after.lifecycle().equals("EXECUTED")&&Objects.equals(before.approvalId(),after.approvalId())&&after.executionActionId()!=null;
        boolean withdraw=java.util.Set.of("OPEN","APPROVED").contains(before.lifecycle())&&after.lifecycle().equals("WITHDRAWN")&&Objects.equals(before.approvalId(),after.approvalId());
        if(!approve&&!execute&&!withdraw)throw invalid();
        Criteria criteria=scope(before.recordId(),before.kind()).and("intentId").is(before.intentId()).and("generation").is(before.generation()).and("lifecycle").is(before.lifecycle());
        criteria=before.approvalId()==null?criteria.and("approvalId").isNull():criteria.and("approvalId").is(before.approvalId());
        criteria=before.executionActionId()==null?criteria.and("executionActionId").isNull():criteria.and("executionActionId").is(before.executionActionId());
        return exact(template.update(IntentControl.class).matching(Query.query(criteria)).apply(Update.update("generation",after.generation()).set("lifecycle",after.lifecycle()).set("approvalId",after.approvalId()).set("executionActionId",after.executionActionId())));
    });}
    private static Mono<Long> exact(Mono<Long> count){return count.flatMap(n->n==1?Mono.just(n):Mono.error(n==0?new OptimisticLockingFailureException("Insurance reference control changed"):new DataIntegrityViolationException("Insurance reference control cardinality invalid"))).switchIfEmpty(Mono.error(new DataIntegrityViolationException("Insurance reference control count missing")));}
    private <T> Mono<T> append(T row){return Mono.defer(()->template.insert(Objects.requireNonNull(row)).switchIfEmpty(Mono.error(new DataIntegrityViolationException("Insurance reference insert not observed"))));}
    @Override public Mono<Identity> appendIdentity(Identity row){return append(row);}
    @Override public Mono<Revision> appendRevision(Revision row){return append(row);}
    @Override public Mono<ReferenceControl> appendReferenceControl(ReferenceControl row){return append(row);}
    @Override public Mono<MakerIntent> appendMakerIntent(MakerIntent row){return append(row);}
    @Override public Mono<IntentControl> appendIntentControl(IntentControl row){return append(row);}
    @Override public Mono<CheckerApproval> appendCheckerApproval(CheckerApproval row){return append(row);}
    @Override public Mono<Action> appendAction(Action row){return append(row);}
    @Override public Mono<Audit> appendAudit(Audit row){return append(row);}
    @Override public Mono<Outbox> appendOutbox(Outbox row){return append(row);}
    private static void id(UUID value){if(value==null||value.equals(new UUID(0,0)))throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Invalid insurance reference persistence operation");}
}
