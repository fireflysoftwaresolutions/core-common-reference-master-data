# Sandbox financial stationery

This catalog provides American English document content, closed input schemas and reconciled fictional examples for financial-platform development. Masters owns the reusable wording and published revisions. Configuration owns tenant, product, purpose, jurisdiction, channel, locale and effective-date selection. Business services supply authorized facts; document management owns issued bytes, versions and retained provenance.

The branded A4 previews are an **offline design profile**, not an available PDF-generation endpoint. The governed HTML renderer currently accepts the restricted template fragments with empty style content. Its security restrictions are unchanged. Do not publish `preview/profile.css` as arbitrary template CSS or embed the preview logo as an external image URL.

## Catalog and source ownership

| Template | Content | Exact facts contract |
| --- | --- | --- |
| Account statement | Opening/closing balances, booked movements, totals, statement notes | `ACCOUNT_STATEMENT / 1` |
| Installment loan agreement | Financial terms, full repayment schedule, conditions, unsigned execution fields | `LOAN_AGREEMENT / 1` |
| Commercial property policy declarations | Coverage schedule, shared occurrence limit, premium, wording and endorsement references | `POLICY_DECLARATIONS / 1` |
| Premium invoice | Itemized premium, illustrative tax, allocations and amount due | `PREMIUM_INVOICE / 1` |
| Claim settlement statement | Approved assessment, deductible, depreciation, prior payments and remaining settlement | `CLAIM_SETTLEMENT / 1` |
| Deposit product agreement | Proposed product/pricing/terms and exact customer/contract/account association; visibly unsigned execution | `PRODUCT_AGREEMENT / 1` |
| Customer relationship profile | Purpose-limited identity/relationship summary, masked contact and retained review references | `CUSTOMER_PROFILE / 1` |
| Account ownership certificate | Exact as-of holder role/association, account identity, certificate purpose and issuing authority reference | `ACCOUNT_OWNERSHIP / 1` |
| Account balance certificate | Booked, held and cash-available amounts at an explicit historical cutoff/source sequence | `ACCOUNT_BALANCE_AS_OF / 1` |
| Financial balance sheet | Current/comparative assets, liabilities/equity, reporting basis and both periods' reconciled source references | `FINANCIAL_BALANCE_SHEET / 1` |

`catalog.json` records exact codes, versions, paths, purposes, document types and business prerequisites. `templates/` contains restricted FreeMarker XHTML; `schemas/` contains closed offline Draft 2020-12 schemas; `examples/` contains synthetic facts. Monetary fields are exact two-decimal USD strings. These examples do not implement another currency's scale, pricing rules, tax calculation or a universal legal form.

The loan example uses twelve monthly periods at 1% interest per period, rounded half up to cents. Its eleven regular payments and adjusted final payment repay principal exactly. Calculations belong to the source business service; templates only format supplied values. Statement movements and the policy/invoice/claim relationship are independently reconciled in `SandboxDocumentCatalogTest`.

The fictional property product explicitly applies the highest applicable scheduled deductible once to a single buildings/contents occurrence. Claim assessment lines name their scheduled coverage codes and retain that exact wording/rule reference. This is a deliberate sandbox product rule, not an assumption about other policies. Negative totals, deductions and other nonnegative source roles fail schema validation; booked account balances retain their sign.

The `CONTRACT_DOCUMENT_V1` producer snapshot is a separate facts contract. It cannot be substituted for these schemas without an explicit authoritative mapping. An application ID, party ID or policy ID is not a contract ID.

The additional agreement/profile/certificate/report examples retain explicit fictional source systems, references, versions and observation instants. They do not implement historical extraction, holder verification, KYC, pricing approval or a GL close. The account certificate's booked amount agrees with the September statement and separately subtracts active holds; no credit facility is included. Its ledger cutoff, UTC time zone and sequence must come from the actual owning service before customer use. A customer's profile is distinct from title evidence; the financial balance sheet is distinct from an account-balance certificate.

The deposit agreement is an unsigned proposal. Its monthly-fee waiver is a proposed rule with separate pricing/wording snapshots; it does not govern or recalculate the existing statement's historical service-fee illustration. A real fee entry retains its actual assessment/effective pricing policy. Current/comparative balance-sheet columns each have explicit fictional GL and reconciliation source versions and balance independently. None of those fixture references claims that the referenced owner already supplies a qualified production snapshot API.

## Design profile

