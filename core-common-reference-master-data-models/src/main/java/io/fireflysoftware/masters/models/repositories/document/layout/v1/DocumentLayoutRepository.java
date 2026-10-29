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

package io.fireflysoftware.masters.models.repositories.document.layout.v1;

import io.fireflysoftware.masters.models.entities.document.layout.v1.DocumentLayoutPublication;
import io.r2dbc.spi.Row;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.*;

import java.util.*;

@Repository
public class DocumentLayoutRepository {
    private final DatabaseClient database;

    public DocumentLayoutRepository(DatabaseClient database) {
        this.database = database;
    }

    public record Action(
            UUID actionId,
            UUID layoutRevisionId,
            String kind,
            String requestCanonical,
            String intentDigest) {}

    public Mono<Action> findAction(UUID id) {
        return database.sql(
                        "SELECT action_id,layout_revision_id,kind,request_canonical,intent_digest"
                                + " FROM document_layout_action WHERE action_id=:id")
                .bind("id", id)
                .map(
                        (r, m) ->
                                new Action(
                                        r.get("action_id", UUID.class),
                                        r.get("layout_revision_id", UUID.class),
                                        r.get("kind", String.class),
                                        r.get("request_canonical", String.class),
                                        r.get("intent_digest", String.class)))
                .one();
    }

    public Mono<DocumentLayoutPublication> find(UUID id) {
        return database.sql(
                        "SELECT * FROM document_layout_publication WHERE layout_revision_id=:id")
                .bind("id", id)
                .map((r, m) -> map(r))
                .one();
    }

    public Mono<DocumentLayoutPublication> lock(UUID id) {
        return database.sql(
                        "SELECT * FROM document_layout_publication WHERE layout_revision_id=:id FOR"
                                + " UPDATE")
                .bind("id", id)
                .map((r, m) -> map(r))
                .one();
    }

    public Mono<DocumentLayoutPublication> findExact(String code, String version, String locale) {
        return database.sql(
                        "SELECT * FROM document_layout_publication WHERE template_code=:code AND"
                                + " version=:version AND locale_code=:locale")
                .bind("code", code)
                .bind("version", version)
                .bind("locale", locale)
                .map((r, m) -> map(r))
                .one();
    }

    public Mono<Void> actionGate(UUID action) {
        return inTransaction().then(gate("MASTERS_LAYOUT_ACTION:" + action));
    }

    private Mono<Void> gate(String key) {
        return database.sql("SELECT pg_advisory_xact_lock(hashtextextended(:key,0))")
                .bind("key", key)
                .map((r, m) -> true)
                .one()
                .then();
    }

    public Mono<Void> lockTemplates(Collection<UUID> templates) {
        var ids = templates.stream().distinct().sorted().toList();
        return inTransaction()
                .thenMany(
                        Flux.fromIterable(ids)
                                .concatMap(id -> gate("MASTERS_LAYOUT_TEMPLATE:" + id)))
                .thenMany(
                        Flux.fromIterable(ids)
                                .concatMap(
                                        id ->
                                                database.sql(
                                                                "SELECT template_id FROM"
                                                                    + " document_template_catalog"
                                                                    + " WHERE template_id=:id FOR"
                                                                    + " UPDATE")
                                                        .bind("id", id)
                                                        .map((r, m) -> true)
                                                        .one()
                                                        .switchIfEmpty(
                                                                Mono.error(
                                                                        new OptimisticLockingFailureException(
                                                                                "Layout source"
                                                                                    + " template"
                                                                                    + " was not"
                                                                                    + " found")))))
                .then();
    }

