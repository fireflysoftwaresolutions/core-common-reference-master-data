# Currency execution policy publications

Currency catalog metadata is useful for display and configuration, but an operation needs an exact retained representation. A currency execution policy pins the canonical catalog observation, explicit posted scale, effectivity bounds, and provenance in immutable versioned bytes. It does not authorize a posting, calculate a price, approve an overdraft, or settle a payment. Product commercial revisions, contract acceptance, and monetary-owner admission must consume exact policy references through their own qualified integrations.

## Ownership and lifecycle

| Record | Owner and purpose | Mutability |
| --- | --- | --- |
| Existing `currencies` catalog | Existing masters CRUD and source metadata | Descriptive fields, precision and status remain legacy metadata; bound UUID/ISO identity cannot be retargeted or deleted. |
| Currency identity/head | Explicit global ISO-to-canonical-UUID binding and publication order | Identity never changes; head advances exactly one per committed version. |
| Policy publication | Exact source bytes, representation and effectivity | Immutable; there is no edit/delete or implicit latest-policy selection. |
| Policy control | Current publication lifecycle | One transition from `PUBLISHED` state 0 to `RETIRED` state 1. |
| Native action receipt | Full original publish/retire intent | Immutable; identical replay reauthorizes and returns the original policy with current control. |
| Audit and outbox | Committed action evidence and delivery intent | Append-only and atomic with the owner action. Persistence does not prove external delivery. |

Retirement prevents a current usable-publication observation. It retains all historical bytes and references. Effectivity uses inclusive start and exclusive optional end at the master's observed time. `effectiveNow` reflects this policy's lifecycle and effectivity at the observed instant. Each monetary owner evaluates admission using its current authority when executing an operation. Legacy catalog `INACTIVE` does not act as an immediate financial kill switch. Later catalog edits never reinterpret previously pinned amounts. Release, expiry, reversal and accounting must retain their original currency representation.

## Obtain an exact source observation

Call `GET /api/v1/currency-execution-policies/currencies/{currencyId}/source-observation` using current permission for that exact currency. The response contains metadata, `encodingVersion`, `canonicalEncoding`, `digest`, `observedAtEpochMillis`, and any current binding/head. Copy the returned digest into the publication request; do not reconstruct it from a partial metadata DTO or assume a hashing algorithm.

`MASTERS_CURRENCY_SOURCE_V1` consists of that version label, one newline, and recursively sorted compact JSON. It includes explicit nulls for currencyId, isoCode, currencyName, symbol, decimalPrecision, isMajor, status, dateCreated and dateUpdated. Legacy naive LocalDateTime values remain exact local ISO strings; no timezone is invented for them. The observation clock is excluded from the source digest. SHA-256 covers the exact UTF-8 encoding. Publication and action encodings use separate version labels and cover their complete immutable definitions and intents.

An observation does not reserve the source or publication head. The publication rechecks both under native owner locks. A stale digest or head conflicts; the caller must deliberately observe and approve a new request. It must not silently retry against a newly selected head or policy.

## Publish an explicitly approved version

`POST /api/v1/currency-execution-policies` accepts a closed JSON request:

```json
{
  "actionId": "22345678-1234-4234-8234-123456789abc",
  "currencyId": "12345678-1234-4234-8234-123456789abc",
  "isoCode": "USD",
  "expectedCurrencyDigest": "<exact lowercase digest returned by source-observation>",
  "expectedHeadVersion": 0,
  "postedScale": 2,
  "effectiveFromEpochMillis": 1792713600000,
  "effectiveToEpochMillis": null,
  "provenanceReference": "Approved representation record v1"
}
```

The placeholder digest above must be replaced before sending. The UUID and ISO must match the canonical source. Existing duplicate ISO catalog rows are not merged or selected by order: the first privileged publication deliberately binds one exact currency UUID, and any competing mapping conflicts. Posted scale must be explicitly present from 0 through 10, match the nonnull current catalog precision, and use an ACTIVE source. An unknown precision is never defaulted to 2. Expected head starts 0 and counts all committed publications, including retired versions. Each successful new action advances it exactly one. Exhaustion rejects without wrapping.

New publication returns 201. Identical retained native action replay returns 200 after current authorization, even when source metadata or the head has subsequently changed. Changed source/head/scale/effectivity/provenance under the same action conflicts with 409. The global action namespace covers both publication and retirement; changing the currency or operation kind does not create a second action.

The immutable returned reference contains policyId, currencyId, isoCode, version, digest and encodingVersion. Consumers must carry the complete exact reference and digest forward. A UUID alone, a mutable catalog lookup, or a current highest version is insufficient.

