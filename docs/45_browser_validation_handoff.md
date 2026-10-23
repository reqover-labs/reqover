**English** | [한국어](45_browser_validation_handoff.ko.md) | [Documentation](README.md)

# PR #53 browser validation handoff

> Historical record at `7f4762b`. The user subsequently lifted the retry prohibition,
> and direct access succeeded. See [review 46](46_rendered_browser_review.md) for current evidence and limits.

October 23, 2026 (KST). Target: unreleased `codex/api-group-workspace`, `d8d7a6d`.
This follow-up changes documentation only; rendered UI validation is still pending.

## Starting state and access decision

After `git fetch origin`, local and remote heads both pointed to
`d8d7a6d91a8752353684c92089dd59b0679a40d1`, ahead/behind 0/0, with no uncommitted
changes. [PR #53](https://github.com/reqover-labs/reqover/pull/53) was OPEN.

Current tool instructions and the existing [validation boundary](40_finalization.md),
[workspace follow-up](41_workspace_followup.md) and [scrolling record](44_ci_retest.md)
provided no new explicit permission to access the previously blocked dashboard.
Tool availability alone does not lift that restriction. No blocked-page retry,
alternate address/browser, proxy, file copy or indirect automation was attempted.
No dashboard server or browser verification script was started.

This is a handoff for a human in an approved environment, not authorization for an
agent to bypass the restriction. Actual clicks, mobile layouts and downloads remain unverified.

## Automated evidence

| Check | This follow-up | Scope |
| --- | --- | --- |
| Java 21 report/CLI rerun | 79 + 25 passed; failures/errors/skips 0 | Renderer/CLI contracts, synthetic loopback checks and history; no rendered browser |
| All Node tests rerun | 48 passed; failures/skips 0 | Models, DOM doubles, generated Java compilation; no actual clicks/downloads |
| Seven CI runner integration cases | Not rerun locally | Previous evidence remains in [document 44](44_ci_retest.md) |
| Five GitHub checks | Queried SUCCESS at starting head `d8d7a6d` | Java 17/21, report export/impact, OSV and saved-scenarios; no UI approval |
| Browser/mobile/download checks | Not performed | All eight flows below pending |

The CI runner uses `/reqover/report` for readiness, so it was excluded from the
local rerun to avoid additional indirect dashboard access. Java unit-test fixture
servers use synthetic `/ok`, `/slow` and `/redirect` routes and stop in `finally`.
No Reqover application, Gradle test worker or Gradle daemon process remained after
the checks. Existing Gradle deprecation warnings remain outside this scope.

Commands executed in PowerShell with JDK 21 selected for the process:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat :reqover-report:test :reqover-cli:test --rerun-tasks --no-daemon --console=plain
$testFiles = @(Get-ChildItem -LiteralPath 'scripts' -Filter '*.test.cjs' | ForEach-Object FullName)
node --test @testFiles
```

## Manual preparation

Record commit, tester/date, browser/version, OS, viewport/device, theme and synthetic
fixture name. Keep personal recordings, credentials, raw logs and downloaded files
out of commits. Store visual evidence separately and document sanitized findings.

| Fixture | Required properties |
| --- | --- |
| Small | 2–5 APIs, individual request details, shared code for Request/Retest maps |
| Medium | 20–49 APIs, fewer than 1000 retained HTTP observations, several list pages/groups |
| Large | At least 50 APIs or 1000 retained HTTP observations; several UTC five-minute windows, duration bands, 4xx/5xx and untimed requests |
| Missing detail | Aggregate rows whose matching time/API request is absent from the latest 100 details |
| Boundaries/errors | 49/50 APIs and 999/1000 observations; empty/legacy input, long paths/names, three distinct runs and recordings, invalid JSON/kind/schema and a scenario exceeding 1 MiB |

Use 1440×900, short desktop 1024×480, mobile 390×844 and narrow 320×568, each in
light/dark themes. Distinguish resized desktop testing from actual mobile-device
testing. Include keyboard-only navigation, table scrolling and file selection.

## Manual checklist — all pending

Only mark a box after performing and recording the actual UI step.

### 1. Small, medium and large view switching

- [ ] Switch Request/Retest maps in the small recording; inspect matching request/code details.
- [ ] Medium workspace starts in Groups; at least 50 APIs or 1000 HTTP observations starts in Distribution. Check 49/50 and 999/1000 boundaries separately.
- [ ] Round-trip Groups → API list → Distribution. Active controls, page and selection count match the content; reselecting the same view preserves position.

### 2. Sidebar, short-window and mobile scrolling

- [ ] Reach and activate the last sidebar menu with scrolling and keyboard navigation in the short window; names and active marker remain visible.
- [ ] Reach the end of the wrapped mobile menu and body; sidebar/body/table scrolling does not trap input or cover actions.
- [ ] Inspect long tables, sticky headings and horizontal scrolling. Moving to another section resets body scroll; reselecting the same view preserves position.

### 3. Heatmap → group → API → matching request

- [ ] Select a known UTC five-minute cell, then Groups → API list → Request map. Time, duration band, count, endpoint and selected request agree.
- [ ] A time-column heading includes all duration bands. Inspect empty cells, Gap, untimed, 4xx/5xx and omitted-data notices.
- [ ] Missing in-scope details disable map/draft actions with an explanation; never substitute a newer request outside the scope.
- [ ] A new time scope clears selection/search/status/count/p95, preserving sort/depth. Reset filters preserves time; All recorded time clears time/band restrictions.

### 4. Filters, sort, checkboxes and selection

- [ ] Combine search/status/minimum count/p95/group restrictions; inspect sort direction, empty results and pagination.
- [ ] Select across pages/views/filters; hidden-selection and available/missing-source counts agree. Select current page affects that page only.
- [ ] Check Clear group and Reset filters. Reset preserves API selection, sort and group depth.
- [ ] `GET /orders/1` links exclude `/orders/10` and case variants. Typed search remains substring-based; `/` and Escape target the visible search control.

### 5. Draft editing, review invalidation and downloads

- [ ] Create drafts, enter concrete GET/HEAD path/status/optional client limit, and review. Editing each input clears review and disables JUnit download again.
- [ ] Recreating the same source reuses the edited draft. Missing-detail APIs and POST must not appear executable.
- [ ] Actually download JSON draft, JUnit and API draft bundle; inspect filename, nonempty content and selected API/scope. Unreviewed JSON remains saveable; JUnit requires review and includes `@Disabled`.
- [ ] Download dashboard HTML and inspect the original observation report and license notice. Imported drafts/runs need separate saves.

### 6. Scenario save/import and invalid files

- [ ] Check empty-name/unreviewed validation and disabled controls, then download scenario, Postman, disabled JMeter and local command after review.
- [ ] Import the saved scenario: new cases require review, existing edits survive and repeated source import does not duplicate cases.
- [ ] Import invalid JSON, wrong kind, unsupported schema and an over-1-MiB file separately. Errors are visible; drafts/metadata survive; selecting the same file again works.
- [ ] Inspect the empty Postman baseUrl and disabled JMeter plan. Downloads must not execute HTTP requests.
- [ ] Save before refresh; confirm page memory resets and restore by import rather than assuming persistent page state.

### 7. Run/recording history selection, removal and comparison

- [ ] Import two runs and select Current/Baseline. Distinguish HTTP status, assertion outcomes and client time; inspect condition/origin/sample/incomplete warnings.
- [ ] Clear Current or Baseline, then search/import. Keep the cleared choice; reject same-ID comparison. Search narrows the list without changing comparison selection.
- [ ] Remove a selected loaded run without automatically selecting a replacement. Original disk files remain intact.
- [ ] Repeat clear → additional import → same-ID comparison → selected removal in Recordings. Adapter times remain distinct from run client times.
- [ ] Check empty/no-match states, malformed history and conflicting IDs; actually save and reimport loaded history/catalog files.

### 8. Long paths, empty states, keyboard focus and themes

- [ ] Long paths/group/scenario/history names remain readable through wrapping, tooltips or table scrolling; actions remain reachable.
- [ ] Use Tab/Shift+Tab, Enter/Space and arrows across menus, views, sorting, checkboxes, selects, file inputs and scroll regions. Focus is visible and never trapped or moved into hidden sections.
- [ ] Empty/legacy recordings, zero matches and missing details explain the state and next available action.
- [ ] Inspect active menus, scrollbars, heatmaps, errors, disabled controls and focus in light/dark themes; record reduced-motion and forced-colors checks separately.

## Findings and narrowly scoped follow-up

For each failure record checklist step, commit, tester/date, environment/fixture,
exact actions, expected/actual result, reproduction count and sanitized evidence.
Then record the smallest fix, related troubleshooting item, regression check, fix
commit and same-environment manual retest.

Consult [troubleshooting](25_diagnostics_troubleshooting.md), [usability changes](35_dashboard_usability_review.md)
and [selection/comparison fixes](41_workspace_followup.md) first. Protect the distinction
between time scope and ordinary filters, exact links and typed search, cleared
Current/Baseline choices, and retained aggregates versus available details.
No rendered UI defect was reproduced here; no functional code, new screenshots or
final UI approval was added. No merge or release is performed.
