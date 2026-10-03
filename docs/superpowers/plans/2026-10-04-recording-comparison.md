# Retained Recording Comparison

**Goal:** Compare before/after retained HTTP recordings without pretending they
are controlled load runs or automatic performance verdicts.

**Architecture:** The Java renderer embeds compact per-endpoint summaries computed
from all retained HTTP observations. A pure offline JS module reads a previous
coverage report or an exported summary and calculates descriptive deltas. An
isolated comparison view handles imports, filters, and explicit condition review.
No collector, probe ABI, coverage JSON schema, CLI, or deployment changes.

## Constraints

- Current statistics use all retained observations, not the graph's latest 100.
- Reuse RequestSummary's final-status, invalid-interval and nearest-rank rules.
- Preserve submillisecond intervals in imported ISO timestamps with native Date
  validation and BigInt nanosecond arithmetic; reject unsupported/malformed dates.
- Unknown/absent measurements remain unknown, not zero.
- New/missing rows mean observed on one side only, not newly created/deleted APIs.
- Surface differing sample counts, small timing samples and unknown statuses.
- Scenario/environment equivalence is not recorded; manual confirmation is not a verdict.
- Read at most 10 MiB and 50,000 observations locally; never upload or persist imports.
- Keep source recordings, generator, maps, existing CI and legacy/no-script tables intact.
- Export aggregate summary JSON only; never commit user-imported files or secrets.

## Tasks

- [x] Inspect summary semantics and existing renderer/workflow boundaries.
- [x] Add failing JS/Java regression tests for precision, scope, missing data and deltas.
- [x] Implement compact summary data and the offline comparison model/view.
- [x] Verify file import, bad/legacy files, filter/reset/export, confirmations,
  desktop/mobile/dark states and existing draft/map behavior.
- [x] Update README, mentoring roadmap, guide and troubleshooting notes.
- [x] Run Java/Node/browser tests and workflow lint: 157 Java / 20 Node tests pass;
  browser reports zero page errors and external requests.
- [x] Review staged files for secrets and publish PR #25 update without merging.

## Commands

```powershell
node --test scripts/recording-comparison.test.cjs
.\gradlew.bat test build --quiet
node scripts/verify-dashboard.cjs sample.html legacy.html tmp/dashboard-check
```

Delivery continues on the existing preview branch and PR #25, without merging.
OSV findings remain a separate unresolved gate; do not disable or claim them fixed.
