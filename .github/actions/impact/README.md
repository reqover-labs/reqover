**English** | [한국어](README.ko.md) | [Project](../../../README.md)

# Impact Action

Analyse changed source paths against an existing coverage report, add a job
summary/PR comment, and optionally keep HTML/impact artifacts. Recording happens
before the Action; it does not install instrumentation or run application tests.

Use `reqover-labs/reqover/.github/actions/impact@v0.4.2` in a `pull_request` job
after full-history checkout (`fetch-depth: 0`), Java setup and report generation.
The runner needs Java 17+, Bash, Git, curl and Python 3, including inside containers.

```yaml
- uses: reqover-labs/reqover/.github/actions/impact@v0.4.2
  with:
    report: build/reqover-report.json
    comment: "false"
    upload-artifact: "true"
    artifact-name: reqover-${{ github.job }}-${{ strategy.job-index || 'single' }}
    analysis-name: ${{ github.job }}-${{ strategy.job-index || 'single' }}
```

Defaults: CLI `version: 0.4.2`, `comment: true`, `upload-artifact: false`,
`fail-on-impact: false`. Upload contains only `report.html`, `impact.md` and
`impact.json`, kept for seven days. Use distinct artifact names for matrix and
repeated steps; separate `analysis-name` values keep independent bot comments.

Comments require `pull-requests: write` and a same-repository PR. Fork comments
are skipped. Do not run untrusted PR code with `pull_request_target` privileges.
Outside PRs, set `base-ref` explicitly. `cli-jar` overrides release download.

The impact gate runs after evidence and fails on observed affected endpoints,
not test failure. Unmatched files or zero candidates never prove safety.
Report metadata can still be private; do not publish arbitrary inputs or secrets.

[All inputs/outputs](../../../docs/26_dashboard_and_ci.md) |
[Complete recording workflow](../../../docs/18_ci_impact_analysis.md) |
[Source metadata](action.yml)
