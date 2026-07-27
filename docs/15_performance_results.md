# 15. Local Performance Results

## Measurement Scope

This is a local sequential HTTP measurement for demo sanity checking, not a production benchmark.

Environment:

- OS: Windows
- Date: 2026-06-30
- JDK: local JDK 21
- Sample: `examples:webflux-sample`
- Endpoint: `GET /auto/reactive/orders/1`
- Warmup requests: 20
- Measured requests: 120
- Tool: `scripts/measure-demo-latency.ps1`

## Results

| Mode | Average ms | p50 ms | p95 ms | p99 ms | Min ms | Max ms |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| Baseline, no agent | 18.58 | 17.88 | 26.77 | 31.44 | 8.27 | 54.62 |
| Reqover agent enabled | 21.57 | 18.15 | 38.19 | 61.65 | 10.24 | 143.59 |

## Interpretation

The local sequential measurement shows a small p50 difference and a larger p95/p99 tail in the agent-enabled run.

This is acceptable for the MVP claim because Reqover is positioned as a development, demo, and staging observability tool at this stage. It is not yet positioned as a production always-on agent.

The result should be treated as an early signal only. A stronger benchmark should use a dedicated load generator, fixed CPU conditions, multiple runs, and separate MVC/WebFlux scenarios.

