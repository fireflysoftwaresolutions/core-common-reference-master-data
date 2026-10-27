/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.models.repositories.currency.policy.impl;

import io.fireflysoftware.masters.models.repositories.currency.policy.CurrencyPolicyRepository;
import io.fireflysoftware.masters.models.repositories.currency.policy.*;
import io.fireflysoftware.masters.models.entities.currency.policy.CurrencyPolicyRows.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.v1.CurrencyDTO;
import io.fireflysoftware.masters.interfaces.enums.commons.v1.StatusEnum;
import java.util.*;
import org.springframework.stereotype.Repository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.core.StatementMapper;
import org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory;
import org.springframework.data.relational.core.query.*;
import reactor.core.publisher.*;

@Repository("currencyPolicyRepository")
public class CurrencyPolicyRepositoryImpl implements CurrencyPolicyRepository {
    private final R2dbcEntityTemplate operations;
    private final CurrencyPolicyTechnicalRepository technical;
    private final CurrencyPolicyObservationRepository observations;

    public CurrencyPolicyRepositoryImpl(R2dbcEntityTemplate operations) {
        this.operations=Objects.requireNonNull(operations);
        var factory=new R2dbcRepositoryFactory(operations);
        this.technical=factory.getRepository(CurrencyPolicyTechnicalRepository.class);
        this.observations=factory.getRepository(CurrencyPolicyObservationRepository.class);
    }
    @Override public boolean usesOperations(R2dbcEntityTemplate value) { return operations==value; }
    @Override public Mono<Long> now() { return technical.now(); }
    @Override public Mono<Void> actionGate(UUID action) { return gate("MASTERS_CURRENCY_ACTION:"+action); }
    @Override public Mono<Void> gates(UUID currency,String iso) {
        var keys=new TreeSet<String>();keys.add("CURRENCY:"+currency);keys.add("ISO:"+iso);
        return Flux.fromIterable(keys).concatMap(key->gate("MASTERS_CURRENCY:"+key)).then();
    }
    private Mono<Void> activeTransaction() {
        return org.springframework.transaction.reactive.TransactionSynchronizationManager.forCurrentTransaction()
                .filter(org.springframework.transaction.reactive.TransactionSynchronizationManager::isActualTransactionActive)
                .switchIfEmpty(Mono.error(new IllegalStateException("Native currency policy transaction required")))
                 .then();
    }
    private Mono<Void> gate(String key) {
        return activeTransaction().then(technical.gate(key)).then();
    }
    private static Criteria currency(UUID id) { return Criteria.where("currencyId").is(Objects.requireNonNull(id)); }
    private <T> Mono<T> one(Class<T> type,Criteria criteria) { return operations.select(type).matching(Query.query(criteria)).one(); }
    @Override public Mono<CurrencyDTO> currency(UUID id,boolean lock) {
        return Mono.defer(()->(lock?activeTransaction().then(technical.lockSource(id)).then():Mono.<Void>empty())
                .then(one(Source.class,currency(id))).map(row->CurrencyDTO.builder().currencyId(row.currencyId())
                .isoCode(row.isoCode()).currencyName(row.currencyName()).symbol(row.symbol())
                .decimalPrecision(row.decimalPrecision()).isMajor(row.isMajor()).status(StatusEnum.valueOf(row.status()))
                .dateCreated(row.dateCreated()).dateUpdated(row.dateUpdated()).build()));
    }
    @Override public Mono<Binding> binding(UUID id) {
        return Mono.defer(()->one(Identity.class,currency(id)).map(row->new Binding(row.currencyId(),row.isoCode(),row.headVersion())));
    }
    @Override public Mono<Void> ensureBinding(UUID id,String iso) {
        // The existing sorted currency/ISO gates serialize this check and insert.
        return Mono.defer(()->operations.select(Identity.class).matching(Query.query(Criteria.where("currencyId").is(id).or("isoCode").is(iso))).first()
                .map(row->true).switchIfEmpty(insertOne(new Identity(iso,id,0L)).thenReturn(true))).then();
    }
    @Override public Mono<Long> advance(UUID id,long expected) {
        return Mono.defer(()->expected==Long.MAX_VALUE?Mono.just(0L):operations.update(Identity.class)
                .matching(Query.query(currency(id).and("headVersion").is(expected).and("headVersion").lessThan(Long.MAX_VALUE)))
                .apply(Update.update("headVersion",Math.addExact(expected,1L))));
    }
    @Override public Mono<Action> action(UUID action,UUID id) {
        return Mono.defer(()->one(io.fireflysoftware.masters.models.entities.currency.policy.CurrencyPolicyRows.Action.class,
                currency(id).and("actionId").is(action)).map(row->new Action(row.actionId(),row.currencyId(),row.policyId(),row.kind(),row.canonicalIntent(),row.intentDigest())));
    }
    @Override public Mono<Long> claim(Action value) {
        return insertCount(new io.fireflysoftware.masters.models.entities.currency.policy.CurrencyPolicyRows.Action(
                value.actionId(),value.currencyId(),value.policyId(),value.kind(),value.canonical(),value.digest()))
                // The owner immediately rejects zero and rolls back; it never continues this failed transaction.
                .onErrorResume(org.springframework.dao.DuplicateKeyException.class,error->Mono.just(0L));
    }
    @Override public Mono<CurrencyPolicyView> find(UUID policy,UUID id) {
        return observations.findByPolicyIdAndCurrencyId(policy,id).map(row->new CurrencyPolicyView(
                new CurrencyPolicyReference(row.policyId().toString(),row.currencyId().toString(),row.isoCode(),row.version(),row.definitionDigest(),row.encodingVersion()),
                row.canonicalEncoding(),row.sourceEncoding(),row.sourceDigest(),row.postedScale(),row.effectiveFrom(),row.effectiveTo(),row.provenanceReference(),
                row.publicationActionId().toString(),row.publishedAt(),row.lifecycle(),row.stateVersion(),row.observedAt(),
                row.lifecycle().equals("PUBLISHED")&&row.observedAt()>=row.effectiveFrom()&&(row.effectiveTo()==null||row.observedAt()<row.effectiveTo())));
    }
    @Override public Mono<Void> insert(UUID policy,CurrencyPolicyPublishRequest r,long version,String encoding,String digest,String source,String sourceDigest,long now,String actor,String issuer) {
        return insertOne(new Publication(policy,UUID.fromString(r.currencyId()),r.isoCode(),version,"MASTERS_CURRENCY_POLICY_V1",encoding,digest,source,sourceDigest,r.postedScale(),r.effectiveFromEpochMillis(),r.effectiveToEpochMillis(),r.provenanceReference(),UUID.fromString(r.actionId()),now,actor,issuer))
                .then(insertOne(new Control(policy,UUID.fromString(r.currencyId()),"PUBLISHED",0L,null,null,null,null))).then();
    }
    @Override public Mono<Long> retire(UUID policy,UUID id,UUID action,long now,String actor,String reason) {
        return operations.update(Control.class).matching(Query.query(currency(id).and("policyId").is(policy).and("lifecycle").is("PUBLISHED").and("stateVersion").is(0L)))
                .apply(Update.update("lifecycle","RETIRED").set("stateVersion",1L).set("retiredActionId",action).set("retiredAt",now).set("retiredBy",actor).set("reason",reason));
    }
    @Override public Mono<Void> event(UUID policy,UUID id,UUID action,String kind,long now,String actor,String issuer,String payload) {
        UUID event=UUID.randomUUID();
        return insertOne(new Audit(event,id,policy,action,kind,now,actor,issuer))
                .then(insertOne(new Outbox(event,id,policy,action,payload,now))).then();
    }
    private Mono<Long> insertCount(Object row) {
        return Mono.defer(() -> {
            var allowed=Set.of(Identity.class,Publication.class,Control.class,
                    io.fireflysoftware.masters.models.entities.currency.policy.CurrencyPolicyRows.Action.class,
                    Audit.class,Outbox.class);
            if(row==null||!allowed.contains(row.getClass()))
                return Mono.error(new IllegalArgumentException("Closed currency row required"));
            var entity=operations.getConverter().getMappingContext().getRequiredPersistentEntity(row.getClass());
            if(entity.hasVersionProperty()||entity.getPropertyAccessor(row)
                    .getProperty(entity.getRequiredIdProperty())==null)
                return Mono.error(new IllegalArgumentException("Assigned currency row identity required"));
            var strategy=operations.getDataAccessStrategy();
            var insert=StatementMapper.InsertSpec.create(entity.getQualifiedTableName());
            for(var entry:strategy.getOutboundRow(row).entrySet())
                insert=insert.withColumn(entry.getKey(),entry.getValue());
            var operation=strategy.getStatementMapper().getMappedObject(insert);
            // Legacy C1 inserts used the bound client without template callbacks or its statement filter.
            return operations.getDatabaseClient().sql(operation).fetch().rowsUpdated()
                    .switchIfEmpty(Mono.error(new DataIntegrityViolationException("Currency insert count missing")));
        });
    }

    private Mono<Void> insertOne(Object row) {
        return insertCount(row).flatMap(count->count==1?Mono.<Void>empty():
                Mono.error(new DataIntegrityViolationException("Exact currency insert count required")));
    }

}
