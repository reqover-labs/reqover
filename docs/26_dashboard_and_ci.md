**English** | [한국어](26_dashboard_and_ci.ko.md)

# Diagnostic dashboard and CI artifacts

The dashboard shipped in **0.4.0**; this guide uses **0.4.2**. The `v0.4.2` Action downloads the released
`reqover-cli-0.4.2.jar`, whose `render` produces this dashboard; no source build
is needed. Request diagnostics require a recording with the optional `requests`
details introduced in 0.4.0. Older JSON still renders; missing details are shown
as unavailable, not as zero-duration successful requests.

## What to review first

The dashboard prioritizes recorded HTTP 5xx, HTTP 4xx, requests exceeding the
selected slow threshold, and methods observed in multiple APIs. It does not infer
business criticality from a class name. Expected 4xx responses can be passing
tests. The review queue is not a test verdict.

- **Request map:** HTTP entry, the request's bucket, its recorded result, and
  independently observed methods. Method edges represent set membership, not
  method-to-method calls or timing.
- **Retest map:** a selected method and the APIs in its reverse index. Select a
  node to inspect it, then follow an API into the existing endpoint table.
- **Animation:** moving dashed associations, with pause and zoom controls. They
  are illustrative, not a replay of measured execution. Reduced-motion settings
  disable animation.
- **Fallback:** the original tables and native request details remain available
  without JavaScript. All assets and icon licenses are inline or packaged locally.

The request map and review queue use the latest 100 HTTP observations. A request
graph displays at most 12 methods; a retest graph displays at most 16 APIs, with
an overflow label. Full request details and the reverse table retain the rest.
Overview statistics use all retained HTTP observations, not only graph samples.

The reverse index preserves existing endpoint labels, including names of manual
non-HTTP scopes. The graph therefore calls these **endpoints**, not a guaranteed
HTTP-only API count. Non-HTTP observations never enter the HTTP timing/error metrics.

## Add it to an existing CI job

Before this step, your application must already have the Reqover agent/starter
attached and have produced `build/reqover-report.json` from a relevant integration
scenario. The Action does not launch or install instrumentation in your application.
See [the recording guide](18_ci_impact_analysis.md) and
[Spring integration](17_integration_guide.md). The starter is on Maven Central as
`io.github.reqover-labs:reqover-spring-boot-starter:0.4.2`; the agent JAR comes
from the [v0.4.2 GitHub Release](https://github.com/reqover-labs/reqover/releases/tag/v0.4.2).

Prerequisites: Ubuntu runner, Java 17+ (21 for the example), Python 3, Bash,
and `actions/checkout` with `fetch-depth: 0`. The example belongs after recording
and Java setup in your existing `pull_request` workflow:

Container jobs such as `container: eclipse-temurin` do not necessarily include
Python. Install Python 3 in the container before the Action. The script checks
that prerequisite and shallow history before downloading the CLI or fetching a base.

```yaml
- name: Retest candidates and dashboard
  id: reqover
  uses: reqover-labs/reqover/.github/actions/impact@v0.4.2
  with:
    report: build/reqover-report.json
    comment: "false"
    upload-artifact: "true"
    artifact-name: reqover-${{ github.job }}-${{ strategy.job-index || 'single' }}
    analysis-name: ${{ github.job }}-${{ strategy.job-index || 'single' }}
```

The checkout of **your application repository** needs `fetch-depth: 0`. The
Action downloads the CLI matching its `version` input (`0.4.2` by default).
To try an unreleased build instead, build the CLI yourself and pass its path as
`cli-jar`; `version` is then ignored. This repository's own CI does that with its
local Action, as shown in [.github/workflows/build.yml](../.github/workflows/build.yml).

Start with `permissions: contents: read` and `comment: "false"`. For same-repository
PR comments, grant `pull-requests: write` and opt into comments. Fork PR comments
are skipped; their summary and artifacts still work. Comment permission failures
are visible warnings and do not erase successful analysis. Do not use
`pull_request_target` to run untrusted PR code with write credentials.

## Outputs and defaults

The job summary contains the impact Markdown. Upload is opt-in. When enabled, a seven-day artifact contains only
`report.html`, `impact.md`, and `impact.json`; it does not glob your workspace or
copy the original report JSON. Download and open `report.html` locally.

| Input | Default | Purpose |
| --- | --- | --- |
| `report` | `build/reqover-report.json` | Previously recorded JSON |
| `version` | `0.4.2` | Release to download the CLI from |
| `cli-jar` | empty | Use an existing CLI JAR (e.g. an unreleased build) instead of downloading |
| `base-ref` | PR base | Explicit Git ref required on push/manual runs |
| `comment` | `true` | Update only the marked Reqover bot comment |
| `upload-artifact` | `false` | Opt in to save the three named output files |
| `artifact-name` | `reqover-report` | Use distinct names in a matrix |
| `analysis-name` | empty | Names the comment marker; give each analysis in one PR its own |
| `fail-on-impact` | `false` | Optional gate, applied after publishing evidence |

Outputs are `markdown`, `has-impact`, `impacted-endpoint-count`,
`unmatched-path-count`, `html-path`, `json-path` (impact JSON), `markdown-path`,
and `artifact-url` (empty if upload is disabled). Each invocation uses an isolated
temporary output directory. The original `markdown` and `has-impact` remain.

When one pull request runs several analyses (a matrix, several services), give
each its own `analysis-name` so each keeps its own comment. Analyses that share
a name should comment from one job only, to avoid competing updates.
Set distinct artifact names for repeated invocations in one job as well; fixed
names are never uploaded unless the caller explicitly opts in.

`fail-on-impact` fails when **observed APIs executed changed code**, not when a
test fails. Leave it off for ordinary review. Zero candidates and unmatched files
never prove an API is safe. Faithful replay, k6 execution, TPS, method timings,
input capture and automatic test execution remain separate planned work. Reviewed
test drafts are created by hand in the dashboard ([guide](27_test_case_drafts.md));
the Action does not generate or run them.

## Security and verification

Only use synthetic or authorized QA recordings. HTML still contains code names,
request IDs, and thread names: artifacts are not inherently safe to publish.
No `.env`, API keys, headers, body inputs, or credentials are collected by
Reqover. The Action cannot sanitize arbitrary secrets injected into report fields.

Local Action tests execute the real CLI in temporary Git repositories, including
spaces in paths, changed/unobserved code, missing report/base-ref errors, and a
base branch advancing after a PR diverged:

```bash
./gradlew :reqover-cli:shadowJar
python3 scripts/test-impact-action.py --cli reqover-cli/build/libs/reqover-cli-0.4.2.jar
```

For visual checks, install Playwright in your test environment and run
`node scripts/verify-dashboard.cjs sample.html legacy.html tmp/dashboard-check`
against synthetic recordings. It verifies graph selection, changing animation
pixels, pause/zoom, filters, download, no external requests, no-script fallback,
reduced motion, legacy data, and 1920/1280/760/390 px layouts.

The OSV dependency scan remains fail-on-vulnerability. The previously reported
Jackson/Tomcat findings are addressed by the [dependency patch and regenerated
SBOM](../CHANGELOG.md); functional tests alone never prove this.
