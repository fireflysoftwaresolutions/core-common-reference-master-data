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
package io.fireflysoftware.masters.core.services.document.v1;

import io.fireflysoftware.masters.core.services.document.layout.v1.*;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;
import io.fireflysoftware.masters.interfaces.dtos.document.v1.DocumentTemplatePublicationRequest;
import io.fireflysoftware.masters.models.entities.document.layout.v1.DocumentLayoutPublication;
import io.fireflysoftware.masters.models.entities.document.v1.DocumentTemplateRevision;
import io.fireflysoftware.masters.models.repositories.document.layout.v1.DocumentLayoutRepository;
import io.fireflysoftware.masters.models.repositories.document.v1.DocumentTemplateRevisionRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.reactive.*;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.*;

import java.time.Clock;
import java.util.*;

/** Authorizes observations before acquiring local publication locks. */
@Service
public class DocumentLayoutPublicationService {
    private final DocumentTemplateRevisionService publications;
    private final DocumentTemplateRevisionRepository masters;
    private final DocumentLayoutRepository layouts;
    private final TransactionalOperator transactions;
    private final DocumentTemplateAccessGuard access;
    private final DocumentLayoutProfileRegistry profiles;
    private final Clock clock;

    public record PublicationOutcome(DocumentLayoutPublicationDTO publication, boolean replayed) {}

    private record Prepared(
            DocumentTemplateRevision source,
            DocumentLayoutOutputProfileMetadata profile,
            String actor) {}

    private static final class ConcurrentAction extends RuntimeException {}

    public DocumentLayoutPublicationService(
            DocumentTemplateRevisionService publications,
            DocumentTemplateRevisionRepository masters,
            DocumentLayoutRepository layouts,
            ReactiveTransactionManager manager,
            DocumentTemplateAccessGuard access,
            DocumentLayoutProfileRegistry profiles,
            @Qualifier("documentLayoutClock") Clock clock) {
        this.publications = publications;
        this.masters = masters;
        this.layouts = layouts;
        this.transactions = TransactionalOperator.create(manager);
        this.access = access;
        this.profiles = profiles;
        this.clock = clock;
    }

    public Mono<DocumentLayoutSourceObservation> observe(UUID templateId, String locale) {
        return outsideTransaction()
                .then(
                        Mono.defer(
                                () -> {
                                    DocumentLayoutRules.uuid(
                                            templateId == null ? null : templateId.toString());
                                    DocumentLayoutRules.identity("OBSERVATION", "1", locale);
                                    return access.authorize(
                                                    "layout-observe", templateResource(templateId))
                                            .then(capture(templateId, locale, "source-observation"))
                                            .flatMap(
                                                    source ->
                                                            access.authorize(
                                                                            "layout-observe",
                                                                            templateResource(
                                                                                    templateId),
                                                                            DocumentLayoutSnapshots
                                                                                    .draftContext(
                                                                                            source,
                                                                                            null,
                                                                                            null))
                                                                    .thenReturn(
                                                                            new DocumentLayoutSourceObservation(
                                                                                    DocumentLayoutSnapshots
                                                                                            .draft(
                                                                                                    source),
                                                                                    DocumentLayoutSnapshots
                                                                                            .sourceCanonical(
                                                                                                    source))));
                                }))
                .onErrorMap(this::integrity);
    }

    public Mono<PublicationOutcome> publish(UUID templateId, DocumentLayoutPublishRequest request) {
        return outsideTransaction()
                .then(
                        Mono.defer(
                                () -> {
                                    var intent =
                                            DocumentLayoutRules.publicationIntent(
                                                    templateId == null
                                                            ? null
                                                            : templateId.toString(),
                                                    request);
                                    return publishAttempt(templateId, request, intent)
                                            .onErrorResume(
                                                    ConcurrentAction.class,
                                                    e ->
                                                            publishAttempt(
                                                                    templateId, request, intent));
                                }))
                .onErrorMap(this::integrity);
    }

