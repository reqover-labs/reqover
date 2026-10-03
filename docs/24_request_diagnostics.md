# Recorded request diagnostics

The development report now preserves individual observations as well as the
endpoint union. Its overview shows recorded HTTP request counts, 4xx/5xx status
counts, average/p95/maximum recorded processing intervals, and an endpoint table
ranked by cumulative interval. Expand a request to see its timestamps, status,
thread names and independently resolved method set.

![Request diagnostics from five synthetic requests to the MVC sample](assets/reqover-request-diagnostics.png)

![An actual HTTP failure expanded to show its recorded code and timestamps](assets/reqover-request-detail.png)

Screenshots were captured on October 3, 2026 from the agent-attached MVC sample
on loopback. Numbers illustrate that recording, not an application benchmark.

## Trying the preview

Build the current branch and run a sample with the agent; release `0.2.0` does
not include this unreleased preview. The current demo script uses the built
version automatically:

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
list. HTML expands at most the most recent 100 HTTP observations; the JSON has
all retained units. A detail search is therefore limited to those displayed
observations. Resolving per-request methods also enlarges JSON exports; capture
small, explicit package scopes and choose an appropriate store bound.

## Data compatibility and limitations

The schema-1 JSON adds an optional `requests` array containing `requestId`,
`unitType`, `endpoint`, `startedAt`, `endedAt`, `statusCode`, `threadNames` and
resolved `classes`. No input values, headers, tokens or exception messages are
captured. Report renders can run outside the source JVM without the registry.

Older files without `requests` remain readable and show **No per-request
diagnostics**. The previous four-argument `CoverageReport` constructor remains
available. Endpoint aggregation, reverse lookup, `impact` and `diff` retain their
existing interpretation; timing/status changes are not added to coverage diff.

Request methods are an unordered set. This preview does not collect invocation
order/count, timed method spans, DB intervals, test cases or load-test runs.
Non-HTTP units remain in the JSON but are excluded from HTTP diagnostics.

The mentoring roadmap toward replay and k6 integration is in
[the improvement plan](23_performance_validation_plan.ko.md).
