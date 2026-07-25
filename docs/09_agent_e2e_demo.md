# 09. Agent E2E Demo

## Purpose

This demo proves that Reqover can attribute automatically inserted probe hits to the active HTTP request bucket.

The important distinction is that the auto demo endpoints do not call `ReqoverProbe.hit(...)` manually. Instead, the Java agent instruments application classes at load time and inserts method-entry probes.

## Demo Target

MVC auto endpoint:

```text
GET /auto/orders/{id}
```

Expected reported classes:

```text
io.reqover.example.mvc.auto.AutoOrderController
io.reqover.example.mvc.auto.AutoOrderService
```

WebFlux auto endpoint:

```text
GET /auto/reactive/orders/{id}
```

Expected reported classes:

```text
io.reqover.example.webflux.auto.AutoReactiveOrderController
io.reqover.example.webflux.auto.AutoReactiveOrderService
```

## Build

```powershell
.\gradlew.bat :reqover-agent:jar :examples:mvc-sample:bootJar :examples:webflux-sample:bootJar
```

## MVC Demo

Start the MVC sample with the agent:

```powershell
java -javaagent:reqover-agent\build\libs\reqover-agent-0.1.0-SNAPSHOT.jar=include=io.reqover.example.mvc.auto -jar examples\mvc-sample\build\libs\mvc-sample-0.1.0-SNAPSHOT.jar
```

Call the auto endpoint:

```text
GET http://localhost:8080/auto/orders/42
```

Open the report:

```text
GET http://localhost:8080/reqover/report
GET http://localhost:8080/reqover/report.html
```

The report should include `GET /auto/orders/{id}` and the auto MVC controller/service classes.

## WebFlux Demo

Start the WebFlux sample with the agent:

```powershell
java -javaagent:reqover-agent\build\libs\reqover-agent-0.1.0-SNAPSHOT.jar=include=io.reqover.example.webflux.auto -jar examples\webflux-sample\build\libs\webflux-sample-0.1.0-SNAPSHOT.jar
```

Call the auto endpoint:

```text
GET http://localhost:8080/auto/reactive/orders/42
```

Open the report:

```text
GET http://localhost:8080/reqover/report
GET http://localhost:8080/reqover/report.html
```

The report should include `GET /auto/reactive/orders/{id}` and the auto WebFlux controller/service classes.

## Automated Verification

The automated E2E test starts the Spring Boot sample jars as separate JVM processes with `-javaagent`.

```powershell
.\gradlew.bat :reqover-agent:test --tests io.reqover.agent.AgentSpringE2ETest
```

The test verifies:

- MVC sample starts with the Reqover agent.
- WebFlux sample starts with the Reqover agent.
- Auto endpoints return HTTP 200.
- `/reqover/report` includes endpoint patterns.
- `/reqover/report` includes auto-instrumented controller and service class names.
- WebFlux attribution survives the Reactor thread hop used by the sample service.

## Troubleshooting Notes

### Gradle cannot resolve `BootJar` in `reqover-agent`

The agent module should not directly reference the Spring Boot Gradle task type from its own build script. The current build avoids that coupling by depending on `:examples:mvc-sample:bootJar` and `:examples:webflux-sample:bootJar`, then passing the expected jar paths as system properties to the test.

### Report does not contain auto classes

Check the agent include/exclude configuration first.

Reqover's default excludes must protect internal runtime packages such as `io.reqover.core.` and `io.reqover.agent.`, but they must not exclude the sample package `io.reqover.example.`. If the broad prefix `io.reqover.` is excluded, the demo application classes will never be instrumented.

### `/reqover/report` is empty

Check these conditions:

- The application was started with `-javaagent`.
- The `include=` prefix matches the package of the target classes.
- A demo endpoint was called before opening the report.
- The report endpoint itself is excluded from request capture to avoid polluting coverage data.

### WebFlux reports global hits instead of endpoint hits

This means the request bucket is not being restored into `CoverageContext` when a method-entry probe runs. The WebFlux adapter relies on Reactor Context and Micrometer Context Propagation to bridge the request bucket back into the ThreadLocal context used by `ReqoverProbe.hit(...)`.

