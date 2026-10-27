package io.fireflysoftware.masters.web.config.openapi;

import java.util.*;
import io.swagger.v3.core.converter.ModelConverters;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceContracts.Observation;

class InsuranceCurrentTemporalMetadataTest {
    @Test void allNewSourceExpiryFieldsUseActualSharedInstantContract(){
        for(var type:List.of(RegisterLocale.class,RegisterRights.class,RegisterSource.class)){
            var schema=new ModelConverters().readAll(type).get(type.getSimpleName());assertNotNull(schema);
            assertEquals("#/components/schemas/FireflyInstant",((io.swagger.v3.oas.models.media.Schema<?>)schema.getProperties().get("effectiveUntil")).get$ref());
        }
    }
    @Test void currentResponseNativeObservationTimestampsUseSameScalar(){
        var schema=new ModelConverters().readAll(Observation.class).get("Observation");assertNotNull(schema);
        for(var name:List.of("observedAt","effectiveUntil"))assertEquals("#/components/schemas/FireflyInstant",((io.swagger.v3.oas.models.media.Schema<?>)schema.getProperties().get(name)).get$ref());
    }
}
