**English** | [한국어](README.ko.md) | [Project](../README.md)

# Core Recording Model

Maven Central library: `io.github.reqover-labs:reqover-core:0.4.1`.
The starter includes it; custom adapters and stores use these APIs directly.

- `CoverageBucket` records a unit's timestamps, status, threads and probe set.
- `UnitInfo` identifies HTTP requests or non-HTTP jobs/messages/tests.
- `CoverageContext` and `ReqoverProbe` route hits to the current bucket.
- `UnitScope.open` manages a unit; `join` temporarily attributes another thread's
  work without finishing or flushing the original unit.
- `CoverageStore` exposes `flush`, `snapshots`, optional `aggregates` and `clear`.

The default `InMemoryCoverageStore` retains at most 10,000 snapshots, with
`oldest-first` or `reject-when-full` retention. Independently, every flush updates
unit counts and code unions. Aggregates survive detail eviction, not restart or
`clear()`. The detail cap is not a total heap cap: distinct units/probes add state.

Custom stores must be thread-safe and avoid blocking completion threads.
Without `aggregates()`, reporting falls back to retained snapshots; use the
[contract tests](src/test/java/io/reqover/core/CoverageStoreContract.java) when
implementing a store. The core does not itself provide Spring wiring, durable
storage, line/branch coverage, method durations or HTTP request replay.

[Architecture](../docs/02_architecture.md) |
[Custom stores and UnitScope](../docs/17_integration_guide.md)
