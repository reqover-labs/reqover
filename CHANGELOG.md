# Changelog

All notable changes to Reqover are documented in this file.

## 0.2.0 - 2026-08-21

Reports now outlive the JVM that produced them, which is what lets Reqover
answer a question in CI instead of only on screen: **which observed APIs execute
the code this change touches?**

### Added

- **Report persistence.** `CoverageReportJson` writes and reads a report as
  JSON with no dependency beyond `reqover-core`. Output is sorted and
  pretty-printed so a committed baseline diffs cleanly.
- **Impact analysis.** `ImpactAnalyzer` matches changed source paths against the
  reverse index and names the endpoints observed executing that code. Paths with
  no observed coverage are reported separately rather than silently dropped.
- **Report diffing.** `CoverageReportDiff` compares two reports: endpoints seen
  on only one side, and the code an endpoint started or stopped executing.
- **`reqover-cli`**, a shaded executable jar with `render`, `diff`, and `impact`
  commands, `text`/`markdown`/`json` output, and `--fail-on-impact` /
  `--fail-on-change` gates. Exit codes: 0 success, 1 gate tripped, 2 bad usage.
- **`reqover-spring-boot-starter`**, so wiring Reqover is one dependency. It adds
  an opt-in HTTP report endpoint (`reqover.report.endpoint.enabled`, **off by
  default**) and a shutdown export (`reqover.report.export.json-path`,
  `.html-path`) that gives a CI job a report file from an integration test run.
- **A `CoverageStore` SPI** so retention is replaceable. `InMemoryCoverageStore`
  is now one implementation of it rather than the only option. (#3)
- **`UnitScope`** and `UnitInfo` factories for units of work that are not HTTP
  requests — scheduled jobs, message listeners, single test cases.
- **In-report filtering.** The HTML report has a text filter over endpoints,
  classes, and methods, matching both JVM descriptors and their readable form.
  The control stays hidden without scripting, and the page still contains every
  row. (#5)
- **A signed Maven Central publication pipeline**: `./gradlew centralBundle`
  stages and signs the upload bundle, and the release workflow uploads it once
  the repository sets `PUBLISH_TO_MAVEN_CENTRAL` and the signing secrets.
- **A reusable `impact` GitHub Action** that comments the endpoints to retest on
  a pull request.
- Configuration for what used to be hard-coded: `reqover.mvc.enabled`,
  `reqover.mvc.include-path-patterns`, `reqover.mvc.exclude-path-patterns`,
  `reqover.mvc.max-snapshots`, `reqover.webflux.exclude-path-prefixes`, and
  `reqover.webflux.max-snapshots`.

### Changed

- The MVC and WebFlux adapters contribute a `CoverageStore` bean instead of an
  `InMemoryCoverageStore` bean. Applications that inject the store by concrete
  type need to change the injection point to `CoverageStore`.
- `CoverageBucket.finish(int)` returns whether this call is the one that
  finished the bucket, so a custom adapter can flush exactly once.
- The MVC auto-configuration now honours an enable flag, matching WebFlux. The
  WebFlux auto-configuration gained `@ConditionalOnClass` so a servlet-only
  application can depend on the starter safely.
- The demo applications use the starter and its report endpoint instead of
  hand-written controllers.

### Known limitations

- Method-entry granularity only; no line or branch coverage
- The default store is still in memory; the SPI makes another one possible but
  Reqover ships no persistent implementation
- Servlet async worker execution is not propagated before async re-dispatch
- Report endpoint authentication is an application responsibility
- Impact analysis reports what was observed while recording; it is a starting
  point for retesting, not a complete change-impact analysis

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
