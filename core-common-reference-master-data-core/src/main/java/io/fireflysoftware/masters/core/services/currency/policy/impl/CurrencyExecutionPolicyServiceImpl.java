/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.masters.core.services.currency.policy.impl;

import io.fireflysoftware.masters.core.services.currency.policy.*;

import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.v1.CurrencyDTO;
import io.fireflysoftware.masters.interfaces.enums.commons.v1.StatusEnum;
import io.fireflysoftware.masters.models.repositories.currency.policy.CurrencyPolicyRepository;

import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Supplier;

@Service("currencyExecutionPolicyService")
public class CurrencyExecutionPolicyServiceImpl implements CurrencyExecutionPolicyService {
    private final CurrencyPolicyRepository repository;
    private final CurrencyPolicyDatabaseContext context;
    private final TransactionalOperator transactions;
    private final CurrencyPolicyAuthority authority;

    public CurrencyExecutionPolicyServiceImpl(
            CurrencyPolicyRepository repository,
            CurrencyPolicyDatabaseContext context,
            ReactiveTransactionManager manager,
            CurrencyPolicyAuthority authority,
            org.springframework.data.r2dbc.core.R2dbcEntityTemplate operations) {
        if (!(manager instanceof org.springframework.r2dbc.connection.R2dbcTransactionManager nativeManager)
                || nativeManager.getConnectionFactory() != operations.getDatabaseClient().getConnectionFactory()
                || !repository.usesOperations(operations) || !context.usesOperations(operations))
            throw new IllegalArgumentException("One original currency operations/factory/transaction graph required");
        this.repository = repository;
        this.context = context;
        this.transactions = TransactionalOperator.create(manager);
        this.authority = authority;
    }

    private record Operation(
            SecurityPrincipal principal,
            UUID currency,
            UUID action,
            CurrencyPolicyRules.Canonical intent) {}

    private record Prepared(
            CurrencyPolicyPublishRequest request,
            CurrencyPolicyRules.Canonical source,
            CurrencyPolicyRules.Canonical definition,
            long version,
            UUID policy) {}

    public Mono<CurrencyPolicySourceObservation> observe(UUID currency) {
        return boundary(
                () ->
                        authority
                                .authorize("source.read", currency.toString(), Map.of())
                                .flatMap(
                                        p ->
                                                run(
                                                        p,
                                                        currency,
                                                        "READ",
                                                        () -> sourceObservation(currency))));
    }

    private Mono<CurrencyPolicySourceObservation> sourceObservation(UUID currency) {
        return repository
                .currency(currency, false)
                .switchIfEmpty(Mono.error(notFound()))
                .flatMap(
                        source ->
                                repository
                                        .gates(currency, source.getIsoCode())
                                        .then(repository.currency(currency, true))
                                        .switchIfEmpty(Mono.error(notFound())))
                .flatMap(
                        current -> {
                            var canonical = CurrencyPolicyRules.source(current);
                            return repository
                                    .binding(currency)
                                    .map(Optional::of)
                                    .defaultIfEmpty(Optional.empty())
                                    .zipWith(repository.now())
                                    .map(
                                            pair ->
                                                    new CurrencyPolicySourceObservation(
                                                            current,
                                                            CurrencyPolicyRules.SOURCE_ENCODING,
                                                            canonical.encoding(),
                                                            canonical.digest(),
                                                            pair.getT2(),
                                                            pair.getT1()
                                                                    .map(
                                                                            b ->
                                                                                    b.currencyId()
                                                                                            .toString())
                                                                    .orElse(null),
                                                            pair.getT1()
                                                                    .map(
                                                                            CurrencyPolicyRepository
                                                                                            .Binding
                                                                                    ::headVersion)
                                                                    .orElse(0L)));
                        });
    }

    public Mono<PublicationOutcome> publish(CurrencyPolicyPublishRequest request) {
        return boundary(
                () -> {
                    var intent = CurrencyPolicyRules.publicationIntent(request);
                    var currency = CurrencyPolicyRules.uuid(request.currencyId());
                    var action = CurrencyPolicyRules.uuid(request.actionId());
                    var safe =
                            Map.<String, Object>of(
                                    "isoCode",
                                    request.isoCode(),
                                    "expectedCurrencyDigest",
                                    request.expectedCurrencyDigest(),
                                    "expectedHeadVersion",
                                    request.expectedHeadVersion(),
                                    "intentDigest",
                                    intent.digest());
                    return authority
                            .authorize("publish", request.currencyId(), safe)
                            .flatMap(
                                    p ->
                                            run(
                                                    p,
                                                    currency,
                                                    "PUBLISH",
                                                    () ->
                                                            publishAuthorized(
                                                                    request,
                                                                    new Operation(
                                                                            p, currency, action,
                                                                            intent))));
                });
    }

    private Mono<PublicationOutcome> publishAuthorized(
            CurrencyPolicyPublishRequest request, Operation op) {
        return repository
                .actionGate(op.action())
                .then(repository.action(op.action(), op.currency()))
                .flatMap(existing -> replay(existing, op).map(v -> new PublicationOutcome(v, true)))
                .switchIfEmpty(Mono.defer(() -> prepare(request, op).flatMap(p -> persist(p, op))));
    }

