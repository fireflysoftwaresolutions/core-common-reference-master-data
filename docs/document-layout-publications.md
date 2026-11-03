# Publish document layouts and legal-notice associations

A document layout publication binds one genuinely new localized master revision to an immutable semantic definition, an exact recognized output-profile reference, and ordered legal-notice revisions. Masters owns this global publication authority. Tenant issuer selection belongs to configuration management; owner facts belong to the relevant contract, account, customer, product or policy service.

Use this API when a new template version must retain its source and notice associations together. Do not publish the main source through the older revision endpoint first: the layout operation creates its source revision in the same local transaction and rejects adoption of an already committed revision. Existing historical revisions remain readable through their original API.

## Prerequisites and authority

Apply the complete forward Flyway migration chain, including the layout migration. Configure a validated Jafly `SecurityContextPort` and action-specific `PolicyDecisionPort`; missing authority, denied/indeterminate decisions, policy failures and unsupported obligations fail closed. Caller-supplied actor headers do not establish publishing rights. Policy runs before mutation transactions; locked source re-observation detects intervening draft or notice changes.

| Permission | Resource |
| --- | --- |
| `masters.document-template.layout-observe` | `document-template:<templateId>` |
| `masters.document-template.layout-publish` | `document-template:<templateId>` |
| `masters.document-template.layout-read` | `document-layout-revision:<layoutRevisionId>` |
| `masters.document-template.layout-resolve` | `document-layout:<code>:<version>:<locale>`, then retained layout context |
| `masters.document-template.layout-retire` | `document-layout-revision:<layoutRevisionId>` |

The policy context uses `scope=GLOBAL_MASTER`; publication and replay also check safe source/retained-layout context. Readers, resolvers and administrators need distinct permissions. Existing draft/notice administration additionally requires the permissions in [template revision authoring](document-template-revisions.md).

A recognized profile is required for new publication and resolution. The default registry is empty and returns 503. A configured but unknown exact tuple conflicts with 409. A project can supply its own `DocumentLayoutProfileRegistry` bean; it takes precedence without `@Primary` or bean overriding.

For an isolated synthetic metadata deployment, the property-backed registry can contain:

```yaml
firefly:
  documents:
    layout:
      output-profiles:
        - reference:
            code: SYNTHETIC_A4
            version: "1"
            media-type: text/html
            manifest-digest: "2222222222222222222222222222222222222222222222222222222222222222"
          section-mode: AUTHORED_PAGES
          page-size: A4
          orientation: PORTRAIT
          allowed-class-tokens: [document-page, body-note]
          max-sections: 16
          max-content-bytes: 262144
```

This tuple is synthetic metadata used to exercise publication. The manifest digest above does not identify a real Firefly renderer, CSS bundle, font pack or logo. A project registry must recognize its actual reviewed profile tuple; registering metadata alone does not create or validate those output assets. Profile reference and metadata are retained together so later configuration changes cannot silently alter an accepted publication.

## Author the main draft and notices

Follow [template revision authoring](document-template-revisions.md) to create the actual type, locale, template catalog and active exact localization through their APIs. Retain returned UUIDs rather than guessing them. The main source must use `text/html`, an empty style string, an admitted object schema and the exact locale. Author the new draft version, but leave it unpublished until the layout operation.

Create legal notices as separate `LEGAL_NOTICE` template catalogs with `text/html`, empty styles and an active localization in the same exact locale. Publish each notice through the existing template-revision API and retain its complete returned reference. Notice schemas accept no caller facts:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "additionalProperties": false
}
```

Optional `properties: {}` and `required: []` are allowed; other notice-schema fields are rejected. A notice cannot smuggle customer variables into legal text through a more permissive schema. Legal authors still own the wording and jurisdictional approval; the catalog category is not legal approval.

| Role | Required placement | Cardinality |
| --- | --- | --- |
| `GENERAL_DISCLOSURE` | `FOOTER` | Exactly one, required |
| `PRIVACY_NOTICE`, `COMPLAINTS_NOTICE`, `SIGNATURE_NOTICE` | `BODY_NOTE` | Each optional, at most once |

Retain the chosen array order. One to four unique roles are allowed. Every notice reference must identify a currently PUBLISHED exact revision with matching template/version/locale, media type, canonicalization and content/schema/style/source digests. A display name or notice code alone is insufficient.

## Observe, construct and publish

Read the current main draft observation:

```http
GET /api/v1/document-layout-publications/templates/{templateId}/source-observation?localeCode=en-US
```

The response contains `draftReference` and `sourceSnapshotCanonicalJson`. Copy the returned reference unchanged; it includes the actual source/localization/locale identities and digests. Editing the draft afterward invalidates that observation. Obtain a new observation and explicitly review a new publication intent rather than replacing only a digest.

The following request construction uses `jq`, two actual API response files, and a new durable action UUID. `draft-observation.json` is the observation above; `legal-notice-revision.json` is the complete published notice response. `ACTION_ID` must be newly generated for this intent and retained for all retries. The profile here matches the synthetic metadata configuration above.

```sh
jq -n --arg action "$ACTION_ID" \
  --slurpfile observation draft-observation.json \
  --slurpfile notice legal-notice-revision.json '
  {
    actionId: $action,
    expectedDraftReference: $observation[0].draftReference,
    outputProfile: {
      code: "SYNTHETIC_A4", version: "1", mediaType: "text/html",
      manifestDigest: "2222222222222222222222222222222222222222222222222222222222222222"
    },
    semanticDefinition: {
      sectionMode: "AUTHORED_PAGES", pageSize: "A4", orientation: "PORTRAIT",
      legalNotices: [{
        role: "GENERAL_DISCLOSURE", placement: "FOOTER",
        masterReference: ($notice[0] | {
          revisionId, templateId, templateCode, version, localeCode, mediaType,
          canonicalizationVersion, sourceDigest, contentDigest, schemaDigest, styleDigest
        })
      }]
    }
  }' > layout-publication-request.json
