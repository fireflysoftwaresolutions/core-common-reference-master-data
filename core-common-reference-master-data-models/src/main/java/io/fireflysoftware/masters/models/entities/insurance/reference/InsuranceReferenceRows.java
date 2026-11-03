package io.fireflysoftware.masters.models.entities.insurance.reference;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;

/** Persisted shapes only; these rows are neither approvals nor authenticated owner observations. */
public final class InsuranceReferenceRows {
    private InsuranceReferenceRows() {}
    @Table("insurance_reference_identity")
    public record Identity(
            @Column("catalog_scope") String catalogScope,
            @Id @Column("record_id") UUID recordId,
            @Column("kind") Kind kind,
            @Column("code") String code) {
        public Identity {scope(catalogScope);id(recordId);Objects.requireNonNull(kind);text(code,100);}
        @Override public String toString() {return "InsuranceReferenceIdentity";}
    }
    @Table("insurance_reference_revision")
    public record Revision(
            @Column("catalog_scope") String catalogScope,
            @Id @Column("publication_id") UUID publicationId,
            @Column("record_id") UUID recordId,
            @Column("kind") Kind kind,
            @Column("revision") long revision,
            @Column("canonical_definition") String canonicalDefinition,
            @Column("definition_digest") String definitionDigest,
            @Column("effective_from") Instant effectiveFrom,
            @Column("effective_until") Instant effectiveUntil,
            @Column("publication_action_id") UUID publicationActionId,
            @ReadOnlyProperty @Column("recorded_at") Instant recordedAt) {
        public Revision {scope(catalogScope);id(recordId);Objects.requireNonNull(kind);id(publicationId);positive(revision);text(canonicalDefinition,131072);digest(definitionDigest);time(effectiveFrom);if(effectiveUntil!=null){time(effectiveUntil);if(!effectiveUntil.isAfter(effectiveFrom))throw invalid();}id(publicationActionId);optionalTime(recordedAt);}
        @Override public String toString() {return "InsuranceReferenceRevision";}
    }
    @Table("insurance_reference_control")
    public record ReferenceControl(
            @Column("catalog_scope") String catalogScope,
            @Id @Column("record_id") UUID recordId,
            @Column("kind") Kind kind,
            @Column("publication_id") UUID publicationId,
            @Column("revision") long revision,
            @Column("generation") long generation,
            @Column("lifecycle") String lifecycle) {
        public ReferenceControl {scope(catalogScope);id(recordId);Objects.requireNonNull(kind);id(publicationId);positive(revision);InsuranceReferenceRows.generation(generation);if(!java.util.Set.of("ACTIVE","WITHDRAWN").contains(lifecycle))throw invalid();}
        @Override public String toString() {return "InsuranceReferenceReferenceControl";}
    }
    @Table("insurance_reference_maker_intent")
    public record MakerIntent(
            @Column("catalog_scope") String catalogScope,
            @Id @Column("intent_id") UUID intentId,
            @Column("record_id") UUID recordId,
            @Column("kind") Kind kind,
            @Column("operation") String operation,
            @Column("canonical_intent") String canonicalIntent,
            @Column("intent_digest") String intentDigest,
            @Column("expected_publication_id") UUID expectedPublicationId,
            @Column("expected_revision") long expectedRevision,
            @Column("expected_generation") long expectedGeneration,
            @Column("maker_issuer") String makerIssuer,
            @Column("maker_subject") String makerSubject,
            @Column("valid_until") Instant validUntil,
            @ReadOnlyProperty @Column("recorded_at") Instant recordedAt) {
        public MakerIntent {scope(catalogScope);id(recordId);Objects.requireNonNull(kind);id(intentId);InsuranceReferenceRows.operation(operation);text(canonicalIntent,131072);digest(intentDigest);if(expectedPublicationId!=null)id(expectedPublicationId);generation(expectedRevision);generation(expectedGeneration);if((expectedPublicationId==null)!=(expectedRevision==0)||expectedPublicationId==null&&expectedGeneration!=0)throw invalid();text(makerIssuer,500);text(makerSubject,255);time(validUntil);optionalTime(recordedAt);}
        @Override public String toString() {return "InsuranceReferenceMakerIntent";}
    }
    @Table("insurance_reference_intent_control")
    public record IntentControl(
            @Column("catalog_scope") String catalogScope,
            @Id @Column("intent_id") UUID intentId,
            @Column("record_id") UUID recordId,
            @Column("kind") Kind kind,
            @Column("generation") long generation,
            @Column("lifecycle") String lifecycle,
            @Column("approval_id") UUID approvalId,
            @Column("execution_action_id") UUID executionActionId) {
        public IntentControl {scope(catalogScope);id(recordId);Objects.requireNonNull(kind);id(intentId);InsuranceReferenceRows.generation(generation);if(!java.util.Set.of("OPEN","APPROVED","WITHDRAWN","EXECUTED").contains(lifecycle))throw invalid();if(approvalId!=null)id(approvalId);if(executionActionId!=null)id(executionActionId);if(lifecycle.equals("OPEN")&&(approvalId!=null||executionActionId!=null))throw invalid();if(lifecycle.equals("APPROVED")&&(approvalId==null||executionActionId!=null))throw invalid();if(lifecycle.equals("EXECUTED")&&(approvalId==null||executionActionId==null))throw invalid();if(!lifecycle.equals("EXECUTED")&&executionActionId!=null)throw invalid();}
        @Override public String toString() {return "InsuranceReferenceIntentControl";}
    }
    @Table("insurance_reference_checker_approval")
    public record CheckerApproval(
            @Column("catalog_scope") String catalogScope,
            @Id @Column("approval_id") UUID approvalId,
            @Column("record_id") UUID recordId,
            @Column("kind") Kind kind,
            @Column("intent_id") UUID intentId,
            @Column("intent_digest") String intentDigest,
            @Column("expected_intent_generation") long expectedIntentGeneration,
            @Column("checker_issuer") String checkerIssuer,
            @Column("checker_subject") String checkerSubject,
            @Column("valid_until") Instant validUntil,
            @ReadOnlyProperty @Column("recorded_at") Instant recordedAt) {
        public CheckerApproval {scope(catalogScope);id(recordId);Objects.requireNonNull(kind);id(approvalId);id(intentId);digest(intentDigest);generation(expectedIntentGeneration);text(checkerIssuer,500);text(checkerSubject,255);time(validUntil);optionalTime(recordedAt);}
        @Override public String toString() {return "InsuranceReferenceCheckerApproval";}
    }
    @Table("insurance_reference_action")
    public record Action(
            @Column("catalog_scope") String catalogScope,
            @Id @Column("action_id") UUID actionId,
            @Column("record_id") UUID recordId,
            @Column("kind") Kind kind,
            @Column("intent_id") UUID intentId,
            @Column("operation") String operation,
            @Column("intent_digest") String intentDigest,
            @Column("publication_id") UUID publicationId,
            @Column("revision") long revision,
            @Column("result_generation") long resultGeneration,
            @ReadOnlyProperty @Column("recorded_at") Instant recordedAt) {
        public Action {scope(catalogScope);id(recordId);Objects.requireNonNull(kind);id(actionId);id(intentId);text(operation,40);digest(intentDigest);if(publicationId!=null)id(publicationId);generation(revision);generation(resultGeneration);optionalTime(recordedAt);}
        @Override public String toString() {return "InsuranceReferenceAction";}
    }
    @Table("insurance_reference_audit")
    public record Audit(
            @Column("catalog_scope") String catalogScope,
            @Id @Column("event_id") UUID eventId,
            @Column("record_id") UUID recordId,
            @Column("kind") Kind kind,
            @Column("action_id") UUID actionId,
            @Column("event_kind") String eventKind,
            @Column("actor_issuer") String actorIssuer,
            @Column("actor_subject") String actorSubject,
            @ReadOnlyProperty @Column("recorded_at") Instant recordedAt) {
        public Audit {scope(catalogScope);id(recordId);Objects.requireNonNull(kind);id(eventId);id(actionId);text(eventKind,40);text(actorIssuer,500);text(actorSubject,255);optionalTime(recordedAt);}
        @Override public String toString() {return "InsuranceReferenceAudit";}
    }
    @Table("insurance_reference_outbox")
    public record Outbox(
            @Column("catalog_scope") String catalogScope,
            @Id @Column("event_id") UUID eventId,
            @Column("record_id") UUID recordId,
            @Column("kind") Kind kind,
            @Column("action_id") UUID actionId,
            @Column("event_kind") String eventKind,
            @ReadOnlyProperty @Column("recorded_at") Instant recordedAt) {
        public Outbox {scope(catalogScope);id(recordId);Objects.requireNonNull(kind);id(eventId);id(actionId);text(eventKind,40);optionalTime(recordedAt);}
        @Override public String toString() {return "InsuranceReferenceOutbox";}
    }
    private static void scope(String scope){if(!InsuranceReferenceContracts.SCOPE.equals(scope))throw invalid();}
    private static void id(UUID value){if(value==null||value.equals(new UUID(0,0)))throw invalid();}
    private static void text(String value,int max){if(value==null||value.isBlank()||value.length()>max||value.codePoints().anyMatch(c->c==0))throw invalid();}
    private static void digest(String value){if(value==null||!value.matches("[0-9a-f]{64}"))throw invalid();}
    private static void positive(long value){if(value<1)throw invalid();}
    private static void generation(long value){if(value<0)throw invalid();}
    private static void time(Instant value){InsuranceReferenceContracts.requireTimestamp(value);}
    private static void optionalTime(Instant value){if(value!=null)time(value);}
    private static void operation(String operation){if(!java.util.Set.of("PUBLISH","WITHDRAW").contains(operation))throw invalid();}
    private static IllegalArgumentException invalid(){return new IllegalArgumentException("Invalid insurance reference row");}
}
