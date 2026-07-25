# 08. Phase 0 MVP Status

## Current Result

Reqover now has a working Phase 0 MVP.

Implemented:

- Core coverage bucket model
- ThreadLocal-based coverage context
- Static probe entry point: `ReqoverProbe.hit(classId, probeId)`
- Probe metadata registry
- In-memory completed bucket store
- Endpoint-level report model
- Code-to-endpoint reverse index
- HTML report renderer
- Spring MVC interceptor adapter
- Spring WebFlux WebFilter adapter
- Spring Boot auto-configuration metadata for MVC and WebFlux adapters
- Reactor Context to ThreadLocal bridge using Micrometer Context Propagation
- MVC sample app
- WebFlux sample app
- ASM method-entry bytecode instrumenter
- Java agent with `premain`
- Separate JVM smoke test for `-javaagent`
- Agent-based Spring MVC E2E test without manual probes
- Agent-based Spring WebFlux E2E test without manual probes

## Verified Behaviors

### Core

- Hits are recorded into the active bucket.
- Hits without active context fall back to the global bucket.
- Snapshots are immutable.
- Probe metadata can be registered and resolved.

### Spring MVC

- Each request creates a coverage bucket.
- Endpoint patterns are captured, for example `GET /orders/{id}`.
- Buckets are flushed after request completion.
- ThreadLocal context is cleared after completion.
- Concurrent `GET /orders/{id}` and `POST /payments` requests remain separated.
- JSON and HTML report endpoints are available in the sample app.
- Reports include code-to-endpoint reverse lookup for change impact demos.

### Spring WebFlux

- Each request creates a coverage bucket.
- Reactor Context carries the bucket across thread hops.
- Micrometer Context Propagation restores the bucket into ThreadLocal for `ReqoverProbe.hit`.
- The WebFlux sample records hits from multiple Reactor threads in one request bucket.
- JSON and HTML report endpoints are available in the sample app.
- Reports include code-to-endpoint reverse lookup for change impact demos.

### Instrumentation

- ASM inserts `ReqoverProbe.hit(classId, probeId)` at method entry.
- Constructors, class initializers, abstract, native, and synthetic methods are skipped.
- Probe metadata is generated during instrumentation.
- A separate JVM smoke test confirms the packaged Java agent instruments a target class through `-javaagent`.
- Spring MVC and WebFlux sample boot jars are started in separate JVMs with `-javaagent`.
- Auto-instrumented sample endpoints are verified through `/reqover/report`.
- The WebFlux auto-instrumentation E2E test verifies attribution after a Reactor thread hop.

## What This Proves

Phase 0 proves the central technical assumption:

> A probe hit can be routed into the active HTTP request bucket, including across a WebFlux thread hop, instead of being recorded only as global coverage.

This does not yet claim JaCoCo-level line or branch coverage. The MVP proves request-level attribution and provides a base for later JaCoCo/report integration.

## Known Limitations

- Coverage precision is method-entry level for the custom instrumentation path.
- JaCoCo analysis/report integration is not implemented yet.
- WebFlux support is validated for standard Reactor chains, not arbitrary raw threads or fire-and-forget tasks.
- Storage is in-memory only.
- Agent include/exclude configuration is intentionally minimal.
- Spring auto-configuration is present, but advanced conditional customization is minimal.
- Agent instrumentation currently records method entry, not exact line hit ranges.

## Commands

Run all tests:

```powershell
.\gradlew.bat test
```

Build all artifacts:

```powershell
.\gradlew.bat build
```

Run MVC sample:

```powershell
.\gradlew.bat :examples:mvc-sample:bootRun
```

Run WebFlux sample:

```powershell
.\gradlew.bat :examples:webflux-sample:bootRun
```

Build the agent:

```powershell
.\gradlew.bat :reqover-agent:jar
```

Example agent usage:

```powershell
java -javaagent:reqover-agent\build\libs\reqover-agent-0.1.0-SNAPSHOT.jar=include=io.reqover.example.mvc -jar examples\mvc-sample\build\libs\mvc-sample-0.1.0-SNAPSHOT.jar
```

Run the Spring agent E2E tests:

```powershell
.\gradlew.bat :reqover-agent:test --tests io.reqover.agent.AgentSpringE2ETest
```

## Demo Endpoints

MVC sample:

```text
GET  /orders/{id}
POST /payments
GET  /auto/orders/{id}
GET  /reqover/report
GET  /reqover/report.html
```

WebFlux sample:

```text
GET /reactive/orders/{id}
GET /auto/reactive/orders/{id}
GET /reqover/report
GET /reqover/report.html
```

## Next Engineering Steps

1. Add source line metadata where feasible.
2. Add JaCoCo analysis integration spike.
3. Add Gradle plugin or documented agent-based run task.
4. Add performance measurement for baseline vs Reqover-enabled requests.
5. Improve HTML report navigation and filtering.
