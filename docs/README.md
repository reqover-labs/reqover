**English** | [한국어](README.ko.md) | [Project](../README.md)

# Documentation

Current usage is based on **0.4.1**. Libraries are on Maven Central under
`io.github.reqover-labs`; agent and CLI executables are in the
[GitHub release](https://github.com/reqover-labs/reqover/releases/tag/v0.4.1).
Java packages remain `io.reqover.*`. The root quickstart pins that release;
`main` may contain subsequent changes.

## Start Here

| Task | Guide |
| --- | --- |
| Install in a Spring Boot application | [Integration and properties](17_integration_guide.md) |
| Find slow/failing recorded requests | [Request diagnostics](24_request_diagnostics.md) |
| Inspect observed relationships and keep CI evidence | [Dashboard and CI](26_dashboard_and_ci.md) |
| Prepare reviewed JSON or disabled JUnit tests | [Test drafts](27_test_case_drafts.md) |
| Compare retained HTTP time/status summaries | [Recording comparison](28_recording_comparison.md) |
| Use code diff and retest candidates in a pull request | [CI impact analysis](18_ci_impact_analysis.md) |
| Understand an error or a data limit | [Troubleshooting](25_diagnostics_troubleshooting.md) |

CLI `diff` compares code relationships; dashboard comparison compares retained
HTTP statistics. Neither runs a load test. Drafts do not replay original inputs.
Endpoint aggregates and the retained/detail-export windows are different scopes.

## Modules

| Module | Purpose |
| --- | --- |
| [Agent](../reqover-agent/README.md) | Recording options and standalone JAR |
| [Starter](../reqover-spring-boot-starter/README.md) | Boot wiring, opt-in reports and exports |
| [MVC](../reqover-spring-mvc/README.md) / [WebFlux](../reqover-spring-webflux/README.md) | Request attribution and lifecycle boundaries |
| [Report](../reqover-report/README.md) | Dashboard, diagnostics, formats and analyses |
| [CLI](../reqover-cli/README.md) / [Action](../.github/actions/impact/README.md) | Offline commands and CI defaults |
| [Core](../reqover-core/README.md) / [Instrumentation](../reqover-instrumentation/README.md) | Extension APIs, store and ASM recording |
| [Examples](../examples/README.md) | Loopback demos and synthetic failure/slow requests |

## Design and Evidence

- [Current architecture](02_architecture.md) and [compatibility policy](20_versioning_and_compatibility.md).
- [Prior art](19_prior_art.md) and [JaCoCo interoperability decision](14_jacoco_interop_decision.md).
- [Method-entry measurement](15_performance_results.md): a dated benchmark, not overall current-release overhead.
- [Security policy](../SECURITY.md), [SBOM](../sbom/README.md) and [OSV remediation history](29_osv_dependency_remediation.md). A documented expiring exception is not a patched dependency.
- [Documentation refresh decisions](31_documentation_refresh.md).

## Historical Material

These are retained for provenance, not current installation or capability claims:

- [Original project plan](00_project_plan.md), [requirements](01_requirements.md) and [Phase 0 spike](03_phase0_spike_plan.md).
- [Phase 0 result](08_phase0_mvp_status.md), [early E2E notes](09_agent_e2e_demo.md), [early demo script](10_demo_script.md) and [original screenshot capture](16_readme_demo_capture.md).
- [Performance-validation plan](23_performance_validation_plan.ko.md) and [review corrections](30_review_corrections.md).
- [Competition archive](competition/README.md), including the v0.2.0 video/report drafts. Keep their versions, measurements and dates intact.

Use the current guides above and [CHANGELOG](../CHANGELOG.md) for released behavior.
