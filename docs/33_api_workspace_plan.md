**English** | [한국어](33_api_workspace_plan.ko.md)

# API Workspace Implementation Plan

Approved mentoring direction, first increment, 2026-10-12. Implement solo.

Goal: preserve existing contracts and maps while providing path groups, API
statistics, sorting/filtering/pagination and explicit test-target selection.

- [x] Write and observe failing model tests for aggregation, missing timing and representative detail selection.
- [x] Add isolated model/view/styles and renderer integration using the existing retained HTTP summary.
- [x] Validate 120 APIs/660 synthetic observations and a small actual sample; verify old dashboard behaviors.
- [x] Update bilingual documentation with scope, limitations and troubleshooting.
- [ ] Submit PR, leave merging and release/tag selection to the maintainer.

No collector changes, service dependency discovery, averaged group percentiles,
credential capture, scenario replay or production circuit control. Recent detail
window remains 100; missing source APIs are explicit. Statistics are retained
observations, not lifetime traffic or TPS. Follow-up: time windows/heatmaps,
history import and external scenario formats, separately scoped.

See [feature and verification details](34_api_workspace.md).
