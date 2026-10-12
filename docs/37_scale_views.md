**English** | [한국어](37_scale_views.ko.md) | [Documentation](README.md)

# Connected Scale Views (Unreleased)

## Three Levels

- **Distribution:** five-minute UTC windows by recorded adapter interval. Every cell
  shows retained count. Color intensity is logarithmic relative to the largest cell;
  red/amber borders identify HTTP 5xx/4xx, not test failures or circuit state.
- **Groups / API list:** choose a window heading for all its intervals or a nonempty
  cell for that interval band. Exact endpoint summaries populate the existing groups
  and sortable list. Grouping depth and all existing filters remain available.
- **Request map:** each API row can open the latest available matching request in the
  existing relationship map. It is an observed method set, not timed invocation order.

For 50+ observed APIs or 1,000+ retained HTTP observations, API workspace initially opens Distribution; otherwise Groups.
The original Overview request map remains available for small recordings. Users can
always choose the other views. This threshold is a UI default, not a capacity limit.

Choosing a new time scope clears the old API selection and text/status/count/p95
filters, preserving sort order and path depth. All recorded time clears the time/band
scope and returns the full retained summary. Ordinary Reset filters keeps the time scope.
The active range is explicit above every view, with totals for that selected scope.

## Accurate Data Boundaries

Java aggregates **all retained HTTP observations before the latest-100 graph/detail cap**.
Window and band endpoint means/percentiles are calculated from their actual samples,
not combined endpoint percentiles. Bands are `[0,100)`, `[100,500)`, `[500,1000)`,
`[1000,2000)`, `[2000,infinity)` ms and Not timed. Unfinished/negative intervals are
Not timed. Zero/submillisecond timing is valid; status rules match RequestSummary.

Only the latest 256 nonempty windows are included. Omitted request count and timestamps
outside native JavaScript Date range are reported explicitly. Gaps between available
windows are compressed and labeled; blank cells mean no retained observation, not
proof that no traffic occurred. Evicted observations cannot be recovered. Rendering
an already truncated coverage JSON only summarizes the details present in that file.

Draft sources still come from the latest 100 HTTP details and must match the chosen
time and band. A historical cell may have accurate counts but no matching recent
detail; draft creation is disabled and missing source counts are displayed. Export
bundles include the optional `scope` with start/end/band. Expectations, original input
and credentials are not invented. Scale views do not execute or persist traffic;
the separate [retest workflow](39_retest_workflow.md) provides file history and local checks.
There is no faithful replay or production control.

The embedded `reqover-time-data` is a separate HTML-only `reqover-time-distribution`
schema-1 aggregate, not a change to canonical coverage JSON or CLI inputs.

## Verification

Java tests cover 150 retained requests despite the 100-detail cap, exact boundaries,
status counts, non-HTTP exclusion, UTC alignment, 256-window cap and extreme timestamps.
Node model tests cover half-open time scoping, interval bands and explicit gaps. An
isolated controller test with DOM doubles exercises Distribution -> Groups -> API ->
Request and proves a recent out-of-scope request cannot supply an older selected window.

Fresh browser visual/E2E, mobile/dark layout and screenshot recapture are **not yet
verified**. Controller doubles do not replace rendered browser QA. The updated browser
verification script contains heatmap-count and drill-down assertions ready for a permitted
environment. Existing screenshots show the earlier workspace iteration.

```bash
./gradlew :reqover-report:test :reqover-cli:test :reqover-cli:shadowJar
node --test scripts/api-workspace.test.cjs scripts/scale-views.test.cjs scripts/dashboard-navigation.test.cjs scripts/test-case-drafts.test.cjs scripts/recording-comparison.test.cjs
```
