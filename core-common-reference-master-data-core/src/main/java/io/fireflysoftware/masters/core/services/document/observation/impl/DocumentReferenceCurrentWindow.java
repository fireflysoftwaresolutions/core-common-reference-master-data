/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation.impl;

import java.time.*;
import java.util.*;
import java.util.function.LongSupplier;
import io.fireflysoftware.masters.core.services.document.observation.*;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.Target;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;

/** Retained native window; DB floor milliseconds are charged one full quantum. */
public record DocumentReferenceCurrentWindow(DocumentReferenceCurrentNativeContext.Stamp stamp,
        R2dbcOperationBudget budget,Instant until) {
    public Instant recordedAt(){return stamp.recordedAt();}
    public static DocumentReferenceCurrentWindow admit(DocumentReferenceCurrentService.OriginalBudget original,
            DocumentReferenceCurrentNativeContext.Stamp stamp,UUID nonce,Target target,Instant beforeWall,long beforeNanos,Clock clock,LongSupplier nanos){
        if(stamp==null||!nonce.equals(stamp.nonce())||!target.equals(stamp.target())||stamp.recordedAt()==null||stamp.until()==null
                ||stamp.recordedAt().toEpochMilli()!=stamp.dbNowMillis()||!stamp.until().isAfter(stamp.recordedAt())
                ||stamp.until().isAfter(original.until()))throw unavailable();
        long millis=Math.subtractExact(Math.subtractExact(stamp.until().toEpochMilli(),stamp.dbNowMillis()),1L);
        if(millis<=0||millis>4999)throw unavailable();
        Instant until=Collections.min(List.of(original.until(),stamp.until(),beforeWall.plusMillis(millis)));
        var retained=original.budget().intersect(R2dbcOperationBudget.observed(beforeWall,until,beforeNanos,Duration.ofSeconds(5),clock,nanos));
        retained.remaining();return new DocumentReferenceCurrentWindow(stamp,retained,until);
    }
    private static DocumentReferenceCurrentFailure unavailable(){return new DocumentReferenceCurrentFailure(DocumentReferenceCurrentFailure.Kind.UNAVAILABLE);}
}
