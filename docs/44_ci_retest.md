**English** | [한국어](44_ci_retest.ko.md) | [Documentation](README.md)

# Sidebar scrolling and saved-scenario CI

Unreleased PR #53 work, 2026-10-23.

The sidebar now scrolls in short desktop windows, keeps a minimum 44px link height,
marks the current page with a rail and background, and retains the wrapped mobile menu.
Thin native scrollbars use theme colors and hover states. Reduced motion and forced colors are respected.
Rendered-browser verification remains pending; generated HTML is not visual approval.

The [retest Action](../.github/actions/retest/README.md) starts an owned loopback Spring JAR,
waits for its readiness endpoint, executes a saved reviewed scenario, writes JSON/HTML and stops the app.
It publishes a fixed-format Job Summary, optionally uploads result artifacts even on assertion failure,
then applies the final gate. An explicit baseline supports comparison with existing condition warnings.

Seven integration cases cover missing consent, pass, status mismatch, client-time limit,
startup failure, invalid baseline and existing output protection. Checks include cleanup, exit codes and GitHub outputs/summary files.
The source SHA is written into a scenario copy. The original saved scenario is unchanged.
All seven runner cases, 48 Node tests, 79 report tests and 25 CLI tests passed locally.
The saved MVC smoke scenario passed all three checks, including the expected 503 response.
Release verification now runs the full Node suite and runner checks; no tag or release was created.

The example workflow runs on PR, main push and manual dispatch with contents:read permissions,
concurrency and a job timeout. Scheduled execution is documented as opt-in, not enabled automatically.
Logs, credentials, remote execution and concurrent load are outside the new Action.

The [release checklist](43_release_readiness.ko.md) keeps competition feedback, rendered QA,
merge/version preparation and CI progress explicit. No release is declared complete before those checks.
