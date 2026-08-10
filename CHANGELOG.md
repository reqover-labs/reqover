# Changelog

All notable changes to Reqover are documented in this file.

## 0.1.0 - 2026-08-10

Initial developer/QA release.

### Added

- Request-scoped coverage buckets for Spring MVC and Spring WebFlux
- Reactor Context propagation across `reactor-http-nio`, `boundedElastic`, and
  `parallel` scheduler hops
- ASM method-entry instrumentation through a fail-closed Java agent
- Endpoint-to-code JSON/HTML reports and a code-to-endpoint reverse index
- Separate-JVM MVC/WebFlux agent E2E tests, including concurrent request isolation
- Java 17 and Java 21 verification workflow
- CycloneDX 1.6 SBOM, OSV dependency scan, and release license bundle

### Known limitations

- Method-entry granularity only; no line or branch coverage
- In-memory snapshot storage
- Servlet async worker execution is not propagated before async re-dispatch
- Report endpoint authentication is an application responsibility
