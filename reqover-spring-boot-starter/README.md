**English** | [한국어](README.ko.md) | [Project](../README.md)

# Spring Boot Starter

The usual entry point for Spring Boot applications: core, report and MVC/WebFlux
adapters in one Maven Central dependency. Java packages remain `io.reqover.*`.

```kotlin
implementation("io.github.reqover-labs:reqover-spring-boot-starter:0.4.2")
```

Attach the [agent](../reqover-agent/README.md) to record application methods.
Choose how to read the report; both the HTTP endpoint and file export are off
unless configured. For a local development application:

```properties
server.address=127.0.0.1
reqover.report.endpoint.enabled=true
reqover.report.export.json-path=build/reqover-report.json
reqover.report.export.html-path=build/reqover-report.html
```

JSON is served at `/reqover/report` and HTML at `/reqover/report.html` by default.
There is no built-in report authentication; use your application's access policy.
File export runs on normal context close, not `SIGKILL`. Contexts exporting to
the same path in one JVM accumulate; the first export replaces prior-run files.

The default store bounds request details but preserves recording-wide endpoint
aggregates within the [store's aggregate limits](../reqover-core/README.md).
JSON exports cap recent details separately. Custom `CoverageStore`
beans replace retention; full aggregates require implementing `aggregates()`.

[Properties and integration](../docs/17_integration_guide.md) |
[Request diagnostics](../docs/24_request_diagnostics.md) |
[CI recording](../docs/18_ci_impact_analysis.md)
