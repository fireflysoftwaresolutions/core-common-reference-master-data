/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.models.entities.currency.policy;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

public final class CurrencyPolicyRows {
    private CurrencyPolicyRows() {}
    @Table(value="currency_policy_identity",schema="public")
    public record Identity(
            @Id String isoCode,
            UUID currencyId,
            Long headVersion) {
        @Override public String toString() { return "CurrencyPolicyIdentityRow"; }
    }

    @Table(value="currency_execution_policy",schema="public")
    public record Publication(
            @Id UUID policyId,
            UUID currencyId,
            String isoCode,
            Long version,
            String encodingVersion,
            String canonicalEncoding,
            String definitionDigest,
            String sourceEncoding,
            String sourceDigest,
            Integer postedScale,
            Long effectiveFrom,
            Long effectiveTo,
            String provenanceReference,
            UUID publicationActionId,
            Long publishedAt,
            String publishedBy,
            String publishedIssuer) {
        @Override public String toString() { return "CurrencyPolicyPublicationRow"; }
    }

    @Table(value="currency_policy_control",schema="public")
    public record Control(
            @Id UUID policyId,
            UUID currencyId,
            String lifecycle,
            Long stateVersion,
            UUID retiredActionId,
            Long retiredAt,
            String retiredBy,
            String reason) {
        @Override public String toString() { return "CurrencyPolicyControlRow"; }
    }

    @Table(value="currency_policy_action",schema="public")
    public record Action(
            @Id UUID actionId,
            UUID currencyId,
            UUID policyId,
            String kind,
            String canonicalIntent,
            String intentDigest) {
        @Override public String toString() { return "CurrencyPolicyActionRow"; }
    }

    @Table(value="currency_policy_audit",schema="public")
    public record Audit(
            @Id UUID eventId,
            UUID currencyId,
            UUID policyId,
            UUID actionId,
            String kind,
            Long observedAt,
            String actor,
            String issuer) {
        @Override public String toString() { return "CurrencyPolicyAuditRow"; }
    }

    @Table(value="currency_policy_outbox",schema="public")
    public record Outbox(
            @Id UUID eventId,
            UUID currencyId,
            UUID policyId,
            UUID actionId,
            String payload,
            Long observedAt) {
        @Override public String toString() { return "CurrencyPolicyOutboxRow"; }
    }

    @Table(value="currency_policy_observation",schema="public")
    public record Observation(
            @Id UUID policyId,
            UUID currencyId,
            String isoCode,
            Long version,
            String encodingVersion,
            String canonicalEncoding,
            String definitionDigest,
            String sourceEncoding,
            String sourceDigest,
            Integer postedScale,
            Long effectiveFrom,
            Long effectiveTo,
            String provenanceReference,
            UUID publicationActionId,
            Long publishedAt,
            String lifecycle,
            Long stateVersion,
            Long observedAt) {
        @Override public String toString() { return "CurrencyPolicyObservationRow"; }
    }

    @Table(value="currencies",schema="public")
    public record Source(
            @Id UUID currencyId,
            String isoCode,
            String currencyName,
            String symbol,
            Integer decimalPrecision,
            Boolean isMajor,
            String status,
            java.time.LocalDateTime dateCreated,
            java.time.LocalDateTime dateUpdated) {
        @Override public String toString() { return "CurrencyPolicySourceRow"; }
    }

}
