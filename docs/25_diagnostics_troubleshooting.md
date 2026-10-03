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

## Dashboard and CI follow-up

The visual map branches from a request bucket to independently observed methods;
there are deliberately no method-to-method call edges. The reverse map connects
code to its observed API candidates. Java-computed request intervals are reused
from the sorted native detail rows, avoiding JS Date's submillisecond rounding.
Graph samples share the detail view's latest-100 ordering and limit.

Browser assertions initially ran before `hashchange` and reduced-motion media
events completed. The URL had changed but the visible section still reflected
the previous event. Wait for the target section/control state rather than adding
arbitrary delays or changing application behavior. Movement checks intentionally
sample different animation frames, including their actual pixels.

The Action previously applied its failure gate before any artifact/comment step,
used text matching to parse JSON, and attempted comments on read-only forks.
It now parses the CLI's JSON structurally, saves three named files, updates only
its marked bot comment on same-repository PRs, and applies the optional gate last.
It does not recursively upload the workspace or include `.env`/credentials.

Reverse indexes also retain names from manual non-HTTP scopes. The graph labels
their fan-out as endpoints rather than asserting every name is an HTTP API.
An additional regression test preserves these names without counting the
non-HTTP observation in the HTTP graph or metrics.

Dashboard follow-up verification: 153 Java tests with zero failures/errors/skips;
six real-CLI Action tests; browser selection, filters, pause/zoom, changing
animation pixels, download/reopen, no-script and legacy fallback, reduced motion,
and 1920/1280/760/390 px layouts. External browser requests and page errors were
zero. Only synthetic sample data is used in documentation images.

An additional Git regression reproduces a PR whose base branch advanced after
divergence. Fetching that base with `--depth=1` made a previously complete checkout
shallow and hid the merge base. The Action now fetches without a depth limit and
the test checks both the correct impact and preserved history.

The existing OSV scan reported Jackson 2.21.5 and Tomcat 10.1.55 advisories on
PR #25. These are unchanged dependencies, not fixed by this UI/Action work. The
security gate remains enabled; a passing build must not be described as passing
all CI while this scan fails.

## Reviewed test drafts

Mapped routes cannot supply missing concrete paths, query values or authentication.
The editor starts expectations empty and separates observed status/adapter interval
from manually reviewed assertions. Draft JSON remains `replayable: false` and
generated JUnit remains disabled even after UI review. Mutating methods cannot
export executable drafts. No collector, probe, report schema or CLI changes are needed.

A legacy navigation test initially searched for `href="#test-case-drafts"` anywhere
in HTML and matched the new CSS selector, not an anchor. Assert the actual anchor
markup (and verify the browser DOM), rather than weakening the legacy contract.

Recorded text is emitted only as escaped Java strings. Control characters use
bounded octal escapes where needed, and identifiers are generated from numeric
case IDs. Compilation against the actual JUnit API checks Unicode, quotes,
comment terminators and literal backslash-u sequences; no generated tests run
during this verification. Test paths reject unresolved/encoded placeholders,
authority changes, query/fragment and invalid URI characters.

Browser coverage includes blank expectations, edit-invalidated review, disabled
export before review, JSON/JUnit contents, multiple independent drafts, mobile
layout, and all existing graph/filter/legacy/no-script behaviors. Drafts live in
page memory only and user-specific exports are never staged automatically.

## Retained recording comparison

Comparison cannot reuse the graph's truncated request window. Java computes
per-endpoint summaries from every retained HTTP observation and embeds only those
aggregates. A 150-request regression includes a 503 outside the newest-100 window.
Imported raw timestamps use native calendar validation and BigInt nanosecond
subtraction; JS Date's millisecond truncation is not used to compute intervals.

Missing measurements remain unknown. Independent known-status denominators yield
percentage-point changes, not test-failure percentages. Endpoint absence and
sample-count differences do not prove deletion or regression. Condition review
does not enable a performance gate. Malformed/oversized imports preserve the last
valid baseline and never upload or persist the input.

Verification: 157 Java tests and 20 Node tests pass, including Java compilation
from the previous draft feature. Browser tests cover comparison import, invalid
and legacy files, filter/reset/export, confirmation, mobile layout and previous
features. Screenshots use an explicitly named synthetic timing baseline, not
claimed benchmark results. The existing OSV gate remains unchanged.

## Dependency Security Follow-Up

The earlier red Jackson/Tomcat checks were not scanner startup errors. A full
query reproduced ten advisories across three components. The subsequent
[dependency remediation](29_osv_dependency_remediation.md) aligns Jackson 2.21.7
and embedded Tomcat 10.1.60 and regenerates the inventory; local OSV queries now
return zero affected Maven components. The previous failure descriptions above
are historical records, not instructions to leave that gate red.

Tomcat 10.1.58 is listed as patched in advisory ranges but was not released after
a failed vote. Verify actual Maven artifacts rather than configuring a nonexistent
version. All embedded Tomcat modules are aligned in both the sample and starter
test scope without imposing a new published-library runtime dependency.

Windows clean failed on the old running demo's locked agent JAR. A separate clean
validation checkout received only the dependency/SBOM edits, then passed all
157 Java and 20 Node tests. The user's running demo was not interrupted. The
existing `fail-on-vuln` policy and full inventory scan were not weakened.
