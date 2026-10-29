# Insurance reference commands and CURRENT

The master-data owner exposes six catalog commands at `/api/v1/insurance-references` when `firefly.masters.insurance-reference.commands.enabled=true`. The feature is disabled by default. Responses are historical action receipts, not current eligibility observations or financial authority.

| POST path | Current domain permission |
| --- | --- |
| `/publication-intents` | `masters.insurance-reference.submit` |
| `/withdrawal-intents` | `masters.insurance-reference.submit` |
| `/actions/recover` | `masters.insurance-reference.recover` |
| `/intents/approve` | `masters.insurance-reference.approve` |
| `/intents/execute` | `masters.insurance-reference.execute` |
| `/intents/withdraw` | `masters.insurance-reference.withdraw-intent` |

Every successful command returns HTTP200 with `Cache-Control: no-store`. Native replay reaches the owner service rather than the generic response cache. Recovery requires fresh permission and the original exact selectors; a missing response does not establish rollback or no effect. Invalid JSON returns400, requests over512KiB return413, unavailable current admission returns403, and a protected result that cannot be released returns503 with a static code. Private policy, source, SQL and commit details are not response bodies.

## Security and configuration

The owner bundles the shared global JWT resource-server and strict current-policy chain. This intentionally protects legacy routes too. The existing strict-policy default remains enabled; there is no anonymous or permissive fallback. Operators must configure URL-level authorization as well as the domain permissions above. A supplied operator chain or policy port retains its existing framework override precedence.

Before enabling commands, configure the shared resource server with the trusted `issuer`, explicit `audiences`, `tenant-required=true` and signed `tenant-claim-paths` under `firefly.security.resource-server`. Configure its real JWKS/decoder and the caller's issuer in `firefly.masters.insurance-reference.issuers`. The service retains the same framework-authenticated principal and requires its nonnull tenant projection; it never repairs the tenant from a request header or claim.

Provide `writer.url`, `writer.username`, `writer.password` and the corresponding `reader` properties under `firefly.masters.insurance-reference`, using external configuration or secret injection. URLs must identify the same PostgreSQL database without inline credentials. Use distinct restricted login identities with the existing `masters_ins_ref_writer` and `masters_ins_ref_reader` role contracts. Neither identity is a schema owner, guard or migrator. Deployment must install and attest the existing native schema separately.

Each graph owns its pool, exact factory, client, template, manager and native context. These are encapsulated rather than registered as additional global connection factories. Pools start with zero connections, have a maximum of four and a two-second acquisition cap. Existing service/native horizons still bound each operation. The business reader uses the existing physically writable repeatable-read transaction because its restricted private native context must be recorded; this does not grant catalog writes.

If no operator `PolicyDecisionPort` exists, explicitly select `policy.backend=opa` and configure `policy.base-uri` and `policy.decision-path` under the feature prefix. HTTPS is supported; HTTP is restricted to literal loopback addresses. Only the selected default backend binds these properties. Missing trust, storage or policy configuration fails startup when commands are enabled. Disabled commands create no feature settings, pools or service.

## Source evidence and limits

An actual `InsuranceReferenceSourceVerifier` must provide accepted current currency, locale and provenance/rights evidence where the existing operation requires it. The default verifier is unavailable. Catalog data, a digest or a sandbox label cannot replace that evidence. Independent approval, current permission, exact native graph and deadline checks remain in the existing service and repositories.

### Exact CURRENT observation

Separately enable `firefly.masters.insurance-reference.current.enabled=true` and provide its `issuers` and dedicated `reader.url`, `reader.username`, `reader.password`. The reader follows the existing `masters_ins_ref_reader` contract; this graph never binds command/writer credentials. The original global JWT/current-policy boundary still applies, along with `masters.insurance-reference.current` for the exact resource.

`GET /api/v1/insurance-references/{kind}/{recordId}/publications/{publicationId}/revisions/{revision}/current` requires exactly `definitionDigest` and `generation` query parameters. These select an immutable publication and its current local control generation; a receipt or intent generation cannot substitute. The service takes two real mapped repeatable-read snapshots around fresh source/currency observation, verifies the complete stored reference and every original/native/provider/effectivity cap, and rechecks the same current caller and PDP. It returns a technical finite observation horizon, not a new grant or financial authority. Historical recovery is independent and never becomes CURRENT.

