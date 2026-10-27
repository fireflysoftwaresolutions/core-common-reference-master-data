package io.fireflysoftware.masters.core.services.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;

class InsuranceReferenceSpringDataContextTest {
    static final String API="io.fireflysoftware.masters.models.repositories.insurance.reference.InsuranceReferenceContextRepository";
    static final String IMPL="io.fireflysoftware.masters.core.services.insurance.reference.impl.InsuranceReferenceNativeContextImpl";
    Class<?> type(String name){return assertDoesNotThrow(()->Class.forName(name));}
    @Test void technicalQueriesUseActualSpringDataRepositoryContract(){
        var api=type(API);assertTrue(Repository.class.isAssignableFrom(api));
        var methods=api.getDeclaredMethods();assertEquals(2,methods.length);
        for(var method:methods){var query=method.getAnnotation(Query.class);assertNotNull(query);assertTrue(query.value().startsWith("SELECT "));assertTrue(query.value().contains("public.masters_ins_ref_"));}
    }
    @Test void optInRepositoryCannotBecomeAnAutomaticallyScannedBean(){assertNotNull(type(API).getAnnotation(NoRepositoryBean.class));}
    @Test void contextRequiresExistingOperationsAndCannotConstructFromBareClient(){
        var impl=type(IMPL);assertDoesNotThrow(()->impl.getConstructor(R2dbcEntityTemplate.class));assertThrows(NoSuchMethodException.class,()->impl.getConstructor(DatabaseClient.class));
    }
    @Test void protectedPathDoesNotRetainHandWrittenSqlRepository(){assertThrows(ClassNotFoundException.class,()->Class.forName("io.fireflysoftware.masters.models.repositories.insurance.reference.impl.InsuranceReferenceContextRepositoryImpl"));}
    InsuranceReferenceNativeContext create(R2dbcEntityTemplate operations){return assertDoesNotThrow(()->(InsuranceReferenceNativeContext)type(IMPL).getConstructor(R2dbcEntityTemplate.class).newInstance(operations));}
    @Test void actualFactoryProducesTypedNativeProjection(){
        var driver=new InsuranceReferenceNativeAdapterTest.Driver();var template=new R2dbcEntityTemplate(driver.client,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE);
        var repository=new org.springframework.data.r2dbc.repository.support.R2dbcRepositoryFactory(template).getRepository(io.fireflysoftware.masters.models.repositories.insurance.reference.InsuranceReferenceContextRepository.class);
        var request=new InsuranceReferenceNativeAdapterTest().request();
        var value=repository.begin(request.nonce(),request.recordId(),request.kind(),request.actionId(),request.intentId(),request.mode(),request.issuer(),request.subject(),java.time.OffsetDateTime.ofInstant(request.authorityUntil(),java.time.ZoneOffset.UTC)).block();
        assertNotNull(value);assertEquals(InsuranceReferenceNativeAdapterTest.NOW,value.recordedAt().toInstant());assertEquals(request.nonce(),value.nonce());
        assertTrue(java.lang.reflect.Proxy.isProxyClass(repository.getClass()));
    }
    @Test void providedTemplateStatementCustomizationRemainsOnNativeQuery(){
        var driver=new InsuranceReferenceNativeAdapterTest.Driver();var template=new R2dbcEntityTemplate(driver.client,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE);var calls=new java.util.concurrent.atomic.AtomicInteger();template.setStatementFilterFunction(statement->{calls.incrementAndGet();return statement;});
        var context=create(template);assertSame(template.getDatabaseClient(),context.databaseClient());context.begin(new InsuranceReferenceNativeAdapterTest().request()).block();assertEquals(1,calls.get());
    }
    @Test void providedClientCustomizationRemainsOnNativeQuery(){
        var driver=new InsuranceReferenceNativeAdapterTest.Driver();var calls=new java.util.concurrent.atomic.AtomicInteger();var client=DatabaseClient.builder().connectionFactory(driver).executeFunction(statement->{calls.incrementAndGet();return statement.execute();}).build();
        var template=new R2dbcEntityTemplate(client,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE);var context=create(template);assertSame(client,context.databaseClient());context.begin(new InsuranceReferenceNativeAdapterTest().request()).block();assertEquals(1,calls.get());
    }
    @org.springframework.data.convert.ReadingConverter
    static final class CountingKindConverter implements org.springframework.core.convert.converter.Converter<String,io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind> {
        final java.util.concurrent.atomic.AtomicInteger calls;
        CountingKindConverter(java.util.concurrent.atomic.AtomicInteger calls){this.calls=calls;}
        @Override public io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind convert(String value){calls.incrementAndGet();return io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Kind.valueOf(value);}
    }
    @Test void callerMappingConverterAndStrategyAreNotReplaced(){
        var driver=new InsuranceReferenceNativeAdapterTest.Driver();var calls=new java.util.concurrent.atomic.AtomicInteger();var conversions=org.springframework.data.r2dbc.convert.R2dbcCustomConversions.of(org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE,new CountingKindConverter(calls));
        var mapping=new org.springframework.data.r2dbc.mapping.R2dbcMappingContext();mapping.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        var converter=new org.springframework.data.r2dbc.convert.MappingR2dbcConverter(mapping,conversions);
        var template=new R2dbcEntityTemplate(driver.client,org.springframework.data.r2dbc.dialect.PostgresDialect.INSTANCE,converter);assertSame(converter,template.getConverter());
        create(template).begin(new InsuranceReferenceNativeAdapterTest().request()).block();assertEquals(1,calls.get());
    }
}
