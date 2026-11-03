/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;

import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.masters.core.services.document.observation.impl.DocumentReferenceCurrentAuthorizationImpl;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.Target;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.fireflyframework.security.api.domain.Decision;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class DocumentReferenceCardPurposeTest {
    final DocumentReferenceCurrentServiceTest source = new DocumentReferenceCurrentServiceTest();

    Target card() {
        var original = source.target();
        return new Target(original.product(), "CARD_ENROLLMENT", original.documentType(),
                original.locale(), original.template(), original.layout());
    }

    @Test void cardPurposeIsExplicitAndPreservesTheCompleteOwnerTuple() {
        var card = card();
        var original = source.target();
        assertNotEquals(original, card);
        assertEquals(original.product(), card.product());
        assertEquals(original.template(), card.template());
        assertEquals(original.layout(), card.layout());
        var result = source.service().observeCurrent(card, source.original()).block();
        assertEquals(card, result.value().target());
        assertEquals(2, source.authority.calls);
        assertEquals(1, source.reader.calls);
    }

    @Test void aDepositOrInsuranceOnlyPolicyCannotAdmitCardPurpose() {
        var calls = new AtomicInteger();
        var authority = new DocumentReferenceCurrentAuthorizationImpl(() -> Mono.just(source.actor),
                (actor, action, resource, context) -> {
                    calls.incrementAndGet();
                    assertEquals("CARD_ENROLLMENT", context.get("purpose"));
                    assertEquals(card().product().definitionDigest(), context.get("definitionDigest"));
                    return Mono.just(Set.of("DEPOSIT_ACCOUNT_ENROLLMENT", "INSURANCE_TERM_PC_QUOTE")
                            .contains(context.get("purpose")) ? Decision.permit() : Decision.deny("purpose"));
                }, Set.of(source.actor.issuer()), source.clock);
        assertThrows(RuntimeException.class, () -> authority.authorize(card()).block());
        assertEquals(1, calls.get());
    }

    @Test void currentResponseRevocationCannotReturnCardEvidence() {
        source.authority.denied = true;
        assertThrows(RuntimeException.class, () -> source.service().observeCurrent(card(), source.original()).block());
        assertEquals(2, source.authority.calls);
        assertEquals(1, source.reader.calls);
    }

    @Test void cardPurposeCannotHideWrongPublicationOrExpiredObservation() {
        source.reader.wrongTarget = true;
        assertThrows(RuntimeException.class, () -> source.service().observeCurrent(card(), source.original()).block());
        source.reader.wrongTarget = false;
        source.reader.wide = true;
        assertThrows(RuntimeException.class, () -> source.service().observeCurrent(card(), source.original()).block());
    }

    @Test void cardActivationShippingAndSignatureAreNotEnrollmentPurposes() {
        var selected = card();
        for (var purpose : List.of("CARD_ACTIVATION", "CARD_SHIPPING", "CARD_CONTRACT_SIGNATURE"))
            assertThrows(IllegalArgumentException.class, () -> new Target(selected.product(), purpose,
                    selected.documentType(), selected.locale(), selected.template(), selected.layout()));
    }
}
