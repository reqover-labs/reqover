**English** | [한국어](README.ko.md) | [Project](../README.md)

# Reports and Diagnostic Dashboard

Maven Central library: `io.github.reqover-labs:reqover-report:0.4.2`.
The starter already includes it. It depends on core, not Spring or a JSON library.

| Feature | Data and boundary |
| --- | --- |
| Endpoint/code and reverse index | Recording-wide data for admitted aggregates; snapshot fallback for other names |
| Request diagnostics | Retained HTTP timestamps, final status, threads and method sets |
| Relationship maps | Animated associations, not measured call order or method spans |
| Reviewed test drafts | JSON or disabled GET/HEAD JUnit files, no original-input replay |
| Recording comparison | Retained time/status summary deltas, no automatic performance verdict |
| CLI code diff/impact | Endpoint/code changes and observed retest candidates |

`CoverageReportGenerator.generate(snapshots, aggregates)` resolves probe IDs to
code names. `CoverageReportJson` uses schema 1; its optional `requests` details
default to the newest 100 units with `omittedRequestDetails`. Endpoint aggregates
and reverse index are not truncated. Older files without details remain readable.

The default store's [aggregate limits](../reqover-core/README.md) still apply:
2,000 distinct names for new aggregate admission and 64 thread names per
aggregate. Names outside admission can disappear after detail eviction; the
JSON detail limit does not turn this into unlimited whole-recording coverage.

Live HTML statistics cover retained HTTP snapshots. Rendering exported JSON can
only use details in that file. Export a live dashboard summary for a complete
retained timing baseline; a summary or test draft is not CLI coverage JSON.

The standalone HTML needs no server or external assets. Browser drafts/imports
stay in memory; refresh loses them. Files may still expose private code/endpoint
names, even though bodies, headers and credentials are not collected.

[Requests](../docs/24_request_diagnostics.md) |
[Dashboard/CI](../docs/26_dashboard_and_ci.md) |
[Drafts](../docs/27_test_case_drafts.md) |
[Comparison](../docs/28_recording_comparison.md)
