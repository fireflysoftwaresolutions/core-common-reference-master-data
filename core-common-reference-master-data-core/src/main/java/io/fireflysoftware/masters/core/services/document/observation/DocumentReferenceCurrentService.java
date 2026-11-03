/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;
import java.time.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.*;
import reactor.core.publisher.Mono;

public interface DocumentReferenceCurrentService {
    record OriginalBudget(R2dbcOperationBudget budget,Instant until){public OriginalBudget{java.util.Objects.requireNonNull(budget);java.util.Objects.requireNonNull(until);}@Override public String toString(){return "DocumentReferenceOriginalBudget";}}
    record Observation(Current value,R2dbcOperationBudget budget,Instant absoluteUntil){@Override public String toString(){return "DocumentReferenceFiniteCurrent";}}
    default Mono<Observation> observeCurrent(Target target){return Mono.error(new DocumentReferenceCurrentFailure(DocumentReferenceCurrentFailure.Kind.UNAVAILABLE));}
    Mono<Observation> observeCurrent(Target target,OriginalBudget original);
}
