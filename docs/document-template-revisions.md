# Immutable document template revisions

Masters owns global document template identities and their localized publication history. Tenant, product, channel, jurisdiction and effective-date bindings belong to configuration management. A published revision is a reusable content snapshot, not a generated customer document or a jurisdiction-approved legal form.

The existing document template catalog, type and localization APIs remain the draft authoring surface. A separate revision API publishes exact localized content, resolves a specific publication for new issuance, retires it, and preserves historical readback.

## Trusted access

All document template catalog, localization, type and revision service operations require a validated Jafly `SecurityContextPort` principal and a permitting `PolicyDecisionPort` decision. Missing ports, an empty principal, policy failure, deny/indeterminate results and unimplemented obligations fail closed with HTTP 403. Caller actor or tenant headers cannot establish authority. Production must connect these ports to its actual authentication and authorization integration before these APIs can be used.

| Action | Operations |
| --- | --- |
| `masters.document-template.read` | Legacy document catalog/type/localization reads and revision history |
| `masters.document-template.draft.write` | Legacy document catalog/type/localization create, update and delete |
| `masters.document-template.publish` | Publish or replay an exact localized revision |
| `masters.document-template.resolve` | Resolve an exact revision for new issuance |
| `masters.document-template.retire` | Retire an existing revision |

Legacy resources are `document-template-catalog`, `document-template-type` and `document-template-localization`. Revision operations use `document-template:<templateId>`, `document-template-revision:<revisionId>` or, for resolution, `document-template:<code>:<version>:<locale>`. Policy context contains `scope=GLOBAL_MASTER`. Read access and resolution access are separate permissions; granting a generation service resolution does not grant administration.

This intentionally changes compatibility for unauthenticated document template CRUD: those requests now receive 403. Other master-data APIs are outside this change. Qualification uses explicit synthetic trusted-principal and action-specific policy fixtures; those fixtures are not a production authentication provider.

## Author and publish

An empty environment needs prerequisite master records before template authoring. Read `/api/v1/language-locales` and select the exact active locale. If it is absent, an authorized administrator can create it through the normal locale API; retain the server-generated `localeId` from the response:

```http
POST /api/v1/language-locales
Content-Type: application/json

{
  "languageCode": "en",
  "countryCode": "US",
  "localeCode": "en-US",
  "languageName": "English",
  "nativeName": "English",
  "regionName": "United States",
  "rtl": false,
  "sortOrder": 1,
  "status": "ACTIVE"
}
```

The generated Java API is `LanguageLocaleApi`; do not guess its pluralization. Omit `localeId` on creation and use the returned identity for localization. Locale duplication does not establish a preferred row: exact revision resolution rejects ambiguous matches. Check existing records before creating another. Locale administration follows the deployment's master-data access controls; the document-template policy table does not grant that separate authority.

Migrations V28 and V29 provide generated UUID defaults for legacy country, currency, institution and locale tables and align persisted country regions with the public enum. Apply the complete forward migration chain in a fresh environment. A null-ID creation failure or unsupported declared region indicates a schema/runtime mismatch; do not seed rows manually or assign guessed IDs to conceal it.

1. Select an active type from `/api/v1/document-template-types` and an existing active language locale. `HTML` is seeded as a type; locale records must exist in the target environment.
2. Create or update a draft through `/api/v1/document-templates`. Supply `typeId`, `templateCode`, `category`, `templateName`, `templateContent`, `version`, `status`, `mediaType`, exactly one of `inputSchemaJson` or `inputSchema`, and optional `styleContent`. Legacy `templateVariables` remains separate metadata; it is not reinterpreted as JSON Schema. Supported publication media types are exactly `text/html` and `text/plain`.
3. Create a matching active localization through `/api/v1/document-template-localizations`, with `templateId`, `localeId`, `templateName`, `templateContent` and `status`. Publication always uses this localized content and name. An absent or inactive localization cannot silently use the base draft content or another locale.
4. Publish using the exact draft version and locale code:

```http
POST /api/v1/document-template-revisions/templates/{templateId}/publish
Content-Type: application/json

{"version":"1.0.0","localeCode":"en-US"}
```

Use `inputSchemaJson` to author exact schema JSON as a string. It is validated through the shared offline engine, rejects duplicate fields and trailing documents, and is canonicalized before persistence. The legacy `inputSchema` map remains a convenience representation; a generic HTTP/client map codec can round long decimal values. Both representations address the same `input_schema` column. Supplying both is rejected with 400, even if they appear equivalent. Reads return the map for inspection and `inputSchemaJson` for exact transport. The generated Java SDK initializes optional maps to empty maps, so exact authoring should explicitly use `new DocumentTemplateCatalogDTO().inputSchema(null).inputSchemaJson(schemaJson)` along with the remaining draft fields. For read-modify-update with the generated SDK, call `draft.setInputSchema(null)` and retain/edit `inputSchemaJson`; a client deliberately choosing the legacy map must instead clear `inputSchemaJson`.

