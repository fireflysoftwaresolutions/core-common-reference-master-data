# Shared Masters policy backend

An independently enabled Masters policy backend can protect document and currency observations without enabling insurance commands. It uses the framework's real `OpaPolicyDecisionAdapter`; it does not grant permissions or activate any business capability.

```yaml
firefly:
  masters:
    policy:
      enabled: true
      backend: opa
      base-uri: https://policy.example.com
      decision-path: firefly/masters/allow
```

The flag is off unless explicitly enabled. Enabling it requires all three settings. The URI must use HTTPS, or HTTP with a literal loopback host, and must not contain credentials, a query, a fragment, or a nonroot path. The decision path is a bounded sequence of letters, digits, underscores, or hyphens separated by single slashes. Invalid configuration refuses startup; there is no permissive neutral fallback.

A supplied `PolicyDecisionPort` bean remains authoritative, and both neutral and legacy default adapters back off. Otherwise the neutral configuration runs before the resource server and the three legacy insurance policy defaults, supplying one shared adapter. Insurance commands, current observations, and sandbox-source capabilities may coexist with it; their flags do not select a second policy service. Legacy policy settings may be absent. Any explicitly supplied legacy backend, URI, or decision path must equal the corresponding neutral setting, or startup refuses with `MASTERS_POLICY_CONFIGURATION_CONFLICT`.

When the neutral flag is off, the existing insurance configuration and framework defaults retain their behavior. Authentication, tenant projection, strict resource-server enforcement, each service's authorization checks, and the separate business feature flags still apply. Source configuration tests do not establish live PDP decisions, signed Main integration, native CURRENT acceptance, or product authority.
