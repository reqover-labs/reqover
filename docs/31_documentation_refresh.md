# Current Usage Documentation Refresh

Baseline: release **0.4.2**, checked against main `0afe11b` on October 6, 2026.
This is a documentation-only update, not a release or runtime behavior change.

## Scope

Root READMEs, English/Korean module guides, the documentation index, architecture,
integration, CI and diagnostic guides describe current released behavior.
Historical planning, Phase 0 results and competition drafts retain their original
versions and measurements, with explicit links to current usage.

## Corrections

| Outdated or ambiguous statement | Current guidance |
| --- | --- |
| Default quickstart includes response DTO accessors | Two recorded classes, three methods; accessors are skipped unless explicitly enabled |
| The old video depicts the current dashboard | Label its v0.2.0 provenance; show the current dashboard as the leading image |
| Artifact upload happens automatically | Opt in with `upload-artifact`, distinct names, full history and stated runner prerequisites |
| Setting a target enables generated JUnit | Review and explicitly remove `@Disabled`; the dashboard does not run tests |
| JSON lacks request timestamps/status | Optional request details contain these fields and have a separate 100-detail default |
| Counts and diagnostics have the same scope | Recording-wide endpoint aggregates versus retained HTTP timings and exported details |
| Endpoint and shutdown export are byte-identical | Shared renderers; shutdown export can accumulate multiple contexts |
| Repeated traffic produces identical JSON | Request IDs, timestamps, durations and threads can differ; use semantic code diff |
| Timing/status comparison is future work | Available in the dashboard; CLI `diff` remains a code-relationship comparison |
| Central publication and SPI contract are pending | Published libraries and implemented contract/retention behavior belong under delivered work |

No global replacement of `0.2.0` or `io.reqover` is appropriate: old release
history is intentional and Java package names are unchanged. Protected organiser
templates and private submission files are not restored or published.

The 0.4.2 publication completed during this update. Current install commands and
artifacts were aligned only after verifying that release. The v0.2.0 video,
0.4.0 diagnostic introduction and 0.4.1 WebFlux fix remain dated milestones.

## Validation

Check relative links and heading anchors in the changed current guides, compare
default values to source/Action metadata, and smoke-test the documented CLI
commands using the checksum-verified 0.4.2 release. Preserve source/configuration,
SBOM and security policy; no application behavior changes require new runtime tests.

Outcome: 56 Markdown files checked, 474 relative links/anchors valid, no
historical link regressions, all nine CLI smoke cases passed against the
checksum-verified 0.4.2 release. All six 0.4.2 library POMs returned HTTP 200
from Maven Central after propagation. Application sources, workflows, build
metadata and the SBOM match the synchronized main; the PR contains documentation only.
