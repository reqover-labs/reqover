# Request diagnostics implementation notes

## What the mentoring note assumed

The baseline stores probe hits in sets. Repeated invocations, method ordering and
elapsed method spans cannot be reconstructed from them. The preview resolves
each snapshot independently; it never turns sorted method names into a timeline.

The snapshot already contains a wall-clock start/end and status, but the report
discarded these while building endpoint unions. Preserving these observations
provides the first real diagnostic screen without changing the hit ABI, bytecode
instrumentation or request context propagation.

## Test recordings can outlive one test

The first MVC diagnostic integration test selected the earliest observation for
the delay endpoint. A previous bad-input test had used the same normalized route
and left a 400 observation in the shared Spring store. The live successful
request returned 200; selecting the old snapshot caused the incorrect assertion.
Use the repository's existing `@BeforeEach` store clear pattern for both MVC and
WebFlux diagnostic tests. Do not change status classification to accommodate test
data from a different request.

## Timing and unknown data

The preview uses recorded adapter wall-clock intervals and separately counts
unknown final HTTP statuses. Invalid/unfinished intervals are absent from timing
statistics, not zero milliseconds. Scheduled jobs remain exported but do not
contribute to HTTP metrics. Timing from a Mono factory or an endpoint union is
not a substitute for a completed reactive request's interval.

## Export size and HTML rendering

Per-request code preservation enlarges exports. The existing bounded store
limits captured observations; HTML detail rendering is limited to the most recent
100 HTTP observations and explicitly describes that limit. JSON preserves the
whole retained recording. Endpoint summaries use cached per-endpoint statistics
so sort comparisons do not repeatedly sort each endpoint's timing samples.

## Verification record

Use `gradlew.bat test build` for the whole build and `:reqover-report:test` /
`:reqover-cli:test` for report compatibility. MVC and WebFlux integration tests
exercise the bounded delay and 503 endpoints. Separate live JVMs with the agent
verify request metadata and WebFlux thread hops. The CLI uses `render --out` and
`impact --changed`; `--output` is not a supported render flag.

Browser checks use the real MVC report at desktop 1440 px and mobile 390 px,
verify failure/slow/text filters and native details, and capture light/dark
screenshots. Images in `docs/assets/reqover-request-*.png` contain synthetic demo
traffic only. The mentoring plan describes replay/input/load features as future
work; those features are not claimed by this implementation.

October 3 verification: full test/build succeeded with 147 tests and no failures,
errors or skips. The final report/CLI checks also passed after the legacy
navigation adjustment. See the implementation plan for live and browser evidence.
