# Request Diagnostics Preview Implementation Plan

> Execute inline in this task. The user requested no subagents and authorized implementation and PR creation from mentoring feedback.

**Goal:** Deliver the October 7 mentoring preview with real per-request timing/status diagnostics and a dated plan toward the November 4–5 presentation.

**Architecture:** Resolve existing coverage snapshots into optional request observations in the report. Preserve the existing endpoint and reverse-index data, compute HTTP diagnostics from the observations, and render a standalone page using the existing HTML renderer. Extend schema 1 additively and preserve the existing four-argument Java report constructor.

**Tech Stack:** Java 17, existing Gradle/JUnit setup, current JSON codec, standalone HTML/CSS/JS.

## Constraints

- Working base: `origin/main` at `3207397`; preserve the earlier local mentor-document branch.
- Durations describe recorded wall-clock adapter intervals, not client response time, CPU cost, method spans or database time.
- Missing/unfinished/negative intervals are unknown and excluded from duration statistics; never zero-filled.
- Status unknown is not success; split 4xx and 5xx. Exclude non-HTTP units from HTTP metrics.
- Old schema-1 JSON lacking observations is readable. Existing coverage, diff and impact behavior remains.
- Capture no request input, headers, tokens or exceptions in this first preview.
- Native worktree creation targets this task's unrelated checkout and cannot select this external Reqover repository; use a sibling Git worktree outside the repository.

## Task 1 Planning and fact check

- [x] Inspect snapshot fields, method-entry instrumentation, JSON schema and prior troubleshooting.
- [x] Confirm Postman performance testing, k6 metrics and OpenTelemetry method instrumentation from official docs.
- [x] Write `docs/23_performance_validation_plan.ko.md` with dates, architecture, five screen designs, stage gates and follow-up PRs.

## Task 2 Request data and JSON

Files: `CoverageReport.java`, `CoverageReportGenerator.java`, `CoverageReportJson.java`; new `RequestObservation.java`, `RequestSummary.java`; tests in `reqover-report/src/test/java/io/reqover/report/`.

- [x] Add behavioral tests for per-request code separation, status retention, duration statistics, non-HTTP exclusion, invalid intervals and optional old JSON fields. Run them to confirm failure first.
- [x] Add an immutable request record with unit type, ID, endpoint, start/end, status, threads and resolved code set.
- [x] Add a request list to the report with a four-argument overload using an empty list.
- [x] Share class/probe resolution between endpoint aggregation and per-request resolution.
- [x] Write/read the optional `requests` JSON array; retain `schemaVersion=1` and old file behavior.
- [x] Derive nearest-rank p95 and mean/max/sum from valid recorded HTTP intervals; derive status counts independently.
- [x] Run report/CLI tests and inspect old fixtures.

## Task 3 Real diagnostic UI

Files: `HtmlCoverageReportRenderer.java`, new `RequestDiagnosticsHtmlRenderer.java`, renderer tests.

- [x] Add behavioral tests for empty legacy data, escaped request values, diagnostics and request detail rendering.
- [x] Add overview values and endpoint diagnostics above coverage tables.
- [x] Add searchable request details, HTTP failure/slow filters and adjustable slow threshold.
- [x] Keep summaries explicitly scoped to retained observations, with p95 sample count and unknown-status count.
- [x] Use native details for request code/thread/time/status disclosure; limit HTML detail rows and label the displayed count.
- [x] Render no fabricated method ordering, trace timing or replay controls.
- [x] Verify desktop/mobile layout and real filtering in a browser, including old data with no observations.

## Task 4 Documentation and demonstration

Files: `docs/24_request_diagnostics.md`, `.ko.md`, `README.md`, `README.ko.md`, `ROADMAP.md`, `CHANGELOG.md`.

- [x] Document observed timing boundaries, HTTP-status semantics, additive JSON compatibility and retention limitations.
- [x] Add the mentor-driven milestone track to the roadmap, keeping future features labeled as planned.
- [x] Run MVC and WebFlux samples with the agent, capture JSON/HTML, verify individual request details and CLI render round-trip.
- [x] Run scoped regression tests followed by the full project suite/build once after implementation stabilizes.

## Task 5 Review and PR

- [x] Inspect the diff alone for constructor/schema compatibility, escaped HTML, request attribution and metrics semantics.
- [x] Record exact checked commands and remaining limitations in a short troubleshooting entry.
- [x] Commit only this worktree's scoped changes, push `codex/mentoring-performance-preview`, open an English PR against `main`, and attach the PR to the task.

Published for maintainer review: [PR #25](https://github.com/reqover-labs/reqover/pull/25).

Subsequent implementation gates are in the dated improvement plan; input capture, test generation and k6 execution are not part of this first PR.

## Verification evidence on October 3

- `gradlew.bat test build`: successful, 147 tests, zero failures/errors/skips.
- Final scoped report/CLI tests and demo JAR packaging: successful after the legacy navigation fix.
- Live MVC: five synthetic requests; expected 400/503 and a bounded 1200 ms delay visible.
- Live WebFlux: three requests under the agent, multi-thread request attribution retained, explicit 503 recorded.
- CLI `render --out`: request diagnostics survive file import; `impact --changed`: both diagnostic endpoints identified.
- Browser: 1440 px desktop and 390 px mobile, light/dark, failure/slow/text filters, request disclosure, legacy empty state, zero page errors and no root horizontal overflow.
- `git diff --check`: clean.