    private Mono<PublicationOutcome> publishAttempt(
            UUID templateId,
            DocumentLayoutPublishRequest request,
            DocumentLayoutRules.Canonical intent) {
        UUID action = DocumentLayoutRules.uuid(request.actionId());
        return access.authorize("layout-publish", templateResource(templateId))
                .then(layouts.findAction(action))
                .flatMap(a -> publicationReplay(a, templateId, intent))
                .switchIfEmpty(
                        Mono.defer(
                                () ->
                                        prepare(templateId, request)
                                                .flatMap(
                                                        p ->
                                                                layouts.actionGate(action)
                                                                        .then(
                                                                                noConcurrentAction(
                                                                                        action))
                                                                        .then(
                                                                                layouts
                                                                                        .lockTemplates(
                                                                                                templateIds(
                                                                                                        templateId,
                                                                                                        request
                                                                                                                .semanticDefinition())))
                                                                        .then(
                                                                                masters.lockSource(
                                                                                                templateId,
                                                                                                request.expectedDraftReference()
                                                                                                        .localeCode())
                                                                                        .switchIfEmpty(
                                                                                                Mono
                                                                                                        .error(
                                                                                                                conflict(
                                                                                                                        "Exact"
                                                                                                                            + " localized"
                                                                                                                            + " draft"
                                                                                                                            + " no longer"
                                                                                                                            + " exists"))))
                                                                        .flatMap(
                                                                                json -> {
                                                                                    var locked =
                                                                                            publications
                                                                                                    .snapshot(
                                                                                                            json,
                                                                                                            new DocumentTemplatePublicationRequest(
                                                                                                                    request.expectedDraftReference()
                                                                                                                            .version(),
                                                                                                                    request.expectedDraftReference()
                                                                                                                            .localeCode()),
                                                                                                            p
                                                                                                                    .actor());
                                                                                    DocumentLayoutSnapshots
                                                                                            .source(
                                                                                                    locked);
                                                                                    requireDraft(
                                                                                            locked,
                                                                                            request
                                                                                                    .expectedDraftReference());
                                                                                    if (!DocumentLayoutSnapshots
                                                                                            .draft(
                                                                                                    locked)
                                                                                            .equals(
                                                                                                    DocumentLayoutSnapshots
                                                                                                            .draft(
                                                                                                                    p
                                                                                                                            .source())))
                                                                                        throw conflict(
                                                                                                "Draft"
                                                                                                    + " changed"
                                                                                                    + " after"
                                                                                                    + " authorization");
                                                                                    return notices(
                                                                                                    request
                                                                                                            .semanticDefinition(),
                                                                                                    true)
                                                                                            .then(
                                                                                                    publications
                                                                                                            .publishNewAuthorized(
                                                                                                                    json,
                                                                                                                    new DocumentTemplatePublicationRequest(
                                                                                                                            locked
                                                                                                                                    .version(),
                                                                                                                            locked
                                                                                                                                    .localeCode()),
                                                                                                                    p
                                                                                                                            .actor()));
                                                                                })
                                                                        .flatMap(
                                                                                source -> {
                                                                                    var canonical =
                                                                                            DocumentLayoutRules
                                                                                                    .canonical(
                                                                                                            new DocumentLayoutDefinition(
                                                                                                                    DocumentLayoutSnapshots
                                                                                                                            .reference(
                                                                                                                                    source),
                                                                                                                    p
                                                                                                                            .profile(),
                                                                                                                    request
                                                                                                                            .semanticDefinition()));
                                                                                    long at =
                                                                                            clock
                                                                                                    .millis();
                                                                                    var row =
                                                                                            new DocumentLayoutPublication(
                                                                                                    UUID
                                                                                                            .randomUUID(),
                                                                                                    source
                                                                                                            .revisionId(),
                                                                                                    templateId,
                                                                                                    source
                                                                                                            .templateCode(),
                                                                                                    source
                                                                                                            .version(),
                                                                                                    source
                                                                                                            .localeCode(),
                                                                                                    canonical
                                                                                                            .json(),
                                                                                                    canonical
                                                                                                            .digest(),
                                                                                                    intent
                                                                                                            .json(),
                                                                                                    intent
                                                                                                            .digest(),
                                                                                                    action,
                                                                                                    at,
                                                                                                    p
                                                                                                            .actor(),
                                                                                                    "PUBLISHED",
                                                                                                    0,
                                                                                                    null,
                                                                                                    null,
                                                                                                    null,
                                                                                                    null);
                                                                                    return layouts
                                                                                            .insert(
                                                                                                    row);
                                                                                })
                                                                        .map(
                                                                                r ->
                                                                                        new PublicationOutcome(
                                                                                                DocumentLayoutSnapshots
                                                                                                        .dto(
                                                                                                                r),
                                                                                                false))
                                                                        .as(
                                                                                transactions
                                                                                        ::transactional))));
    }

