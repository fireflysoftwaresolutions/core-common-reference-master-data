package io.fireflysoftware.masters.web.controllers.insurance.reference;

import static org.junit.jupiter.api.Assertions.*;
import io.fireflysoftware.masters.interfaces.dtos.insurance.reference.InsuranceReferenceCommands.*;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.media.Schema;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InsuranceReferenceSchemaTest {
    @Test void actualCommandSchemasAreClosedAndRequireTheirExactSelectors() {
        for (var type : java.util.List.of(SubmitPublication.class, SubmitWithdrawal.class, IntentAction.class, Recovery.class)) {
            var schemas = new ModelConverters().readAll(type);
            var schema = schemas.get(type.getSimpleName());
            assertEquals(Boolean.FALSE, schema.getAdditionalProperties(), type.getSimpleName());
            var required = Set.copyOf(schema.getRequired());
            assertTrue(required.contains("actionId"));
            assertFalse(schema.getProperties().containsKey("actor"));
            assertFalse(schema.getProperties().containsKey("authority"));
        }
        var action = new ModelConverters().readAll(IntentAction.class).get("IntentAction");
        assertEquals(Set.of("actionId", "recordId", "kind", "intentId", "intentDigest", "expectedIntentGeneration"), Set.copyOf(action.getRequired()));
        var generation = (Schema<?>) action.getProperties().get("expectedIntentGeneration");
        assertEquals(0, generation.getMinimum().intValueExact());
    }

    @Test void actualDraftDefinesBothClosedConcreteBodiesWithoutInventedServerAllocations() {
        var schemas = new ModelConverters().readAll(SubmitPublication.class);
        var draft = schemas.get("PublicationDraft");
        assertEquals(Boolean.FALSE, draft.getAdditionalProperties());
        assertFalse(draft.getProperties().containsKey("publicationId"));
        assertFalse(draft.getRequired().contains("effectiveUntil"));
        var body = schemas.get("Body");
        assertEquals(Set.of("#/components/schemas/PremiumTax", "#/components/schemas/Wording"),
                Set.copyOf(body.getOneOf().stream().map(value -> ((Schema<?>) value).get$ref()).toList()));
        for (var name : java.util.List.of("PremiumTax", "Wording", "Provenance", "Applicability", "Head")) {
            assertEquals(Boolean.FALSE, schemas.get(name).getAdditionalProperties(), name);
            assertNotNull(schemas.get(name).getRequired(), name);
        }
        assertFalse(schemas.get("Head").getRequired().contains("publicationId"));
        assertNotNull(schemas.get("PremiumTax").getProperties().get("currencyPolicy"));
        assertEquals(Boolean.FALSE, schemas.get("CurrencyPolicyReference").getAdditionalProperties());
        assertEquals(Set.of("policyId", "currencyId", "isoCode", "version", "digest", "encodingVersion"),
                Set.copyOf(schemas.get("CurrencyPolicyReference").getRequired()));
    }
}
