package io.fireflysoftware.masters.core.services.insurance.reference;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.lang.reflect.*;
import io.r2dbc.spi.*;
import org.reactivestreams.Publisher;
import org.junit.jupiter.api.Test;
import org.fireflyframework.core.transaction.*;
import org.fireflyframework.security.api.domain.*;
import org.fireflyframework.security.spi.*;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import io.fireflysoftware.masters.core.services.insurance.source.*;
import io.fireflysoftware.masters.core.services.insurance.source.impl.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.Type;
import io.fireflysoftware.masters.models.entities.insurance.source.InsuranceSandboxFactRows.*;
import reactor.core.publisher.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Controlled storage/caller boundaries only; real persistence and issuer acceptance need owned gates. */
class InsuranceReferenceDurableSourceTest {
    static final Instant NOW=InsuranceReferenceServiceTest.NOW;
    static final UUID SOURCE=new UUID(2,1),RIGHTS=new UUID(2,2),LOCALE=new UUID(2,3),ACT=new UUID(2,4);
    static final Reference RIGHT_REF=new Reference(Type.RIGHTS,RIGHTS,1,0,"a".repeat(64));
    static final Reference LOCALE_REF=new Reference(Type.LOCALE,LOCALE,1,0,"b".repeat(64));
    static InsuranceReferenceContracts.Revision definition(){
        var old=InsuranceReferenceServiceTest.definition();String text=((InsuranceReferenceContracts.Wording)old.definition()).content();
        return new InsuranceReferenceContracts.Revision(old.kind(),old.recordId(),old.publicationId(),old.revision(),old.code(),old.effectiveFrom(),old.effectiveUntil(),old.definition(),
            new InsuranceReferenceContracts.Provenance(InsuranceReferenceContracts.Origin.SANDBOX,InsuranceSandboxFacts.sourceUri(SOURCE),"v1",InsuranceSandboxSourceRules.sha(text),InsuranceSandboxFacts.rightsUri(RIGHT_REF)));
    }
    static RegisterSource command(InsuranceReferenceContracts.Revision d){
        var head=new InsuranceReferenceCommands.Head(null,0,0);var draft=new InsuranceReferenceCommands.PublicationDraft(d.recordId(),d.kind(),d.code(),d.effectiveFrom(),d.effectiveUntil(),d.definition(),d.provenance(),head);
        String intent=InsuranceReferenceIntentCodec.digest(InsuranceReferenceIntentCodec.encode(new InsuranceReferenceIntentCodec.Intent("PUBLISH",d,head)));
        return new RegisterSource(ACT,SOURCE,0,0,new SubmittedPublication(new UUID(2,9),intent,d.publicationId(),1),draft,InsuranceSandboxSourceRules.sourceText(d),RIGHT_REF,LOCALE_REF,NOW.plusSeconds(60));
    }
    static InsuranceReferenceCommands.Receipt retained(RegisterSource c){return new InsuranceReferenceCommands.Receipt(c.submitted().actionId(),c.draft().recordId(),c.draft().kind(),new UUID(2,10),c.submitted().intentDigest(),c.submitted().publicationId(),c.submitted().revision(),0,"SUBMIT_PUBLICATION");}
    @Test void durableSourceHasIndependentRegisteredFactContractsAndReaderInsteadOfSandboxSuccessFlag(){assertTrue(InsuranceSandboxFactReader.class.isInterface());assertTrue(InsuranceSandboxFactRegistrationService.class.isInterface());assertNotNull(Reference.class);}
    @Test void sourceRegistrationBindsActualRetainedSubmitAndFullOriginalDraft(){var d=definition();var c=command(d);assertEquals(d,InsuranceSandboxSourceRules.submitted(c,retained(c)));var wrong=new InsuranceReferenceCommands.Receipt(c.submitted().actionId(),d.recordId(),d.kind(),new UUID(2,10),c.submitted().intentDigest(),new UUID(2,11),1,0,"SUBMIT_PUBLICATION");assertThrows(RuntimeException.class,()->InsuranceSandboxSourceRules.submitted(c,wrong));}
    @Test void sourceLabelOrGuessedRightsReferenceDoesNotSatisfyExactBindings(){var d=definition();assertThrows(RuntimeException.class,()->InsuranceSandboxSourceRules.sourceBinding(SOURCE,2,d,InsuranceSandboxSourceRules.sourceText(d),RIGHT_REF));assertThrows(RuntimeException.class,()->InsuranceSandboxSourceRules.sourceBinding(SOURCE,1,d,"other content",RIGHT_REF));assertThrows(RuntimeException.class,()->InsuranceSandboxSourceRules.sourceBinding(SOURCE,1,d,InsuranceSandboxSourceRules.sourceText(d),new Reference(Type.RIGHTS,new UUID(2,20),1,0,"a".repeat(64))));}
    @Test void factExpiryCannotOutliveRegisteredDependencyOrDayLimit(){assertDoesNotThrow(()->InsuranceSandboxSourceRules.requireWindow(NOW,NOW.plusSeconds(60),NOW.plusSeconds(120)));assertThrows(RuntimeException.class,()->InsuranceSandboxSourceRules.requireWindow(NOW,NOW.plusSeconds(60),NOW.plusSeconds(30)));assertThrows(RuntimeException.class,()->InsuranceSandboxSourceRules.requireWindow(NOW,NOW.plusSeconds(86401)));}
    @Test void mixedPartialLocaleReferencesCannotSkipBinding(){assertThrows(RuntimeException.class,()->InsuranceSandboxFactCodec.locale(LOCALE,null,0L,"a".repeat(64)));assertNull(InsuranceSandboxFactCodec.locale(null,null,null,null));}
    @Test void wrongRecoveredIntentDoesNotAcquireSourceDatabase(){var f=new Fixture();var pub=mock(InsuranceReferencePublicationService.class);when(pub.recover(any())).thenReturn(Mono.error(new IllegalStateException("controlled owner unavailable")));assertThrows(RuntimeException.class,()->f.registration(pub).registerSource(command(definition())).block());assertEquals(0,f.base.driver.acquires);}
    @Test void deniedCurrentSourcePolicyDoesNotAcquireDatabase(){var f=new Fixture();f.permit=false;assertThrows(RuntimeException.class,()->f.registration(mock(InsuranceReferencePublicationService.class)).registerLocale(new RegisterLocale(ACT,LOCALE,0,0,new UUID(2,30),"en-US",NOW.plusSeconds(60))).block());assertEquals(0,f.base.driver.acquires);}
    @Test void localeRegistrationUsesRealMappedVersionHeadAndActionInOneTransaction(){var f=new Fixture();f.seedLocale();var result=f.registration(mock(InsuranceReferencePublicationService.class)).registerLocale(new RegisterLocale(ACT,LOCALE,0,0,new UUID(2,30),"en-US",NOW.plusSeconds(60))).block();assertNotNull(result);assertEquals(Type.LOCALE,result.reference().type());assertEquals(3,f.base.driver.sql.stream().filter(s->s.startsWith("INSERT")).count());assertEquals(1,f.base.driver.commits);assertEquals(1,f.base.driver.closes);}
    @Test void currentPolicyWithdrawalAfterMappedWritesRollsBackBeforeReceipt(){var f=new Fixture();f.seedLocale();f.denyAt=2;assertThrows(RuntimeException.class,()->f.registration(mock(InsuranceReferencePublicationService.class)).registerLocale(new RegisterLocale(ACT,LOCALE,0,0,new UUID(2,30),"en-US",NOW.plusSeconds(60))).block());assertEquals(0,f.base.driver.commits);assertEquals(1,f.base.driver.rollbacks);}
    @Test void missingRestrictedRoleAttestationDoesNotReadOrWriteFacts(){var f=new Fixture();f.role=false;assertThrows(RuntimeException.class,()->f.registration(mock(InsuranceReferencePublicationService.class)).registerLocale(new RegisterLocale(ACT,LOCALE,0,0,new UUID(2,30),"en-US",NOW.plusSeconds(60))).block());assertTrue(f.base.driver.sql.isEmpty());assertEquals(1,f.base.driver.rollbacks);}
    @Test void exactStoredSourceRightsAndLocaleAreReadThroughRealMappedRepositories(){
        var f=new Fixture();var d=definition();f.seedDefinition(d,false,false);var result=new InsuranceSandboxFactReaderImpl(f.authority,f.persistence).observe(d).block();
        assertNotNull(result);assertEquals(InsuranceReferenceCodec.digest(d),result.definitionDigest());assertTrue(!result.effectiveUntil().isAfter(NOW.plusMillis(4999)));
        assertEquals(0,f.base.driver.sql.stream().filter(sql->sql.startsWith("INSERT")||sql.startsWith("UPDATE")).count());assertEquals(1,f.base.driver.commits);
    }
    @Test void changedLocaleEpochAndWithdrawnRightsRefuseWholeCurrentSource(){
        for(boolean changedEpoch:List.of(false,true)){
            var f=new Fixture();f.seedDefinition(definition(),changedEpoch,!changedEpoch);
            assertThrows(RuntimeException.class,()->new InsuranceSandboxFactReaderImpl(f.authority,f.persistence).observe(definition()).block());
            assertEquals(0,f.base.driver.commits);assertEquals(1,f.base.driver.rollbacks);
        }
    }
    @Test void freshCurrentSourcePolicyWithdrawalWithholdsExactStoredGraph(){
        var f=new Fixture();f.seedDefinition(definition(),false,false);f.denyAt=2;
        assertThrows(RuntimeException.class,()->new InsuranceSandboxFactReaderImpl(f.authority,f.persistence).observe(definition()).block());assertEquals(0,f.base.driver.commits);
    }
    static final class Fixture {
        final InsuranceReferenceServiceTest.Harness base=new InsuranceReferenceServiceTest.Harness();boolean permit=true,role=true;int decisions,denyAt;final AtomicLong ids=new AtomicLong(100);
        final org.fireflyframework.security.api.domain.SecurityPrincipal actor=new SecurityPrincipal("registrar","https://synthetic.invalid","platform",Set.of(),Set.of(),Map.of("exp",NOW.plusSeconds(120)),null,null,Set.of(),Map.of());
        final ConnectionFactory driver=new ConnectionFactory(){
            public Publisher<? extends Connection> create(){return Mono.from(base.driver.create()).map(connection->(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                if(m.getName().equals("createStatement")){String sql=(String)a[0];if(sql.contains("pg_catalog.clock_timestamp()")||sql.contains("AS allowed")||sql.contains("AS acquired"))return technical(sql);}
                try{return m.invoke(connection,a);}catch(InvocationTargetException e){throw e.getCause();}
            }));}
            public ConnectionFactoryMetadata getMetadata(){return base.driver.getMetadata();}
        };
        final OwnedLeaseConnectionFactory factory=new OwnedLeaseConnectionFactory(driver);final DatabaseClient client=DatabaseClient.create(factory);
        final R2dbcEntityTemplate operations=new R2dbcEntityTemplate(client,PostgresDialect.INSTANCE);final R2dbcTransactionManager manager=new R2dbcTransactionManager(factory);
        final InsuranceSandboxFactPersistence persistence=new InsuranceSandboxFactPersistence(new InsuranceSandboxFactPersistence.Session(operations,manager,true),new InsuranceSandboxFactPersistence.Session(operations,manager,false),base.clock,base.monotonic);
        final InsuranceSandboxFactAuthority authority=new InsuranceSandboxFactAuthority(()->Mono.just(actor),(p,a,r,c)->{assertTrue(a.startsWith("masters.insurance-reference.sandbox-source."));assertEquals("SANDBOX",c.get("scope"));decisions++;return Mono.just(permit&&decisions!=denyAt?Decision.permit():Decision.deny("controlled"));},Set.of("https://synthetic.invalid"),base.clock,base.monotonic);
        InsuranceSandboxFactRegistrationService registration(InsuranceReferencePublicationService pub){return new InsuranceSandboxFactRegistrationServiceImpl(authority,persistence,pub,()->new UUID(2,ids.incrementAndGet()));}
        void seedLocale(){var values=new LinkedHashMap<String,Object>();values.put("locale_id",new UUID(2,30));values.put("language_code","en");values.put("country_code","US");values.put("locale_code","en-US");values.put("language_name","English");values.put("native_name","English");values.put("region_name","United States");values.put("rtl",false);values.put("sort_order",1);values.put("status","ACTIVE");values.put("date_created",LocalDateTime.ofInstant(NOW.minusSeconds(10),ZoneOffset.UTC));values.put("date_updated",LocalDateTime.ofInstant(NOW.minusSeconds(10),ZoneOffset.UTC));values.put("source_epoch",0L);base.driver.answers.put("insurance_sandbox_locale_source",new ArrayDeque<>(List.of(values)));}
        void seedDefinition(InsuranceReferenceContracts.Revision d,boolean changedEpoch,boolean withdrawnRights){
            var wording=(InsuranceReferenceContracts.Wording)d.definition();seedLocale();
            var values=base.driver.answers.get("insurance_sandbox_locale_source").getFirst();values.put("locale_id",wording.localeId());values.put("locale_code",wording.localeCode());
            var raw=new LocaleSource(wording.localeId(),"en","US",wording.localeCode(),"English","English","United States",false,1,"ACTIVE",LocalDateTime.ofInstant(NOW.minusSeconds(10),ZoneOffset.UTC),LocalDateTime.ofInstant(NOW.minusSeconds(10),ZoneOffset.UTC),0);
            String localeEncoding=InsuranceSandboxFactCodec.localeEncoding(raw);String localeHash=InsuranceSandboxSourceRules.sha(localeEncoding);Instant until=NOW.plusSeconds(60);
            String localeCanonical=InsuranceSandboxFactCodec.fact(Type.LOCALE,LOCALE,1,0,InsuranceSandboxFactCodec.localePayload(raw.localeId(),raw.localeCode(),0,localeEncoding,localeHash),until);
            var locale=new LocaleVersion(new UUID(2,201),"SANDBOX",LOCALE,1,0,raw.localeId(),raw.localeCode(),0,localeEncoding,localeHash,localeCanonical,InsuranceSandboxFactCodec.digest(localeCanonical),until,ACT);
            var localeRef=new Reference(Type.LOCALE,LOCALE,1,0,locale.digest());String text=wording.content(),sourceDigest=InsuranceSandboxSourceRules.sha(text);
            String rightsCanonical=InsuranceSandboxFactCodec.fact(Type.RIGHTS,RIGHTS,1,0,InsuranceSandboxFactCodec.rightsPayload(SOURCE,d.kind().name(),sourceDigest,localeRef),until);
            var rights=new RightsVersion(new UUID(2,202),"SANDBOX",RIGHTS,1,0,SOURCE,d.kind().name(),text,sourceDigest,LOCALE,1L,0L,locale.digest(),rightsCanonical,InsuranceSandboxFactCodec.digest(rightsCanonical),until,ACT);
            var rightsRef=new Reference(Type.RIGHTS,RIGHTS,1,0,rights.digest());String encoding=InsuranceReferenceCodec.encode(d),digest=InsuranceReferenceCodec.digest(d);
            String sourceCanonical=InsuranceSandboxFactCodec.fact(Type.SOURCE,SOURCE,1,0,InsuranceSandboxFactCodec.sourcePayload(encoding,digest,sourceDigest,rightsRef,localeRef),until);
            var source=new SourceVersion(new UUID(2,203),"SANDBOX",SOURCE,1,0,d.recordId(),d.kind().name(),d.publicationId(),d.revision(),ACT,"a".repeat(64),encoding,digest,text,sourceDigest,RIGHTS,1,0,rights.digest(),LOCALE,1L,0L,locale.digest(),sourceCanonical,InsuranceSandboxFactCodec.digest(sourceCanonical),until,ACT);
            seed(locale);seed(rights);seed(source);
            seed(new LocaleControl(LOCALE,"SANDBOX",locale.versionId(),1,0,locale.digest(),"ACTIVE"));
            seed(new RightsControl(RIGHTS,"SANDBOX",rights.versionId(),1,withdrawnRights?1:0,rights.digest(),withdrawnRights?"WITHDRAWN":"ACTIVE"));
            seed(new SourceControl(SOURCE,"SANDBOX",source.versionId(),1,0,source.digest(),"ACTIVE"));
            if(changedEpoch)values.put("source_epoch",1L);
        }
        void seed(Object record){
            try{var values=new LinkedHashMap<String,Object>();for(var c:record.getClass().getRecordComponents()){
                Object value=c.getAccessor().invoke(record);if(value!=null)values.put(c.getName().replaceAll("([a-z0-9])([A-Z])","$1_$2").toLowerCase(Locale.ROOT),value);
            }
            var table=record.getClass().getAnnotation(org.springframework.data.relational.core.mapping.Table.class).value();base.driver.answers.put(table,new ArrayDeque<>(List.of(values)));
            }catch(Exception failure){throw new AssertionError(failure);}
        }
        Statement technical(String sql){return (Statement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Statement.class},(p,m,a)->{
            if(m.getName().equals("execute")){String name=sql.contains("AS allowed")?"allowed":sql.contains("AS acquired")?"acquired":"observed_at";Object value=name.equals("allowed")?role:name.equals("acquired")?1:OffsetDateTime.ofInstant(NOW,ZoneOffset.UTC);return Mono.just(scalar(name,value));}
            if(m.getReturnType()==Statement.class)return p;throw new UnsupportedOperationException(m.getName());
        });}
        Result scalar(String name,Object value){
            var values=new LinkedHashMap<String,Object>();values.put(name,value);var meta=base.driver.metadata(values);
            Row row=(Row)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Row.class},(p,m,a)->m.getName().equals("get")?value:m.getName().equals("getMetadata")?meta:null);
            return new Result(){public Publisher<Long> getRowsUpdated(){return Mono.empty();}public <T>Publisher<T>map(BiFunction<Row,RowMetadata,? extends T>f){return Mono.just(f.apply(row,meta));}public <T>Publisher<T>map(Function<? super io.r2dbc.spi.Readable,? extends T>f){return Mono.just(f.apply(row));}public Result filter(Predicate<Segment>f){return this;}public <T>Publisher<T>flatMap(Function<Segment,? extends Publisher<? extends T>>f){return Flux.empty();}};
        }
    }
}
