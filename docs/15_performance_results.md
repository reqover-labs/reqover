# 15. Release-Candidate Performance Evidence

## Status

The earlier 2026-06-30 Windows numbers were retired because they predated the
final dependency, packaging, and agent-safety changes and did not identify a
source commit. They are not submission evidence.

The final table is generated only after the runtime changes are committed. Run:

```bash
./scripts/capture-performance-evidence.sh 18180 50 300
```

The script refuses a dirty worktree and writes the following under
`docs/evidence/performance/<commit>/`:

- exact commit SHA, UTC timestamp, OS, CPU, memory, and Java version
- baseline and agent JSON summaries
- all raw latency samples in milliseconds
- nearest-rank p50, p95, and p99 comparison

## Interpretation Boundary

This is a sequential loopback HTTP sanity check. The baseline and agent modes
use the same MVC sample JAR and the same `GET /auto/orders/{id}` endpoint; the
difference is whether the shaded Reqover Java agent instruments the selected
application package.

It is useful for catching an obvious regression in the release candidate. It is
not a production load test, throughput claim, capacity plan, or service-level
guarantee. The official result report should include numbers only after the
evidence directory and commit SHA are present.