    private Mono<Prepared> prepare(UUID templateId, DocumentLayoutPublishRequest request) {
        return capture(
                        templateId,
                        request.expectedDraftReference().localeCode(),
                        "source-observation")
                .flatMap(
                        source -> {
                            requireDraft(source, request.expectedDraftReference());
                            var profile = profiles.requireRecognized(request.outputProfile());
                            DocumentLayoutRules.profileMetadata(profile);
                            if (!profile.reference().equals(request.outputProfile()))
                                throw conflict("Registry returned a different output profile");
                            var frozen =
                                    new DocumentLayoutOutputProfileMetadata(
                                            profile.reference(),
                                            profile.sectionMode(),
                                            profile.pageSize(),
                                            profile.orientation(),
                                            List.copyOf(profile.allowedClassTokens()),
                                            profile.maxSections(),
                                            profile.maxContentBytes());
                            return notices(request.semanticDefinition(), false)
                                    .then(
                                            access.authorize(
                                                    "layout-publish",
                                                    templateResource(templateId),
                                                    DocumentLayoutSnapshots.draftContext(
                                                            source,
                                                            frozen,
                                                            request.semanticDefinition())))
                                    .map(
                                            principal ->
                                                    new Prepared(
                                                            source, frozen, principal.subject()));
                        });
    }

    private Mono<PublicationOutcome> publicationReplay(
            DocumentLayoutRepository.Action action,
            UUID template,
            DocumentLayoutRules.Canonical intent) {
        return stored(action.layoutRevisionId())
                .flatMap(
                        row ->
                                access.authorize(
                                                "layout-publish",
                                                templateResource(row.templateId()),
                                                DocumentLayoutSnapshots.context(row))
                                        .then(
                                                Mono.fromSupplier(
                                                        () -> {
                                                            if (!"PUBLISH".equals(action.kind())
                                                                    || !template.equals(
                                                                            row.templateId())
                                                                    || !intent.json()
                                                                            .equals(
                                                                                    action
                                                                                            .requestCanonical())
                                                                    || !intent.digest()
                                                                            .equals(
                                                                                    action
                                                                                            .intentDigest()))
                                                                throw conflict(
                                                                        "Action identifier is"
                                                                            + " already bound to a"
                                                                            + " different intent");
                                                            return new PublicationOutcome(
                                                                    DocumentLayoutSnapshots.dto(
                                                                            row),
                                                                    true);
                                                        })));
    }

    public Mono<DocumentLayoutPublicationDTO> get(UUID id) {
        return outsideTransaction()
                .then(access.authorize("layout-read", layoutResource(id)))
                .then(stored(id))
                .flatMap(
                        row ->
                                access.authorize(
                                                "layout-read",
                                                layoutResource(id),
                                                DocumentLayoutSnapshots.context(row))
                                        .thenReturn(DocumentLayoutSnapshots.dto(row)));
    }

    public Mono<DocumentLayoutPublicationDTO> resolve(
            String code,
            String version,
            String locale,
            DocumentLayoutOutputProfileReference profile) {
        return outsideTransaction()
                .then(
                        Mono.defer(
                                () -> {
                                    DocumentLayoutRules.identity(code, version, locale);
                                    DocumentLayoutRules.profileReference(profile);
                                    return access.authorize(
                                                    "layout-resolve",
                                                    "document-layout:"
                                                            + code
                                                            + ":"
                                                            + version
                                                            + ":"
                                                            + locale)
                                            .then(
                                                    layouts.findExact(code, version, locale)
                                                            .switchIfEmpty(Mono.error(notFound())))
                                            .flatMap(
                                                    row ->
                                                            access.authorize(
                                                                            "layout-resolve",
                                                                            layoutResource(
                                                                                    row
                                                                                            .layoutRevisionId()),
                                                                            DocumentLayoutSnapshots
                                                                                    .context(row))
                                                                    .then(
                                                                            Mono.defer(
                                                                                    () -> {
                                                                                        var
                                                                                                definition =
                                                                                                        DocumentLayoutSnapshots
                                                                                                                .definition(
                                                                                                                        row);
                                                                                        if (!"PUBLISHED"
                                                                                                        .equals(
                                                                                                                row
                                                                                                                        .lifecycle())
                                                                                                || !profile
                                                                                                        .equals(
                                                                                                                definition
                                                                                                                        .profileMetadata()
                                                                                                                        .reference()))
                                                                                            throw conflict(
                                                                                                    "Exact"
                                                                                                        + " layout"
                                                                                                        + " publication"
                                                                                                        + " is unavailable");
                                                                                        var
                                                                                                recognized =
                                                                                                        profiles
                                                                                                                .requireRecognized(
                                                                                                                        profile);
                                                                                        DocumentLayoutRules
                                                                                                .profileMetadata(
                                                                                                        recognized);
                                                                                        if (!definition
                                                                                                .profileMetadata()
                                                                                                .equals(
                                                                                                        recognized))
                                                                                            throw conflict(
                                                                                                    "Recognized"
                                                                                                        + " output"
                                                                                                        + " profile"
                                                                                                        + " metadata"
                                                                                                        + " differs"
                                                                                                        + " from"
                                                                                                        + " the retained"
                                                                                                        + " snapshot");
                                                                                        return masters.isUsable(
                                                                                                        row
                                                                                                                .sourceRevisionId())
                                                                                                .flatMap(
                                                                                                        usable ->
                                                                                                                usable
                                                                                                                        ? notices(
                                                                                                                                        definition
                                                                                                                                                .semanticDefinition(),
                                                                                                                                        false)
                                                                                                                                .thenReturn(
                                                                                                                                        DocumentLayoutSnapshots
                                                                                                                                                .dto(
                                                                                                                                                        row))
                                                                                                                        : Mono
                                                                                                                                .error(
                                                                                                                                        conflict(
                                                                                                                                                "Layout"
                                                                                                                                                    + " source"
                                                                                                                                                    + " is no"
                                                                                                                                                    + " longer"
                                                                                                                                                    + " usable")));
                                                                                    })));
                                }))
                .onErrorMap(this::integrity);
    }

