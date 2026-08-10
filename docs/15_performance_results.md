# 15. Release-Candidate Performance Evidence

## Release-candidate measurement

The earlier 2026-06-30 Windows numbers were retired because they predated the
final dependency, packaging, and agent-safety changes and did not identify a
source commit. They are not submission evidence.

The release-candidate sanity measurement was captured with:

```bash
./scripts/capture-performance-evidence.sh 18180 50 300
```

The script refuses a dirty worktree. Evidence for commit
`ae83d1209c9a1ad632567a9569ece2c925570947` is stored in
[`docs/evidence/performance/ae83d1209c9a/`](evidence/performance/ae83d1209c9a/).

| Mode | Average | p50 | p95 | p99 | Min | Max |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Baseline | 1.352 ms | 1.192 ms | 2.160 ms | 4.085 ms | 0.945 ms | 8.440 ms |
| Reqover agent | 1.102 ms | 1.094 ms | 1.308 ms | 1.502 ms | 0.876 ms | 1.912 ms |

Captured at `2026-08-10T07:19:11Z` on macOS 15.7.3, Apple M1,
16 GB RAM, and Homebrew OpenJDK 17.0.19. Each mode used 50 warm-up and 300
measured requests; percentiles use the nearest-rank method. The evidence contains:

- exact commit SHA, UTC timestamp, OS, CPU, memory, and Java version
- baseline and agent JSON summaries
- all raw latency samples in milliseconds
- nearest-rank p50, p95, and p99 comparison

## Interpretation Boundary

This is one sequential loopback HTTP sanity-check round. The baseline ran first
and the agent ran second, so cache, scheduling, thermal, and run-order effects
are not controlled. The baseline and agent modes
use the same MVC sample JAR and the same `GET /auto/orders/{id}` endpoint; the
difference is whether the shaded Reqover Java agent instruments the selected
application package.

It is useful for catching an obvious regression in the release candidate. It is
not a production load test, throughput claim, capacity plan, or service-level
guarantee. The lower values observed in the second, agent-enabled run must not be
described as a Reqover performance improvement or as a general overhead result.
No comparative performance claim is used in the official result report.
