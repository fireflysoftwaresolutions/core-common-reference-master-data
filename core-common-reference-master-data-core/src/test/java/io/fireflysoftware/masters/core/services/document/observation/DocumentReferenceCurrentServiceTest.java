/* Copyright 2026 Firefly Software Foundation. Licensed under the Apache License, Version 2.0. */
package io.fireflysoftware.masters.core.services.document.observation;

import io.fireflysoftware.masters.core.services.document.observation.impl.DocumentReferenceCurrentServiceImpl;
import io.fireflysoftware.masters.core.services.document.layout.v1.*;
import io.fireflysoftware.masters.interfaces.dtos.document.layout.v1.*;
import io.fireflysoftware.masters.interfaces.dtos.document.observation.DocumentReferenceCurrentContracts.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class DocumentReferenceCurrentServiceTest {
    final Clock clock=Clock.fixed(Instant.parse("2026-10-28T12:00:00Z"),ZoneOffset.UTC);
    final AtomicLong nanos=new AtomicLong();
    final java.util.function.LongSupplier time=nanos::get;
    final UUID tenant=UUID.fromString("11111111-1111-4111-8111-111111111111");
    final SecurityPrincipal actor=SecurityPrincipal.builder().issuer("https://issuer.example").subject("reader").tenantId(tenant.toString()).claims(Map.of("exp",clock.instant().plusSeconds(60))).build();
    final Reader reader=new Reader(); final Authority authority=new Authority(); boolean withdrawn;
    DocumentLayoutOutputProfileMetadata profile(){return new DocumentLayoutOutputProfileMetadata(new DocumentLayoutOutputProfileReference("SYNTHETIC_A4","1","text/html","2".repeat(64)),"AUTHORED_PAGES","A4","PORTRAIT",List.of("document-page"),16,262144);}
    DocumentLayoutMasterReference source(){return new DocumentLayoutMasterReference("22222222-2222-4222-8222-222222222222","33333333-3333-4333-8333-333333333333","DEPOSIT_AGREEMENT","draft.01","en-US","text/html","MASTERS_JSON_V1","a".repeat(64),"b".repeat(64),"c".repeat(64),DocumentLayoutRules.EMPTY_STYLE_DIGEST);}
    DocumentLayoutDefinition definition(){var n=new DocumentLayoutMasterReference("44444444-4444-4444-8444-444444444444","55555555-5555-4555-8555-555555555555","NOTICE_GENERAL","notice.1","en-US","text/html","MASTERS_JSON_V1","d".repeat(64),"e".repeat(64),"f".repeat(64),DocumentLayoutRules.EMPTY_STYLE_DIGEST);return new DocumentLayoutDefinition(source(),profile(),new DocumentLayoutSemanticDefinition("AUTHORED_PAGES","A4","PORTRAIT",List.of(new DocumentLayoutNoticeReference(DocumentLayoutNoticeReference.Role.GENERAL_DISCLOSURE,DocumentLayoutNoticeReference.Placement.FOOTER,n))));}
    Target target(){var scope=new ProductScope(tenant,UUID.fromString("66666666-6666-4666-8666-666666666666"),UUID.fromString("77777777-7777-4777-8777-777777777777"),UUID.fromString("88888888-8888-4888-8888-888888888888"),UUID.fromString("99999999-9999-4999-8999-999999999999"),"1".repeat(64));return new Target(scope,"DEPOSIT_ACCOUNT_ENROLLMENT","DEPOSIT_AGREEMENT","en-US",new ExactReference("core-common-reference-master-data","DOCUMENT_TEMPLATE",UUID.fromString(source().revisionId()),"OPAQUE",source().version(),source().sourceDigest()),new ExactReference("core-common-reference-master-data","DOCUMENT_LAYOUT",UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),"OPAQUE",source().version(),DocumentLayoutRules.canonical(definition()).digest()));}
    DocumentReferenceCurrentService service(){return new DocumentReferenceCurrentServiceImpl(reader,authority,r->{if(withdrawn)throw new IllegalStateException();assertEquals(profile().reference(),r);return profile();},clock,time);}
    DocumentReferenceCurrentService.OriginalBudget original(){var now=clock.instant();return new DocumentReferenceCurrentService.OriginalBudget(R2dbcOperationBudget.observed(now,now.plusSeconds(3),nanos.get(),Duration.ofSeconds(3),clock,time),now.plusSeconds(3));}
    @Test void exactOpaqueReferencesRetainOriginalBudgetAndTwoPermissions(){var result=service().observeCurrent(target(),original()).block();assertEquals(target(),result.value().target());assertEquals(2,authority.calls);assertEquals(1,reader.calls);assertEquals(Duration.ofSeconds(3),result.budget().remaining());assertEquals(clock.millis(),result.value().observedAtEpochMillis());assertEquals(clock.millis()+3000,result.value().effectiveUntilEpochMillis());}
    @Test void differentProductPublicationCannotReuseNativeTarget(){reader.wrongTarget=true;assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());}
    @Test void exhaustedOriginalBudgetBeforePermissionNeverReads(){var b=original();nanos.set(Duration.ofSeconds(3).toNanos());assertThrows(RuntimeException.class,()->service().observeCurrent(target(),b).block());assertEquals(0,reader.calls);assertEquals(0,authority.calls);}
    @Test void lateFirstPermissionNeverReceivesNewBudget(){authority.first=()->nanos.set(Duration.ofSeconds(3).toNanos());assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());assertEquals(0,reader.calls);}
    @Test void lateFinalPermissionWithholdsCurrent(){authority.last=()->nanos.set(Duration.ofSeconds(3).toNanos());assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());}
    @Test void lateNativeReadWithholdsCurrent(){reader.after=()->nanos.set(Duration.ofSeconds(3).toNanos());assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());}
    @Test void finalPermissionWithdrawalWithholdsCurrent(){authority.denied=true;assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());}
    @Test void changedPrincipalAtFinalPermissionWithholdsCurrent(){authority.changed=true;assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());}
    @Test void wrongTenantNeverReachesNativeRead(){authority.wrongTenant=true;assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());assertEquals(0,reader.calls);}
    @Test void expiredPrincipalNeverReachesNativeRead(){authority.expired=true;assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());assertEquals(0,reader.calls);}
    @Test void registryWithdrawalWithholdsCurrent(){reader.after=()->withdrawn=true;assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());}
    @Test void nativeEmptyNeverBecomesCurrent(){reader.empty=true;assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());}
    @Test void nativeFutureObservationRefuses(){reader.future=true;assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());}
    @Test void nativeWindowBeyondOriginalRefuses(){reader.wide=true;assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());}
    @Test void wrongStoredCanonicalBytesRefuse(){reader.corrupt=true;assertThrows(RuntimeException.class,()->service().observeCurrent(target(),original()).block());}
    @Test void foreignOwnerAndNumericVersionFormatRefuse(){var x=target().template();assertThrows(IllegalArgumentException.class,()->new ExactReference("masters",x.kind(),x.id(),x.versionFormat(),x.versionText(),x.digest()));assertThrows(IllegalArgumentException.class,()->new ExactReference(x.owner(),x.kind(),x.id(),"NUMERIC","1",x.digest()));}
    @Test void legacyPurposeAndMissingLocaleRefuse(){var t=target();assertThrows(IllegalArgumentException.class,()->new Target(t.product(),"DEPOSIT_ACCOUNT_ORIGINATION",t.documentType(),t.locale(),t.template(),t.layout()));assertThrows(IllegalArgumentException.class,()->new Target(t.product(),t.purpose(),t.documentType(),null,t.template(),t.layout()));}
    @Test void differentOwnerVersionsRefuse(){var t=target();var l=t.layout();assertThrows(IllegalArgumentException.class,()->new Target(t.product(),t.purpose(),t.documentType(),t.locale(),t.template(),new ExactReference(l.owner(),l.kind(),l.id(),l.versionFormat(),"different",l.digest())));}
    final class Authority implements DocumentReferenceCurrentAuthorization {int calls;boolean denied,changed,wrongTenant,expired;Runnable first=()->{},last=()->{};public Mono<SecurityPrincipal> authorize(Target target){return Mono.defer(()->{calls++;if(calls==1)first.run();else last.run();if(calls==2&&denied)return Mono.empty();if(wrongTenant||expired||calls==2&&changed)return Mono.just(SecurityPrincipal.builder().issuer(actor.issuer()).subject(changed?"other":"reader").tenantId(wrongTenant?"other":tenant.toString()).claims(Map.of("exp",expired?clock.instant():clock.instant().plusSeconds(60))).build());return Mono.just(actor);});}}
    final class Reader implements DocumentReferenceCurrentReader {int calls;boolean wrongTarget,empty,future,wide,corrupt;Runnable after=()->{};public Mono<Snapshot> read(Target t,SecurityPrincipal a,DocumentReferenceCurrentService.OriginalBudget b){return Mono.defer(()->{calls++;if(empty)return Mono.empty();var d=definition();var c=DocumentLayoutRules.canonical(d);Target captured=t;if(wrongTarget){var p=t.product();captured=new Target(new ProductScope(p.tenantId(),p.rootId(),p.productId(),p.revisionId(),UUID.randomUUID(),p.definitionDigest()),t.purpose(),t.documentType(),t.locale(),t.template(),t.layout());}var result=new Snapshot(captured,d,corrupt?c.json()+" ":c.json(),clock.instant().plusMillis(future?1:0),wide?b.until().plusSeconds(1):b.until(),b.budget());after.run();return Mono.just(result);});}}
}