    public Mono<DocumentLayoutPublicationDTO> retire(UUID id, DocumentLayoutRetireRequest request) {
        return outsideTransaction()
                .then(
                        Mono.defer(
                                () -> {
                                    var intent =
                                            DocumentLayoutRules.retirementIntent(
                                                    id == null ? null : id.toString(), request);
                                    return retireAttempt(id, request, intent)
                                            .onErrorResume(
                                                    ConcurrentAction.class,
                                                    e -> retireAttempt(id, request, intent));
                                }))
                .onErrorMap(this::integrity);
    }

    private Mono<DocumentLayoutPublicationDTO> retireAttempt(
            UUID id, DocumentLayoutRetireRequest request, DocumentLayoutRules.Canonical intent) {
        UUID action = DocumentLayoutRules.uuid(request.actionId());
        return access.authorize("layout-retire", layoutResource(id))
                .then(layouts.findAction(action))
                .flatMap(
                        a ->
                                stored(a.layoutRevisionId())
                                        .flatMap(
                                                row ->
                                                        access.authorize(
                                                                        "layout-retire",
                                                                        layoutResource(
                                                                                row
                                                                                        .layoutRevisionId()),
                                                                        DocumentLayoutSnapshots
                                                                                .context(row))
                                                                .then(
                                                                        Mono.fromSupplier(
                                                                                () -> {
                                                                                    if (!"RETIRE"
                                                                                                    .equals(
                                                                                                            a
                                                                                                                    .kind())
                                                                                            || !id
                                                                                                    .equals(
                                                                                                            row
                                                                                                                    .layoutRevisionId())
                                                                                            || !intent.json()
                                                                                                    .equals(
                                                                                                            a
                                                                                                                    .requestCanonical())
                                                                                            || !intent.digest()
                                                                                                    .equals(
                                                                                                            a
                                                                                                                    .intentDigest()))
                                                                                        throw conflict(
                                                                                                "Action"
                                                                                                    + " identifier"
                                                                                                    + " is already"
                                                                                                    + " bound"
                                                                                                    + " to a"
                                                                                                    + " different"
                                                                                                    + " intent");
                                                                                    return DocumentLayoutSnapshots
                                                                                            .dto(
                                                                                                    row);
                                                                                }))))
                .switchIfEmpty(
                        Mono.defer(
                                () ->
                                        stored(id)
                                                .flatMap(
                                                        row ->
                                                                access.authorize(
                                                                                "layout-retire",
                                                                                layoutResource(id),
                                                                                DocumentLayoutSnapshots
                                                                                        .context(
                                                                                                row))
                                                                        .flatMap(
                                                                                principal ->
                                                                                        layouts.actionGate(
                                                                                                        action)
                                                                                                .then(
                                                                                                        noConcurrentAction(
                                                                                                                action))
                                                                                                .then(
                                                                                                        layouts.lock(
                                                                                                                        id)
                                                                                                                .switchIfEmpty(
                                                                                                                        Mono
                                                                                                                                .error(
                                                                                                                                        notFound())))
                                                                                                .flatMap(
                                                                                                        locked ->
                                                                                                                layouts
                                                                                                                        .retire(
                                                                                                                                id,
                                                                                                                                action,
                                                                                                                                request
                                                                                                                                        .reason(),
                                                                                                                                principal
                                                                                                                                        .subject(),
                                                                                                                                Math
                                                                                                                                        .max(
                                                                                                                                                locked
                                                                                                                                                        .publishedAtEpochMillis(),
                                                                                                                                                clock
                                                                                                                                                        .millis())))
                                                                                                .map(
                                                                                                        DocumentLayoutSnapshots
                                                                                                                ::dto)
                                                                                                .as(
                                                                                                        transactions
                                                                                                                ::transactional)))));
    }