The JSON document encoded in `inputSchemaJson` can look like:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "type": "object",
  "properties": { "recipientName": { "type": "string", "minLength": 1 } },
  "required": ["recipientName"],
  "additionalProperties": false
}
```

Publication validates this schema through Jafly's offline validator against the bundled Draft 2020-12 metaschema, compiles allowed local references, and requires a root `type: object`. It does not validate customer payloads, render a document, or establish legal fitness. The shared validator has a deliberately closed profile, bounded inputs, enabled format assertions and no remote schema retrieval. See the validators module's `docs/offline-json-schema-validation.md` for precise limits and supported references. Publishable localized content is limited to 262,144 UTF-8 bytes and style to 65,536 bytes; authoring endpoints can impose smaller DTO constraints.

Publication returns HTTP 200 for both a new publication and a replay of the same source snapshot. A transaction locks the catalog, exact localization, locale and type source rows. A unique `(templateId, version, localeCode)` constraint prevents duplicate publication identities. Duplicate locale-code matches fail with 409; selection does not pick an arbitrary locale row. If the source changed for that identity, replay fails with 409. Change the draft version deliberately before publishing a different snapshot.

Publication and retirement bypass generic HTTP response caching through Jafly's method-level opt-out. Current authorization runs on each call, and same-source publication returns the retained current revision, including its RETIRED state; replay never reactivates it. `X-Idempotency-Key` remains an optional ignored header for generated-SDK compatibility. It cannot select an old response or replace the native publication/retirement rules. For administration deployments, keep `firefly.web.idempotency.enabled=false` unless the other protected mutations have separately qualified current-policy replay behavior; the revision opt-out does not qualify unrelated legacy CRUD.

The catalog mapper retains explicit `typeId` and returns a nonblocking nested type identity. Retrieve full type metadata from the type endpoint. JSONB draft fields use the PostgreSQL JSON codec so structured schema and legacy variables survive actual repository persistence.

## Read, resolve and retire

| Route | Behavior |
| --- | --- |
| `GET /api/v1/document-template-revisions/{revisionId}` | Authorized immutable historical read, including retired content |
| `GET /api/v1/document-template-revisions/resolve?templateCode=...&version=...&localeCode=...` | Exact selection for new issuance; requires a published revision and active matching catalog/localization/locale/type |
| `POST /api/v1/document-template-revisions/{revisionId}/retire` | Idempotent retirement; retains content, digests and publication identity |

Resolution never selects a latest version or falls back to another locale. An absent exact tuple returns 404. A retired revision or inactive/mismatched source identity returns 409. Draft content edits do not rewrite the published snapshot. Historical reads remain available under the read permission after retirement or source deactivation. There is no revision update, delete or reactivation route.

Database guards reject revision content/identity/publication-metadata mutation and deletion, template code/type reassignment after publication, localization parent/locale reassignment, and protected type-code changes. Foreign keys prevent deleting a published parent/localization/locale through legacy cascades. Ordinary draft content edits remain possible. These protections cover ordinary application persistence paths; they do not claim to constrain a database administrator who can alter tables or disable triggers.

## Exact SDK content and digests

The existing generated masters SDK owns `DocumentTemplateRevisionsApi` and the revision/publication models. Do not create a parallel hand-written client. Generated Java remains under `target/` as build output; the checked-in OpenAPI resource is the producer contract.

Each revision retains identifiers, exact version/locale, media type, localized content, input schema, style, legacy variables, complete source snapshot, SHA-256 digests, publication actor/time, lifecycle and retirement metadata. Actors come from the validated principal rather than a request field.

`inputSchemaCanonicalJson` and `sourceSnapshotCanonicalJson` preserve exact JSON as strings through the SDK. Downstream generation must pass `inputSchemaCanonicalJson` directly to the shared validator; reserializing a generic `Map<String,Object>` can lose decimal precision in a client mapper. Structured map fields remain useful for inspection.

`canonicalizationVersion=MASTERS_JSON_V1` names the digest algorithm: recursive UTF-16 key sorting, array-order preservation, exact integer/decimal normalization without insignificant trailing zeros, preserved Unicode without normalization, and compact UTF-8 JSON. This is a defined masters algorithm, not a claim of RFC 8785 conformance. Content and style digests cover their exact UTF-8 text; absent style is explicitly the empty string. Schema and source digests cover canonical JSON. Plain numeric representations are limited to 128 characters before exponent expansion, preventing a small exponent token from allocating an unbounded plain number. Canonical draft schema JSON must still fit 65,536 UTF-8 bytes after normalization. The source snapshot includes the base draft content and localized content, schema/style, variables and source identity. Semantically identical JSON numeric formatting and key order produce the same source digest; changed source content produces a conflict for the same publication identity.

## Errors and qualification

400 means invalid publication identity/content/media/schema; 403 means trusted permission was unavailable or denied; 404 means an exact source/revision was absent; 409 means source-state, replay, ambiguity or retention/integrity conflict. Conflict responses sanitize database details.

Private PostgreSQL tests exercise real migrations, publication races, replay/conflicting replay, rollback, separate-client readback, canonical Unicode/numeric retention, retirement, trusted-access denial and actual legacy repository/service safeguards. Maven Failsafe runtime SDK tests launch the actual packaged web Boot jar as private child processes on random loopback ports. A test-only `loader.path` bootstrap supplies explicit synthetic authentication and action-specific policy fixtures; an ordinary `java -jar` launch separately proves missing trusted ports deny access. The production jar contains no synthetic trust provider. Neither qualification establishes tenant binding, generation, signing, delivery, a live identity provider or legal approval. Use the repository Maven reactor verification command with Java 25; OpenAPI generation reserves port 18080 and ordinary parallel test runs should set `-DskipOpenApiGen`.