    private Mono<Prepared> prepare(CurrencyPolicyPublishRequest request, Operation op) {
        return repository
                .gates(op.currency(), request.isoCode())
                .then(repository.currency(op.currency(), true))
                .switchIfEmpty(Mono.error(notFound()))
                .flatMap(
                        source -> {
                            var canonical = CurrencyPolicyRules.source(source);
                            requireSource(request, source, canonical);
                            return repository
                                    .ensureBinding(op.currency(), request.isoCode())
                                    .then(repository.binding(op.currency()))
                                    .switchIfEmpty(
                                            Mono.error(
                                                    CurrencyPolicyRules.conflict(
                                                            "ISO currency identity already bound")))
                                    .map(
                                            binding -> {
                                                if (!binding.isoCode().equals(request.isoCode())
                                                        || binding.headVersion()
                                                                != request.expectedHeadVersion()) {
                                                    throw CurrencyPolicyRules.conflict(
                                                            "Exact current currency publication"
                                                                    + " head required");
                                                }
                                                long version =
                                                        CurrencyPolicyRules.nextVersion(
                                                                binding.headVersion());
                                                return new Prepared(
                                                        request,
                                                        canonical,
                                                        CurrencyPolicyRules.definition(
                                                                request, canonical, version),
                                                        version,
                                                        UUID.randomUUID());
                                            });
                        });
    }

    private static void requireSource(
            CurrencyPolicyPublishRequest request,
            CurrencyDTO source,
            CurrencyPolicyRules.Canonical canonical) {
        if (!request.isoCode().equals(source.getIsoCode())
                || !canonical.digest().equals(request.expectedCurrencyDigest())
                || source.getStatus() != StatusEnum.ACTIVE
                || source.getDecimalPrecision() == null
                || !source.getDecimalPrecision().equals(request.postedScale())) {
            throw CurrencyPolicyRules.conflict(
                    "Exact active currency source and explicit precision required");
        }
    }

    private Mono<PublicationOutcome> persist(Prepared prepared, Operation op) {
        var r = prepared.request();
        var p = op.principal();
        var reference =
                new CurrencyPolicyReference(
                        prepared.policy().toString(),
                        op.currency().toString(),
                        r.isoCode(),
                        prepared.version(),
                        prepared.definition().digest(),
                        CurrencyPolicyRules.POLICY_ENCODING);
        return claim(op, prepared.policy(), "PUBLISH")
                .then(
                        requireOne(
                                repository.advance(op.currency(), r.expectedHeadVersion()),
                                "Currency publication head changed"))
                .then(repository.now())
                .flatMap(
                        now ->
                                repository
                                        .insert(
                                                prepared.policy(),
                                                r,
                                                prepared.version(),
                                                prepared.definition().encoding(),
                                                prepared.definition().digest(),
                                                prepared.source().encoding(),
                                                prepared.source().digest(),
                                                now,
                                                p.subject(),
                                                p.issuer())
                                        .then(
                                                repository.event(
                                                        prepared.policy(),
                                                        op.currency(),
                                                        op.action(),
                                                        "PUBLISH",
                                                        now,
                                                        p.subject(),
                                                        p.issuer(),
                                                        CurrencyPolicyRules.event(
                                                                reference,
                                                                "PUBLISH",
                                                                op.action().toString(),
                                                                op.intent(),
                                                                now))))
                .then(required(prepared.policy(), op.currency()))
                .map(v -> new PublicationOutcome(v, false));
    }

    public Mono<CurrencyPolicyView> get(UUID policy, UUID currency) {
        return boundary(
                () ->
                        authority
                                .authorize(
                                        "read",
                                        currency.toString(),
                                        Map.of("policyId", policy.toString()))
                                .flatMap(
                                        p ->
                                                run(
                                                        p,
                                                        currency,
                                                        "READ",
                                                        () -> required(policy, currency))));
    }

    public Mono<CurrencyPolicyView> byAction(UUID action, UUID currency) {
        return boundary(
                () ->
                        authority
                                .authorize(
                                        "read",
                                        currency.toString(),
                                        Map.of("actionId", action.toString()))
                                .flatMap(
                                        p ->
                                                run(
                                                        p,
                                                        currency,
                                                        "READ",
                                                        () ->
                                                                repository
                                                                        .action(action, currency)
                                                                        .switchIfEmpty(
                                                                                Mono.error(
                                                                                        notFound()))
                                                                        .flatMap(
                                                                                a ->
                                                                                        required(
                                                                                                a
                                                                                                        .policyId(),
                                                                                                currency)))));
    }

