/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.Target;
import reactor.core.publisher.Mono;

@FunctionalInterface
public interface DocumentReferenceCurrentAuthorization {Mono<SecurityPrincipal> authorize(Target target);}
