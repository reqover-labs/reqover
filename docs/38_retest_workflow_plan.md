**English** | [한국어](38_retest_workflow_plan.ko.md)

# Retest Workflow Plan

2026-10-12. User approved all five priority items. Implement solo with existing
collectors, coverage JSON, impact Action and original maps unchanged.

- Credential-free schema-1 scenario files, explicit review, bounded imports.
- Postman Collection 2.1 and disabled JMeter JMX export for reviewed GET/HEAD.
- Explicit CLI consent and literal loopback origin; sequential checks, 1-10 repeats,
  1000 attempts maximum, 10-second full-response deadline and 60-second run budget.
- Typed result JSON and static HTML; assertions separate from HTTP error counters.
- Local run and recording archives, name/environment/revision, bounded catalogs,
  UI file import/selection/removal/comparison and portable history export.

Tests cover invalid inputs, review, origin/path boundaries, actual owned loopback
communication, status/timing failures, redirect refusal, history contracts, official
Postman schema and real JMeter headless operation. Browser E2E/visual QA remains
explicitly pending; no local-file browser restriction workaround is used.

Generated/private artifacts stay in ignored `.reqover/` or `tmp/`; no automatic
uploads, credential capture, production run, persistent browser storage or release.
