**English** | [한국어](15_performance_results.ko.md)

# 15. Measured agent overhead

## What this measures

The cost a request pays for running under the Reqover agent, as a function of
how many instrumented method entries that request walks through.

The headline: **about 24 ns per instrumented method entry**, plus a fixed
per-request cost too small for this method to separate from noise. A request
that enters a handful of instrumented methods pays nothing measurable. A
request that enters five thousand pays about 0.14 ms.

## Method

Two modes — the sample application alone, and the same JAR under
`-javaagent` — measured over **six rounds**, each round restarting both JVMs
and running 50 warm-up plus 300 measured requests per mode. **The order flips
every round**: odd rounds run the baseline first, even rounds run the agent
first.

The number reported is the **median of the per-round paired differences**, not
a comparison of pooled averages. Within a round both modes see the same machine
state minutes apart, in an order that alternates, so cache, JIT, scheduling, and
thermal drift affect both sides rather than whichever ran second.

```bash
./scripts/capture-performance-evidence.sh 18190 50 300 "" 6
REQOVER_BENCHMARK_ENDPOINT_PATH=/auto/depth/5000 \
  ./scripts/capture-performance-evidence.sh 18193 50 300 "" 6
```

The script refuses a dirty worktree, so the commit SHA identifies the measured
code. Raw samples, per-round summaries, and the environment record for every
run below are in
[`docs/evidence/performance/8a004266710f/`](evidence/performance/8a004266710f/).

### Why the previous measurement was retired

The 2026-08-10 capture ran the baseline once, then the agent once, and reported
the agent as **faster** than the baseline. That is the shape a single-order
measurement produces when drift happens to point the same way, and it is not
evidence of anything. It has been replaced rather than reinterpreted.

## Results

Measured at `8a004266710f`, 2026-08-25, on macOS 26.5.2, Apple M1 Pro, 16 GB,
Microsoft OpenJDK 21.0.11. Percentiles use the nearest-rank method.

| Endpoint | Instrumented entries per request | Baseline p50 | Median Δ p50 | Rounds the agent was slower |
| --- | ---: | ---: | ---: | ---: |
| `/auto/orders/1` | ~4 | 1.357 ms | **−0.038 ms** | 2 / 6 |
| `/auto/depth/0` | ~5 | 1.341 ms | **+0.010 ms** | 4 / 6 |
| `/auto/depth/1000` | ~1005 | 1.305 ms | **+0.059 ms** | 6 / 6 |
| `/auto/depth/5000` | ~5005 | 1.357 ms | **+0.138 ms** | 6 / 6 |

The "rounds the agent was slower" column is the honesty check. At four and five
entries the agent lands on either side of the baseline depending on the round,
which means the cost is below what this setup can resolve — the −0.038 ms on
the first row is noise, not a speed-up. At a thousand entries and above, every
single round agrees.

A linear fit over the three `/auto/depth/{n}` points:

```text
Δ ≈ 23.9 ns × (instrumented method entries) + 21 µs
```

Measured segment by segment, the marginal cost falls as the call count rises —
49 ns per entry between 5 and 1005, 20 ns between 1005 and 5005 — which is what
JIT warm-up on a hot path looks like. **Treat ~50 ns per entry as the
conservative figure and ~20 ns as the warm one.**

### What the per-entry cost consists of

On each instrumented method entry, `ReqoverProbe.hit` fetches the active bucket
from a `ThreadLocal` (or the global fallback), looks up the class in a
`ConcurrentHashMap`, and adds the probe id to a `Set`. A repeated entry into the
same method — the common case, and what `/auto/depth/{n}` exercises — is the
map lookup and a set add that returns false.

Fixed per-request cost, paid once whether or not any probe fires: creating a
bucket, binding it to the request, and flushing a snapshot to the store. The
`/auto/depth/0` row is that cost, and it is inside the noise floor here.

## What this does not measure

Read this before quoting any number above.

- **One machine, one JVM, one endpoint shape.** A laptop under a sequential
  loopback load. No claim is made about other hardware or other applications.
- **Sequential requests, not concurrent load.** Every measurement is one
  request at a time. Contention on the shared store under real concurrency is
  not covered, and it is the most likely place for a surprise.
- **Latency only.** Allocation rate, GC pressure, and heap growth from retained
  snapshots are not measured. The in-memory store's bound exists precisely
  because retention is the resource that grows.
- **Not startup cost.** Class-load-time instrumentation makes startup slower by
  an amount nobody has measured yet.
- **A synthetic depth endpoint.** `/auto/depth/{n}` recurses through one
  instrumented method. Real code spreads entries across many classes, so its
  map lookups miss cache more often than this does.

This is enough to answer "will the agent make my QA environment noticeably
slower" — for a request touching a few hundred instrumented methods, about
10 µs — and not enough to answer "what does this cost in production", which is
a question Reqover would rather you did not need to ask.

## Reproducing

```bash
./scripts/capture-performance-evidence.sh <port> <warmup> <measured> <output-dir> <rounds>
```

Requires a clean worktree, JDK 17 or 21, and a free port. Fewer than two rounds
prints a warning and produces a smoke check rather than a measurement.
Measurement method and the reasoning behind it:
[docs/11_performance_measurement.md](11_performance_measurement.md).
