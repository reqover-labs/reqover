# Diagnostic Dashboard and CI Integration

**Goal:** Make recorded failures, delays, and shared-code retest candidates easy
to inspect locally and in a pull request, without changing collection semantics.

**Architecture:** Extend the standalone HTML renderer with a progressive-enhancement
dashboard. Inline local CSS, JavaScript, and licensed Lucide icons. Visualize
request-to-method membership and method-to-endpoint relationships; never invent
method order, duration, exceptions, or inputs. Keep the existing tables usable
without JavaScript. Extend the existing composite Action rather than adding a
second installation mechanism.

**Execution:** Inline implementation and self-review, as requested by the user.
No subagents. Continue on `codex/mentoring-performance-preview` and update PR #25.

## Constraints

- No changes to production agent, core, or Spring adapters.
- Request selection and detail views show the most recent 100 HTTP observations.
- Graph nodes are bounded, with explicit overflow counts; full method sets remain
  available in request details and the reverse index.
- Failure means a recorded final HTTP 4xx/5xx, not a collected exception.
- Delay uses recorded server-adapter intervals, not client latency or CPU time.
- Shared code means observed in at least two endpoints, not business criticality.
- Animation illustrates associations, not a measured call timeline. Honor reduced
  motion and provide pause, zoom, and reset controls.
- CI requires an already recorded JSON report, Java 17+, and checkout history.
  Support a source-built CLI for this unreleased dashboard.
- Publish only explicitly named report files. Never upload `.env` or credentials.
- Retain the existing security scan and its failure policy.

## Tasks

- [x] Inspect current renderer, tests, mentoring notes, and composite Action.
- [x] Write dashboard regression tests; run to confirm missing behavior.
- [x] Implement sidebar, risk-focused relationship workspace, inspector, and animation.
- [x] Write and exercise Action integration tests against the real CLI.
- [x] Add automatic HTML artifacts, structured outputs, fork-safe comments, and
  an optional source-built CLI to the existing Action.
- [x] Wire the Action into the repository's own CI workflow; remote run pending push.
- [x] Verify light/dark and desktop/mobile layouts, filtering, selection, animation,
  reduced motion, legacy reports, and hostile strings.
- [x] Update English/Korean guides, README, changelog, and troubleshooting notes.
- [x] Run build/tests and self-review the diff, including non-HTTP labels and Git history.
- [x] Commit/push and update [PR #25](https://github.com/reqover-labs/reqover/pull/25) without merging.

## Local Verification

- Java `test build`: 153 tests, zero failures/errors/skips.
- Composite script: six real-CLI tests, including an advancing PR base.
- Actionlint 1.7.12: build workflow passes validation.
- Playwright: node selection, retest links, actual animation pixel changes,
  pause/zoom, filtering, download/reopen, legacy and no-script fallback,
  reduced motion, light/dark, and 1920/1280/760/390 px layouts pass.
- Browser page errors and external requests: zero.

## Verification Commands

```powershell
.\gradlew.bat :reqover-report:test :reqover-cli:test :reqover-cli:shadowJar
python scripts/test-impact-action.py --cli reqover-cli/build/libs/reqover-cli-0.2.0.jar
.\gradlew.bat test build --quiet
```

Browser checks use the bundled Playwright runtime on a locally rendered report.
Screenshots contain only synthetic sample requests. Existing OSV findings must be
reported separately from functional verification; no advisories are suppressed.
