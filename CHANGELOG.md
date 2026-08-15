# Changelog

All notable changes to Reqover are documented in this file.

## 0.1.1 - 2026-08-14

Report readability. No change to instrumentation, attribution or the JSON report.

### Changed

- HTML report renders JVM descriptors the way a reader says them:
  `find(J)Lcom/example/OrderResponse;` is shown as `find(long): OrderResponse`.
  Unparseable descriptors are printed verbatim rather than hidden.
- The summary line reports how many methods are reached by more than one
  endpoint, replacing three counters that restated what the page already listed.
  Those methods are highlighted in both directions of the report.
- A class instrumented through more than one registration path is listed once
  instead of repeated per class id.
- Endpoints carry the HTTP verb as the only colour on the page; request ids and
  thread names are monospace text rather than pills.
- Wider content column so long Java descriptors stop wrapping on every row.

### Added

- Dark presentation when the reader's system asks for it.

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
