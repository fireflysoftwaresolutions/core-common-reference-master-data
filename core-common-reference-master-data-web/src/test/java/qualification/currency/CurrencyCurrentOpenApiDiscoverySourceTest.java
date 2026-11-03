package qualification.currency;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import io.swagger.v3.core.converter.*;
import io.fireflysoftware.masters.core.services.currency.observation.*;
import io.fireflysoftware.masters.web.controllers.currency.policy.CurrencyPolicyCurrentController;
import org.springframework.context.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
class CurrencyCurrentOpenApiDiscoverySourceTest {
 @Test void currentControllerRemainsDefaultOffAndRequiresExactNamedService()throws Exception{
  var condition=CurrencyPolicyCurrentController.class.getAnnotation(ConditionalOnProperty.class);
  assertEquals("firefly.masters.currency-policy.current",condition.prefix());assertFalse(condition.matchIfMissing());
  assertEquals(java.util.Map.of("firefly.masters.currency-policy.current.enabled",true),io.fireflysoftware.masters.web.openapi.OpenApiGenApplication.metadataDefaults());
  var method=io.fireflysoftware.masters.web.openapi.OpenApiGenApplication.class.getMethod("currencyPolicyNativeCurrentObservationService");
  assertArrayEquals(new String[]{"currencyPolicyNativeCurrentObservationService"},method.getAnnotation(org.springframework.context.annotation.Bean.class).value());
  assertArrayEquals(new String[]{"openapi-gen"},method.getAnnotation(org.springframework.context.annotation.Profile.class).value());
  var qualifier=CurrencyPolicyCurrentController.class.getConstructors()[0].getParameters()[0].getAnnotation(org.springframework.beans.factory.annotation.Qualifier.class);
  assertEquals("currencyPolicyNativeCurrentObservationService",qualifier.value());
 }
 @Configuration(proxyBeanMethods=false) static class MetadataOnly {
  @Bean("currencyPolicyNativeCurrentObservationService") CurrencyPolicyCurrentObservationService metadataOnlyCurrent(){return new io.fireflysoftware.masters.web.openapi.OpenApiGenApplication().currencyPolicyNativeCurrentObservationService();}
 }
 @Test void exactNamedUnavailableMetadataBeanConstructsActualControllerWithoutNativeResources(){
  try(var context=new AnnotationConfigApplicationContext()){
   context.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("metadata-only",java.util.Map.of("firefly.masters.currency-policy.current.enabled",true)));
   context.register(MetadataOnly.class,CurrencyPolicyCurrentController.class);context.refresh();
   assertNotNull(context.getBean(CurrencyPolicyCurrentController.class));
   assertNotNull(context.getBean("currencyPolicyNativeCurrentObservationService",CurrencyPolicyCurrentObservationService.class));
   assertTrue(context.getBeansOfType(io.r2dbc.spi.ConnectionFactory.class).isEmpty());
  }
 }
 @Test void bothActualCurrentContractsRequireDistinctSchemaNames(){
  var insurance=new ModelConverters().resolveAsResolvedSchema(new AnnotatedType(io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.Current.class).resolveAsRef(true));
  var currency=new ModelConverters().resolveAsResolvedSchema(new AnnotatedType(io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Current.class).resolveAsRef(true));
  assertEquals("#/components/schemas/Current",insurance.schema.get$ref());
  assertEquals("#/components/schemas/CurrencyPolicyCurrent",currency.schema.get$ref());
  assertEquals(java.util.Set.of("target","publication","recordedAt","effectiveUntil"),currency.referencedSchemas.get("CurrencyPolicyCurrent").getProperties().keySet());
  assertEquals("#/components/schemas/CurrencyPolicyCurrentTarget",((io.swagger.v3.oas.models.media.Schema<?>)currency.referencedSchemas.get("CurrencyPolicyCurrent").getProperties().get("target")).get$ref());
  var insuranceTarget=new ModelConverters().resolveAsResolvedSchema(new AnnotatedType(io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCurrentContracts.Target.class).resolveAsRef(true));
  var currencyTarget=new ModelConverters().resolveAsResolvedSchema(new AnnotatedType(io.fireflysoftware.masters.interfaces.dtos.currency.policy.CurrencyPolicyCurrentContracts.Target.class).resolveAsRef(true));
  assertEquals("#/components/schemas/Target",insuranceTarget.schema.get$ref());
  assertEquals("#/components/schemas/CurrencyPolicyCurrentTarget",currencyTarget.schema.get$ref());
 }
}
