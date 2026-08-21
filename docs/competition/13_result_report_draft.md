# 13. Result Report Draft

> Working source only. The official A4 DOCX/PDF generated from the contest template is the submission artifact. Numeric claims in this Markdown file must be refreshed from the final release candidate before use.

## Project Information

- Project name: Reqover
- Repository: https://github.com/reqover-labs/reqover
- Team name: `<팀명 입력>`
- Registration number: `<접수번호 입력>`
- Demo video URL: `<YouTube URL 입력>`

## 1. Project Summary

Reqover is a Java/Spring developer tool that attributes coverage hits to HTTP request endpoints.

Traditional coverage tools answer whether code executed. Reqover answers which observed request executed it. This helps developers choose regression test targets after a code change and understand runtime code paths by endpoint.

The implementation focuses on Spring MVC and Spring WebFlux applications. It provides a Java agent that inserts method-entry probes into application classes, request adapters that create per-request coverage buckets, and JSON/HTML reports that show endpoint-to-code and code-to-endpoint relationships.

Version 0.2.0 takes that record out of the browser. A report can be written to a file, and a command line tool turns it plus a list of changed files into the list of endpoints to retest — as a pull request comment, or as an exit code a pipeline can gate on.

## 2. Motivation

Modern backend systems often expose many endpoints that share services, utilities, and infrastructure code. When a method changes, developers usually rely on static call graphs, manual knowledge, or broad regression testing to decide which APIs to retest.

Line and branch coverage are useful, but they are global. They do not directly show which endpoint caused a covered line or method to execute. Reqover adds request attribution on top of coverage-style instrumentation.

The decision that shaped 0.2.0 followed from using 0.1.x: an attribution report that only exists while the application is running is consulted rarely, because someone has to remember to open it. The same data delivered into code review — at the moment a reviewer is deciding what to retest — is consulted every time. That is why the release is built around persisting the report and answering a question about a diff, rather than around a richer page.

## 3. Main Features

### Request-Scoped Coverage Bucket

Each HTTP request receives a coverage bucket. Probe hits during request handling are recorded into the active bucket instead of a single global coverage store.

### Spring MVC Support

The MVC adapter uses a Spring interceptor to create and clear request coverage context. It records normalized endpoint patterns such as `GET /orders/{id}`.

### Spring WebFlux Support

The WebFlux adapter uses Reactor Context and Micrometer Context Propagation to keep request attribution across thread hops. This is the main technical differentiator of the MVP.

### Java Agent Auto Instrumentation

Reqover provides a `-javaagent` entry point. It instruments selected application packages at class-load time using ASM and inserts `ReqoverProbe.hit(classId, probeId)` at method entry.

### Reports

The report module generates:

- endpoint-to-code coverage
- code-to-endpoint reverse index
- request count
- request IDs
- observed thread names
- class, method, descriptor, and first line metadata where available
- a self-contained HTML report with client-side filtering over endpoints, classes, and methods
- a JSON document that can be written to disk and read back in another JVM

### Change Impact Analysis in CI (0.2.0)

A report written to disk is fully resolved: class and method names are inside the document, so nothing needs the JVM that produced it. That is what makes the reverse index usable outside the browser.

`reqover-cli` reads such a report and answers which observed endpoints executed the code a change touched. Changed source paths are matched against the reverse index by package-shaped path suffix, so Gradle, Maven, and multi-module layouts all work without configuration. Output is text, Markdown for a pull request comment, or JSON for another tool. `--fail-on-impact` turns the analysis into a pipeline gate, with exit code 1 reserved for a tripped gate and 2 for bad usage or input.

The starter can export the report when the application context closes, so an integration test run leaves a report file behind. A composite GitHub Action wraps the whole sequence and comments the result on the pull request.

This is the capability that distinguishes Reqover from reading a coverage number: the reverse lookup becomes an answer in review, not a page someone remembers to open.

### Storage SPI and Non-HTTP Units of Work (0.2.0)

`CoverageStore` separates attribution from retention; `InMemoryCoverageStore` is one implementation of it, with a configurable bound. `UnitScope` opens a bucket for a unit of work that is not an HTTP request — a scheduled job, a message listener, a single test case — which the `UnitInfo` record was always general enough to describe.

## 4. Architecture

```text
HTTP request
  -> Spring MVC interceptor or WebFlux filter
  -> CoverageBucket created
  -> application method executes
  -> Java agent inserted probe calls ReqoverProbe.hit(...)
  -> hit is routed to active request bucket
  -> request completes
  -> bucket snapshot handed to a CoverageStore
  -> report generator aggregates endpoint coverage
  -> HTML for a reader, or JSON to a file
  -> CLI: impact analysis against a diff, or a diff of two recordings
  -> exit code, job summary, or pull request comment
```

