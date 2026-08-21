**English** | [한국어](18_ci_impact_analysis.ko.md)

# 18. Impact analysis in CI

Reqover records which endpoints executed which methods. Point that record at a
diff and it answers the question a reviewer actually has:

> I changed these files. **Which APIs should be retested?**

This document is the whole loop: getting a report out of a test run, asking it
about a change, and wiring it into a pull request.

## What this can and cannot tell you

Read the limits first, because they decide whether the output is useful to you.

**It can tell you:** these observed endpoints ran code in the files you changed.

**It cannot tell you:** that anything else is unaffected. Impact analysis only
knows about code that was *observed running* while the report was recorded. A
changed file that does not appear in the report lands in the "no observed
coverage" list, and that means one of two things Reqover cannot distinguish:

- nothing calls it, or
- nobody exercised it during the run that produced the report.

So the quality of the answer is the quality of the recording. A report from a
thorough integration suite is worth acting on; a report from one manual click is
not. Treat the output as **where to start looking**, never as proof of safety.

## Step 1 — get a report out of a test run

A report normally lives in memory and dies with the JVM. The starter can write
it to a file when the application context closes:

```properties
reqover.report.export.json-path=build/reqover-report.json
reqover.report.export.html-path=build/reqover-report.html
```

Run the application with the agent attached, drive your integration tests
through it, and let it shut down normally.

```bash
java -javaagent:reqover-agent-0.2.0.jar=include=com.example \
  -jar build/libs/your-app.jar \
  --reqover.report.export.json-path=build/reqover-report.json
```

Two things to know:

- The export runs on **normal context shutdown**. A process killed with
  `SIGKILL` writes nothing. In CI, stop the application with `SIGTERM` (the
  default `kill`) and wait for it to exit.
- Export failures are logged and swallowed. Reqover will not be the reason your
  shutdown fails, which also means a missing file is a quiet failure — check that
  the file exists before analysing it.

If you prefer, you can save what the HTTP endpoint serves instead. It is the
same document, byte for byte:

```bash
curl -sf http://127.0.0.1:8080/reqover/report > build/reqover-report.json
```

The written JSON is fully resolved — class and method names are in the document —
so nothing needs the recording JVM to read it back.

## Step 2 — ask what a change affects

```bash
git diff --name-only origin/main...HEAD \
  | java -jar reqover-cli-0.2.0.jar impact \
      --report build/reqover-report.json \
      --changed-files -
```

```
Reqover impact analysis
  changed paths analysed: 3
  impacted endpoints:     2

Endpoints to retest:
  GET /orders/{id}
      via com.example.order.OrderService#find(long): OrderResponse
  POST /payments
      via com.example.SharedValidator#validate(String)

Changed paths with no observed coverage (1):
  README.md
```

Use `origin/main...HEAD` with three dots: it lists what your branch changed, not
what moved on the base branch since you forked.

`--format markdown` produces a table sized for a pull request comment, and
`--format json` produces machine-readable output for another step in your
pipeline.

## Step 3 — put it on the pull request

The repository ships a composite action:

```yaml
name: reqover

on: pull_request

permissions:
  contents: read
  pull-requests: write

jobs:
  impact:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: 21

      # Whatever produces build/reqover-report.json for your project:
      # boot the app with the agent, run the integration suite, shut it down.
      - name: Record a report
        run: ./scripts/record-reqover-report.sh

      - uses: reqover-labs/reqover/.github/actions/impact@v0.2.0
        with:
          report: build/reqover-report.json
```

`fetch-depth: 0` matters — the action needs history to diff against the base.

The action's inputs:

| Input            | Default   | What it does                                                     |
| ---------------- | --------- | ---------------------------------------------------------------- |
| `report`         | *required* | Path to the recorded report JSON                                  |
| `version`        | `0.2.0`   | Release to download the CLI from                                  |
| `base-ref`       | PR base   | Git ref to diff against; required outside a pull request          |
| `fail-on-impact` | `false`   | Fail the step when any observed endpoint runs changed code        |
| `comment`        | `true`    | Post the analysis as a pull request comment                       |

