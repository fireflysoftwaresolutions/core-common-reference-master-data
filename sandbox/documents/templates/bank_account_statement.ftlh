<section class="document-page">
<h1>Account statement</h1>
<p class="sandbox-label">
<strong>Sandbox specimen</strong> · Fictional institution and customer data.</p>
<p class="document-reference">${facts.documentReference} · ${facts.issuedOn}</p>
<p>${facts.partyName}</p>
<dl class="key-details">
<div>
<dt>Account</dt>
<dd>${facts.accountNumber}</dd>
</div>
<div>
<dt>Statement from</dt>
<dd>${facts.periodStart}</dd>
</div>
<div>
<dt>Statement through</dt>
<dd>${facts.periodEnd}</dd>
</div>
<div>
<dt>Currency</dt>
<dd>${facts.currency}</dd>
</div>
</dl>
<div class="balance-summary">
<dl class="key-details">
<div>
<dt>Opening balance</dt>
<dd>${facts.openingBalance?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Money in</dt>
<dd>${facts.totalCredits?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Money out</dt>
<dd>${facts.totalDebits?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Closing balance</dt>
<dd>${facts.closingBalance?number?string["#,##0.00"]}</dd>
</div>
</dl>
</div>
<h2>Account activity</h2>
<table>
<thead>
<tr>
<th scope="col">Date</th>
<th scope="col">Description</th>
<th scope="col">Money out</th>
<th scope="col">Money in</th>
<th scope="col">Balance</th>
</tr>
</thead>
<tbody>
<#list facts.movements as row>
<tr>
<td>${row.date}</td>
<td>${row.description}</td>
<td class="amount">${row.debit?number?string["#,##0.00"]}</td>
<td class="amount">${row.credit?number?string["#,##0.00"]}</td>
<td class="amount">${row.balance?number?string["#,##0.00"]}</td>
</tr>
</#list>
</tbody>
</table>
<div class="document-legal-note">
<strong>Demonstration document.</strong> Firefly Sandbox Financial Services is a fictional demonstration entity. This specimen is not a customer agreement, insurance coverage, payment confirmation, or jurisdiction-approved legal form. Replace its approved wording, entity details, disclosures, and product terms through a new governed revision before customer use.</div>
</section>
<section class="document-page">
<h1>Statement notes and service information</h1>
<p class="sandbox-label">
<strong>Sandbox specimen</strong> · Fictional institution and customer data.</p>
<p class="document-reference">${facts.documentReference} · ${facts.issuedOn}</p>
<p>${facts.partyName} · ${facts.accountNumber}</p>
<h2>Your statement</h2>
<p>The running balance reflects the booked movements shown for this statement period. Pending authorizations and reservations are not included in this closing booked balance.</p>
<p>${facts.rateNote}</p>
<h2>Reviewing your account activity</h2>
<p>Compare the references, dates, and amounts with your own records. For a transaction you do not recognize, retain its reference and use the institution’s configured error-reporting channel. The applicable account agreement and jurisdiction-specific disclosure define reporting deadlines and the investigation process.</p>
<h2>Fees and interest</h2>
<p>The service fee appearing in the activity table is an explicit synthetic booked movement. No interest is accrued for this example account. The statement does not create a fee, accrue interest, or change available funds.</p>
<h2>Balances and reservations</h2>
<p>A booked balance records completed ledger effects. Available funds may also reflect active reservations and account controls. A released reservation is not a credit, and a reversed posting remains linked to the original record.</p>
<h2>Contact and document reference</h2>
<p>${facts.contactReference}</p>
<p>Quote ${facts.documentReference} and the account reference when contacting support. Do not send passwords, full card numbers, or authentication codes.</p>
<div class="document-legal-note">
<strong>Demonstration document.</strong> Firefly Sandbox Financial Services is a fictional demonstration entity. This specimen is not a customer agreement, insurance coverage, payment confirmation, or jurisdiction-approved legal form. Replace its approved wording, entity details, disclosures, and product terms through a new governed revision before customer use.</div>
</section>