`preview/profile.css` defines A4 margins, Manrope typography, hierarchy, tabular amounts, alternating table rows, explicit page sections, continuation headers and numbered footers. The official Firefly SVGs and licensed font files are copied unchanged; `preview/asset-manifest.json` records their source paths and SHA-256 digests. The OFL notice accompanies the fonts.

Each page includes a reference, template/version identity, fictional issuer and legal demonstration note. Signature fields are visibly unsigned. Policy and claim specimens distinguish a calculation from coverage or payment. The preview's institution name, address and contact are fictional fixed design data. A deployed controlled profile must obtain approved institution details, brand assets and disclosures through its versioned configuration contract rather than copying those values into a customer template.

Wide schedules use an ordinary table in print. Small-screen previews display repayment rows as labeled cards; all financial values remain present. Print page sections are intentionally authored for these bounded examples. Variable-length production statements need a separately qualified pagination mechanism, repeated headers, overflow rejection and retained output-profile provenance.

## Validate and preview locally

Use Java 25 and the repository Maven reactor. From the repository root:

```sh
mvn -DskipOpenApiGen verify
```

The catalog test validates every schema/example/template, rejects missing and unknown facts, checks customer-text escaping and reconciles financial examples. The existing actual Boot/SDK tests remain part of reactor verification. Preview output belongs outside this repository; generated HTML, PDFs, class files and customer data must not be committed.

`tools/RenderSandboxDocumentPreviews.java` is a standalone authoring CLI. Compile it against the repository's resolved Jafly utils, validators and Jackson runtime dependencies. Its arguments are the absolute `sandbox/documents` directory and an external fragment-output directory. It uses the actual Jafly offline validator and restricted renderer, retaining a SHA-256 manifest of the validated fragments. No service publication or customer issuance occurs.

Then compose the controlled visual preview:

```sh
python3 sandbox/documents/tools/layout_previews.py \
  --source sandbox/documents \
  --rendered /absolute/preview-work/fragments \
  --output /absolute/preview-work/site
```

The layout tool checks fragment and packaged-asset digests before composition. Serve that directory through a local static HTTP server so font and asset behavior can be inspected. Browser printing is an authoring preview, not proof of a document-service PDF capability. Check every resulting page, page count, footer label, complete table and exact amount. Do not hide content to force a page to fit.

## Adopt through the real services

Follow [immutable document template revisions](../../docs/document-template-revisions.md) for trusted authoring, localization, publication and retained readback. Use the generated producer SDKs, not direct database seeds.

1. Resolve an active HTML template type and the exact active `en-US` locale in the target environment. Record their actual IDs; do not reuse another environment's UUIDs.
2. Create a draft using the catalog entry's exact code/version/category/name, template bytes, `text/html`, empty style and schema JSON string. With the generated Java SDK explicitly set `inputSchema(null)` before `inputSchemaJson(schemaJson)`.
3. Create the exact active localization with the same localized content. Publish that precise version/locale; retain the returned revision identity and digests. A conflicting source for an existing publication identity requires a deliberate new version.
4. Create the exact tenant/purpose/product-scope/channel/jurisdiction/locale/effective-date binding through configuration's trusted API. Bind the returned published revision. Never resolve a guessed latest revision or infer jurisdiction from language.
5. Collect authorized business facts matching that entry's exact schema and prerequisites. Supply a stable issuance action, explicit resource identity, exact configuration/master pins, document type and retention policy. The business journey must own a durable facts/action record before a remote issuance can be safely retried.
6. Issue and read back through document management's genuine SDK. Compare retained version, content bytes, digest and provenance. Recover a lost response with the same sealed request; do not create a fresh action or delete issued evidence after a later attachment failure.

These steps describe adoption of the supported restricted HTML content. Official-logo/font/PDF service output requires a separately implemented and qualified controlled rendering profile. The preview profile name in the catalog is a design reference, not a supported service selector.

## Project customization

Create a new governed revision for approved clauses, institution details, supported products or jurisdiction disclosures. Keep schema, source-owner mapping, exact arithmetic policy, renderer profile and delivery/signature prerequisites explicit. Replace the fictional issuer and sandbox adapters only through their actual configuration/adapter contracts. Requalification must cover the genuine experience-to-core chain and retained bytes, not just a rendered screenshot.

Additional document families need their own source authority, schema, business prerequisites and reviewed layout. This catalog does not establish complete banking or insurance document coverage, legal approval, signature, delivery, funding, coverage or settlement.
