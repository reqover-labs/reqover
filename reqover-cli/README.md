**English** | [한국어](README.ko.md) | [Project](../README.md)

# Reqover CLI

Offline commands over an existing coverage report. The CLI does not attach an
agent, start an application, send HTTP requests or run tests.

Download `reqover-cli-0.4.1.jar` from the
[v0.4.1 release](https://github.com/reqover-labs/reqover/releases/tag/v0.4.1).
It is not a Maven Central library. JDK 17+ is required. Commands below assume
the downloaded JAR is in the current directory and the input files already exist.

```bash
java -jar reqover-cli-0.4.1.jar version
java -jar reqover-cli-0.4.1.jar help
java -jar reqover-cli-0.4.1.jar render --report report.json --out report.html
java -jar reqover-cli-0.4.1.jar diff --baseline before.json --current after.json --format markdown
java -jar reqover-cli-0.4.1.jar impact --report report.json --changed src/main/java/com/example/OrderService.java --format markdown
```

`render` requires `--report`; without `--out`, it prints HTML to stdout instead
of creating a file. `impact` requires `--report` and exactly one of `--changed`
or `--changed-files`; use `--changed-files -` to read paths from stdin.

| Command | Result |
| --- | --- |
| `render` | Standalone HTML dashboard, with only the details present in the input |
| `diff` | Endpoint/code changes, not timing/status deltas |
| `impact` | Observed endpoints to retest and unmatched paths |

Exit codes: `0` success, `1` an explicitly enabled `--fail-on-impact` or
`--fail-on-change` gate tripped, `2` bad usage/input. No match does not prove safety.
Use coverage report JSON, not test-draft JSON or dashboard-summary JSON.

Source build: `./gradlew :reqover-cli:shadowJar`, then use
`reqover-cli/build/libs/reqover-cli-0.4.1.jar` in place of the downloaded filename.

[CI walkthrough](../docs/18_ci_impact_analysis.md) |
[Timing/status comparison](../docs/28_recording_comparison.md)
