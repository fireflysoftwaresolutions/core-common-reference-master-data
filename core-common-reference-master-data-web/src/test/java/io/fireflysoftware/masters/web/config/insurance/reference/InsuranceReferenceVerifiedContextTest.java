package io.fireflysoftware.masters.web.config.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.fireflyframework.security.webflux.authentication.FireflyAuthenticationToken;
import org.fireflyframework.security.webflux.context.ReactorSecurityContextAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import reactor.core.publisher.Mono;

/** Context identity unit controls only; constructed tokens do not qualify JWT authentication. */
class InsuranceReferenceVerifiedContextTest {
    @Test void unitContextPreservesTheExactSharedPrincipalObject() {
        var principal = principal("tenant");
        var context = new SecurityContextImpl(new FireflyAuthenticationToken(principal));
        var port = InsuranceReferenceCommandConfiguration.verifiedContext(new ReactorSecurityContextAdapter());
        assertSame(principal, port.currentPrincipal().contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))).block());
    }

    @Test void unitContextNeverRepairsANullTenantFromClaims() {
        var principal = principal(null);
        var context = new SecurityContextImpl(new FireflyAuthenticationToken(principal));
        var port = InsuranceReferenceCommandConfiguration.verifiedContext(new ReactorSecurityContextAdapter());
        assertThrows(IllegalStateException.class, () -> port.currentPrincipal()
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))).block());
        assertNull(principal.tenantId());
    }

    @Test void unitContextRefusesAnAbsentAuthenticationOrDifferentProjectedObject() {
        var principal = principal("tenant");
        assertThrows(IllegalStateException.class, () -> InsuranceReferenceCommandConfiguration
                .verifiedContext(new ReactorSecurityContextAdapter()).currentPrincipal().block());
        var context = new SecurityContextImpl(new FireflyAuthenticationToken(principal));
        var copy = principal.toBuilder().build();
        assertEquals(principal, copy);
        assertNotSame(principal, copy);
        assertThrows(IllegalStateException.class, () -> InsuranceReferenceCommandConfiguration
                .verifiedContext(() -> Mono.just(copy)).currentPrincipal()
                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context))).block());
    }

    private static SecurityPrincipal principal(String tenant) {
        return SecurityPrincipal.builder().subject("source-context-only").issuer("https://issuer.example.test")
                .tenantId(tenant).claims(java.util.Map.of("tenant_id", "claim-must-not-repair-projection")).build();
    }
}