## Configure and use the generated client

Keep the owner configuration explicit:

```yaml
firefly:
  security:
    resource-server:
      strict-policy: true
```

The flag is mandatory even when a project supplies a custom PDP. It does not create an identity, a policy rule, or permission by itself. A project using the framework fallback must deploy the qualified strict-capable framework build; empty or failed policy rules must deny. A project supplying its own PDP must enforce the exact currency/action and fulfill the native no-obligation contract. The owner always rechecks the actual current decision.

The SDK is generated from the running owner's OpenAPI contract. Configure its existing ApiClient with the owner base URL and a legitimate current credential from your project authentication integration; keep tokens out of source code and logs. Use CurrencyExecutionPoliciesApi for these operations:

```java
var observed = api.observeCurrencyPolicySource(currencyId);
var exactPolicy = api.getCurrencyExecutionPolicy(policyId, currencyId);
var retainedReceipt = api.getCurrencyExecutionPolicyByAction(actionId, currencyId);
```

These are reactive results. Compose them with the domain's authorized CQRS/orchestration flow. Do not block an event-loop thread or publish automatically merely because an observation exists. Obtain the current observation, record the explicit business approval of its exact source/head and representation, and then submit the complete approved CurrencyPolicyPublishRequest. A 409 requires a deliberate decision about a new observation and intent; do not replace expected values inside an automatic retry loop. Recover a lost response by exact original action and retain the complete returned policy reference and encoding digest.

## Read and retire exact retained publications

- `GET /api/v1/currency-execution-policies/{policyId}?currencyId={currencyId}` reads an exact policy under current exact-currency permission.
- `GET /api/v1/currency-execution-policies/by-action/{actionId}?currencyId={currencyId}` recovers a retained native action receipt after a lost response.
- `POST /api/v1/currency-execution-policies/{policyId}/retire` accepts actionId, currencyId, expectedStateVersion 0 and a bounded reason. An identical currently authorized retirement replay retains state 1. A different new retirement action conflicts after retirement.

Explicit currency context lets PDP authorization run before a scoped database lookup; it is a resource identifier, not permission. A denied resource returns 403; an absent or invisible exact record returns 404. Global hidden action collisions return 409 rather than empty 200/201. There is no public mutation of canonical policy bytes.

## Exact request types and error handling

Native JSON rejects unknown or duplicate keys, trailing tokens, scalar coercions, noncanonical lowercase UUIDs, nonuppercase three-letter ISO codes, and nonlowercase 64-character hexadecimal digests. Text must contain valid Unicode scalar values: isolated UTF-16 surrogates are rejected before encoding or persistence, while valid supplementary characters and combining sequences remain exact without normalization. Head versions, scales and timestamps must be exact bounded JSON integers; decimals, exponent notation and numeric strings are rejected. UTC policy times range from 1900-01-01 through 9999-12-31, including legitimate negative epoch milliseconds. Optional end must be strictly later than start. Omitting it means no specified end, not an inferred expiration.

| Status | Meaning | Caller response |
| --- | --- | --- |
|400|Closed request or exact type is invalid|Correct the request; preserve the original business intent.|
|403|Current trusted identity or resource permission is unavailable, expired or denied|Obtain legitimate current authority; do not fall back to legacy CRUD.|
|404|Exact authorized-context record does not exist or is invisible|Reconcile the exact original action when appropriate; do not infer monetary finality.|
|409|Caller transaction is already active, stale source/head, changed action intent, identity collision, terminal control or protected catalog constraint|Invoke outside a caller transaction; inspect owner observations without silently changing approved intent.|
|503|Native database runtime lacks required nonprivileged security configuration|Correct deployment roles; do not bypass RLS or run the application as migration owner.|

## Trusted policy boundary

Native operations require `firefly.security.resource-server.strict-policy=true`; a missing, disabled, or malformed flag denies access even when a supplied project policy would permit. Use the qualified strict-capable framework policy implementation. Its empty-rule and rule-error behavior must deny access; the legacy RBAC-defer mode is not a currency-policy permission.

A project must provide genuine SecurityContextPort and PolicyDecisionPort integrations. The native authority requires a bounded nonblank validated subject, an absolute HTTP(S) issuer and a current finite `Instant` exp claim projected by the trusted bridge. Missing, malformed, expired or unbounded expiry is denied. The service never accepts a caller JWT object, unsigned body claims, or a request-supplied authority toggle.