```

Send that complete JSON body to `POST /api/v1/document-layout-publications/templates/{templateId}`. The route UUID must match the observed source. Unknown or duplicate JSON fields, scalar coercion and trailing documents are rejected. Native action identity is in `actionId`; these endpoints bypass generic response-cache idempotency and do not require an invented retry header.

New publication returns 201. An accepted same-action/same-intent replay returns 200 with the original identity and current lifecycle. Changing the route, profile, notice order/pins or another intent field while retaining the action conflicts. A new action cannot adopt the already committed main source. On a genuinely revised draft, use a deliberate new source version, fresh observation and new action.

The local transaction creates the source revision, immutable layout, ordered notice foreign-key links, action, history and outbox evidence together. An outbox insertion failure rolls them back together. Outbox retention does not establish that a transport publisher has delivered an event.

## Read, resolve and retire

Historical readback uses `GET /api/v1/document-layout-publications/{layoutRevisionId}`. It retains canonical JSON/digest, source snapshot, profile metadata, notice associations and lifecycle after retirement or registry withdrawal.

New-use resolution is exact:

```http
GET /api/v1/document-layout-publications/resolve?templateCode={code}&version={version}&localeCode=en-US&outputProfileCode=SYNTHETIC_A4&outputProfileVersion=1&outputProfileManifestDigest={digest}
```

Resolution requires a PUBLISHED layout, usable source/notices and currently recognized profile metadata equal to the retained snapshot. Historical read access does not make a retired publication eligible for a new document.

Retire with `POST /api/v1/document-layout-publications/{layoutRevisionId}/retire`:

```json
{"actionId":"<new retained action UUID>","expectedStateVersion":0,"reason":"Approved template withdrawal"}
```

Replace the action placeholder with a canonical actual UUID. Retirement changes PUBLISHED/version 0 to RETIRED/version 1 and appends immutable evidence. Replay the same accepted action and body to recover its outcome. A different action against the withdrawn original version conflicts. Publication replay returns the current retired lifecycle; it does not republish the layout. Retirement, read and replay continue to use retained metadata if the deployment later removes the registry entry.

## Recovery and downstream integration

On a lost publication response, repeat the retained action and exact request under current authorization. On a conflict, read the owner record and inspect the specific source/profile/action mismatch. Never edit retained canonical text, rewrite digests, delete notice links or mutate SQL rows to repair a publication. Correct a draft through its normal authoring API and publish a new deliberate version when appropriate.

`DocumentLayoutPublicationsApi` is the generated Java client for all five operations. Use the matching producer/SDK version and preserve structured references; independently reconstructing canonical hashes or dropping schema/style pins weakens the contract. A custom registry's output is checked and copied before persistence.

This service currently publishes layout metadata and legal-notice associations. Document generation still needs explicit tenant/issuer/profile binding, controlled HTML composition and Firefly brand assets, a controlled PDF renderer, authoritative financial/customer facts and signature/delivery orchestration. The legacy ten-template issuance path does not automatically consume these new layout publications.

## Verification

Run the service's Maven `verify` lifecycle with its required JDK and disposable PostgreSQL runtime support. It exercises rules, native migrations/immutability, current authorization outside SQL transactions, plain-bean registry precedence, actual loopback SDK calls and restart. The normal producer lifecycle generates its public contract from the running OpenAPI application before generating SDK sources. A static YAML copy or successful publication does not prove printed output or a complete customer journey.
