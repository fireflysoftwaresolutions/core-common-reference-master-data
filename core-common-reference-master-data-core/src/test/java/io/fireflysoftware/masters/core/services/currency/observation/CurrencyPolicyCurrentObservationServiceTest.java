/*
 * Copyright 2026 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fireflysoftware.masters.core.services.currency.observation;

import io.fireflysoftware.masters.core.services.currency.observation.impl.CurrencyPolicyCurrentObservationServiceImpl;
import io.fireflysoftware.masters.core.services.currency.policy.*;
import io.fireflysoftware.masters.interfaces.dtos.currency.policy.*;
import org.fireflyframework.core.transaction.R2dbcOperationBudget;
import org.fireflyframework.security.api.domain.SecurityPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.*;
import reactor.core.publisher.Mono;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class CurrencyPolicyCurrentObservationServiceTest {
    static final UUID C=UUID.fromString("11111111-1111-4111-8111-111111111111"),P=UUID.fromString("22222222-2222-4222-8222-222222222222"),A=UUID.fromString("33333333-3333-4333-8333-333333333333");
    final AtomicLong nanos=new AtomicLong();
    final Clock clock=Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"),ZoneOffset.UTC);
    final java.util.function.LongSupplier time=nanos::get;
    final SecurityPrincipal principal=SecurityPrincipal.builder().issuer("https://issuer.example").subject("reader").claims(Map.of("exp",clock.instant().plusSeconds(60))).build();
    final Owner owner=new Owner(); final Authority authority=new Authority();
    CurrencyPolicyCurrentObservationService service(){return new CurrencyPolicyCurrentObservationServiceImpl(owner,authority,clock,time);}
    R2dbcOperationBudget budget(){return R2dbcOperationBudget.observed(clock.instant(),clock.instant().plusSeconds(3),time.getAsLong(),Duration.ofSeconds(3),clock,time);}
    CurrencyPolicyView value(){return value(null);}
    CurrencyPolicyView value(Long end){
        String source="MASTERS_CURRENCY_SOURCE_V1\n{\"currencyId\":\""+C+"\",\"currencyName\":\"Test dollar\",\"dateCreated\":null,\"dateUpdated\":null,\"decimalPrecision\":2,\"isMajor\":false,\"isoCode\":\"USD\",\"status\":\"ACTIVE\",\"symbol\":\"$\"}";
        String sourceDigest=digest(source);var request=new CurrencyPolicyPublishRequest(A.toString(),C.toString(),"USD",sourceDigest,0L,2,clock.millis()-1000,end,"sandbox-reference");
        var canonical=CurrencyPolicyRules.definition(request,new CurrencyPolicyRules.Canonical(source,sourceDigest),1);
        return new CurrencyPolicyView(new CurrencyPolicyReference(P.toString(),C.toString(),"USD",1L,canonical.digest(),CurrencyPolicyRules.POLICY_ENCODING),canonical.encoding(),source,sourceDigest,2,request.effectiveFromEpochMillis(),end,"sandbox-reference",A.toString(),clock.millis()-500,"PUBLISHED",0L,clock.millis(),true);
    }
    static String digest(String s){try{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new AssertionError(e);}}
    @Test void exactCurrentPublicationRetainsOriginalBudget(){owner.value=value();var result=service().observe(owner.value.reference(),2,budget()).block();assertEquals(owner.value,result.policy());assertEquals(Duration.ofSeconds(3),result.budget().remaining());assertEquals(2,authority.calls);assertEquals(1,owner.calls);}
    @Test void exactReferenceAndScaleAreRequired(){owner.value=value();var wrong=new CurrencyPolicyReference(P.toString(),C.toString(),"USD",2L,owner.value.reference().digest(),CurrencyPolicyRules.POLICY_ENCODING);assertThrows(RuntimeException.class,()->service().observe(wrong,2,budget()).block());assertThrows(RuntimeException.class,()->service().observe(owner.value.reference(),3,budget()).block());}
    @Test void retiredAndFalseEffectiveAreRefused(){var v=value();owner.value=new CurrencyPolicyView(v.reference(),v.canonicalEncoding(),v.sourceEncoding(),v.sourceDigest(),v.postedScale(),v.effectiveFromEpochMillis(),null,v.provenanceReference(),v.publicationActionId(),v.publishedAtEpochMillis(),"RETIRED",1L,v.observedAtEpochMillis(),false);assertThrows(RuntimeException.class,()->service().observe(v.reference(),2,budget()).block());}
    @Test void alteredCanonicalBytesAreRefused(){var v=value();owner.value=new CurrencyPolicyView(v.reference(),v.canonicalEncoding()+" ",v.sourceEncoding(),v.sourceDigest(),v.postedScale(),v.effectiveFromEpochMillis(),null,v.provenanceReference(),v.publicationActionId(),v.publishedAtEpochMillis(),v.lifecycle(),0L,v.observedAtEpochMillis(),true);assertThrows(RuntimeException.class,()->service().observe(v.reference(),2,budget()).block());}
    @Test void finalCurrentPolicyWithdrawalWithholdsObservation(){owner.value=value();authority.denyFinal=true;assertThrows(RuntimeException.class,()->service().observe(owner.value.reference(),2,budget()).block());assertEquals(2,authority.calls);}
    @Test void originalBudgetCannotBeRenewedAfterOwnerIo(){owner.value=value();owner.after=()->nanos.set(Duration.ofSeconds(3).toNanos());assertThrows(RuntimeException.class,()->service().observe(owner.value.reference(),2,budget()).block());}
    @Test void emptyAndErroredOwnerNeverBecomeCurrent(){owner.value=null;assertThrows(RuntimeException.class,()->service().observe(value().reference(),2,budget()).block());owner.failure=true;assertThrows(RuntimeException.class,()->service().observe(value().reference(),2,budget()).block());}
    @Test void differentActorAtFinalReadRefuses(){owner.value=value();authority.changeFinal=true;assertThrows(RuntimeException.class,()->service().observe(owner.value.reference(),2,budget()).block());}
    @Test void policyEndNarrowsOriginalBudget(){owner.value=value(clock.millis()+1000);var result=service().observe(owner.value.reference(),2,budget()).block();assertEquals(Duration.ofSeconds(1),result.budget().remaining());}
    @Test void staleObservationCannotReceiveFreshLocalHorizon(){var v=value();owner.value=copy(v,v.sourceEncoding(),v.sourceDigest(),v.canonicalEncoding(),v.reference(),clock.millis()-6000);assertThrows(RuntimeException.class,()->service().observe(v.reference(),2,budget()).block());}
    @Test void futureObservationIsNotCurrent(){var v=value();owner.value=copy(v,v.sourceEncoding(),v.sourceDigest(),v.canonicalEncoding(),v.reference(),clock.millis()+1);assertThrows(RuntimeException.class,()->service().observe(v.reference(),2,budget()).block());}
    @Test void closedSourceRejectsExtraFieldEvenWithMatchingHashes(){var v=value();String source=v.sourceEncoding().replace("{","{\"additional\":true,");String hash=digest(source);var request=new CurrencyPolicyPublishRequest(A.toString(),C.toString(),"USD",hash,0L,2,v.effectiveFromEpochMillis(),null,"sandbox-reference");var canonical=CurrencyPolicyRules.definition(request,new CurrencyPolicyRules.Canonical(source,hash),1);var ref=new CurrencyPolicyReference(P.toString(),C.toString(),"USD",1L,canonical.digest(),CurrencyPolicyRules.POLICY_ENCODING);owner.value=copy(v,source,hash,canonical.encoding(),ref,clock.millis());assertThrows(RuntimeException.class,()->service().observe(ref,2,budget()).block());}
    @Test void finalPolicyDelayCannotRenewOriginalBudget(){owner.value=value();authority.afterFinal=()->nanos.set(Duration.ofSeconds(3).toNanos());assertThrows(RuntimeException.class,()->service().observe(owner.value.reference(),2,budget()).block());}
    static CurrencyPolicyView copy(CurrencyPolicyView v,String source,String hash,String canonical,CurrencyPolicyReference ref,long observed){return new CurrencyPolicyView(ref,canonical,source,hash,v.postedScale(),v.effectiveFromEpochMillis(),v.effectiveToEpochMillis(),v.provenanceReference(),v.publicationActionId(),v.publishedAtEpochMillis(),v.lifecycle(),v.stateVersion(),observed,v.effectiveNow());}
    final class Authority extends CurrencyPolicyAuthority {
        int calls;boolean denyFinal,changeFinal;Runnable afterFinal=()->{};
        Authority(){super(null,null,null);}
        @Override public Mono<SecurityPrincipal> authorize(String operation,String currency,Map<String,Object> context){assertEquals("read",operation);assertEquals(C.toString(),currency);calls++;if(calls==2)afterFinal.run();if(calls==2&&denyFinal)return Mono.error(new IllegalStateException("denied"));return Mono.just(calls==2&&changeFinal?principal.toBuilder().subject("other").build():principal);}
    }
    final class Owner implements CurrencyExecutionPolicyService {
        CurrencyPolicyView value;int calls;boolean failure;Runnable after=()->{};
        public Mono<CurrencyPolicySourceObservation> observe(UUID c){throw new AssertionError();}
        public Mono<PublicationOutcome> publish(CurrencyPolicyPublishRequest r){throw new AssertionError();}
        public Mono<CurrencyPolicyView> byAction(UUID a,UUID c){throw new AssertionError();}
        public Mono<CurrencyPolicyView> retire(UUID p,CurrencyPolicyRetireRequest r){throw new AssertionError();}
        @Override public Mono<CurrencyPolicyView> get(UUID policy,UUID currency){return Mono.defer(()->{calls++;assertEquals(P,policy);assertEquals(C,currency);after.run();return failure?Mono.error(new IllegalStateException("owner unavailable")):Mono.justOrEmpty(value);});}
    }
}
