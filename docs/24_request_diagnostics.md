**English** | [한국어](24_request_diagnostics.ko.md) | [Documentation](README.md)

# Recorded request diagnostics

Since 0.4.0 the report preserves individual observations as well as the
endpoint union. Its overview shows recorded HTTP request counts, 4xx/5xx status
counts, average/p95/maximum recorded processing intervals, and an endpoint table
ranked by cumulative interval. Expand a request to see its timestamps, status,
thread names and independently resolved method set.

![Request diagnostics from 900 dummy requests to the MVC sample, with intentional slow and 503 endpoints](assets/reqover-request-diagnostics.png)

![A slow request expanded to show its timestamps and recorded code](assets/reqover-request-detail.png)

Screenshots were captured on October 3, 2026 from the agent-attached MVC sample
on loopback. Numbers illustrate that recording, not an application benchmark.

## Using it with 0.4.x

Request diagnostics need no extra switch of their own. Add the starter from Maven Central and
attach the agent from the [v0.4.2 release](https://github.com/reqover-labs/reqover/releases/tag/v0.4.2):

```kotlin
implementation("io.github.reqover-labs:reqover-spring-boot-starter:0.4.2")
```

```bash
java -javaagent:reqover-agent-0.4.2.jar=include=com.example -jar build/libs/your-app.jar
```

Turn on a way to read the report, which is off by default: either
`reqover.report.endpoint.enabled=true` and open `/reqover/report.html` while the
application runs, or `reqover.report.export.html-path` / `json-path` for a file
at shutdown. See the [integration guide](17_integration_guide.md#3-decide-how-you-read-the-report)
for Maven and the full property list.

### Bundled demo

From a checkout of the repository (for example the `v0.4.2` tag), the demo
script builds the agent and a sample and runs them together:

```powershell
.\scripts\run-agent-demo.ps1 -App mvc -Port 8080
```

While the script is waiting, in a second terminal:

```powershell
Invoke-RestMethod http://127.0.0.1:8080/auto/diagnostics/delay/1200
Invoke-WebRequest http://127.0.0.1:8080/auto/diagnostics/failure -SkipHttpErrorCheck
```

Open `http://127.0.0.1:8080/reqover/report.html`. The failure endpoint deliberately
returns 503. Delay accepts only 0–2000 ms. These are read-only synthetic examples,
not an application benchmark. On macOS/Linux use `./scripts/run-agent-demo.sh`
and `curl` for the same GET requests.

For WebFlux, run the webflux sample and use
`/auto/reactive/diagnostics/delay/1200` and `/auto/reactive/diagnostics/failure`.
Reactive delay completes after subscription, not when its Mono factory returns.

## Reading the numbers

- **Observed processing time:** the wall-clock interval from the adapter's
  bucket creation to its finish callback. In MVC this starts after handler
  mapping. It is not full network response time, CPU time, method duration or
  DB time. Clock adjustments can affect it; monotonic timing is follow-up work.
- **Timed samples:** unfinished or negative intervals are excluded. Zero and
  submillisecond intervals are valid. Missing intervals are not zero-filled.
- **p95:** nearest-rank over valid retained HTTP intervals. A small recording
  has a small sample; the displayed sample count matters.
- **HTTP failures:** 4xx and 5xx are separate. The displayed percentage uses
  known final statuses (200–599) as its denominator. Unknown status is reported
  separately. An expected 4xx can be a passing test; this is not an assertion.
- **Cumulative:** sum of recorded valid intervals for that endpoint, equivalent
  to average times valid count for the same retained window. It is not a CPU
  or system resource score and not a business-impact estimate.

The store's existing bound/eviction policy still controls which requests are
available. These are retained-observation statistics, not all traffic or TPS.
The overview totals stay fixed while a text or status/slow filter narrows the
list. HTML expands at most the most recent 100 HTTP observations. JSON exports
also default to the newest 100 unit details, preserving their original order,
and include `omittedRequestDetails` when older details were left out. Endpoint
unions, the reverse index and completed request count still preserve the full
recording aggregates supplied by the store, including evicted observations.
Timing/status statistics cover only retained details, not those aggregates.
Statistics rendered from exported JSON cover only its exported detail
window. A trusted local caller can select a different cap with
`CoverageReportJson.write(report, requestDetailsLimit)`, including zero to omit
details. Use small package scopes and an appropriate store bound.

## Data compatibility and limitations

The schema-1 JSON adds an optional `requests` array containing `requestId`,
`unitType`, `endpoint`, `startedAt`, `endedAt`, `statusCode`, `threadNames` and
resolved `classes`. No input values, headers, tokens or exception messages are
captured. Report renders can run outside the source JVM without the registry.

Older files without `requests` remain readable and show **No per-request
diagnostics**. The previous four-argument `CoverageReport` constructor remains
available. Endpoint aggregation, reverse lookup, `impact` and `diff` retain their
existing interpretation; timing/status changes are not added to coverage diff.

The Java record has five components since 0.4.0. Record-pattern consumers and
code inspecting component count must adapt; the four-argument constructor remains.
See the [compatibility policy](20_versioning_and_compatibility.md) for the minor
release boundary, even though the JSON extension is additive.

Request methods are an unordered set. Reqover does not collect invocation
order/count, timed method spans, DB intervals, test executions or load-test runs.
Reviewed test drafts are a separate [dashboard feature](27_test_case_drafts.md).
Non-HTTP units remain in the JSON but are excluded from HTTP diagnostics.

Replay and load-test integration are not part of request diagnostics.
