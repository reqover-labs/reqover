**English** | [한국어](39_retest_workflow.ko.md) | [Documentation](README.md)

# Scenarios, Local Checks and History (Unreleased)

Source-branch functionality, not in published 0.4.2 artifacts. Build this branch's
CLI before using the new commands; using the released JAR will not provide them.

## Save, Review and Export

The new **Scenarios and runs** menu has Scenario, Run history and Recordings views.
Existing requests still become drafts in Test drafts. Remove unwanted/mutating
drafts, set concrete paths/expected status/client limits, and review each explicitly.
Scenario name, environment and code revision are manually supplied metadata.

Save scenario writes `reqover-test-scenario`, schema 1: name/environment/revision,
UTC save time and at most 100 cases containing ID, method, concrete path, expected
status, optional client limit and review flag. Empty path/expectation is allowed
for unfinished drafts, but unsafe paths/numeric conditions are not. No captured
headers, body, credentials, query values or observed code are included.

Import appends cases without overwriting existing edits. Newly imported cases
always require review again. Invalid imports leave the old state intact. Duplicate
source identities reuse existing edited drafts. Saved files, not page memory, provide
persistence; refresh loses unsaved page state. Original HTML download still saves
the original recording, not imported scenarios/results; save those separately.

Reviewed GET/HEAD-only scenarios export as [Postman Collection 2.1](https://schema.postman.com/)
with an empty `baseUrl` variable and status/client-time assertions, or a disabled
[JMeter 5.6.3 JMX plan](https://jmeter.apache.org/usermanual/component_reference.html#HTTP_Request).
JMeter needs an explicitly configured `reqover.host`, port/protocol and an enabled
TestPlan after review. Disabled plans may exit the headless driver without an active
test; they intentionally send no traffic. Generated labels neutralize user-supplied
`${...}` function syntax. No automatic engine execution occurs during export.

## Bounded Local Execution

```bash
./gradlew :reqover-cli:shadowJar
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar run \
  --scenario scenario.json --base-url http://127.0.0.1:8080 \
  --confirm-local --repeat 2 --history-dir .reqover/runs
```

Only literal `127.0.0.1` or `[::1]` HTTP(S) origins are accepted. No userinfo, query,
fragment or non-root base path. No remote QA hosts in this increment. GET can have
side effects: use a disposable target, not production or a valuable local service.
No proxy, redirects, authentication, cookies or request bodies are configured.

1-10 repeats, 100 cases maximum, at most 1000 planned attempts, sequential execution.
Connect timeout is 5 seconds; the full response future has a 10-second deadline,
including body consumption. A 60-second overall budget stops further attempts;
interruption cancels the pending future and stops. Cancellation cannot undo server
work. This is bounded regression checking, not a concurrent load generator.

Exit 0 means every planned attempt met its reviewed status and optional client-time
expectation; exit 1 means failed assertions or unexecuted planned attempts; exit 2
means invalid input/usage/storage. Expected 4xx/5xx can pass. Times use monotonic
client elapsed time, not the agent adapter interval. Tiny-sample percentiles are
descriptive, not a capacity/stability benchmark.

## Results and Run History

Each execution writes unique `.run.json` and static HTML files. It stores case IDs,
actual status or null, monotonic elapsed time, stable outcome codes, completion,
planned attempts, scenario metadata/fingerprint and explicit origin. No response
body/header or exception text is stored. History writeability is checked before traffic.
CREATE_NEW prevents accidental overwrite; a write failure can leave a partial pair.

```bash
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar runs --history-dir .reqover/runs --out history.json
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar run-report --result run.json --out run.html
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar run-compare --baseline before.run.json --current after.run.json --out comparison.html
```

The catalog returns the latest 20 of at most 1000 files, with omitted count. The UI
loads at most 20 runs, filters metadata, selects current/baseline, shows pass/fail/not-run
and client timing, exports loaded history and removes entries from page memory only.
Deleting a loaded entry does not delete disk files. UI shows first 100 attempt details;
summary uses every attempt. Comparison checks declared scenario conditions, origin
and environment, allows code revisions to differ and warns on sample/count/completion
differences. Files are not authenticated evidence; no automatic performance verdict.

## Recording Archive (Separate from Runs)

```bash
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar recording-save \
  --report coverage.json --name "Before change" --environment local --revision abc
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar recordings --out recording-catalog.json
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar recording-report --entry snapshot.recording.json --out restored.html
```

Named coverage archives persist canonical report data and HTML under `.reqover/recordings`.
Unknown raw JSON fields are dropped rather than copying headers/secrets. Original
exported-detail omissions are stored separately and displayed in restored HTML.
Input is limited to 10 MiB/10,000 details, archives to 20 MiB; catalogs scan at most
1000 files and return the latest 20. A clipped input cannot recover missing traffic.

Recordings view imports archives/catalogs and retains only name/environment/revision,
summary and omission metadata, not raw detail strings. Current/baseline comparison
uses the existing adapter-interval/HTTP-status comparison rules. Keep this distinct
from client test-run timing and assertions. Loaded catalog export is portable summary
history, not a replacement for the disk archive's full canonical report.

## Verification and Boundaries

Models/controller doubles cover save/import/re-review, invalid-import preservation,
history bounds, review gates, source data stripping and declared-condition comparison.
Java tests use disposable owned loopback servers, checking consent, status/time
assertions, redirect refusal and persistent history. End-to-end CLI fixture executes
8 local requests: failed-before exit 1, fixed-after exit 0, two archived runs, comparison
and report output. Postman export validates against the official schema; verified
Apache JMeter 5.6.3 binaries execute the reviewed plan with real status assertions.

Fresh app-browser visual/E2E/mobile/dark validation remains pending. No browser
restriction workaround was used. No remote execution, faithful replay, login/token
handling, variable extraction, concurrent load generation, scheduling, method/DB spans
or automatic publication is added. GET/HEAD is a method gate, not proof of no side effects.
Generated files can still contain private paths, labels and code names; never commit
them automatically. `.reqover/` and test artifacts are ignored.