CURRENT returns HTTP 200 with no-store only for the exact eligible reference, HTTP 400 for invalid selectors, HTTP 403 for denied current permission, HTTP 409 for known NOT_CURRENT and HTTP 503 for unavailable or uncertain evidence. A known negative read survives transaction wrapping only after acknowledged complete rollback, no commit/boundary and a still-live original/native cap. Cleanup, clock and arbitrary failures remain unavailable.

### Durable sandbox source facts

The separately default-off `firefly.masters.insurance-reference.sandbox-source.enabled` graph stores explicitly SANDBOX-scoped, immutable versioned locale snapshots, content-rights facts and exact publication-source definitions. Configure its `issuers` and distinct `writer`/`reader` URL/username/password properties. They must use the command database and trust set, with `masters_ins_ref_source_registrar` and `masters_ins_ref_source_reader`. The reader is physically read-only/repeatable-read; mapped writes use the dedicated registrar graph. These storage privileges never stand in for a current caller or PDP grant.

| POST path under `/api/v1/insurance-references/sandbox-facts` | Exact current operation |
| --- | --- |
| `/locales` | `masters.insurance-reference.sandbox-source.locale.register` |
| `/rights` | `masters.insurance-reference.sandbox-source.rights.register` |
| `/sources` | `masters.insurance-reference.sandbox-source.source.register` |
| `/withdraw` | `masters.insurance-reference.sandbox-source.<type>.withdraw` |

All four use strict bounded JSON, no-store and native owner replay rather than the generic response cache. Source reads separately require `masters.insurance-reference.sandbox-source.source.read`. Register a real locale snapshot and exact owned synthetic-content rights, then call the real publication SUBMIT. Source registration binds its actual reserved publication/revision and the complete caller-retained draft through protected recovery; independent approval and execution follow. A guessed UUID, digest, source URI or SANDBOX label never supplies eligibility. Locale mutation/restoration advances a durable epoch and cannot revive an old admission. Withdrawal preserves history and cannot reactivate the same version. Tax additionally needs the existing real current currency-policy connector; wording qualification does not establish positive tax/currency evidence.

Provision and attest the existing restricted owner/guard/writer/reader roles and both source roles before applying V34–V36, even while the feature flags remain off. Roles are cluster-global, so existing-role reuse and deployment ownership require explicit DBA review. Use separate administrative migration and runtime credentials; the source/schema guards do not grant runtime elevation. V36 corrects only deferred context/witness record dispatch and retains the original completion/clock/authority guards.

The 367 ordinary source tests and a fresh owned native36 service workflow qualify normal wording registration/publication/CURRENT, restart, approved withdrawal, exact negative CURRENT and historical recovery. The original Springdoc producer and Generator now emit all 212 operations, including CURRENT and source registration, without hand-editing generated contracts or SDK code. The coherent original Maven build passes 544 cases across 64 suites; its genuine SDK contains 261 generated Java files and 830 packaged classes, with both packaged specifications matching the emitted contract. Production application HTTPS and SDK transport acceptance for the new operations remain separate pending work. This source does not claim legal/financial admission, positive tax currency, all native failure cases or a complete document/customer journey.

## Original specification and SDK generation

The original `generate-openapi` build profile enables commands, CURRENT and sandbox-source declarations only in the lightweight `OpenApiGenApplication` process. Its existing documentation-only mock registrar supplies controller service interfaces; it does not start the production business pools or prove successful commands. Ordinary application invocations keep commands, CURRENT and sandbox-source features disabled by default. The complete 212-operation contract is emitted by that original documentation-only process; no generated output is patched by hand.

The production OpenAPI metadata configuration also applies to that original producer. It declares the global bearer requirement, including legacy operations, and retains other documented requirements as additional conditions. The shared `FireflyInstant` scalar and the SDK's standard schema mapping preserve UTC Instant values without changing the legacy date library. The original Generator uses its standard oneOf interfaces and deduction options for tax and wording bodies; no discriminator field is added to requests.

`src/main/resources/openapi.yml` is the canonical producer output. Maven derives `/api-spec/openapi.yml` in the packaged SDK at `process-resources`; it is no longer a second source specification. The packaged equality and genuine SDK wire tests run during the original SDK build. Their success, the resulting class origins and real signed application use must be established by their own qualification gates.
