package io.fireflysoftware.masters.core.services.document.v1;

import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.spi.PolicyDecisionPort;
import org.fireflyframework.security.spi.SecurityContextPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.function.Supplier;

/** One trusted policy boundary protects immutable publications and the legacy document draft APIs. */
@Component
public class DocumentTemplateAccessGuard {
    private final ObjectProvider<SecurityContextPort> contexts;
    private final ObjectProvider<PolicyDecisionPort> policies;

    public DocumentTemplateAccessGuard(ObjectProvider<SecurityContextPort> contexts, ObjectProvider<PolicyDecisionPort> policies) {
        this.contexts = contexts;
        this.policies = policies;
    }

    public Mono<SecurityPrincipal> authorize(String operation, String resource) {
        return authorize(operation, resource, Map.of());
    }

    public Mono<SecurityPrincipal> authorize(String operation, String resource, Map<String, Object> safeContext) {
        return Mono.defer(() -> {
            var values = new java.util.HashMap<String, Object>(safeContext);
            values.put("scope", "GLOBAL_MASTER");
            var policyContext = Map.copyOf(values);
            var context = contexts.getIfAvailable();
            var policy = policies.getIfAvailable();
            if (context == null || policy == null) return Mono.error(forbidden());
            return context.currentPrincipal()
                    .filter(principal -> principal.subject() != null && !principal.subject().isBlank() && principal.subject().length() <= 255)
                    .flatMap(principal -> policy.authorize(principal, "masters.document-template." + operation, resource,
                                    policyContext)
                            .filter(decision -> decision.granted() && decision.obligations().isEmpty())
                            .map(decision -> principal))
                    .switchIfEmpty(Mono.error(forbidden()));
        }).onErrorMap(error -> forbidden());
    }

    public <T> Mono<T> mono(String operation, String resource, Supplier<Mono<T>> work) {
        return authorize(operation, resource).then(Mono.defer(work)).onErrorMap(this::protectedState);
    }

    public <T> Flux<T> flux(String operation, String resource, Supplier<Flux<T>> work) {
        return authorize(operation, resource).thenMany(Flux.defer(work)).onErrorMap(this::protectedState);
    }

    private Throwable protectedState(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof DataIntegrityViolationException) {
                return new ResponseStatusException(HttpStatus.CONFLICT, "Document template integrity constraint rejected the operation");
            }
        }
        return error;
    }

    private static ResponseStatusException forbidden() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "Trusted document template permission is required");
    }
}
