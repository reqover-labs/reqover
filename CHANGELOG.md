# Changelog

All notable changes to Reqover are documented in this file.

## Unreleased

### Fixed

- **Hibernate 6 and Mockito proxies are excluded too.** Hibernate 6 names its
  proxy `Order$HibernateProxy` with no suffix, which the earlier marker missed,
  and a Mockito mock (`$MockitoMock$`) was recorded as application code.
- **Builder and fluent setters count as trivial accessors.** On a real
  Lombok-heavy service builder methods were a fifth of the reverse index.

- **A test run's report covers every application context, not just the last
  one closed.** Spring's test context cache keeps one context per distinct
  configuration and closes them all at JVM exit, and each overwrote the
  others' export. On a real 167-test Spring Boot suite with five contexts, the
  exported report listed 20 of the 50 endpoints the tests called. Exports to
  one path in the same JVM now accumulate; the first export of a run still
  replaces the file. Request ids are unique across the JVM so the merged
  report does not reuse `req-1`.

- **Known Jackson and Tomcat advisories are patched.** Jackson moves to 2.21.7
  and embedded Tomcat core/EL/WebSocket to 10.1.60 in the MVC sample and the
  starter's test runtime, clearing the OSV dependency scan. Published starter
  consumers get no new Tomcat constraint. The SBOM is regenerated and the
  blocking scan policy is unchanged.

- **A report from a newer schema is refused instead of misparsed.** The writer
  has always stamped `schemaVersion`, but the reader ignored it, so a future
  document fed to an older build would parse into a silently wrong result. A
  document with no `schemaVersion` still reads as version 1.

### Changed

- **Impact Action artifact upload is opt-in.** `upload-artifact` defaults to
  `false` to avoid fixed-name collisions in matrix or repeated invocations.
  Explicit uploads must use distinct names. The Action requires Python 3 and
  fails early on shallow history, before any base fetch or CLI download.

- **`accessors=record` agent option.** Skipping trivial accessors hides a class
  made only of them, such as a request DTO record, from impact analysis.
  Recording for CI can keep them; the default is unchanged.

- **Reports no longer list each Spring bean twice.** Runtime proxy classes
  (`$$SpringCGLIB$$`, `$HibernateProxy$`, `$ByteBuddy$`) sit in the
  application's package, so an `include` matched them and every proxied service
  appeared next to its own `$$SpringCGLIB$$0` twin. They are now always
  excluded; the real method behind the proxy is still recorded.
- **Trivial accessors are not instrumented.** A method whose whole body reads
  or writes one of its own instance fields — a Lombok `@Getter`/`@Setter` or a
  record accessor — runs whenever Jackson serializes the object, so a shared
  response envelope topped the reverse lookup as "used by every API". The
  match is on the bytecode shape, not the name: a getter that computes
  anything is still recorded, and so is a request handler — a method with a
  Spring web annotation, or any method of a `@Controller`/`@RestController`.
  A DTO with only field accessors no longer appears in the reverse lookup.
- **Requests that match no controller are not recorded.** Spring Boot maps a
  static resource handler to `/**`, so every unmapped URL piled up under one
  `GET /**` endpoint. The MVC interceptor and the WebFlux filter now skip
  requests served by `ResourceHttpRequestHandler` / `ResourceWebHandler` under
  that catch-all pattern. A resource handler you map yourself, such as
  `/downloads/**`, is still recorded.

- **Agent overhead is measured over alternating rounds** rather than one
  baseline-then-agent run. The retired 2026-08-10 capture reported the agent as
  *faster* than the baseline, which is what run-order drift produces. The new
  measurement gives about 24 ns per instrumented method entry and states what
  it does not cover. See [docs/15_performance_results.md](docs/15_performance_results.md).

### Added

- **Retained recording comparison.** Import/export aggregate HTTP summaries
  without executing requests. Explicit comparability confirmation is required.
  Arithmetic-only drift is suppressed with an epsilon; missing `endedAt` marks
  unfinished observations. Truncated detail exports are rejected as baselines.

- **Reviewed test drafts.** Selected observations can produce JSON drafts and
  disabled JUnit 5 tests for manually reviewed GET/HEAD requests. No original
  inputs or faithful replay are assumed. Raw and encoded pipe characters are
  rejected before URI generation, with actual Java compilation coverage.

- **Offline diagnostic dashboard and CI artifacts.** Animated observed
  associations and retest candidates preserve the table fallback; downloadable
  HTML includes icon notices. The Action can render HTML, expose impact outputs
  and retain evidence before an optional impact gate.

- **Retained request diagnostics.** HTTP status counts, average/p95/maximum
  adapter-observed intervals and per-request method sets are available in the
  report. JSON exports default to the newest 100 unit details and report the
  omitted count, without truncating endpoint unions or the reverse index. The
  four-argument `CoverageReport` constructor remains available; Java record
  patterns must adapt to its new fifth component in the next minor release.

- **A configurable eviction policy for the in-memory store.**
  `reqover.mvc.snapshot-eviction` and `reqover.webflux.snapshot-eviction`
  choose what happens at `max-snapshots`: `oldest-first` (the default, and the
  only behaviour until now) or `reject-when-full`, which keeps the first N
  snapshots and ignores later flushes. The slot is reserved with a CAS, so
  concurrent flushes cannot overshoot the bound.
- **`CoverageStoreContract`**, an abstract JUnit suite in the `reqover-core`
  tests that a second `CoverageStore` implementation extends to pin the SPI's
  invariants — in particular that `snapshots()` stays stable while another
  thread is flushing. (#14; contributed in #15 by @VedantMadane, the first
  change to Reqover from outside the team)
- **Compatibility policy** ([docs/20_versioning_and_compatibility.md](docs/20_versioning_and_compatibility.md)):
  what a version number promises, what counts as public API, how the report
  schema evolves, and what happens when a release is broken. Maven Central is
  permanent, so this had to exist before the first upload (#4).
- **Prior art** ([docs/19_prior_art.md](docs/19_prior_art.md)): where Reqover
  sits next to OpenClover, the commercial test-impact products, APM tracing,
  and traffic replay — including the cases where one of those is the better
  answer.
- **Project governance, roadmap, and support docs**, and GitHub Discussions as
  the place for questions.
- **`/auto/depth/{n}`** in the MVC sample: an endpoint that walks a chosen
  number of instrumented method entries, so overhead can be measured as a
  function of instrumented surface rather than at a single point.

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