    public Mono<CurrencyPolicyView> retire(UUID policy, CurrencyPolicyRetireRequest request) {
        return boundary(
                () -> {
                    var intent = CurrencyPolicyRules.retirementIntent(policy.toString(), request);
                    var currency = CurrencyPolicyRules.uuid(request.currencyId());
                    var action = CurrencyPolicyRules.uuid(request.actionId());
                    return authority
                            .authorize(
                                    "retire",
                                    currency.toString(),
                                    Map.of(
                                            "policyId",
                                            policy.toString(),
                                            "intentDigest",
                                            intent.digest()))
                            .flatMap(
                                    p ->
                                            run(
                                                    p,
                                                    currency,
                                                    "RETIRE",
                                                    () ->
                                                            retireAuthorized(
                                                                    policy,
                                                                    request,
                                                                    new Operation(
                                                                            p, currency, action,
                                                                            intent))));
                });
    }

    private Mono<CurrencyPolicyView> retireAuthorized(
            UUID policy, CurrencyPolicyRetireRequest request, Operation op) {
        return repository
                .actionGate(op.action())
                .then(repository.action(op.action(), op.currency()))
                .flatMap(existing -> replay(existing, op))
                .switchIfEmpty(
                        Mono.defer(
                                () ->
                                        required(policy, op.currency())
                                                .flatMap(
                                                        view ->
                                                                repository.gates(
                                                                        op.currency(),
                                                                        view.reference().isoCode()))
                                                .then(required(policy, op.currency()))
                                                .flatMap(
                                                        current ->
                                                                persistRetirement(
                                                                        policy, request, current,
                                                                        op))));
    }

    private Mono<CurrencyPolicyView> persistRetirement(
            UUID policy,
            CurrencyPolicyRetireRequest request,
            CurrencyPolicyView current,
            Operation op) {
        if (!current.lifecycle().equals("PUBLISHED") || current.stateVersion() != 0) {
            return Mono.error(CurrencyPolicyRules.conflict("Currency policy already retired"));
        }
        var p = op.principal();
        return claim(op, policy, "RETIRE")
                .then(repository.now())
                .flatMap(
                        now ->
                                requireOne(
                                                repository.retire(
                                                        policy,
                                                        op.currency(),
                                                        op.action(),
                                                        now,
                                                        p.subject(),
                                                        request.reason()),
                                                "Currency policy control changed")
                                        .then(
                                                repository.event(
                                                        policy,
                                                        op.currency(),
                                                        op.action(),
                                                        "RETIRE",
                                                        now,
                                                        p.subject(),
                                                        p.issuer(),
                                                        CurrencyPolicyRules.event(
                                                                current.reference(),
                                                                "RETIRE",
                                                                op.action().toString(),
                                                                op.intent(),
                                                                now))))
                .then(required(policy, op.currency()));
    }

    private Mono<Void> claim(Operation op, UUID policy, String kind) {
        return requireOne(
                repository.claim(
                        new CurrencyPolicyRepository.Action(
                                op.action(),
                                op.currency(),
                                policy,
                                kind,
                                op.intent().encoding(),
                                op.intent().digest())),
                "Currency action already belongs to another immutable intent");
    }

    private static Mono<Void> requireOne(Mono<Long> rows, String reason) {
        return rows.filter(n -> n == 1)
                .switchIfEmpty(Mono.error(CurrencyPolicyRules.conflict(reason)))
                .then();
    }

    private Mono<CurrencyPolicyView> replay(
            CurrencyPolicyRepository.Action existing, Operation op) {
        if (!existing.canonical().equals(op.intent().encoding())
                || !existing.digest().equals(op.intent().digest())) {
            return Mono.error(
                    CurrencyPolicyRules.conflict("Currency action immutable intent differs"));
        }
        return required(existing.policyId(), op.currency());
    }

    private Mono<CurrencyPolicyView> required(UUID policy, UUID currency) {
        return repository.find(policy, currency).switchIfEmpty(Mono.error(notFound()));
    }

    private <T> Mono<T> boundary(Supplier<Mono<T>> work) {
        return org.springframework.transaction.reactive.TransactionSynchronizationManager
                .forCurrentTransaction()
                .map(
                        org.springframework.transaction.reactive.TransactionSynchronizationManager
                                ::isActualTransactionActive)
                .onErrorResume(
                        org.springframework.transaction.NoTransactionException.class,
                        error -> Mono.just(false))
                .flatMap(
                        active ->
                                active
                                        ? Mono.error(
                                                new ResponseStatusException(
                                                        HttpStatus.CONFLICT,
                                                        "Currency policy operation requires its own"
                                                            + " transaction boundary"))
                                        : Mono.defer(work));
    }

    private <T> Mono<T> run(
            SecurityPrincipal p, UUID currency, String purpose, Supplier<Mono<T>> work) {
        return transactions
                .transactional(
                        context.establish(p, currency, purpose)
                                .then(Mono.defer(work))
                                .map(
                                        value -> {
                                            CurrencyPolicyAuthority.requireCurrent(p);
                                            return value;
                                        }))
                .onErrorMap(this::mapped);
    }

    private Throwable mapped(Throwable error) {
        Throwable cause = error;
        for (int depth = 0; cause != null && depth < 8; depth++, cause = cause.getCause()) {
            if (cause instanceof DataIntegrityViolationException) {
                return CurrencyPolicyRules.conflict(
                        "Currency policy integrity constraint rejected operation");
            }
        }
        return error;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Exact currency policy resource not found");
    }
}
