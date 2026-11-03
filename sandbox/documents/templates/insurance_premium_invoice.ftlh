<section class="document-page">
<h1>Premium invoice</h1>
<p class="sandbox-label">
<strong>Sandbox specimen</strong> · Fictional institution and customer data.</p>
<p class="document-reference">${facts.documentReference} · ${facts.issuedOn}</p>
<dl class="key-details">
<div>
<dt>Invoice</dt>
<dd>${facts.invoiceNumber}</dd>
</div>
<div>
<dt>Billed to</dt>
<dd>${facts.partyName}</dd>
</div>
<div>
<dt>Policy reference</dt>
<dd>${facts.policyNumber}</dd>
</div>
<div>
<dt>Issue date</dt>
<dd>${facts.issuedOn}</dd>
</div>
<div>
<dt>Payment due</dt>
<dd>${facts.dueDate}</dd>
</div>
<div>
<dt>Currency</dt>
<dd>${facts.currency}</dd>
</div>
</dl>
<h2>Premium charges</h2>
<table>
<thead>
<tr>
<th scope="col">Description</th>
<th scope="col">Amount</th>
</tr>
</thead>
<tbody>
<#list facts.charges as row>
<tr>
<td>${row.description}</td>
<td class="amount">${row.amount?number?string["#,##0.00"]}</td>
</tr>
</#list>
</tbody>
</table>
<div class="totals">
<dl class="key-details">
<div>
<dt>Premium subtotal</dt>
<dd>${facts.subtotal?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Illustrative tax</dt>
<dd>${facts.tax?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Invoice total</dt>
<dd>${facts.total?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Allocated payments</dt>
<dd>${facts.paid?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Amount due</dt>
<dd>${facts.outstanding?number?string["#,##0.00"]}</dd>
</div>
</dl>
</div>
<h2>Payment information</h2>
<p>${facts.paymentInstructions}</p>
<p>Quote ${facts.paymentReference} with your payment. An invoice or a submitted payment instruction is not a receipt. The owning payment and allocation services establish what was received and applied.</p>
<h2>Premium and coverage</h2>
<p>Coverage follows the separately issued policy and its authorized binding conditions.</p>
<div class="document-legal-note">
<strong>Demonstration document.</strong> Firefly Sandbox Financial Services is a fictional demonstration entity. This specimen is not a customer agreement, insurance coverage, payment confirmation, or jurisdiction-approved legal form. Replace its approved wording, entity details, disclosures, and product terms through a new governed revision before customer use.</div>
</section>