    private Mono<DocumentTemplateRevision> capture(UUID id, String locale, String actor) {
        return masters.observeSource(id, locale)
                .switchIfEmpty(Mono.error(notFound()))
                .map(
                        json -> {
                            var version = DocumentLayoutSnapshots.version(json);
                            DocumentLayoutRules.identity("OBSERVATION", version, locale);
                            var source =
                                    publications.snapshot(
                                            json,
                                            new DocumentTemplatePublicationRequest(version, locale),
                                            actor);
                            DocumentLayoutSnapshots.source(source);
                            return source;
                        });
    }

    private Mono<Void> notices(DocumentLayoutSemanticDefinition definition, boolean lock) {
        return Flux.fromIterable(definition.legalNotices())
                .sort(Comparator.comparing(n -> n.masterReference().revisionId()))
                .concatMap(
                        n -> {
                            var id = DocumentLayoutRules.uuid(n.masterReference().revisionId());
                            return (lock ? layouts.lockUsableRevision(id) : masters.isUsable(id))
                                    .flatMap(
                                            usable ->
                                                    usable
                                                            ? masters.findById(id)
                                                                    .switchIfEmpty(
                                                                            Mono.error(
                                                                                    conflict(
                                                                                            "Legal"
                                                                                                + " notice"
                                                                                                + " revision"
                                                                                                + " is unavailable")))
                                                            : Mono.error(
                                                                    conflict(
                                                                            "Legal notice revision"
                                                                                + " is no longer"
                                                                                + " usable")))
                                    .doOnNext(
                                            r ->
                                                    DocumentLayoutSnapshots.notice(
                                                            r, n.masterReference()));
                        })
                .then();
    }

    private static void requireDraft(
            DocumentTemplateRevision source, DocumentLayoutExpectedDraftReference expected) {
        if (!DocumentLayoutSnapshots.draft(source).equals(expected))
            throw conflict("Draft does not match the exact source observation");
    }

    private static Collection<UUID> templateIds(
            UUID main, DocumentLayoutSemanticDefinition semantics) {
        var ids = new ArrayList<UUID>();
        ids.add(main);
        semantics
                .legalNotices()
                .forEach(n -> ids.add(DocumentLayoutRules.uuid(n.masterReference().templateId())));
        return ids;
    }

    private Mono<Void> noConcurrentAction(UUID action) {
        return layouts.findAction(action)
                .flatMap(a -> Mono.<Void>error(new ConcurrentAction()))
                .then();
    }

    private Mono<DocumentLayoutPublication> stored(UUID id) {
        return Mono.defer(
                () -> {
                    DocumentLayoutRules.uuid(id == null ? null : id.toString());
                    return layouts.find(id).switchIfEmpty(Mono.error(notFound()));
                });
    }

    private static Mono<Void> outsideTransaction() {
        return TransactionSynchronizationManager.forCurrentTransaction()
                .flatMap(
                        manager ->
                                manager.isActualTransactionActive()
                                        ? Mono.<Void>error(
                                                conflict(
                                                        "Layout authority must be called outside an"
                                                                + " ambient SQL transaction"))
                                        : Mono.empty())
                .onErrorResume(NoTransactionException.class, e -> Mono.empty());
    }

    private Throwable integrity(Throwable error) {
        return error instanceof DataIntegrityViolationException
                        || error instanceof IncorrectResultSizeDataAccessException
                        || error instanceof OptimisticLockingFailureException
                ? conflict("Layout publication integrity rejected the operation")
                : error;
    }

    private static String templateResource(UUID id) {
        return "document-template:" + id;
    }

    private static String layoutResource(UUID id) {
        return "document-layout-revision:" + id;
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Exact layout source or publication was not found");
    }

    private static ResponseStatusException conflict(String message) {
        return DocumentLayoutRules.conflict(message);
    }
}
