**English** | [한국어](28_recording_comparison.ko.md)

# Compare retained recordings

Since 0.4.0, the dashboard compares a previous Reqover JSON recording or
exported aggregate summary with its current retained HTTP recording. It shows
endpoint counts, average/p95 adapter intervals, and HTTP error-percentage changes.
This is descriptive before/after evidence, **not a controlled load-test result or
automatic performance pass/fail**.

![Two dummy recordings of the same traffic, the second with a slower endpoint](assets/reqover-recording-comparison.png)

The screenshot baseline doubles the sample recording's timing values to exercise
the UI. It is explicitly named `synthetic-baseline.json`. These numbers do not
demonstrate an application improvement or a benchmark.

## Use

Open **Compare recordings** and import a previous coverage report JSON. Export
the current summary for a later session. Imports stay in page memory, are not
uploaded or written to localStorage, and are limited to 10 MiB / 50,000 raw
observations. Clearing the baseline removes the comparison and manual confirmation.
Invalid imports keep the last valid baseline and show an error.

Raw exports with `omittedRequestDetails` are incomplete and cannot be used for
full retained comparisons. Import the live report's aggregate summary instead.
Floating-point changes within an absolute/relative epsilon are displayed as
zero; meaningful submillisecond changes remain visible.

The current summary is computed by Java from **all retained HTTP observations**,
not the request graph's latest-100 display window. Imported raw JSON uses matching
final-status, invalid-interval, and nearest-rank p95 rules. Native Date validates
calendar values; BigInt nanosecond subtraction preserves submillisecond intervals.
Unsupported timestamp ranges/formats fail explicitly rather than becoming zero.

The view supports endpoint search and sorting by p95 delta, HTTP error delta,
current observations, or name. Time deltas are current minus baseline milliseconds.
HTTP error deltas are **percentage points**, with known final statuses as each
side's independent denominator. Unknown statuses remain separate; expected 4xx
responses may represent successful tests.

## What differences mean

- Missing or unfinished timing is not zero milliseconds.
- Unknown final status is not a success or zero-percent error rate.
- “Not observed before/now” is not proof an API was introduced or removed.
- Different retained/timed counts, unknown statuses, and fewer than 20 timing
  samples are surfaced. The 20-sample notice is a UX warning, not a statistical
  confidence threshold or proof larger samples are sufficient.
- Scenario, inputs, environment, package scope, and retention equivalence are
  not captured. Manual confirmation records only the user's acknowledgement.
- There is no regression gate or resource-consumption estimate. The measured
  interval is still server-adapter wall-clock time, not client latency or TPS.

Summary exports use `kind: "reqover-recorded-summary"`, schema 1, generation time,
and per-endpoint counters/interval statistics. They contain no request IDs, method
lists, headers, bodies or credentials. Endpoint names may still be private; do not
publish imports or exports automatically. This format is not coverage JSON for
the CLI's `impact` command, nor the test-draft format.

Legacy files without individual requests remain readable with measurements
unavailable. Corrupt summary counters/timings and unsupported future schemas are
rejected. Existing collection, coverage JSON, CLI and CI artifact selection stay
unchanged; no collector or method-span functionality is added.

## Verify

```bash
./gradlew test build
node --test scripts/test-case-drafts.test.cjs scripts/recording-comparison.test.cjs
```

Java checks prove that 150 retained requests remain in the comparison summary
even though graph data is capped at 100, including errors outside that window.
JS checks cover precision, final-status denominators, missing data, nearest-rank
p95, legacy/future/invalid files and inconsistent summaries. Browser checks cover
import, retained state after invalid files, filter/reset/export, confirmation,
mobile layout and all prior graph/draft/legacy/no-script behaviors.
