package io.fireflysoftware.masters.models.repositories.insurance.source.impl;

import java.time.Instant;
import java.util.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.r2dbc.core.*;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.relational.core.query.*;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.Type;
import io.fireflysoftware.masters.models.entities.insurance.source.InsuranceSandboxFactRows.*;
import io.fireflysoftware.masters.models.repositories.insurance.source.*;
import reactor.core.publisher.Mono;

/** Assigned-ID Spring Data generated inserts retain actual transaction-bound affected-row counts. */
public final class InsuranceSandboxFactRepositoryImpl implements InsuranceSandboxFactRepository {
    private final R2dbcEntityTemplate operations;
    private final InsuranceSandboxTechnicalRepository technical;
    public InsuranceSandboxFactRepositoryImpl(R2dbcEntityTemplate operations){this.operations=Objects.requireNonNull(operations);technical=new R2dbcRepositoryFactory(operations).getRepository(InsuranceSandboxTechnicalRepository.class);}
    @Override public Mono<Boolean> runtimeAllowed(String expectedRole){return technical.runtimeAllowed(expectedRole);}
    @Override public Mono<Instant> now(){return technical.now().map(java.time.OffsetDateTime::toInstant).switchIfEmpty(Mono.error(invalid()));}
    @Override public Mono<Void> gate(String key){
        if(key==null||!key.startsWith("masters-insurance-sandbox:")||key.length()>180)return Mono.error(invalid());
        return TransactionSynchronizationManager.forCurrentTransaction().filter(TransactionSynchronizationManager::isActualTransactionActive)
            .switchIfEmpty(Mono.error(invalid())).then(technical.gate(key).filter(value->value==1).switchIfEmpty(Mono.error(invalid()))).then();
    }
    private static Criteria scope(UUID id){return Criteria.where("catalogScope").is(InsuranceSandboxFacts.SCOPE).and("factId").is(Objects.requireNonNull(id));}
    private <T> Mono<T> one(Class<T> type,Criteria criteria){return operations.select(type).matching(Query.query(criteria)).one();}
    @Override public Mono<Head> head(Type type,UUID factId){return switch(type){
        case LOCALE->one(LocaleControl.class,scope(factId)).map(r->new Head(r.versionId(),r.version(),r.generation(),r.digest(),r.lifecycle()));
        case RIGHTS->one(RightsControl.class,scope(factId)).map(r->new Head(r.versionId(),r.version(),r.generation(),r.digest(),r.lifecycle()));
        case SOURCE->one(SourceControl.class,scope(factId)).map(r->new Head(r.versionId(),r.version(),r.generation(),r.digest(),r.lifecycle()));};}
    @Override public Mono<LocaleSource> localeSource(UUID id){return operations.select(LocaleSource.class).matching(Query.query(Criteria.where("localeId").is(id))).one();}
    @Override public Mono<LocaleVersion> locale(UUID id,long version){return one(LocaleVersion.class,scope(id).and("version").is(version));}
    @Override public Mono<RightsVersion> rights(UUID id,long version){return one(RightsVersion.class,scope(id).and("version").is(version));}
    @Override public Mono<SourceVersion> source(UUID id,long version){return one(SourceVersion.class,scope(id).and("version").is(version));}
    @Override public Mono<Action> action(UUID id){return one(Action.class,Criteria.where("catalogScope").is(InsuranceSandboxFacts.SCOPE).and("actionId").is(id));}
    @Override public Mono<Void> insertLocale(LocaleVersion row){return insertOne(row);}
    @Override public Mono<Void> insertRights(RightsVersion row){return insertOne(row);}
    @Override public Mono<Void> insertSource(SourceVersion row){return insertOne(row);}
    @Override public Mono<Void> insertAction(Action row){return insertOne(row);}
    @Override public Mono<Void> insertHead(Type type,UUID id,Head h){return insertOne(switch(type){
        case LOCALE->new LocaleControl(id,InsuranceSandboxFacts.SCOPE,h.versionId(),h.version(),h.generation(),h.digest(),h.lifecycle());
        case RIGHTS->new RightsControl(id,InsuranceSandboxFacts.SCOPE,h.versionId(),h.version(),h.generation(),h.digest(),h.lifecycle());
        case SOURCE->new SourceControl(id,InsuranceSandboxFacts.SCOPE,h.versionId(),h.version(),h.generation(),h.digest(),h.lifecycle());});}
    @Override public Mono<Void> compareAndSetHead(Type type,UUID id,Head before,Head next){
        if(before.generation()==Long.MAX_VALUE||next.generation()!=before.generation()+1)return Mono.error(invalid());
        boolean withdraw=before.lifecycle().equals("ACTIVE")&&next.lifecycle().equals("WITHDRAWN")&&before.versionId().equals(next.versionId())&&before.version()==next.version()&&before.digest().equals(next.digest());
        boolean register=before.version()!=Long.MAX_VALUE&&next.generation()!=Long.MAX_VALUE&&next.lifecycle().equals("ACTIVE")&&next.version()==before.version()+1&&!next.versionId().equals(before.versionId());
        if(!withdraw&&!register)return Mono.error(invalid());
        Class<?> row=switch(type){case LOCALE->LocaleControl.class;case RIGHTS->RightsControl.class;case SOURCE->SourceControl.class;};
        var query=Query.query(scope(id).and("versionId").is(before.versionId()).and("version").is(before.version()).and("generation").is(before.generation()).and("digest").is(before.digest()).and("lifecycle").is(before.lifecycle()));
        return exact(operations.update(row).matching(query).apply(Update.update("versionId",next.versionId()).set("version",next.version()).set("generation",next.generation()).set("digest",next.digest()).set("lifecycle",next.lifecycle())));
    }
    private Mono<Void> insertOne(Object row){return Mono.defer(()->{
        if(row==null||!Set.of(LocaleVersion.class,RightsVersion.class,SourceVersion.class,LocaleControl.class,RightsControl.class,SourceControl.class,Action.class).contains(row.getClass()))return Mono.error(invalid());
        var entity=operations.getConverter().getMappingContext().getRequiredPersistentEntity(row.getClass());
        if(entity.hasVersionProperty()||entity.getPropertyAccessor(row).getProperty(entity.getRequiredIdProperty())==null)return Mono.error(invalid());
        var strategy=operations.getDataAccessStrategy();var spec=StatementMapper.InsertSpec.create(entity.getQualifiedTableName());
        for(var entry:strategy.getOutboundRow(row).entrySet())spec=spec.withColumn(entry.getKey(),entry.getValue());
        // Closed assigned-ID rows use real counts; no generic template callback/filter equivalence is claimed.
        return exact(operations.getDatabaseClient().sql(strategy.getStatementMapper().getMappedObject(spec)).fetch().rowsUpdated());
    });}
    private static Mono<Void> exact(Mono<Long> count){return count.switchIfEmpty(Mono.error(invalid())).flatMap(n->n==1?Mono.empty():Mono.error(n==0?new OptimisticLockingFailureException("Sandbox fact changed"):invalid()));}
    private static DataIntegrityViolationException invalid(){return new DataIntegrityViolationException("Exact sandbox fact persistence required");}
}
