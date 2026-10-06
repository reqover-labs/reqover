# Performance Preview Review Corrections

The [PR #25 review](https://github.com/reqover-labs/reqover/pull/25#issuecomment-5982134952)
identified boundary and integration regressions. Corrections are intentionally
limited to report export, Action prerequisites, offline views and public docs.

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

Latest main was synchronized without rewriting the shared feature history; its
dependency patches, accessor/proxy handling, multi-context exports and workflow
updates are preserved. No `.env`, credentials or raw meeting data are included.
