# Diagnostics Troubleshooting

## Bounded Exports

Default JSON includes only the newest 100 unit details. `omittedRequestDetails`
reports exclusions; recording-wide endpoint aggregates and reverse lookup remain intact.
Offline timing statistics describe the exported detail window, not all traffic.
Choose an explicit local export limit only for trusted, appropriately scoped data.
Evicted observations can remain in endpoint counts/code unions, but not in
retained timing/status statistics. Do not fabricate timing from aggregate counts.

## GitHub Action Prerequisites

Use `fetch-depth: 0`. Shallow checkouts fail before network work because a
reliable merge base cannot be assumed. Java 17+, Bash, Git, curl and Python 3
must exist inside the job environment, including a configured container.

Artifact upload defaults to false. To keep evidence, set `upload-artifact: true`
and choose distinct `artifact-name` values for matrix jobs and repeated calls.
The optional impact gate runs after artifacts and does not evaluate test success.

## Offline HTML

The graph shows associations, not invocation order or measured method spans.
Pause and zoom controls are independent of statistics; reduced-motion settings
disable animation. Without scripting, the underlying report tables remain.
Downloaded pages must contain the complete Lucide/Feather notice before the
script takes its initial HTML snapshot.

## Recording Baselines

Self-comparison should show zero deltas, not floating-point rounding noise.
Missing or null `endedAt` means unfinished and contributes no timed interval.
If `omittedRequestDetails` is positive, export the live aggregate summary for a
baseline instead: truncated raw details cannot describe the full recording.
`scripts/recording-comparison.test.cjs` exercises these cases and preserves
meaningful small deltas. This view does not prove a controlled benchmark result.

## Verification

Test drafts accept concrete GET/HEAD paths only after manual review. Raw or
encoded pipes are invalid URI paths and are rejected before JUnit generation.
Observed failures are not automatically copied into expected assertions.
Generated tests are disabled by default and require an explicitly selected
local/QA target. `scripts/test-case-drafts.test.cjs` compiles generated Java,
including hostile metadata, and checks path validation.

`scripts/test-impact-action.py` covers metadata, outputs, gates, malformed input
and shallow clones with unreachable origins. `scripts/verify-dashboard.cjs`
covers animation pixels, selection, filters, license-preserving download,
desktop/mobile layouts, no-script and legacy fallbacks, and external requests.

Keep the existing OSV scan blocking. Passing feature tests is not evidence that
dependencies are free of known advisories. Never use private traffic or secrets
in public report artifacts.
