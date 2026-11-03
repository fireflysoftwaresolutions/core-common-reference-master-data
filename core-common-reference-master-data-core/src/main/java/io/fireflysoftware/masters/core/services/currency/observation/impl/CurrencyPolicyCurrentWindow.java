package io.fireflysoftware.masters.core.services.currency.observation.impl;

import java.time.*;
import java.util.*;
import java.util.function.LongSupplier;
import io.fireflysoftware.masters.core.services.currency.observation.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;

/** Retained native window; DB floor milliseconds are charged one full quantum. */
public record CurrencyPolicyCurrentWindow(CurrencyPolicyCurrentNativeContext.Stamp stamp,
        R2dbcOperationBudget budget,Instant until) {
    public Instant recordedAt(){return stamp.recordedAt();}
    public static CurrencyPolicyCurrentWindow admit(CurrencyPolicyCurrentObservationService.OriginalBudget original,
            CurrencyPolicyCurrentNativeContext.Stamp stamp,UUID nonce,Target target,Instant beforeWall,long beforeNanos,Clock clock,LongSupplier nanos){
        if(stamp==null||!nonce.equals(stamp.nonce())||!target.equals(stamp.target())||stamp.recordedAt()==null||stamp.effectiveUntil()==null
                ||stamp.recordedAt().toEpochMilli()!=stamp.dbNowMillis()||!stamp.effectiveUntil().isAfter(stamp.recordedAt())
                ||stamp.effectiveUntil().isAfter(original.until()))throw unavailable();
        long millis=Math.subtractExact(Math.subtractExact(stamp.effectiveUntil().toEpochMilli(),stamp.dbNowMillis()),1L);
        if(millis<=0||millis>4999)throw unavailable();
        Instant until=Collections.min(List.of(original.until(),stamp.effectiveUntil(),beforeWall.plusMillis(millis)));
        var retained=original.budget().intersect(R2dbcOperationBudget.observed(beforeWall,until,beforeNanos,Duration.ofSeconds(5),clock,nanos));
        retained.remaining();return new CurrencyPolicyCurrentWindow(stamp,retained,until);
    }
    private static CurrencyPolicyCurrentFailure unavailable(){return new CurrencyPolicyCurrentFailure(CurrencyPolicyCurrentFailure.Kind.UNAVAILABLE);}
}
