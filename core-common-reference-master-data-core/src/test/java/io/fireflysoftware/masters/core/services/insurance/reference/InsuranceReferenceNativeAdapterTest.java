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
import reactor.core.publisher.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind;

/** Networkless actual DatabaseClient mapping controls, not native role/authority qualification. */
class InsuranceReferenceNativeAdapterTest {
    static final UUID NONCE=UUID.fromString("00000000-0000-4000-8000-000000000001"),RECORD=UUID.fromString("00000000-0000-4000-8000-000000000002"),ACTION=UUID.fromString("00000000-0000-4000-8000-000000000003");
    static final Instant NOW=Instant.parse("2026-10-26T00:00:00.123456Z"),UNTIL=NOW.plusSeconds(3);
    static final String IMPL="io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferenceNativeContextImpl";
    InsuranceReferenceNativeContext.Request request(){return new InsuranceReferenceNativeContext.Request(NONCE,RECORD,Kind.PREMIUM_TAX,ACTION,null,"SUBMIT_PUBLICATION","https://synthetic.invalid","synthetic-maker",UNTIL);}
    InsuranceReferenceNativeContext adapter(Driver driver){return assertDoesNotThrow(()->(InsuranceReferenceNativeContext)Class.forName(IMPL).getConstructor(org.springframework.data.r2dbc.core.R2dbcEntityTemplate.class).newInstance(new org.springframework.data.r2dbc.core.R2dbcEntityTemplate(driver.client,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE)));}
    @Test void exactQualifiedClientIdentityIsPreserved(){var d=new Driver();assertSame(d.client,adapter(d).databaseClient());}
    @Test void beginCallsOnlyBoundNativeFunction(){var d=new Driver();var stamp=adapter(d).begin(request()).block();assertEquals(NONCE,stamp.nonce());assertEquals(1,d.sql.size());assertTrue(d.sql.getFirst().contains("public.masters_ins_ref_begin("));assertEquals(9,d.bound.size());assertTrue(d.bound.containsValue(OffsetDateTime.ofInstant(UNTIL,ZoneOffset.UTC)));assertFalse(d.sql.getFirst().contains("insurance_reference_revision"));}
    @Test void actualStoredMicrosecondStampIsNotReplacedByLocalClock(){var d=new Driver();var stamp=adapter(d).begin(request()).block();assertEquals(NOW,stamp.recordedAt());assertEquals(UNTIL,stamp.effectiveUntil());assertEquals(NOW.toEpochMilli(),stamp.dbNowMillis());}
    @Test void nullableIntentIsTypedNullBinding(){var d=new Driver();adapter(d).begin(request()).block();assertTrue(d.bound.containsValue(UUID.class));}
    @Test void missingNativeObservationFailsClosed(){var d=new Driver();d.count=0;assertThrows(InsuranceReferenceNativeContext.Unavailable.class,()->adapter(d).begin(request()).block());}
    @Test void multipleNativeObservationsAreNotAcceptedAsOne(){var d=new Driver();d.count=2;assertThrows(InsuranceReferenceNativeContext.Unavailable.class,()->adapter(d).begin(request()).block());}
    @Test void returnedDifferentNonceIsNotAccepted(){var d=new Driver();d.row.put("context_nonce",ACTION);assertThrows(InsuranceReferenceNativeContext.Unavailable.class,()->adapter(d).begin(request()).block());}
    @Test void nativeFloorMismatchFailsClosed(){var d=new Driver();d.row.put("db_now_ms",NOW.toEpochMilli()+1);assertThrows(InsuranceReferenceNativeContext.Unavailable.class,()->adapter(d).begin(request()).block());}
    @Test void returnedHorizonCannotExceedOriginalAuthority(){var d=new Driver();d.row.put("effective_until",OffsetDateTime.ofInstant(UNTIL.plusSeconds(1),ZoneOffset.UTC));assertThrows(InsuranceReferenceNativeContext.Unavailable.class,()->adapter(d).begin(request()).block());}
    @Test void completeRequiresExactTrueFromSameNonceFunction(){var d=new Driver();assertEquals(Boolean.TRUE,adapter(d).complete(request()).block());assertEquals(1,d.sql.size());assertTrue(d.sql.getFirst().contains("public.masters_ins_ref_complete("));assertTrue(d.bound.containsValue(NONCE));d=new Driver();d.complete=false;var failed=d;assertThrows(InsuranceReferenceNativeContext.Unavailable.class,()->adapter(failed).complete(request()).block());}
    @Test void nativeFailureIsPayloadFreeAndDoesNotBecomeSuccess(){var d=new Driver();d.failure=new IllegalStateException("synthetic private database diagnostic");var error=assertThrows(InsuranceReferenceNativeContext.Unavailable.class,()->adapter(d).begin(request()).block());assertNull(error.getCause());assertFalse(error.toString().contains("private database"));}
    @Test void implementationHasNoAutomaticActivation(){var d=new Driver();var value=adapter(d);for(var a:value.getClass().getAnnotations())assertFalse(a.annotationType().getName().equals("org.springframework.stereotype.Service")||a.annotationType().getName().equals("org.springframework.stereotype.Repository"));assertThrows(InsuranceReferenceNativeContext.Unavailable.class,()->InsuranceReferenceNativeContext.unavailable(d.client).begin(request()).block());assertTrue(d.sql.isEmpty());}
    static final class Driver implements ConnectionFactory {
        final List<String> sql=new ArrayList<>();final Map<Object,Object> bound=new LinkedHashMap<>();final Map<String,Object> row=new HashMap<>();int count=1;boolean complete=true;RuntimeException failure;
        final DatabaseClient client;
        Driver(){row.put("context_nonce",NONCE);row.put("record_id",RECORD);row.put("kind","PREMIUM_TAX");row.put("db_now_ms",NOW.toEpochMilli());row.put("recorded_at",OffsetDateTime.ofInstant(NOW,ZoneOffset.UTC));row.put("effective_until",OffsetDateTime.ofInstant(UNTIL,ZoneOffset.UTC));client=DatabaseClient.create(this);}
        final Connection connection=(Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->switch(m.getName()){
            case "createStatement"->statement((String)a[0]);case "close"->Mono.empty();case "isAutoCommit"->true;case "toString"->"NetworklessNativeContextDriver";default->throw new UnsupportedOperationException(m.getName());});
        Statement statement(String query){sql.add(query);return (Statement)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Statement.class},(p,m,a)->{
            if(m.getName().equals("bind")||m.getName().equals("bindNull")){Object value=a[1];if(value instanceof io.r2dbc.spi.Parameter parameter)value=parameter.getValue()==null?parameter.getType().getJavaType():parameter.getValue();bound.put(a[0],value);return p;}
            if(m.getName().equals("execute")){if(failure!=null)return Mono.error(failure);return Mono.just(new Result(){
                final List<String> names=query.contains("_complete(")?List.of("complete"):List.of("context_nonce","record_id","kind","db_now_ms","recorded_at","effective_until");
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
