# Insurance reference commands

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

This HTTP wiring does not add a CURRENT observation endpoint. Historical recovery never substitutes for one. Original producer/SDK regeneration, real application HTTPS, native pool behavior and positive source-provider journeys require separate qualification of this source; no generated SDK or specification is edited by hand.

## Original specification and SDK generation

The original `generate-openapi` build profile enables the six controller declarations only in the lightweight `OpenApiGenApplication` process. Its existing documentation-only mock registrar supplies controller service interfaces; it does not start the production business pools or prove successful commands. Ordinary application invocations keep commands disabled by default.

The production OpenAPI metadata configuration also applies to that original producer. It declares the global bearer requirement, including legacy operations, and retains other documented requirements as additional conditions. The shared `FireflyInstant` scalar and the SDK's standard schema mapping preserve UTC Instant values without changing the legacy date library. The original Generator uses its standard oneOf interfaces and deduction options for tax and wording bodies; no discriminator field is added to requests.

`src/main/resources/openapi.yml` is the canonical producer output. Maven derives `/api-spec/openapi.yml` in the packaged SDK at `process-resources`; it is no longer a second source specification. The packaged equality and genuine SDK wire tests run during the original SDK build. Their success, the resulting class origins and real signed application use must be established by their own qualification gates.
