# 10. Demo Script

## Goal

The demo should prove one idea quickly:

> Reqover shows which HTTP endpoint executed which code path.

Avoid explaining bytecode instrumentation first. Show the report first, then explain how it works.

## Demo 1. Endpoint-Separated Coverage

Start MVC sample:

```bash
./gradlew :examples:mvc-sample:bootRun
```

Call:

```text
GET  http://localhost:8080/orders/1
POST http://localhost:8080/payments
GET  http://localhost:8080/reqover/report.html
```

Show:

- `GET /orders/{id}` has order service coverage.
- `POST /payments` has payment service coverage.
- Shared validator code can appear in both endpoints.

Message:

```text
Traditional coverage says this code ran. Reqover adds which request made it run.
```

## Demo 2. Code-to-Endpoint Impact Lookup

Open:

```text
GET http://localhost:8080/reqover/report.html
```

Show the reverse index area.

Message:

```text
When a service method changes, the reverse index tells us which observed APIs should be retested first.
```

## Demo 3. Agent Auto Instrumentation

Run:

```bash
./scripts/run-agent-demo.sh mvc 8080
```

Then open:

```text
http://localhost:8080/reqover/report.html
```

Show:

- `/auto/orders/{id}` appears in the report.
- `AutoOrderController` and `AutoOrderService` appear even though they do not manually call `ReqoverProbe.hit(...)`.

Message:

```text
The application code does not need manual probe calls. The Java agent inserts method-entry probes.
```

## Demo 4. WebFlux Thread Hop

Run:

```bash
./scripts/run-agent-demo.sh webflux 8080
```

Then open:

```text
http://localhost:8080/reqover/report.html
```

Show:

- `/auto/reactive/orders/{id}` appears in the report.
- The endpoint has multiple thread names.
- The code is still attributed to the same endpoint bucket.

Message:

```text
Even when WebFlux changes threads, Reqover keeps the request coverage bucket attached.
```

## 3-Minute Video Structure

1. 0:00-0:20 Problem: coverage does not answer which API executed a line or method.
2. 0:20-0:55 MVC endpoint-separated report.
3. 0:55-1:25 Code-to-endpoint reverse lookup.
4. 1:25-2:10 Java agent auto instrumentation.
5. 2:10-2:40 WebFlux thread-hop attribution.
6. 2:40-3:00 Architecture, limits, next steps.

## Recording Checklist

- Use a clean terminal.
- Use browser zoom around 125% for readability.
- Keep one terminal for commands and one browser window for reports.
- Clear old app processes before recording.
- Mention that Reqover is method-entry coverage today, not JaCoCo-level branch coverage.
- Mention Apache-2.0 project license and generated SBOM.

For non-interactive checks, add `-StopAfterReport` so the script stops the sample application immediately after printing the report JSON.
