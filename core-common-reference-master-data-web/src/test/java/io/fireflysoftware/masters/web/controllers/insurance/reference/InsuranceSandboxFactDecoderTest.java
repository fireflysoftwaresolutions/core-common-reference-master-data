package io.fireflysoftware.masters.web.controllers.insurance.reference;

import java.nio.charset.StandardCharsets;
import java.lang.reflect.InvocationTargetException;
import io.fireflysoftware.masters.interfaces.dtos.insurance.source.InsuranceSandboxFacts.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InsuranceSandboxFactDecoderTest {
    Object decode(String method,String raw) throws Exception {
        var decoder=Class.forName("io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceSandboxFactDecoder");
        return decoder.getDeclaredMethod(method,byte[].class).invoke(null,(Object)raw.getBytes(StandardCharsets.UTF_8));
    }
    String locale(){return "{\"actionId\":\"10000000-0000-0000-0000-000000000001\",\"factId\":\"20000000-0000-0000-0000-000000000001\",\"expectedVersion\":0,\"expectedGeneration\":0,\"localeId\":\"30000000-0000-0000-0000-000000000001\",\"localeCode\":\"en-US\",\"effectiveUntil\":\"2030-01-01T00:00:00Z\"}";}
    @Test void actualClosedLocaleRequestRetainsAllTypedFields() throws Exception {
        var value=(RegisterLocale)decode("locale",locale());assertEquals("en-US",value.localeCode());assertEquals(0,value.expectedVersion());assertEquals("2030-01-01T00:00:00Z",value.effectiveUntil().toString());
    }
    @Test void duplicateUnknownAndNumericStringFieldsRefuse() throws Exception {
        for(var raw:new String[]{locale().replace("\"localeCode\":","\"unclaimed\":true,\"localeCode\":"),locale().replace("\"expectedVersion\":0","\"expectedVersion\":\"0\""),locale().replace("\"expectedVersion\":0","\"expectedVersion\":0,\"expectedVersion\":0")})
            assertInstanceOf(IllegalArgumentException.class,assertThrows(InvocationTargetException.class,()->decode("locale",raw)).getCause());
    }
    @Test void malformedUtf8AndOversizeRefuseBeforeService() throws Exception {
        var decoder=Class.forName("io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceSandboxFactDecoder");var method=decoder.getDeclaredMethod("locale",byte[].class);
        for(var raw:new byte[][]{new byte[]{(byte)0xc3,(byte)0x28},new byte[512*1024+1]})
            assertInstanceOf(IllegalArgumentException.class,assertThrows(InvocationTargetException.class,()->method.invoke(null,(Object)raw)).getCause());
    }
    @Test void allFourAdministrationPostsOptOutOfResponseCache() throws Exception {
        var controller=Class.forName("io.fireflysoftware.masters.web.controllers.insurance.reference.InsuranceSandboxFactController");int count=0;
        for(var method:controller.getDeclaredMethods())if(method.isAnnotationPresent(org.springframework.web.bind.annotation.PostMapping.class)){
            assertTrue(method.isAnnotationPresent(org.fireflyframework.web.idempotency.annotation.DisableIdempotency.class));count++;
        }
        assertEquals(4,count);assertFalse(controller.getAnnotation(org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class).matchIfMissing());
    }
}