PDP receives `masters.currency-policy.source.read`, `.publish`, `.read`, or `.retire`, resource `currency/{currencyId}`, and safe `GLOBAL_MASTER` context. A grant with unmet obligations is denied. Missing ports, empty decisions and policy errors deny access. Document-template permission does not grant currency-policy authority. Native reads and mutations bypass transport idempotency/query caches so original receipts always undergo current authorization. External PDP work completes before database transactions and locks; identity expiry is rechecked within the native work before completion. This does not claim instantaneous revocation across an external policy system.

All five service entry points reject an already active reactive transaction with 409 before principal resolution, policy evaluation or database work. Invoke this owner outside a caller transaction. The owner opens its own scoped transaction after authorization; wrapping the call in an outer transaction would otherwise put extensible authorization work behind locks held by the caller.

## Database roles, isolation and recovery

Migrations run through a separate bootstrap owner. Preprovision `firefly_currency_policy_guard` as a NOLOGIN, non-superuser, non-BYPASSRLS role. Migration grants it read-only identity visibility for the catalog trigger and transfers only that fixed-search-path trigger function. The application must not be a member of that role, own any protected policy relation, have public-schema CREATE, be superuser, or bypass RLS. All six native policy tables require enabled and forced RLS. An absent or unsafe guard configuration leaves legacy metadata compatible but native currency-policy operations refuse with 503.

After migration, grant the application only its required table operations through deployment-owned role configuration. Native policy use needs SELECT on the six policy tables, INSERT on them, UPDATE on `currency_policy_identity` and `currency_policy_control`, and SELECT/UPDATE on the canonical `currencies` source for owner row locking. It needs no DELETE or schema/function ownership for native policy rows. Existing legacy catalog CRUD may need additional catalog permissions under its existing deployment policy; those do not create a financial admission grant. Keep migration credentials outside the running application and do not grant the application membership in the guard role.

If V31 was applied before the required guard role existed, native operations deliberately remain unavailable. Provisioning that role afterward does not silently rewrite an already applied migration. An authorized deployment owner must perform and qualify the documented migration-equivalent role/function ownership grants in its own controlled environment; never switch the application to the bootstrap owner to bypass the 503. No external environment is changed by repository tests.

Native authorization establishes only the exact server-validated currency and purpose with transaction-local settings on the same reactive connection. The application does not derive settings from headers. Commit, rollback and cancellation clear them before pooled reuse. These controls qualify application paths; they do not constrain a database administrator or stolen SQL credentials.

Publication takes a global action gate, sorted currency/ISO gates, then the canonical currency row and identity head. Raw legacy writers already hold row locks, so their trigger tries the same gates and fails fast on contention instead of waiting in reverse order. It rechecks the immutable binding before identity changes/deletion. Policy, action, identity head, control, audit and outbox commit atomically. A failed audit/outbox insert or cancelled transaction must leave no action, policy, head advance or binding behind. An initial identity must match the exact current catalog UUID/ISO and cannot commit alone at head zero. Retirement control must retain a matching retirement action, audit and outbox; a publication action cannot substitute for retirement evidence. Replay after a restart uses retained immutable intent and current authority.

## Retained outbox event contract

Each committed action appends a closed JSON payload with `payloadVersion=MASTERS_CURRENCY_POLICY_EVENT_V1`, actionKind, actionId, complete policyReference, exact intentEncoding and intentDigest, and observedAtEpochMillis. The durable outbox row supplies its event identifier. PUBLISH and RETIRE both carry the same retained representation reference for that exact policy: policyId, currencyId, ISO code, publication version, definition digest and encoding version. Retirement does not substitute a later publication or change the original digest.

Database checks enforce the closed version, exact reference against immutable publication bytes, exact original action intent, and matching audit/outbox identity and timestamp. A mismatched reference rejects the entire native action, including its head advance and claim. Future delivery and consumer deduplication must retain the durable event identifier and exact reference. This slice persists delivery intent; it does not claim an external broker delivery, batch completion, GL posting or reconciliation result.

## Dependent financial services

End-of-day, reconciliation, accounting and reporting owners must retain the complete policy reference and original representation alongside their authoritative amounts, booking dates and commercial snapshots. They must not reselect a later policy to reinterpret historical entries. Required GL posting, IFRS 9/IFRS 17 measurement, revaluation, fee and settlement capabilities consume this metadata through their own owner decisions and calculations. A retained currency-policy revision supplies exact representation and provenance; it is not evidence that those accounting capabilities have executed.

Product and contract adoption, account enrollment representation migration, multileg posting, fixed-fee execution, FX, overdraft and rail settlement remain separate required deliveries. They must use the accepted exact policy contract and their own owner authority rather than treating publication metadata as permission to move money.