    public Mono<Boolean> lockUsableRevision(UUID id) {
        return database.sql(
                        "SELECT r.revision_id FROM document_template_revision r JOIN"
                            + " document_template_catalog c ON c.template_id=r.template_id AND"
                            + " c.template_code=r.template_code JOIN document_template_localization"
                            + " l ON l.localization_id=r.localization_id AND"
                            + " l.template_id=r.template_id AND l.locale_id=r.locale_id JOIN"
                            + " language_locale loc ON loc.locale_id=r.locale_id AND"
                            + " loc.locale_code=r.locale_code JOIN document_template_type_catalog t"
                            + " ON t.type_id=c.type_id WHERE r.revision_id=:id AND"
                            + " r.lifecycle='PUBLISHED' AND c.status='ACTIVE' AND l.status='ACTIVE'"
                            + " AND loc.status='ACTIVE' AND t.status='ACTIVE' FOR SHARE OF"
                            + " r,c,l,loc,t")
                .bind("id", id)
                .map((r, m) -> true)
                .one()
                .defaultIfEmpty(false);
    }

    public Mono<DocumentLayoutPublication> insert(DocumentLayoutPublication r) {
        return database.sql(
                        "INSERT INTO"
                            + " document_layout_publication(layout_revision_id,source_revision_id,template_id,template_code,version,locale_code,canonical_json,definition_digest,publication_intent_canonical,publication_intent_digest,publication_action_id,published_at_epoch_millis,published_by,lifecycle,state_version)"
                            + " VALUES(:id,:source,:template,:code,:version,:locale,:canonical,:digest,:intent,:intentDigest,:action,:at,:actor,'PUBLISHED',0)")
                .bind("id", r.layoutRevisionId())
                .bind("source", r.sourceRevisionId())
                .bind("template", r.templateId())
                .bind("code", r.templateCode())
                .bind("version", r.version())
                .bind("locale", r.localeCode())
                .bind("canonical", r.canonicalJson())
                .bind("digest", r.definitionDigest())
                .bind("intent", r.publicationIntentCanonical())
                .bind("intentDigest", r.publicationIntentDigest())
                .bind("action", r.publicationActionId())
                .bind("at", r.publishedAtEpochMillis())
                .bind("actor", r.publishedBy())
                .fetch()
                .rowsUpdated()
                .then(find(r.layoutRevisionId()));
    }

    public Mono<DocumentLayoutPublication> retire(
            UUID id, UUID action, String reason, String actor, long at) {
        return database.sql(
                        "UPDATE document_layout_publication SET"
                            + " lifecycle='RETIRED',state_version=1,retirement_action_id=:action,retired_at_epoch_millis=:at,retired_by=:actor,retirement_reason=:reason"
                            + " WHERE layout_revision_id=:id AND lifecycle='PUBLISHED' AND"
                            + " state_version=0")
                .bind("action", action)
                .bind("at", at)
                .bind("actor", actor)
                .bind("reason", reason)
                .bind("id", id)
                .fetch()
                .rowsUpdated()
                .flatMap(
                        n ->
                                n == 1
                                        ? find(id)
                                        : Mono.error(
                                                new OptimisticLockingFailureException(
                                                        "Layout publication is not in its expected"
                                                                + " state")));
    }

    private static Mono<Void> inTransaction() {
        return org.springframework.transaction.reactive.TransactionSynchronizationManager
                .forCurrentTransaction()
                .filter(
                        org.springframework.transaction.reactive.TransactionSynchronizationManager
                                ::isActualTransactionActive)
                .switchIfEmpty(
                        Mono.error(
                                new IllegalStateException(
                                        "Layout locks require a local transaction")))
                .then();
    }

    private static DocumentLayoutPublication map(Row r) {
        return new DocumentLayoutPublication(
                r.get("layout_revision_id", UUID.class),
                r.get("source_revision_id", UUID.class),
                r.get("template_id", UUID.class),
                r.get("template_code", String.class),
                r.get("version", String.class),
                r.get("locale_code", String.class),
                r.get("canonical_json", String.class),
                r.get("definition_digest", String.class),
                r.get("publication_intent_canonical", String.class),
                r.get("publication_intent_digest", String.class),
                r.get("publication_action_id", UUID.class),
                r.get("published_at_epoch_millis", Long.class),
                r.get("published_by", String.class),
                r.get("lifecycle", String.class),
                r.get("state_version", Integer.class),
                r.get("retirement_action_id", UUID.class),
                r.get("retired_at_epoch_millis", Long.class),
                r.get("retired_by", String.class),
                r.get("retirement_reason", String.class));
    }
}
