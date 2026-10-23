**English** | [한국어](35_dashboard_usability_review.ko.md) | [Documentation](README.md)

# Dashboard Usability Follow-Up

October 12, 2026, PR #53. Collector/CLI/coverage JSON contracts stay unchanged.

Source review identified hidden group restrictions, missing bulk-source availability,
repeated draft creation, repeated editor rendering, substring-based API navigation,
scroll carry-over and lost pagination when toggling views.

Changes: clearable group context, Reset filters (selection/sort/depth preserved),
selection actions visible in Groups, available/missing source counts before actions,
deduplicated creation preserving edited drafts, one batch editor render, exact API
navigation with ordinary substring search/Escape unchanged, scroll reset between
sections, separately remembered group/list pages, bounded table scrolling with
sticky column headings and a disabled Request map tooltip.
The `/` shortcut targets the visible workspace search, never a hidden report input.

Verification uses pure selection/draft models and an isolated filter controller
test with minimal DOM doubles, not browser/page automation. Java renderer/CLI
tests and JS syntax checks cover integration. The previous browser validation
does not prove this follow-up's layouts.

Fresh browser E2E, mobile/dark visual QA and screenshot recapture remain pending.
The browser script has additional reset/selection/dedup/edit-preservation assertions
ready to run in an available environment. Existing screenshots show the initial
iteration, not the newly added controls. No claim of exhaustive usability coverage.
