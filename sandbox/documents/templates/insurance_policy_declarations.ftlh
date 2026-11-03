<section class="document-page">
<h1>Commercial property policy declarations</h1>
<p class="sandbox-label">
<strong>Sandbox specimen</strong> · Fictional institution and customer data.</p>
<p class="document-reference">${facts.documentReference} · ${facts.issuedOn}</p>
<dl class="key-details">
<div>
<dt>Policyholder</dt>
<dd>${facts.partyName}</dd>
</div>
<div>
<dt>Policy</dt>
<dd>${facts.policyNumber}</dd>
</div>
<div>
<dt>Insured premises</dt>
<dd>${facts.riskAddress}</dd>
</div>
<div>
<dt>Coverage from</dt>
<dd>${facts.effectiveDate}</dd>
</div>
<div>
<dt>Coverage through</dt>
<dd>${facts.expiryDate}</dd>
</div>
<div>
<dt>Policy status</dt>
<dd>${facts.policyState}</dd>
</div>
</dl>
<h2>Coverage schedule</h2>
<table>
<thead>
<tr>
<th scope="col">Coverage</th>
<th scope="col">Limit</th>
<th scope="col">Deductible</th>
<th scope="col">Basis</th>
</tr>
</thead>
<tbody>
<#list facts.coverages as row>
<tr>
<td>${row.coverage}</td>
<td class="amount">${row.limit?number?string["#,##0.00"]}</td>
<td class="amount">${row.deductible?number?string["#,##0.00"]}</td>
<td>${row.basis}</td>
</tr>
</#list>
</tbody>
</table>
<p>All limits are in ${facts.currency}. The listed coverages are subject to a shared occurrence limit of ${facts.occurrenceLimit?number?string["#,##0.00"]} ${facts.currency}; adding the row limits does not increase that shared limit.</p>
<dl class="key-details">
<div>
<dt>Premium before tax</dt>
<dd>${facts.annualPremium?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Illustrative tax</dt>
<dd>${facts.tax?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Total premium</dt>
<dd>${facts.totalPremium?number?string["#,##0.00"]}</dd>
</div>
</dl>
<div class="document-legal-note">
<strong>Demonstration document.</strong> Firefly Sandbox Financial Services is a fictional demonstration entity. This specimen is not a customer agreement, insurance coverage, payment confirmation, or jurisdiction-approved legal form. Replace its approved wording, entity details, disclosures, and product terms through a new governed revision before customer use.</div>
</section>
<section class="document-page">
<h1>Policy conditions and endorsements</h1>
<p class="sandbox-label">
<strong>Sandbox specimen</strong> · Fictional institution and customer data.</p>
<p class="document-reference">${facts.documentReference} · ${facts.issuedOn}</p>
<dl class="key-details">
<div>
<dt>Policy</dt>
<dd>${facts.policyNumber}</dd>
</div>
<div>
<dt>Wording version</dt>
<dd>${facts.wordingReference}</dd>
</div>
<div>
<dt>Endorsements</dt>
<dd>${facts.endorsements}</dd>
</div>
<div>
<dt>Distributor</dt>
<dd>${facts.brokerReference}</dd>
</div>
</dl>
<h2>Coverage and insured interests</h2>
<p>The approved wording must identify insured parties, property, perils, exclusions, valuation and effective dates. These fictional declarations do not replace that wording.</p>
<h2>Shared occurrence deductible</h2>
<p>For this sandbox product, one occurrence affecting buildings and contents uses the highest scheduled deductible once against the combined property assessment. Do not add deductibles. Wording reference: ${facts.wordingReference} / shared-occurrence-deductible.</p>
<h2>Claims notification and evidence</h2>
<p>Report an incident through the configured claims channel, retain the loss reference, and take reasonable steps to prevent further loss where safe. Requested evidence, coverage decisions, reserves, and settlement authority are separate recorded steps.</p>
<h2>Premium and changes</h2>
<p>A premium invoice and its payment allocation do not independently establish coverage. Binding, endorsements, renewal, lapse, cancellation, and refunds require their own authorized effective-dated policy transitions and documents.</p>
<h2>Exclusions and sublimits</h2>
<p>Do not infer a peril is covered because it is absent from this illustration. Approved project wording must state exclusions and applicable sublimits explicitly. Endorsements must identify the wording and policy version they amend.</p>
<h2>Service and complaints</h2>
<p>Use the institution’s configured policy-service and complaints channels. A complaint decision does not itself change coverage or authorize a payment.</p>
<div class="document-legal-note">
<strong>Demonstration document.</strong> Firefly Sandbox Financial Services is a fictional demonstration entity. This specimen is not a customer agreement, insurance coverage, payment confirmation, or jurisdiction-approved legal form. Replace its approved wording, entity details, disclosures, and product terms through a new governed revision before customer use.</div>
</section>