The implementation uses method-entry coverage. It does not claim JaCoCo-level branch precision.

## 5. Verification

Verified commands:

```bash
./gradlew test
./gradlew build --quiet
./gradlew cyclonedxBom
```

Automated tests cover:

- core bucket hit routing
- global fallback behavior
- metadata registry
- unit-of-work scoping for non-HTTP work, including flush-exactly-once
- ASM instrumentation
- Java agent smoke test in a separate JVM
- Spring MVC endpoint attribution
- Spring WebFlux thread-hop attribution
- agent-based MVC sample E2E without manual probes
- agent-based WebFlux sample E2E without manual probes
- MVC and WebFlux auto-configuration conditions, property binding, and store back-off
- report JSON round-trip, malformed-input rejection, and deterministic output
- impact analysis path matching, including nested classes and non-matching neighbours
- report diffing between two recordings
- CLI commands, output formats, and exit codes
- report endpoint conditions and the shutdown export

The CI build additionally runs the whole loop end to end: it boots the demo application under the agent, exports a report on shutdown, and fails if the impact analysis does not name the endpoint that executed the changed class.

Performance evidence is generated from the final release candidate with the same endpoint in baseline and agent-enabled modes. It is treated as a local sequential sanity check, not a production benchmark. Details and raw samples are in `docs/15_performance_results.md` and `docs/evidence/performance/`.

## 6. Demo Scenario

Demo 1 shows MVC endpoint-separated coverage.

Demo 2 shows code-to-endpoint reverse lookup for change impact analysis.

Demo 3 shows Java agent auto instrumentation without manual probe calls.

Demo 4 shows WebFlux thread-hop attribution.

Demo 5 shows the CI loop: `scripts/run-impact-demo.sh` records traffic, exports the report on shutdown, and asks which endpoints a change to one demo class would affect. The script fails if the analysis does not name the expected endpoint, so it doubles as a test.

Detailed commands are documented in `docs/10_demo_script.md` and `docs/18_ci_impact_analysis.md`.

## 7. Open Source Readiness

Prepared files:

- `LICENSE`
- `THIRD_PARTY_NOTICES.md`
- `CONTRIBUTING.md`
- `SECURITY.md`
- `CODE_OF_CONDUCT.md`
- GitHub issue templates
- CycloneDX SBOM generation and an OSV dependency scan that fails the build on a finding
- GitHub Actions build workflow across Java 17 and 21, with every third-party action pinned by commit SHA
- a tag-driven release workflow that verifies the tag matches the project version and targets `main`
- a signed Maven Central publication pipeline (`./gradlew centralBundle`), inert until the repository opts in
- a reusable composite GitHub Action other projects can adopt
- bilingual documentation: README and the integration, architecture, and CI guides exist in English and Korean

## 8. License and SBOM

Reqover source code is licensed under Apache License 2.0.

The project generates a CycloneDX SBOM:

```bash
./gradlew cyclonedxBom
```

Expected output:

```text
build/reports/bom/reqover.cdx.json
```

JaCoCo is not currently linked or forked in the implementation. If a future version integrates or modifies JaCoCo internals, EPL-2.0 obligations must be reviewed separately.

## 9. AI Model Statement

상용 생성형 AI(Codex)는 일부 코드·테스트·문서의 초안 작성 및 검토 보조에 활용했으며, 팀이 요구사항을 결정하고 결과를 검증·수정하여 최종 반영했다. 출품작에는 AI 모델이나 외부 추론 API가 포함되지 않는다.

## 10. Limitations

- Coverage precision is method-entry level.
- The default store keeps snapshots in memory only. `CoverageStore` is the extension point for storing them elsewhere, but no persistent implementation ships; exporting the report to a file is the supported way to keep one.
- Impact analysis is bounded by what was observed while recording. A changed file it cannot match is reported as having no observed coverage, which means "not seen", not "not affected".
- The Java agent requires explicit package-prefix includes and always excludes JDK, ASM, and Reqover runtime packages.
- WebFlux support is validated for standard Reactor chains, not arbitrary unmanaged threads.
- Servlet async worker execution is not attributed before the request is re-dispatched.
- The report endpoint ships disabled and carries no authentication of its own; guarding it is the application's responsibility.
- The shutdown export runs on normal context close, so a process terminated with `SIGKILL` writes nothing.
- The signed Maven Central publication pipeline is implemented but has not been run, so no artifact resolves from Central yet.

## 11. Future Work

- JaCoCo report interoperability spike
- source-line and branch-level precision improvements
- Gradle/Maven plugin packaging, so a build can record and analyse without a shell script
- a persistent `CoverageStore` implementation on top of the SPI
- first Maven Central release through the prepared pipeline
- larger performance benchmark
- linking attribution to trace ids so a report can be correlated with distributed traces
