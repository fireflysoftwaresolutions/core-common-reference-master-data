/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import io.fireflysoftware.masters.core.services.document.observation.impl.DocumentReferenceCurrentAuthorizationImpl;
import org.fireflyframework.security.api.domain.*;
import org.fireflyframework.security.spi.*;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

class DocumentReferenceCurrentAuthorizationTest {
    final DocumentReferenceCurrentServiceTest source=new DocumentReferenceCurrentServiceTest();
    DocumentReferenceCurrentAuthorization service(SecurityContextPort contexts,PolicyDecisionPort policies){return new DocumentReferenceCurrentAuthorizationImpl(contexts,policies,Set.of(source.actor.issuer()),source.clock);}
    @Test void actualPolicyActionResourceAndCompleteOriginalIntentAreBound(){var target=source.target();var calls=new AtomicInteger();var value=service(()->Mono.just(source.actor),(actor,action,resource,context)->{calls.incrementAndGet();assertSame(source.actor,actor);assertEquals("masters.document-template.current",action);assertEquals("document-layout-revision:"+target.layout().id(),resource);assertEquals("GLOBAL_MASTER",context.get("scope"));assertEquals(target.product().tenantId().toString(),context.get("tenantId"));assertEquals(target.product().rootId().toString(),context.get("rootId"));assertEquals(target.product().productId().toString(),context.get("productId"));assertEquals(target.product().revisionId().toString(),context.get("revisionId"));assertEquals(target.product().publicationId().toString(),context.get("publicationId"));assertEquals(target.product().definitionDigest(),context.get("definitionDigest"));assertEquals(target.purpose(),context.get("purpose"));assertEquals(target.documentType(),context.get("documentType"));var intent=(Map<?,?>)context.get("target");assertEquals(target.locale(),intent.get("locale"));assertEquals(Map.of("id",target.template().id().toString(),"version",target.template().versionText(),"digest",target.template().digest()),intent.get("template"));assertEquals(Map.of("id",target.layout().id().toString(),"version",target.layout().versionText(),"digest",target.layout().digest()),intent.get("layout"));return Mono.just(Decision.permit());}).authorize(target).block();assertSame(source.actor,value);assertEquals(1,calls.get());}
    @Test void foreignIssuerTenantAndExpiredPrincipalRefuseBeforePolicy(){var calls=new AtomicInteger();for(var actor:List.of(source.actor.toBuilder().issuer("https://foreign.example").build(),source.actor.toBuilder().tenantId(UUID.randomUUID().toString()).build(),source.actor.toBuilder().claims(Map.of("exp",source.clock.instant())).build())){assertThrows(RuntimeException.class,()->service(()->Mono.just(actor),(p,a,r,c)->{calls.incrementAndGet();return Mono.just(Decision.permit());}).authorize(source.target()).block());}assertEquals(0,calls.get());}
    @Test void deniedIndeterminateEmptyAndFailedPolicyNeverGrant(){for(var decision:List.of(Mono.just(Decision.deny("denied")),Mono.just(Decision.indeterminate("unknown")),Mono.<Decision>empty(),Mono.<Decision>error(new IllegalStateException("policy unavailable"))))assertThrows(RuntimeException.class,()->service(()->Mono.just(source.actor),(p,a,r,c)->decision).authorize(source.target()).block());}
    @Test void unsupportedObligationsNeverBecomeSuccess(){assertThrows(RuntimeException.class,()->service(()->Mono.just(source.actor),(p,a,r,c)->Mono.just(Decision.permit(List.of(Obligation.of("mask",Map.of()))))).authorize(source.target()).block());}
    @Test void absentTrustedPrincipalDoesNotInvokePolicy(){var calls=new AtomicInteger();assertThrows(RuntimeException.class,()->service(Mono::empty,(p,a,r,c)->{calls.incrementAndGet();return Mono.just(Decision.permit());}).authorize(source.target()).block());assertEquals(0,calls.get());}
}
