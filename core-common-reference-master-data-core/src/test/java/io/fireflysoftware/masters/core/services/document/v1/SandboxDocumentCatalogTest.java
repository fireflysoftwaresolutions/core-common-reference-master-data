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
package io.fireflysoftware.masters.core.services.document.v1;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.fireflyframework.utils.template.RestrictedHtmlTemplateRenderer;
import org.fireflyframework.validators.schema.OfflineJsonSchemaValidator;
import org.junit.jupiter.api.Test;

class SandboxDocumentCatalogTest {
  private static final Path ROOT = Path.of("../sandbox/documents").toAbsolutePath().normalize();
  private static final ObjectMapper JSON =
      new ObjectMapper()
          .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
          .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS);

  @Test
  void financialSpecimensReconcileAndUseConsistentPolicyDates() throws Exception {
    var statement = facts("bank_account_statement");
    BigDecimal running = money(statement, "openingBalance");
    BigDecimal credits = BigDecimal.ZERO;
    BigDecimal debits = BigDecimal.ZERO;
    LocalDate start = LocalDate.parse(statement.path("periodStart").asText());
    LocalDate end = LocalDate.parse(statement.path("periodEnd").asText());
    LocalDate previous = start;
    for (JsonNode movement : statement.path("movements")) {
      LocalDate date = LocalDate.parse(movement.path("date").asText());
      assertThat(date).isAfterOrEqualTo(previous).isAfterOrEqualTo(start).isBeforeOrEqualTo(end);
      previous = date;
      assertThat(money(movement, "debit").signum() + money(movement, "credit").signum())
          .as("Exactly one positive booked debit or credit")
          .isEqualTo(1);
      credits = credits.add(money(movement, "credit"));
      debits = debits.add(money(movement, "debit"));
      running = running.add(money(movement, "credit")).subtract(money(movement, "debit"));
      assertThat(running).isEqualByComparingTo(money(movement, "balance"));
    }
    assertThat(running).isEqualByComparingTo(money(statement, "closingBalance"));
    assertThat(credits).isEqualByComparingTo(money(statement, "totalCredits"));
    assertThat(debits).isEqualByComparingTo(money(statement, "totalDebits"));

    var loan = facts("loan_agreement");
    BigDecimal remaining = money(loan, "principal");
    BigDecimal paid = BigDecimal.ZERO;
    int number = 1;
    LocalDate due = LocalDate.parse(loan.path("firstDueDate").asText());
    for (JsonNode installment : loan.path("schedule")) {
      assertThat(installment.path("number").asInt()).isEqualTo(number);
      if (number < loan.path("schedule").size()) {
        assertThat(money(installment, "payment")).isEqualByComparingTo(money(loan, "installment"));
      }
      assertThat(LocalDate.parse(installment.path("dueDate").asText())).isEqualTo(due);
      assertThat(money(installment, "openingPrincipal")).isEqualByComparingTo(remaining);
      assertThat(money(installment, "interest"))
          .isEqualByComparingTo(
              remaining
                  .multiply(new BigDecimal("0.01"))
                  .setScale(2, java.math.RoundingMode.HALF_UP));
      assertThat(money(installment, "payment"))
          .isEqualByComparingTo(
              money(installment, "principal").add(money(installment, "interest")));
      remaining = remaining.subtract(money(installment, "principal"));
      assertThat(money(installment, "closingPrincipal")).isEqualByComparingTo(remaining);
      paid = paid.add(money(installment, "payment"));
      due = due.plusMonths(1);
      number++;
    }
    assertThat(remaining).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(paid).isEqualByComparingTo(money(loan, "totalPayments"));
    assertThat(paid.subtract(money(loan, "principal")))
        .isEqualByComparingTo(money(loan, "financeCharge"));
    assertThat(due.minusMonths(1)).isEqualTo(LocalDate.parse(loan.path("finalDueDate").asText()));

    var policy = facts("insurance_policy_declarations");
    var invoice = facts("insurance_premium_invoice");
    assertThat(money(policy, "annualPremium").add(money(policy, "tax")))
        .isEqualByComparingTo(money(policy, "totalPremium"));
    assertThat(money(invoice, "subtotal")).isEqualByComparingTo(money(policy, "annualPremium"));
    assertThat(money(invoice, "tax")).isEqualByComparingTo(money(policy, "tax"));
    BigDecimal charges = BigDecimal.ZERO;
    for (JsonNode charge : invoice.path("charges")) charges = charges.add(money(charge, "amount"));
    assertThat(charges).isEqualByComparingTo(money(invoice, "subtotal"));
    assertThat(charges.add(money(invoice, "tax"))).isEqualByComparingTo(money(invoice, "total"));
    assertThat(money(invoice, "total").subtract(money(invoice, "paid")))
        .isEqualByComparingTo(money(invoice, "outstanding"));
    assertThat(money(invoice, "total")).isEqualByComparingTo(money(policy, "totalPremium"));
    assertThat(invoice.path("policyNumber").asText())
        .isEqualTo(policy.path("policyNumber").asText());

    var claim = facts("insurance_claim_settlement");
    assertThat(policy.path("deductibleRuleCode").asText()).isEqualTo("HIGHEST_APPLICABLE_ONCE");
    assertThat(claim.path("deductibleRuleCode").asText())
        .isEqualTo(policy.path("deductibleRuleCode").asText());
    assertThat(claim.path("deductibleRuleReference").asText())
        .isEqualTo(policy.path("wordingReference").asText() + " / shared-occurrence-deductible");
    BigDecimal highestApplicableDeductible = BigDecimal.ZERO;
    BigDecimal approved = BigDecimal.ZERO;
    for (JsonNode item : claim.path("assessment")) {
      approved = approved.add(money(item, "approved"));
      JsonNode applicable = null;
      for (JsonNode coverage : policy.path("coverages")) {
        if (coverage.path("code").asText().equals(item.path("coverageCode").asText())) {
          assertThat(applicable).as("Unique scheduled coverage").isNull();
          applicable = coverage;
        }
      }
      assertThat(applicable).as("Assessment links to scheduled coverage").isNotNull();
      assertThat(money(item, "approved")).isLessThanOrEqualTo(money(applicable, "limit"));
      highestApplicableDeductible =
          highestApplicableDeductible.max(money(applicable, "deductible"));
    }
    assertThat(money(claim, "deductible")).isEqualByComparingTo(highestApplicableDeductible);
    assertThat(approved).isLessThanOrEqualTo(money(policy, "occurrenceLimit"));
    assertThat(approved).isEqualByComparingTo(money(claim, "grossApproved"));
    assertThat(
            approved
                .subtract(money(claim, "deductible"))
                .subtract(money(claim, "retainedDepreciation"))
                .subtract(money(claim, "priorPayments")))
        .isEqualByComparingTo(money(claim, "netSettlement"));
    assertThat(claim.path("policyNumber").asText()).isEqualTo(policy.path("policyNumber").asText());
    LocalDate loss = LocalDate.parse(claim.path("lossDate").asText());
    assertThat(loss)
        .isAfterOrEqualTo(LocalDate.parse(policy.path("effectiveDate").asText()))
        .isBeforeOrEqualTo(LocalDate.parse(policy.path("expiryDate").asText()));
    assertThat(LocalDate.parse(claim.path("issuedOn").asText())).isAfterOrEqualTo(loss);
  }

  private static JsonNode facts(String name) throws Exception {
    return JSON.readTree(Files.readString(ROOT.resolve("examples/" + name + ".json")));
  }

  private static BigDecimal money(JsonNode node, String name) {
    return new BigDecimal(node.path(name).asText());
  }

  @Test
  void productCustomerAndCertificateFamiliesHaveOwnedFactsAndReconciledHistoricalAmounts()
      throws Exception {
    var catalog = JSON.readTree(Files.readString(ROOT.resolve("catalog.json")));
    var codes = new HashSet<String>();
    catalog.path("templates").forEach(entry -> codes.add(entry.path("templateCode").asText()));
    assertThat(codes)
        .contains(
            "SBX_DEPOSIT_PRODUCT_AGREEMENT",
            "SBX_CUSTOMER_PROFILE",
            "SBX_ACCOUNT_OWNERSHIP_CERTIFICATE",
            "SBX_ACCOUNT_BALANCE_CERTIFICATE",
            "SBX_FINANCIAL_BALANCE_SHEET");
    var agreement = facts("deposit_product_agreement");
    var customer = facts("customer_profile");
    var title = facts("account_ownership_certificate");
    var balance = facts("account_balance_certificate");
    assertThat(agreement.path("executionStatus").asText()).isEqualTo("UNSIGNED_SANDBOX");
    assertThat(agreement.path("resourceReference")).isEqualTo(agreement.path("contractReference"));
    assertThat(customer.path("resourceReference")).isEqualTo(customer.path("partyReference"));
    assertThat(title.path("resourceReference")).isEqualTo(title.path("accountReference"));
    assertThat(balance.path("resourceReference")).isEqualTo(balance.path("accountReference"));
    assertThat(agreement.path("partyReference")).isEqualTo(customer.path("partyReference"));
    assertThat(title.path("holders").get(0).path("partyReference"))
        .isEqualTo(customer.path("partyReference"));
    assertThat(agreement.path("accountReference")).isEqualTo(title.path("accountReference"));
    assertThat(balance.path("accountReference")).isEqualTo(title.path("accountReference"));
    assertThat(balance.path("asOf")).isEqualTo(title.path("asOf"));
    assertThat(
            java.time.Instant.parse(balance.path("asOf").asText())
                .atOffset(java.time.ZoneOffset.UTC)
                .toLocalDate())
        .isEqualTo(LocalDate.parse(facts("bank_account_statement").path("periodEnd").asText()));
    assertThat(money(balance, "bookedBalance"))
        .isEqualByComparingTo(money(facts("bank_account_statement"), "closingBalance"));
    assertThat(money(balance, "bookedBalance").subtract(money(balance, "heldAmount")))
        .isEqualByComparingTo(money(balance, "cashAvailable"));
    for (var holder : title.path("holders")) {
      assertThat(LocalDate.parse(holder.path("validFrom").asText()))
          .isBeforeOrEqualTo(LocalDate.parse(balance.path("valueDate").asText()));
    }
    assertThat(LocalDate.parse(customer.path("lastReviewDate").asText()))
        .isBeforeOrEqualTo(LocalDate.parse(customer.path("reviewDueDate").asText()));
    for (String name :
        List.of(
            "deposit_product_agreement",
            "customer_profile",
            "account_ownership_certificate",
            "account_balance_certificate",
            "financial_balance_sheet")) {
      var example = facts(name);
      assertThat(example.path("sources").size()).isPositive();
      for (var source : example.path("sources")) {
        assertThat(source.path("version").asText()).isNotBlank();
        assertThat(java.time.Instant.parse(source.path("observedAt").asText())).isNotNull();
      }
    }
    var sheet = facts("financial_balance_sheet");
    for (String key : List.of("current", "comparative")) {
      var p = sheet.path(key);
      assertThat(money(p, "cash").add(money(p, "customerLoans")).add(money(p, "investments")))
          .isEqualByComparingTo(money(p, "totalAssets"));
      assertThat(money(p, "customerDeposits").add(money(p, "borrowings")))
          .isEqualByComparingTo(money(p, "totalLiabilities"));
      assertThat(money(p, "shareCapital").add(money(p, "retainedEarnings")))
          .isEqualByComparingTo(money(p, "totalEquity"));
      assertThat(money(p, "totalLiabilities").add(money(p, "totalEquity")))
          .isEqualByComparingTo(money(p, "totalAssets"));
      assertThat(money(p, "totalLiabilitiesAndEquity"))
          .isEqualByComparingTo(money(p, "totalAssets"));
    }
  }

  @Test
  void unsignedFinancialRolesRejectNegativeValuesButAccountBalancesRemainSigned() throws Exception {
    var invalidFields =
        Map.of(
            "bank_account_statement",
                List.of(
                    "/totalDebits", "/totalCredits", "/movements/0/debit", "/movements/0/credit"),
            "loan_agreement",
                List.of(
                    "/principal",
                    "/financeCharge",
                    "/totalPayments",
                    "/installment",
                    "/lateFee",
                    "/schedule/0/principal",
                    "/schedule/0/interest",
                    "/schedule/0/payment",
                    "/schedule/0/openingPrincipal",
                    "/schedule/0/closingPrincipal"),
            "insurance_policy_declarations",
                List.of(
                    "/annualPremium",
                    "/occurrenceLimit",
                    "/tax",
                    "/totalPremium",
                    "/coverages/0/limit",
                    "/coverages/0/deductible"),
            "insurance_premium_invoice",
                List.of(
                    "/subtotal", "/tax", "/total", "/paid", "/outstanding", "/charges/0/amount"),
            "insurance_claim_settlement",
                List.of(
                    "/grossApproved",
                    "/deductible",
                    "/retainedDepreciation",
                    "/priorPayments",
                    "/netSettlement",
                    "/assessment/0/approved"),
            "deposit_product_agreement",
                List.of("/monthlyFee", "/feeWaiverBalance", "/overdraftLimit"),
            "account_balance_certificate", List.of("/heldAmount"),
            "financial_balance_sheet",
                List.of(
                    "/current/cash",
                    "/current/customerLoans",
                    "/current/investments",
                    "/current/totalAssets",
                    "/current/customerDeposits",
                    "/current/borrowings",
                    "/current/totalLiabilities",
                    "/current/shareCapital",
                    "/comparative/cash",
                    "/comparative/totalAssets"));
    var validator = new OfflineJsonSchemaValidator();
    for (var entry : invalidFields.entrySet()) {
      String schema = Files.readString(ROOT.resolve("schemas/" + entry.getKey() + ".json"));
      for (String pointer : entry.getValue()) {
        var negative = facts(entry.getKey());
        assertThat(negative.at(pointer).isTextual())
            .as("Existing monetary fact " + pointer)
            .isTrue();
        int separator = pointer.lastIndexOf('/');
        ((com.fasterxml.jackson.databind.node.ObjectNode)
                negative.at(pointer.substring(0, separator)))
            .put(pointer.substring(separator + 1), "-1.00");
        assertThat(validator.validateInstance(schema, negative.toString()).valid())
            .as(entry.getKey() + pointer + " cannot be negative")
            .isFalse();
      }
    }
    String schema = Files.readString(ROOT.resolve("schemas/bank_account_statement.json"));
    var overdrawn = facts("bank_account_statement");
    ((com.fasterxml.jackson.databind.node.ObjectNode) overdrawn).put("openingBalance", "-1.00");
    ((com.fasterxml.jackson.databind.node.ObjectNode) overdrawn).put("closingBalance", "-2.00");
    ((com.fasterxml.jackson.databind.node.ObjectNode) overdrawn.at("/movements/0"))
        .put("balance", "-3.00");
    assertThat(validator.validateInstance(schema, overdrawn.toString()).valid()).isTrue();
  }

  @Test
  void catalogExamplesSatisfyTheirExactSchemaAndRestrictedRenderer() throws Exception {
    JsonNode catalog = JSON.readTree(Files.readString(ROOT.resolve("catalog.json")));
    assertThat(catalog.path("scope").asText()).isEqualTo("SANDBOX_ONLY");
    assertThat(catalog.path("localeCode").asText()).isEqualTo("en-US");
    assertThat(catalog.path("templates").isArray()).isTrue();
    assertThat(catalog.path("templates").size()).isPositive();
    var identities = new HashSet<String>();
    var files = new HashSet<Path>();
    var validator = new OfflineJsonSchemaValidator();
    var renderer = new RestrictedHtmlTemplateRenderer();
    for (JsonNode entry : catalog.path("templates")) {
      String code = entry.path("templateCode").asText();
      assertThat(code).matches("SBX_[A-Z][A-Z0-9_]{0,45}");
      assertThat(identities.add(code)).isTrue();
      assertThat(entry.path("version").asText()).isEqualTo("1.0.0");
      assertThat(entry.path("factsContractVersion").asText()).isEqualTo("1");
      assertThat(entry.path("businessPrerequisites").size()).isPositive();
      String schema = source(entry, "schema", files);
      String template = source(entry, "template", files);
      String example = source(entry, "example", files);
      assertThat(template.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)
          .isLessThan(10000);
      assertThat(validator.validateSchema(schema).valid()).as(code + " schema").isTrue();
      assertThat(validator.validateInstance(schema, example).valid()).as(code + " facts").isTrue();
      var facts = JSON.readTree(example);
      var unknown = facts.deepCopy();
      ((com.fasterxml.jackson.databind.node.ObjectNode) unknown).put("unexpectedField", true);
      assertThat(validator.validateInstance(schema, unknown.toString()).valid()).isFalse();
      var incomplete = facts.deepCopy();
      ((com.fasterxml.jackson.databind.node.ObjectNode) incomplete).remove("documentReference");
      assertThat(validator.validateInstance(schema, incomplete.toString()).valid()).isFalse();
      Map<String, Object> model = Map.of("facts", JSON.convertValue(facts, Map.class));
      var rendered = renderer.render(template, model);
      assertThat(rendered.success()).as(code + " rendered " + rendered.code()).isTrue();
      assertThat(rendered.html())
          .contains("Sandbox specimen", facts.path("documentReference").asText());
      ((com.fasterxml.jackson.databind.node.ObjectNode) facts)
          .put("partyName", "<script>unsafe & value</script>");
      var escaped = renderer.render(template, Map.of("facts", JSON.convertValue(facts, Map.class)));
      assertThat(escaped.success()).as(code + " escaped rendering").isTrue();
      assertThat(escaped.html())
          .contains("&lt;script&gt;unsafe &amp; value&lt;/script&gt;")
          .doesNotContain("<script>");
    }
    for (String directory : new String[] {"templates", "schemas", "examples"}) {
      try (var paths = Files.list(ROOT.resolve(directory))) {
        assertThat(paths.filter(Files::isRegularFile).map(Path::toAbsolutePath).toList())
            .allMatch(files::contains);
      }
    }
  }

  private static String source(JsonNode entry, String field, HashSet<Path> files) throws Exception {
    Path path = ROOT.resolve(entry.path(field).asText()).normalize();
    assertThat(path).startsWith(ROOT);
    assertThat(Files.isRegularFile(path)).isTrue();
    assertThat(files.add(path)).isTrue();
    return Files.readString(path);
  }
}