It writes the analysis to the job summary, exposes it as the `markdown` output,
and updates its previous comment rather than adding a new one each push.

> `fail-on-impact: true` fails the build **when the change is covered**, which is
> the opposite of what you usually want. It is for the narrow case of a module
> that is supposed to be unreachable from the API surface. For normal review,
> leave it off and read the comment.

## Command reference

`reqover` below means `java -jar reqover-cli-0.2.0.jar`.

### `render`

Turn a recorded report into the standalone HTML page.

```bash
reqover render --report build/reqover-report.json --out build/reqover-report.html
```

Useful for publishing the report as a CI artifact without keeping the
application running.

### `impact`

```bash
reqover impact --report <file> (--changed-files <file>|- | --changed a,b) \
               [--format text|markdown|json] [--out <file>] [--fail-on-impact]
```

Pass changed paths as a newline-separated file, as `-` for standard input, or
inline with `--changed a.java,b.java`. Exactly one of the two forms is required.

### `diff`

```bash
reqover diff --baseline <file> --current <file> \
             [--format text|markdown] [--out <file>] [--fail-on-change]
```

Compares two recordings: endpoints seen on only one side, and the code an
endpoint started or stopped executing. This is most useful with a **committed
baseline** — record a report from a fixed scenario, commit it, and let CI compare
each build against it. Reports are written sorted and pretty-printed exactly so
that this diffs cleanly in git.

Note what a difference means: both sides describe *observed traffic*, so a change
can be a code change, a different scenario, or traffic that did not run this
time. The diff reports the change; deciding which it was is your job.

### Exit codes

| Code | Meaning                                              |
| ---- | ----------------------------------------------------- |
| `0`  | Success; no gate tripped                              |
| `1`  | A `--fail-on-impact` or `--fail-on-change` gate tripped |
| `2`  | Bad usage, unreadable file, or a file that is not a report |

The separation matters in CI: `2` means your pipeline is misconfigured, `1`
means the pipeline worked and the gate caught something.

## How file matching works

The report stores binary class names such as
`com.example.order.OrderService`. The analyser turns each one into the source
path it would have been declared in — `com/example/order/OrderService.java` — and
matches a changed path when it ends with that at a directory boundary.

So `src/main/java/com/example/order/OrderService.java` matches, and
`src/main/java/com/example/notorder/OrderService.java` does not. Layout does not
matter: Gradle, Maven, and multi-module repositories all work, because only the
package-shaped tail is compared.

Details worth knowing:

- **Nested classes** match through their declaring file. `OrderService$Row` maps
  to `OrderService.java`.
- **Kotlin** is matched too: a `.kt` variant of each path is tried.
- **Windows separators** and leading `./` are normalised.
- **A secondary top-level class in a differently named file is not matched.**
  That file lands in the unmatched list, so the miss is visible rather than
  silent.

## Troubleshooting

**The report file was never written.** The application was killed rather than
shut down, or the export path is not set. Check the process exits on `SIGTERM`
and look for the `[reqover] wrote the JSON report to ...` line on stdout.

**Every changed path is unmatched.** Usually the agent had no `include=` covering
your packages, so nothing was recorded at all. Open the HTML report: if it lists
no endpoints, the problem is upstream of the analysis.

**The endpoint you expected is missing.** It was not exercised during the
recording. Impact analysis cannot infer it.

**`reqover: ... is not a Reqover report`.** The file is not the JSON this tool
writes — a truncated download, or an HTML page saved by mistake.

## Related

- [Spring integration guide](17_integration_guide.md) — the full property list
- [System architecture](02_architecture.md) — how attribution is recorded
- `scripts/run-impact-demo.sh` — the whole loop against the demo application, in
  one command
