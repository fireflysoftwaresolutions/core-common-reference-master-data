package io.fireflysoftware.masters.core.services.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import io.r2dbc.spi.*;
import org.reactivestreams.Publisher;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import reactor.core.publisher.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.Target;

/** Real Spring Data proxy/projection over controlled driver rows, no SQL or native server execution. */
class InsuranceReferenceCurrentNativeAdapterTest {
    static final UUID NONCE=InsuranceReferenceNativeAdapterTest.NONCE,RECORD=InsuranceReferenceNativeAdapterTest.RECORD,PUBLICATION=InsuranceReferenceNativeAdapterTest.ACTION;
    static final Instant NOW=InsuranceReferenceNativeAdapterTest.NOW,UNTIL=InsuranceReferenceNativeAdapterTest.UNTIL;
    static final String DIGEST="a".repeat(64),IMPL="io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferenceCurrentNativeContextImpl";
    InsuranceReferenceCurrentNativeContext.Request request(){return new InsuranceReferenceCurrentNativeContext.Request(NONCE,new Target(Kind.PREMIUM_TAX,RECORD,PUBLICATION,1,DIGEST,0),"https://synthetic.invalid","reader",UNTIL);}
    InsuranceReferenceCurrentNativeContext adapter(Driver driver){return assertDoesNotThrow(()->(InsuranceReferenceCurrentNativeContext)Class.forName(IMPL).getConstructor(R2dbcEntityTemplate.class).newInstance(new R2dbcEntityTemplate(driver.client,PostgresDialect.INSTANCE)));}
    @Test void exactCurrentSpringDataProxyMapsFullTargetAndNativeTimestamp(){var d=new Driver();var value=adapter(d);assertSame(d.client,value.databaseClient());var stamp=value.begin(request()).block();assertEquals(request().target(),stamp.target());assertEquals(NOW,stamp.recordedAt());assertEquals(UNTIL,stamp.effectiveUntil());assertEquals(10,d.bound.size());assertTrue(d.sql.getFirst().contains("public.masters_ins_ref_current_begin("));assertFalse(d.sql.getFirst().contains("insurance_reference_revision"));}
    @Test void differentRetainedPublicationDigestOrGenerationIsNotAccepted(){for(String field:List.of("publication_id","definition_digest","control_generation")){var d=new Driver();d.row.put(field,switch(field){case "publication_id"->new UUID(1,10);case "definition_digest"->"b".repeat(64);default->1L;});assertThrows(RuntimeException.class,()->adapter(d).begin(request()).block());}}
    @Test void ordinaryEmptyCurrentSnapshotHasTheStaticNotCurrentClassification(){var d=new Driver();d.count=0;var error=assertThrows(InsuranceReferenceCurrentFailure.class,()->adapter(d).begin(request()).block());assertEquals(InsuranceReferenceCurrentFailure.Kind.NOT_CURRENT,error.kind());}
    @Test void multipleNativeRowsAreNotAcceptedAsOne(){var d=new Driver();d.count=2;assertThrows(RuntimeException.class,()->adapter(d).begin(request()).block());}
    @Test void laterNativeHorizonOrWrongMillisecondFloorRefuses(){for(boolean horizon:List.of(false,true)){var d=new Driver();if(horizon)d.row.put("effective_until",OffsetDateTime.ofInstant(UNTIL.plusMillis(1),ZoneOffset.UTC));else d.row.put("db_now_ms",NOW.toEpochMilli()+1);assertThrows(RuntimeException.class,()->adapter(d).begin(request()).block());}}
    @Test void currentCompletionUsesExactCurrentFunctionAndLiteralTrue(){var d=new Driver();assertTrue(adapter(d).complete(request()).block());assertTrue(d.sql.getFirst().contains("public.masters_ins_ref_current_complete("));d.complete=false;assertThrows(RuntimeException.class,()->adapter(d).complete(request()).block());}
    @Test void currentAdapterHasNoAutomaticActivationOrGenericRecoveryMode(){var d=new Driver();assertEquals(0,adapter(d).getClass().getAnnotations().length);assertThrows(RuntimeException.class,()->InsuranceReferenceCurrentNativeContext.unavailable(d.client).begin(request()).block());assertTrue(d.sql.isEmpty());}
    static final class Driver implements ConnectionFactory {
        final List<String> sql=new ArrayList<>();final Map<Object,Object> bound=new LinkedHashMap<>();final Map<String,Object> row=new HashMap<>();int count=1;boolean complete=true;RuntimeException failure;
        final DatabaseClient client;
        Driver(){row.put("context_nonce",NONCE);row.put("record_id",RECORD);row.put("kind","PREMIUM_TAX");row.put("db_now_ms",NOW.toEpochMilli());row.put("recorded_at",OffsetDateTime.ofInstant(NOW,ZoneOffset.UTC));row.put("effective_until",OffsetDateTime.ofInstant(UNTIL,ZoneOffset.UTC));row.put("publication_id",PUBLICATION);row.put("revision",1L);row.put("definition_digest",DIGEST);row.put("control_generation",0L);client=DatabaseClient.create(this);}
        final Connection connection=(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->switch(m.getName()){
            case "createStatement"->statement((String)a[0]);case "close"->Mono.empty();case "isAutoCommit"->true;case "toString"->"NetworklessNativeContextDriver";default->throw new UnsupportedOperationException(m.getName());});
        Statement statement(String query){sql.add(query);return (Statement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Statement.class},(p,m,a)->{
            if(m.getName().equals("bind")||m.getName().equals("bindNull")){Object value=a[1];if(value instanceof io.r2dbc.spi.Parameter parameter)value=parameter.getValue()==null?parameter.getType().getJavaType():parameter.getValue();bound.put(a[0],value);return p;}
            if(m.getName().equals("execute")){if(failure!=null)return Mono.error(failure);return Mono.just(new Result(){
                final List<String> names=query.contains("_complete(")?List.of("complete"):List.of("context_nonce","record_id","kind","publication_id","revision","definition_digest","control_generation","db_now_ms","recorded_at","effective_until");
                List<ColumnMetadata> columns=names.stream().map(name->(ColumnMetadata)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ColumnMetadata.class},(p,m,a)->switch(m.getName()){
                    case "getName"->name;case "getJavaType"->name.equals("complete")?Boolean.class:row.get(name).getClass();case "getNullability"->Nullability.NON_NULL;case "getType"->new io.r2dbc.spi.Type(){public Class<?> getJavaType(){return name.equals("complete")?Boolean.class:row.get(name).getClass();}public String getName(){return "NetworklessTypedColumn";}};default->null;})).toList();
                RowMetadata metadata=(RowMetadata)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{RowMetadata.class},(p,m,a)->switch(m.getName()){
                    case "getColumnMetadatas"->columns;case "contains"->names.contains(a[0]);case "getColumnMetadata"->columns.get(a[0] instanceof Integer index?index:names.indexOf(a[0]));default->throw new UnsupportedOperationException(m.getName());});
                Row mapped=(Row)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Row.class},(p,m,a)->{if(m.getName().equals("get"))return query.contains("_complete(")?complete:row.get(a[0] instanceof Integer index?names.get(index):(String)a[0]);if(m.getName().equals("getMetadata"))return metadata;throw new UnsupportedOperationException(m.getName());});
                public Publisher<Long> getRowsUpdated(){return Mono.empty();}
                public <T>Publisher<T> map(BiFunction<Row,RowMetadata,? extends T> f){return Flux.range(0,count).map(i->f.apply(mapped,metadata));}
                public <T>Publisher<T> map(Function<? super io.r2dbc.spi.Readable,? extends T> f){return Flux.range(0,count).map(i->f.apply(mapped));}
                public Result filter(Predicate<Segment> f){return this;}
                public <T>Publisher<T> flatMap(Function<Segment,? extends Publisher<? extends T>> f){return Flux.empty();}
            });}if(m.getReturnType()==Statement.class)return p;throw new UnsupportedOperationException(m.getName());});}
        public Publisher<? extends Connection> create(){return Mono.just(connection);}public ConnectionFactoryMetadata getMetadata(){return ()->"PostgreSQL";}
    }
}
