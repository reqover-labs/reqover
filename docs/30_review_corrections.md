# Performance Preview Review Corrections

The [PR #25 review](https://github.com/reqover-labs/reqover/pull/25#issuecomment-5982134952)
identified boundary and integration regressions. Corrections are intentionally
limited to report export, Action prerequisites, offline views and public docs.

## Review Delivery

PR #25 now contains planning, dependency-remediation evidence and this review
record only. Runtime changes were preserved in smaller stacked PRs:

1. [#34 Request diagnostics](https://github.com/reqover-labs/reqover/pull/34), base main.
2. [#35 Dashboard and CI evidence](https://github.com/reqover-labs/reqover/pull/35), base #34.
3. [#36 Reviewed test drafts](https://github.com/reqover-labs/reqover/pull/36), draft, base #35.
4. [#37 Recording comparison](https://github.com/reqover-labs/reqover/pull/37), draft, base #36.

None of these split PRs was merged, and the original shared branch history was not rewritten.
Feature guides and public troubleshooting notes travel with their owning PRs.

## Corrections

| Finding | Correction | Regression coverage |
| --- | --- | --- |
| Artifact name collisions | Upload defaults to false; explicit upload requires distinct names for matrix/repeated calls | Action metadata check; repository job explicitly opts in |
| Unbounded request detail JSON | Latest 100 details by default; full endpoint unions remain; omitted count and explicit local limit | 150-record export test, explicit full export and graph ordering |
| Python missing in containers | Document Java/Bash/Git/curl/Python prerequisites; fail before network work when Python is unavailable | Action prerequisite guard and container guide |
| Self-comparison noise | Absolute/relative epsilon suppresses arithmetic-only delta; meaningful submillisecond changes remain | Identical-value drift and nonzero-delta tests |
| Missing endedAt | Absent/null end denotes unfinished, without timing or final status | Imported unfinished observation test |
| JUnit path pipe | Reject raw/encoded `|` in reviewed paths before generation | Path rejection and actual Java compilation tests |
| Downloaded icon notices | Emit notices before the capture script, not after it | Full license text checked in downloaded HTML |
| Shallow history fetch | Detect shallow checkout before base fetching and require fetch-depth 0 | Unreachable-origin shallow fixture proves no fetch |

Bounded detail JSON is not a complete timing baseline. The comparison importer
rejects an `omittedRequestDetails` export and directs users to the live report's
complete aggregate summary. The cap preserves original detail order and does not
collapse identical observations.

Public documentation uses a separate preview section and neutral performance
validation naming. Internal agent plans and member-specific assignments are
removed; setup examples pin a commit instead of a deletable branch. Local offline
views are supported, while hosted/multi-user dashboard services remain outside
scope. Observed impact is explicitly average recorded interval times valid timed
calls, not a CPU/resource estimate or all-traffic frequency metric.

## Verification

The full build and SBOM generation passed on the corrected latest-main snapshot.
The initial stack tip's runtime, workflows and test files matched that snapshot.
Each split was also checked with its focused Java tests. The Action suite passed
all 8 cases, Node passed all 23 cases (including actual generated-Java compilation),
and Playwright covered graph animation pixels, filters, drafts, comparison,
license-preserving downloads, 1920/1280/760/390-pixel layouts, legacy/no-script
fallbacks and reduced motion, with zero external requests or page errors.

A first full Windows run hit two temporary-log deletion locks in E2E cleanup.
The agent suite passed on rerun, followed by a successful full build. No
production code or unrelated cleanup logic was changed to hide that failure.

Initial GitHub Java 17/21 and export/impact jobs passed on all four split PRs.
The #35-#37 scans then failed on Spring MVC 6.2.19 and GHSA-pc63-qcmh-9cmg
because those branches predated main's `osv-scanner.toml` (#32), which excepts
that advisory until December 31, 2026; it was not a new risk in these PRs. It was preserved during the final
synchronization; passing that policy does not mean the dependency is patched.
See the [security record](29_osv_dependency_remediation.md).

Main `935d3db` additionally merged endpoint-wide recording aggregates (#30),
reference probes (#31) and publication coordinates (#33). All split branches
were synchronized without rewriting history. A new regression verifies that
recording-wide endpoint counts/code stay complete while request timing/status
and method details stay limited to retained snapshots. No timings are invented
for evicted observations. The new publication group and SBOM are preserved.

Final verification against that main update passed 185 Java tests with no
failures/errors/skips, the full build and SBOM lock check, all 23 Node and 8
Action cases, and the complete Playwright flow with zero external requests/errors.
The exception-aware full OSV query reports zero unexcepted findings and one
explicitly dated exception, as explained in the security record.

Latest main was synchronized without rewriting the shared feature history; its
dependency patches, accessor/proxy handling, multi-context exports and workflow
updates are preserved. No `.env`, credentials or raw meeting data are included.
