[English](41_workspace_followup.md) | [한국어](41_workspace_followup.ko.md) | [Documentation](README.md)

# Workspace selection and comparison follow-up

2026-10-23. Unreleased PR #53 source work; published 0.4.2 artifacts are unchanged.

## Behavior

- Map-inspector API links select the exact endpoint, including path case, instead of a substring search.
- Run-history search preserves an explicitly cleared current or baseline selection.
- Recording imports preserve cleared selections. Defaults apply only to the initial nonempty import.
- Removing a selected entry clears its selection rather than silently substituting another entry.
- Selecting the same current and baseline entry shows a status message instead of a self-comparison table.
- Empty histories, unmatched searches and comparisons without observations display explicit states.
- Scenario validation errors appear beside disabled actions. Theme text colors and long metadata wrapping are explicit.

## Validation

Three failing isolated controller regressions reproduced the exact-link, selection and empty-state problems.
After the fixes, all five focused controller tests and all 48 Node tests passed.
These use model/controller doubles and generated Java compilation, not rendered-browser automation.

Java report (79 tests) and CLI (25 tests) passed with zero failures/errors.
The owned loopback fixture completed eight requests, before-fix exit 1 -> after-fix exit 0,
two run histories and one recording archive with generated reports/comparison.
The existing synthetic 120-API report was regenerated with the updated CLI; new controls and handlers are embedded.
Rendered browser E2E, narrow-screen layout, light/dark contrast, keyboard focus and downloads remain pending.
No alternate URL, browser or indirect execution was used to bypass the local-page restriction.

## Manual review

1. Small reports: map inspector -> API link -> exactly one endpoint.
2. Medium reports: groups -> API table -> filter/select -> view switch with selection retained.
3. Large reports: time distribution -> time/duration scope -> groups/API -> matching request map.
4. Scenarios: empty cases, invalid name, restored name, save/import and mandatory re-review.
5. Run history: import two, clear baseline, search, same-entry guard, remove current.
6. Recordings: empty list, import two, clear baseline, import another, same-entry guard.
7. 390px layout, long metadata, keyboard focus, both themes and invalid files.

History search filters rows without changing current/baseline comparison choices.
See the [retest workflow](39_retest_workflow.md) for file persistence and page-state limits.
