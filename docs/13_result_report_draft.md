# 13. Result Report Draft

## Project Information

- Project name: Reqover
- Repository: https://github.com/Open-Source-Competition/Reqover
- Team name: `<팀명 입력>`
- Registration number: `<접수번호 입력>`
- Demo video URL: `<YouTube URL 입력>`

## 1. Project Summary

Reqover is a Java/Spring developer tool that attributes coverage hits to HTTP request endpoints.

Traditional coverage tools answer whether code executed. Reqover answers which observed request executed it. This helps developers choose regression test targets after a code change and understand runtime code paths by endpoint.

The MVP focuses on Spring MVC and Spring WebFlux applications. It provides a Java agent that inserts method-entry probes into application classes, request adapters that create per-request coverage buckets, and JSON/HTML reports that show endpoint-to-code and code-to-endpoint relationships.

## 2. Motivation

Modern backend systems often expose many endpoints that share services, utilities, and infrastructure code. When a method changes, developers usually rely on static call graphs, manual knowledge, or broad regression testing to decide which APIs to retest.

Line and branch coverage are useful, but they are global. They do not directly show which endpoint caused a covered line or method to execute. Reqover adds request attribution on top of coverage-style instrumentation.

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
- HTML report for demos

## 4. Architecture

```text
HTTP request
  -> Spring MVC interceptor or WebFlux filter
  -> CoverageBucket created
  -> application method executes
  -> Java agent inserted probe calls ReqoverProbe.hit(...)
  -> hit is routed to active request bucket
  -> request completes
  -> bucket snapshot stored in memory
  -> report generator aggregates endpoint coverage
```

The current MVP uses method-entry coverage. It does not claim JaCoCo-level branch precision.

## 5. Verification

Verified commands:

```powershell
.\gradlew.bat test
.\gradlew.bat build --quiet
.\gradlew.bat cyclonedxBom
```

Automated tests cover:

- core bucket hit routing
- global fallback behavior
- metadata registry
- ASM instrumentation
- Java agent smoke test in a separate JVM
- Spring MVC endpoint attribution
- Spring WebFlux thread-hop attribution
- agent-based MVC sample E2E without manual probes
- agent-based WebFlux sample E2E without manual probes

## 6. Demo Scenario

Demo 1 shows MVC endpoint-separated coverage.

Demo 2 shows code-to-endpoint reverse lookup for change impact analysis.

Demo 3 shows Java agent auto instrumentation without manual probe calls.

Demo 4 shows WebFlux thread-hop attribution.

Detailed commands are documented in `docs/10_demo_script.md`.

## 7. Open Source Readiness

Prepared files:

- `LICENSE`
- `THIRD_PARTY_NOTICES.md`
- `CONTRIBUTING.md`
- `SECURITY.md`
- `CODE_OF_CONDUCT.md`
- GitHub issue templates
- CycloneDX SBOM generation
- GitHub Actions build workflow

## 8. License and SBOM

Reqover source code is licensed under Apache License 2.0.

The project generates a CycloneDX SBOM:

```powershell
.\gradlew.bat cyclonedxBom
```

Expected output:

```text
build/reports/bom/reqover-sbom.json
```

JaCoCo is not currently linked or forked in the implementation. If a future version integrates or modifies JaCoCo internals, EPL-2.0 obligations must be reviewed separately.

## 9. AI Model Statement

Reqover does not embed an AI model in the submitted software.

If AI tools were used during development assistance, they are not part of the runtime artifact and no AI model weights or inference APIs are included in the project.

## 10. Limitations

- Coverage precision is method-entry level.
- In-memory storage is used for the MVP.
- Report UI is a demo-oriented HTML renderer.
- The Java agent include/exclude policy is minimal.
- WebFlux support is validated for standard Reactor chains, not arbitrary unmanaged threads.
- Production authentication, persistence, and retention policies are not implemented.

## 11. Future Work

- JaCoCo report interoperability spike
- source-line and branch-level precision improvements
- Gradle/Maven plugin packaging
- persistent storage backend
- richer report filtering and export
- larger performance benchmark
- authenticated report endpoint

