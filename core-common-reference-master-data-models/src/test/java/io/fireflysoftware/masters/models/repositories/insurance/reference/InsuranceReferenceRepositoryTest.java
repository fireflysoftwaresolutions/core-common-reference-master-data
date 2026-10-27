package io.fireflysoftware.masters.models.repositories.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import java.util.function.*;
import io.r2dbc.spi.*;
import org.reactivestreams.Publisher;
import org.junit.jupiter.api.Test;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.*;

class InsuranceReferenceRepositoryTest {
    static final UUID RECORD=UUID.fromString("00000000-0000-4000-8000-000000000001");
    static final UUID PUBLICATION=UUID.fromString("00000000-0000-4000-8000-000000000002");
    static final UUID NEXT=UUID.fromString("00000000-0000-4000-8000-000000000003");
    static final String ROWS="io.fireflysoftware.masters.models.entities.insurance.reference.InsuranceReferenceRows$";
    static final String API="io.fireflysoftware.masters.models.repositories.insurance.reference.InsuranceReferenceRepository";
    static final String IMPL="io.fireflysoftware.masters.models.repositories.insurance.reference.impl.InsuranceReferenceRepositoryImpl";
    static final String KIND="io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts$Kind";
    Class<?> type(String n) {return assertDoesNotThrow(()->Class.forName(n));}
    Object kind() {return Enum.valueOf((Class)type(KIND),"PREMIUM_TAX");}
    Object row(String name,Object... values) {return assertDoesNotThrow(()->type(ROWS+name).getDeclaredConstructors()[0].newInstance(values));}
    Object control(UUID publication,long revision,long generation,String state) {
        return row("ReferenceControl","GLOBAL_MASTER",RECORD,kind(),publication,revision,generation,state);
    }
    Object repository(Driver d) {return assertDoesNotThrow(()->type(IMPL).getConstructor(R2dbcEntityTemplate.class).newInstance(d.template()));}
    Mono<?> call(Object target,String name,Object... args) {
        Method m=Arrays.stream(target.getClass().getMethods()).filter(v->v.getName().equals(name)&&v.getParameterCount()==args.length).findFirst().orElseThrow();
        try{return (Mono<?>)m.invoke(target,args);}catch(InvocationTargetException e){return Mono.error(e.getCause());}catch(Exception e){throw new AssertionError(e);}
    }
    String executeRead(String method,Object... args) {var d=new Driver();call(repository(d),method,args).block();assertEquals(1,d.sql.size());return d.sql.getFirst();}
    void scoped(String sql,String... columns) {assertTrue(sql.startsWith("SELECT")||sql.startsWith("UPDATE"));assertTrue(sql.contains("catalog_scope"));for(String c:columns)assertTrue(sql.contains(c),sql);assertFalse(sql.contains("FOR UPDATE"));}
    @Test void nineClosedRecordTypesCarryExplicitGlobalScopeAndTables() {
        for(String n:List.of("Identity","Revision","ReferenceControl","MakerIntent","IntentControl","CheckerApproval","Action","Audit","Outbox")) {
            var c=type(ROWS+n);assertTrue(c.isRecord());assertNotNull(c.getAnnotation(Table.class));
            assertTrue(Arrays.stream(c.getRecordComponents()).anyMatch(p->p.getName().equals("catalogScope")));
        }
    }
    @Test void scopedRevisionReadBindsPublicationRecordKindAndVersion() {scoped(executeRead("revision",RECORD,kind(),PUBLICATION,1L),"publication_id","record_id","kind","revision");}
    @Test void identityReadBindsKindAndRecord() {scoped(executeRead("identity",RECORD,kind()),"record_id","kind");}
    @Test void referenceControlReadBindsKindAndRecord() {scoped(executeRead("control",RECORD,kind()),"record_id","kind");}
    @Test void intentReadBindsRecordKindAndIntent() {scoped(executeRead("intent",RECORD,kind(),PUBLICATION),"record_id","kind","intent_id");}
    @Test void intentControlReadBindsRecordKindAndIntent() {scoped(executeRead("intentControl",RECORD,kind(),PUBLICATION),"record_id","kind","intent_id");}
    @Test void checkerReadBindsExactRecordIntentAndDigest() {scoped(executeRead("approval",RECORD,kind(),PUBLICATION,NEXT,"a".repeat(64)),"record_id","kind","intent_id","approval_id","intent_digest");}
    @Test void durableActionReadBindsExactRecordKindAndAction() {scoped(executeRead("action",RECORD,kind(),PUBLICATION),"record_id","kind","action_id");}
    @Test void publicRepositoryHasNoGenericCrudOrDeleteSurface() {
        Set<String> names=new TreeSet<>();for(var m:type(API).getMethods())names.add(m.getName());
        assertTrue(names.containsAll(List.of("identity","revision","control","intent","intentControl","approval","action","compareAndSetControl","compareAndSetIntent")));
        assertFalse(names.stream().anyMatch(n->n.equals("save")||n.equals("delete")||n.equals("findAll")||n.equals("update")));
    }
    @Test void controlCasMatchesAllCurrentBindingsAndChangesOnlyCurrentColumns() {
        var d=new Driver();Object r=repository(d);assertEquals(1L,call(r,"compareAndSetControl",control(PUBLICATION,1,0,"ACTIVE"),control(NEXT,2,1,"ACTIVE")).block());
        String sql=d.sql.getFirst();scoped(sql,"record_id","kind","publication_id","revision","generation","lifecycle");
        assertFalse(sql.contains("canonical_definition"));assertFalse(sql.contains("definition_digest"));assertTrue(d.bound.containsValue("GLOBAL_MASTER"));
    }
    @Test void zeroControlCasFailsClosed() {var d=new Driver();d.rows=0;assertThrows(RuntimeException.class,()->call(repository(d),"compareAndSetControl",control(PUBLICATION,1,0,"ACTIVE"),control(NEXT,2,1,"ACTIVE")).block());}
    @Test void multipleControlCasFailsClosed() {var d=new Driver();d.rows=2;assertThrows(RuntimeException.class,()->call(repository(d),"compareAndSetControl",control(PUBLICATION,1,0,"ACTIVE"),control(NEXT,2,1,"ACTIVE")).block());}
    @Test void exhaustedGenerationFailsBeforeAnySql() {var d=new Driver();assertThrows(RuntimeException.class,()->call(repository(d),"compareAndSetControl",control(PUBLICATION,1,Long.MAX_VALUE,"ACTIVE"),control(NEXT,2,Long.MAX_VALUE,"ACTIVE")).block());assertTrue(d.sql.isEmpty());}
    @Test void nonSequentialReplacementFailsBeforeAnySql() {var d=new Driver();assertThrows(RuntimeException.class,()->call(repository(d),"compareAndSetControl",control(PUBLICATION,1,0,"ACTIVE"),control(NEXT,3,1,"ACTIVE")).block());assertTrue(d.sql.isEmpty());}
    @Test void withdrawalPreservesPublicationAndRevision() {var d=new Driver();assertEquals(1L,call(repository(d),"compareAndSetControl",control(PUBLICATION,1,0,"ACTIVE"),control(PUBLICATION,1,1,"WITHDRAWN")).block());}
    @Test void intentExecutionPreservesStoredApprovalPointer() {
        var before=row("IntentControl","GLOBAL_MASTER",PUBLICATION,RECORD,kind(),2L,"APPROVED",NEXT,null);
        var after=row("IntentControl","GLOBAL_MASTER",PUBLICATION,RECORD,kind(),3L,"EXECUTED",NEXT,RECORD);
        var d=new Driver();assertEquals(1L,call(repository(d),"compareAndSetIntent",before,after).block());scoped(d.sql.getFirst(),"intent_id","record_id","kind","generation","lifecycle","approval_id","execution_action_id");
    }
    @Test void wrongGlobalScopeDataIsRejected() {var c=type(ROWS+"Identity").getDeclaredConstructors()[0];var failure=assertThrows(InvocationTargetException.class,()->c.newInstance("TENANT",RECORD,kind(),"SYNTHETIC_TAX"));assertInstanceOf(IllegalArgumentException.class,failure.getCause());}
    @Test void protectedBodiesDoNotAppearInRowToString() {
        var revision=row("Revision","GLOBAL_MASTER",PUBLICATION,RECORD,kind(),1L,"sensitive synthetic wording","a".repeat(64),Instant.parse("2026-10-26T00:00:00Z"),null,NEXT,Instant.parse("2026-10-26T00:00:00Z"));
        assertFalse(revision.toString().contains("sensitive"));
    }
    @Test void mappingsHaveNoAutomaticActivation() {
        for(Class<?> c:List.of(type(API),type(IMPL),type(ROWS+"Identity")))for(var a:c.getAnnotations())assertFalse(a.annotationType().getName().equals("org.springframework.stereotype.Repository")||a.annotationType().getName().equals("org.springframework.stereotype.Service"));
    }
    @Test void revisionInsertUsesMappedColumnsAndOmitsNativeRecordedTime() {
        var d=new Driver();var revision=row("Revision","GLOBAL_MASTER",PUBLICATION,RECORD,kind(),1L,"{synthetic}","a".repeat(64),Instant.parse("2026-10-26T00:00:00Z"),null,NEXT,Instant.parse("2026-10-26T00:00:00Z"));
        call(repository(d),"appendRevision",revision).block();String sql=d.sql.getFirst();assertTrue(sql.startsWith("INSERT INTO \"insurance_reference_revision\""),sql);assertTrue(sql.contains("canonical_definition"));assertTrue(sql.contains("definition_digest"));assertFalse(sql.contains("recorded_at"));assertTrue(d.bound.containsValue("GLOBAL_MASTER"));
    }
    @Test void sameProvidedTemplateRetainsActualSpringDataEntityCallbacks() throws Exception {
        var d=new Driver();var template=d.template();var calls=new java.util.concurrent.atomic.AtomicInteger();
        org.springframework.data.r2dbc.mapping.event.BeforeConvertCallback<Object> callback=(entity,table)->{calls.incrementAndGet();return Mono.just(entity);};
        template.setEntityCallbacks(org.springframework.data.mapping.callback.ReactiveEntityCallbacks.create(callback));
        var repository=type(IMPL).getConstructor(R2dbcEntityTemplate.class).newInstance(template);
        call(repository,"appendIdentity",row("Identity","GLOBAL_MASTER",RECORD,kind(),"SYNTHETIC_TAX")).block();assertEquals(1,calls.get());assertEquals(1,d.sql.size());
    }
    @Test void allNineAppendMethodsAreClosedTypedOperations() {
        var methods=Arrays.stream(type(API).getMethods()).filter(m->m.getName().startsWith("append")).toList();assertEquals(9,methods.size());
        for(var m:methods){assertEquals(1,m.getParameterCount());assertTrue(m.getParameterTypes()[0].getName().startsWith(ROWS));assertFalse(m.getParameterTypes()[0]==Object.class);}
    }
    @Test void wrongRecordControlReplacementIsRejectedBeforeSql() {
        var d=new Driver();var after=row("ReferenceControl","GLOBAL_MASTER",NEXT,kind(),NEXT,2L,1L,"ACTIVE");
        assertThrows(RuntimeException.class,()->call(repository(d),"compareAndSetControl",control(PUBLICATION,1,0,"ACTIVE"),after).block());assertTrue(d.sql.isEmpty());
    }
    @Test void executionCannotSubstituteAnotherStoredCheckerApproval() {
        var before=row("IntentControl","GLOBAL_MASTER",PUBLICATION,RECORD,kind(),2L,"APPROVED",NEXT,null);
        var after=row("IntentControl","GLOBAL_MASTER",PUBLICATION,RECORD,kind(),3L,"EXECUTED",RECORD,RECORD);
        var d=new Driver();assertThrows(RuntimeException.class,()->call(repository(d),"compareAndSetIntent",before,after).block());assertTrue(d.sql.isEmpty());
    }
    @Test void executedIntentCannotBeReopenedOrReapproved() {
        var before=row("IntentControl","GLOBAL_MASTER",PUBLICATION,RECORD,kind(),3L,"EXECUTED",NEXT,RECORD);
        var after=row("IntentControl","GLOBAL_MASTER",PUBLICATION,RECORD,kind(),4L,"OPEN",null,null);
        var d=new Driver();assertThrows(RuntimeException.class,()->call(repository(d),"compareAndSetIntent",before,after).block());assertTrue(d.sql.isEmpty());
    }
    @Test void missingCasCountIsNotSuccess() {
        var d=new Driver();d.noCount=true;assertThrows(RuntimeException.class,()->call(repository(d),"compareAndSetControl",control(PUBLICATION,1,0,"ACTIVE"),control(NEXT,2,1,"ACTIVE")).block());
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"Identity","Revision","ReferenceControl","MakerIntent","IntentControl","CheckerApproval","Action","Audit","Outbox"})
    void eachClosedAppendUsesTheRealMappedTableAndNoNativeTimestampWrite(String name) throws Exception {
        Class<?> type=type(ROWS+name);var values=new ArrayList<Object>();
        for(var c:type.getRecordComponents()) {
            String n=c.getName();Object value;
            if(c.getType()==UUID.class)value=n.equals("recordId")?RECORD:n.equals("expectedPublicationId")||n.equals("approvalId")&&name.equals("IntentControl")||n.equals("executionActionId")?null:NEXT;
            else if(c.getType()==long.class)value=n.equals("revision")?1L:0L;
            else if(c.getType()==Instant.class)value=n.equals("effectiveUntil")?null:Instant.parse("2026-10-26T00:00:00Z");
            else if(c.getType().isEnum())value=kind();
            else value=switch(n){case "catalogScope"->"GLOBAL_MASTER";case "lifecycle"->name.equals("IntentControl")?"OPEN":"ACTIVE";case "operation","eventKind"->"PUBLISH";case "code"->"SYNTHETIC_TAX";case "definitionDigest","intentDigest"->"a".repeat(64);case "makerIssuer","checkerIssuer","actorIssuer"->"https://synthetic.invalid";case "makerSubject","checkerSubject","actorSubject"->"synthetic-staff";default->"{synthetic}";};
            values.add(value);
        }
        var value=type.getDeclaredConstructors()[0].newInstance(values.toArray());var d=new Driver();
        call(repository(d),"append"+name,value).block();assertEquals(1,d.sql.size());String sql=d.sql.getFirst();
        assertTrue(sql.startsWith("INSERT INTO \""+type.getAnnotation(Table.class).value()+"\""));assertTrue(sql.contains("catalog_scope"));assertFalse(sql.contains("recorded_at"));assertTrue(d.bound.containsValue("GLOBAL_MASTER"));
    }
    @Test void virginIntentCannotCarryAnExistingHeadWithZeroRevision() throws Exception {
        var c=type(ROWS+"MakerIntent").getDeclaredConstructors()[0];
        var failure=assertThrows(InvocationTargetException.class,()->c.newInstance("GLOBAL_MASTER",NEXT,RECORD,kind(),"PUBLISH","{synthetic}","a".repeat(64),PUBLICATION,0L,0L,"https://synthetic.invalid","synthetic-staff",Instant.parse("2026-11-01T00:00:00Z"),null));
        assertInstanceOf(IllegalArgumentException.class,failure.getCause());
    }
    static class Driver implements ConnectionFactory {
        final List<String> sql=new ArrayList<>();final Map<Object,Object> bound=new LinkedHashMap<>();long rows=1;boolean noCount;
        final Connection connection=(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->switch(m.getName()) {
            case "createStatement" -> statement((String)a[0]);case "getMetadata" -> new ConnectionMetadata(){public String getDatabaseProductName(){return "PostgreSQL";}public String getDatabaseVersion(){return "17";}};
            case "isAutoCommit" -> true;case "validate" -> Mono.just(true);case "close","beginTransaction","commitTransaction","rollbackTransaction","setAutoCommit","setTransactionIsolationLevel" -> Mono.empty();
            case "toString" -> "NetworklessReferenceDriver";default -> throw new UnsupportedOperationException(m.getName());
        });
        Statement statement(String query) {sql.add(query);return (Statement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Statement.class},(p,m,a)->{
            if(m.getName().equals("bind")){bound.put(a[0],a[1]);return p;}
            if(m.getName().equals("execute"))return Mono.just(new Result(){
                public Publisher<Long> getRowsUpdated(){return noCount?Mono.empty():Mono.just(rows);}
                public <T>Publisher<T> map(BiFunction<Row,RowMetadata,? extends T> f){return Flux.empty();}
                public <T>Publisher<T> map(Function<? super io.r2dbc.spi.Readable,? extends T> f){return Flux.empty();}
                public Result filter(Predicate<Segment> f){return this;}
                public <T>Publisher<T> flatMap(Function<Segment,? extends Publisher<? extends T>> f){return Flux.empty();}
            });if(m.getReturnType()==Statement.class)return p;throw new UnsupportedOperationException(m.getName());
        });}
        public Publisher<? extends Connection> create(){return Mono.just(connection);}public ConnectionFactoryMetadata getMetadata(){return ()->"PostgreSQL";}
        R2dbcEntityTemplate template(){return new R2dbcEntityTemplate(DatabaseClient.create(this),PostgresDialect.INSTANCE);}
    }
}
