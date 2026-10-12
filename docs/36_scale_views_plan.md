**English** | [한국어](36_scale_views_plan.ko.md)

# Scale Views Plan

2026-10-12, follow-up to API workspace. Implement three connected levels:
all-retained time distribution, scoped path groups/API rows, and recent request details.

Java computes five-minute UTC windows and interval bands before the 100-detail cap.
Every window and band has independently calculated endpoint summaries. No average
of percentiles. Keep the latest 256 nonempty windows and report omitted/unsupported
counts. Show labeled compressed gaps rather than a misleading continuous timeline.

Selected time/interval scope constrains statistics and draft sources. Missing recent
details remain explicit. Request-map navigation chooses the exact retained source.
No new collector, persistent history, automatic replay or load generator.

Tests: full-retained versus latest-100, boundaries/unfinished/negative/non-HTTP,
window limits, extreme timestamps, source scoping and controller drill-down.
Fresh browser visual/E2E checks remain pending under the current local-file restriction.
