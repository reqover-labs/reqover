**English** | [한국어](README.ko.md)

# Saved scenario checks

Unreleased source feature: use this Action and the source-built CLI from the same revision.
Published 0.4.2 binaries do not contain the required run commands.

The Action starts an owned disposable Spring JAR on loopback, polls an explicit readiness path,
executes reviewed schema-1 GET/HEAD cases, writes results and stops its application.
The existing impact Action analyzes observed code/API relationships; this Action checks expectations.

See the [working workflow](../../workflows/retest.yml) and [complete usage](README.ko.md).

```yaml
permissions:
  contents: read
steps:
  # Checkout, Java 17+, Python 3.9+, and app/CLI build must precede this step.
  - uses: ./.github/actions/retest
    with:
      cli-jar: path/to/source-cli.jar
      app-jar: path/to/test-app.jar
      scenario: reviewed-scenario.json
      ready-path: /actuator/health
      confirm-local: 'true'
      upload-artifact: 'true'
```

Both confirmation and artifact upload default to false. Ports and artifact names must be unique
for multiple invocations in the same job. The application must accept Spring server address/port
arguments. HTTP 200 readiness must represent the application's actual initialization state.

Exit 0 means all planned checks passed; 1 means failed assertions or uncompleted attempts;
2 means input/startup/execution/comparison setup failure. The summary distinguishes assertion
status mismatches, client-time limits and network/timeout outcomes.

Job Summary and summary.json/md are written even after a handled failure. Available result.json/html
and optional comparison.html are uploaded only when opted in, before applying the failure gate.
Server logs and environment variables are excluded. Results still contain reviewed paths and metadata.
GITHUB_SHA, when present, is recorded in a copy of the scenario without modifying its source file.

An explicit previous result.json can be supplied as baseline. Different origins, conditions and
environments remain labelled by the comparison renderer. No automatic prior-run download or performance verdict.

Limits follow the CLI: 100 cases, 1-10 sequential repeats, 1,000 attempts, ten seconds per request,
sixty seconds per run. Startup is bounded to sixty seconds. No redirects, remote hosts, authentication,
body/query replay or concurrent load. Cleanup runs on handled failure/cancellation; runner-level termination
still requires the job timeout. Fork PRs receive no secrets or write permissions.

The provided workflow supports PR, main push and manual execution. Scheduling is opt-in; a team can
add `schedule: [{cron: '0 0 * * 1-5'}]` under on for weekdays at 09:00 KST, preserving concurrency and timeout.
No schedule is enabled by this change.
