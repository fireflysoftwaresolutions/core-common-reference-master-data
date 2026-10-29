<section class="document-page">
<h1>Installment loan agreement</h1>
<p class="sandbox-label">
<strong>Sandbox specimen</strong> · Fictional institution and customer data.</p>
<p class="document-reference">${facts.documentReference} · ${facts.issuedOn}</p>
<p>Borrower: ${facts.partyName}</p>
<dl class="key-details">
<div>
<dt>Agreement</dt>
<dd>${facts.agreementNumber}</dd>
</div>
<div>
<dt>Principal</dt>
<dd>${facts.principal?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Currency</dt>
<dd>${facts.currency}</dd>
</div>
<div>
<dt>Annual fixed interest rate</dt>
<dd>${facts.annualRate}</dd>
</div>
<div>
<dt>Illustrative APR</dt>
<dd>${facts.aprDisplay}</dd>
</div>
<div>
<dt>Finance charge</dt>
<dd>${facts.financeCharge?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Total payments</dt>
<dd>${facts.totalPayments?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Term</dt>
<dd>${facts.term}</dd>
</div>
<div>
<dt>Monthly installment</dt>
<dd>${facts.installment?number?string["#,##0.00"]}</dd>
</div>
<div>
<dt>Effective date</dt>
<dd>${facts.effectiveDate}</dd>
</div>
</dl>
<h2>1. Credit advanced</h2>
<p>This example records a fixed-rate advance of ${facts.principal?number?string["#,##0.00"]} ${facts.currency}. Disbursement is a separate financial operation and requires its own authoritative payment and ledger receipt. Creating or signing this document does not disburse funds.</p>
<h2>2. Repayment obligation</h2>
<p>The scheduled monthly installment is ${facts.installment?number?string["#,##0.00"]} ${facts.currency}, beginning ${facts.firstDueDate} and ending ${facts.finalDueDate}. The final installment is adjusted as shown in the schedule to extinguish the remaining principal. The complete schedule appears on the following page.</p>
<div class="document-legal-note">
<strong>Demonstration document.</strong> Firefly Sandbox Financial Services is a fictional demonstration entity. This specimen is not a customer agreement, insurance coverage, payment confirmation, or jurisdiction-approved legal form. Replace its approved wording, entity details, disclosures, and product terms through a new governed revision before customer use.</div>
</section>
<section class="document-page">
<h1>Repayment schedule</h1>
<p class="sandbox-label">
<strong>Sandbox specimen</strong> · Fictional institution and customer data.</p>
<p class="document-reference">${facts.documentReference} · ${facts.issuedOn}</p>
<dl class="key-details">
<div>
<dt>Agreement</dt>
<dd>${facts.agreementNumber}</dd>
</div>
<div>
<dt>Customer name</dt>
<dd>${facts.partyName}</dd>
</div>
<div>
<dt>Repayment account</dt>
<dd>${facts.repaymentReference}</dd>
</div>
</dl>
<h2>Scheduled installments</h2>
<table class="repayment-schedule">
<thead>
<tr>
<th scope="col">No.</th>
<th scope="col">Due date</th>
<th scope="col">Opening</th>
<th scope="col">Principal</th>
<th scope="col">Interest</th>
<th scope="col">Payment</th>
<th scope="col">Remaining</th>
</tr>
</thead>
<tbody>
<#list facts.schedule as row>
<tr>
<td>${row.number}</td>
<td>${row.dueDate}</td>
<td class="amount">${row.openingPrincipal?number?string["#,##0.00"]}</td>
<td class="amount">${row.principal?number?string["#,##0.00"]}</td>
<td class="amount">${row.interest?number?string["#,##0.00"]}</td>
<td class="amount">${row.payment?number?string["#,##0.00"]}</td>
<td class="amount">${row.closingPrincipal?number?string["#,##0.00"]}</td>
</tr>
</#list>
</tbody>
</table>
<p>The scheduled payments, including interest, sum to ${facts.totalPayments?number?string["#,##0.00"]} ${facts.currency}. ${facts.interestBasis}. Other products require their own authoritative pricing, fee, and calendar facts.</p>
<div class="document-legal-note">
<strong>Demonstration document.</strong> Firefly Sandbox Financial Services is a fictional demonstration entity. This specimen is not a customer agreement, insurance coverage, payment confirmation, or jurisdiction-approved legal form. Replace its approved wording, entity details, disclosures, and product terms through a new governed revision before customer use.</div>
</section>
<section class="document-page">
<h1>Agreement terms and signatures</h1>
<p class="sandbox-label">
<strong>Sandbox specimen</strong> · Fictional institution and customer data.</p>
<p class="document-reference">${facts.documentReference} · ${facts.issuedOn}</p>
<h2>3. Payment allocation</h2>
<p>Each accepted payment is allocated to the scheduled interest and principal shown in the repayment schedule. A payment is applied only after its financial receipt is confirmed. Reversals retain their original posting link and may reopen the relevant obligation.</p>
<h2>4. Early repayment</h2>
<p>The borrower may request an authoritative payoff quote for a specified date. This demonstration product has no early-repayment fee. A quote does not itself settle the loan.</p>
<h2>5. Missed payments and assistance</h2>
<p>The example late fee is ${facts.lateFee?number?string["#,##0.00"]} ${facts.currency}. Contact the configured servicing team to discuss a missed payment. A restructuring requires a separately approved, effective-dated change and its own document history.</p>
<h2>6. Statements and communications</h2>
<p>Servicing notices and statements use the customer’s authorized delivery preferences. Electronic delivery and signature consent must be established separately; a document ID is not evidence of consent.</p>
<h2>7. Product and jurisdiction terms</h2>
<p>${facts.governingTerms}. This specimen does not select a governing law, waive a statutory right, or establish regulatory approval. Project-specific approved clauses must be published as a new revision.</p>
<h2>8. Execution</h2>
<p>${facts.signatureState}</p>
<div class="signature-grid">
<div>
<p>Borrower: ${facts.partyName}</p>
<p>Signature ____________________</p>
<p>Date ____________________</p>
</div>
<div>
<p>Authorized institution representative</p>
<p>Signature ____________________</p>
<p>Date ____________________</p>
</div>
</div>
<div class="document-legal-note">
<strong>Demonstration document.</strong> Firefly Sandbox Financial Services is a fictional demonstration entity. This specimen is not a customer agreement, insurance coverage, payment confirmation, or jurisdiction-approved legal form. Replace its approved wording, entity details, disclosures, and product terms through a new governed revision before customer use.</div>
</section>
