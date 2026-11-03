<section class="document-page">
<h1>Claim settlement statement</h1>
<p class="sandbox-label">
<strong>Sandbox specimen</strong> · Fictional institution and customer data.</p>
<p class="document-reference">${facts.documentReference} · ${facts.issuedOn}</p>
<dl class="key-details">
<div>
<dt>Claim</dt>
<dd>${facts.claimNumber}</dd>
</div>
<div>
<dt>Policy</dt>
<dd>${facts.policyNumber}</dd>
</div>
<div>
<dt>Claimant</dt>
<dd>${facts.partyName}</dd>
</div>
<div>
<dt>Date of loss</dt>
<dd>${facts.lossDate}</dd>
</div>
<div>
<dt>Approved decision</dt>
<dd>${facts.decisionReference}</dd>
</div>
</dl>
<h2>Approved assessment</h2>
<table>
<thead>
<tr>
<th scope="col">Assessment item</th>
<th scope="col">Approved amount</th>
</tr>
</thead>
<tbody>
<#list facts.assessment as row>
<tr>
<td>${row.description}</td>
<td class="amount">${row.approved?number?string["#,##0.00"]}</td>
</tr>
</#list>
</tbody>
</table>
<div class="totals">
<dl class="key-details">
<div>
<dt>Approved gross amount</dt>
<dd>${facts.grossApproved?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Deductible</dt>
<dd>${facts.deductible?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Retained depreciation</dt>
<dd>${facts.retainedDepreciation?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Prior payments</dt>
<dd>${facts.priorPayments?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Remaining settlement</dt>
<dd>${facts.netSettlement?number?string["#,##0.00"]}</dd>
</div>
</dl>
</div>
<dl class="key-details">
<div>
<dt>Financial status</dt>
<dd>${facts.settlementState}</dd>
</div>
<div>
<dt>Payment reference</dt>
<dd>${facts.paymentReference}</dd>
</div>
<div>
<dt>Assessment evidence</dt>
<dd>${facts.evidenceReference}</dd>
</div>
</dl>
<p>The remaining settlement equals approved gross less the stated deductible, retained depreciation, and prior payments. Payment status must come from the actual payment and ledger records.</p>
<div class="document-legal-note">
<strong>Demonstration document.</strong> Firefly Sandbox Financial Services is a fictional demonstration entity. This specimen is not a customer agreement, insurance coverage, payment confirmation, or jurisdiction-approved legal form. Replace its approved wording, entity details, disclosures, and product terms through a new governed revision before customer use.</div>
</section>
<section class="document-page">
<h1>Settlement explanation and review</h1>
<p class="sandbox-label">
<strong>Sandbox specimen</strong> · Fictional institution and customer data.</p>
<p class="document-reference">${facts.documentReference} · ${facts.issuedOn}</p>
<p>${facts.claimNumber} · ${facts.partyName}</p>
<h2>Decision and calculation</h2>
<p>The referenced approved decision and evidence determine the amounts shown. This illustration does not determine coverage, adjust a reserve, authorize settlement, or discharge an obligation.</p>
<h2>Deductible basis</h2>
<p>The assessed building and contents damage arises from one occurrence. The shared-occurrence rule applies the higher scheduled deductible once: ${facts.deductible?number?string["#,##0.00"]} ${facts.currency}. It does not add both deductibles. Wording reference: ${facts.deductibleRuleReference}.</p>
<h2>Retained depreciation</h2>
<p>The displayed retained amount is excluded from the current remaining settlement. Any later release requires the applicable policy terms, requested completion evidence, a new authorized decision and a linked payment record.</p>
<h2>Payment and receipt</h2>
<p>No payment is instructed by this specimen. A genuine settlement communication must identify the actual financial status and retained provider/ledger references. A timeout or submitted instruction is not proof of payment.</p>
<h2>Questions and review</h2>
<p>Use the configured claims-service or complaint channel and quote the claim and decision references. Applicable review rights, deadlines, escalation contacts, and approved legal wording must be supplied for the project’s jurisdiction.</p>
<div class="document-legal-note">
<strong>Demonstration document.</strong> Firefly Sandbox Financial Services is a fictional demonstration entity. This specimen is not a customer agreement, insurance coverage, payment confirmation, or jurisdiction-approved legal form. Replace its approved wording, entity details, disclosures, and product terms through a new governed revision before customer use.</div>
</section>
