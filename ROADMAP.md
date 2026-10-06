**English** | [한국어](ROADMAP.ko.md)

# Roadmap

What we intend to do next, in the order we intend to do it, and why. There are
no release promises — this is a side project maintained by two people, and a date we
cannot keep is worse than no date.

Everything here is open to argument. If something below matters to you, or the
order looks wrong for how you would use Reqover, say so in the issue or in
[Discussions](https://github.com/reqover-labs/reqover/discussions) — that is
the fastest way to change what we work on.

## Now

**Lightweight pre-deploy validation.** 0.4.0 shipped request diagnostics, the
offline dashboard, reviewed test drafts and recording comparison
([plan](docs/23_performance_validation_plan.ko.md), Korean). Next: selective
input capture with masking, method-level timing, and a k6 run/import loop.
Faithful replay and production APM remain non-goals.

**Improve installation and real-project feedback.** Libraries are already
published on Maven Central as `io.github.reqover-labs`, with agent/CLI JARs on
GitHub Releases. Validate integration beyond our samples and keep the
[current guides](docs/README.md) accurate. The
[compatibility policy](docs/20_versioning_and_compatibility.md) applies to those releases.

**Validate retention and impact scope on larger recordings.** The store SPI
contract tests, configurable eviction policy and recording-wide endpoint
aggregates are implemented. Next, measure their memory cost with more distinct
endpoints/probes, verify custom-store behavior and distinguish retained timing
from full-recording code relationships.

## Delivered in the Current Release

- Six Maven Central libraries; standalone agent and CLI release artifacts.
- Optional accessor/reference recording, multi-context exports and endpoint aggregates.
- Offline dashboard, reviewed disabled test drafts, retained-summary comparison and opt-in CI artifacts.
- WebFlux catch-all pattern handling fixed in 0.4.1.

See [CHANGELOG](CHANGELOG.md) for release boundaries; completed work is not a future promise.

## Next

**Cost as a function of instrumented surface.**
The measurement in
[docs/15_performance_results.md](docs/15_performance_results.md) now gives a
per-method-entry number on one machine and one endpoint shape. What it does not
cover: allocation and GC pressure, concurrent load rather than sequential
requests, and a real application instead of a sample. "Measured, acceptable
overhead" is the claim we want to keep earning.

**A build plugin.**
Recording and analysing currently needs a shell script around the application.
A Gradle and Maven plugin would make `record` and `impact` build tasks, which
is how this belongs in a project that does not want to maintain glue.

**Attribution for Servlet async.**
[#2](https://github.com/reqover-labs/reqover/issues/2) · Known gap

Work executed on a Servlet async worker before the request is re-dispatched is
not attributed today. It is documented as a limitation, but a limitation with
an issue open against it is a better state than one without.

**A persistent `CoverageStore`.**
The SPI exists and nothing implements it beyond memory. Exporting a report to a
file covers the common case, but a store that survives a restart is what makes
a long-running staging recording practical.

## Later

**JaCoCo report interoperability.**
[docs/14_jacoco_interop_decision.md](docs/14_jacoco_interop_decision.md)
records why we did not link JaCoCo and what a future spike would have to
resolve, EPL-2.0 obligations included. Reqover complements JaCoCo; being
readable by the same tooling would make that concrete.

**Source-line and branch precision.**
Method entry is a deliberate simplification, not a permanent one. Finer
granularity costs more per probe, so this only follows a performance story we
trust.

**Attribution units beyond HTTP.**
`UnitScope` already opens a bucket for a scheduled job, a message listener, or
a single test case. What is missing is the adapter layer that makes that
automatic rather than manual.

## Not planned

Saying no is part of a roadmap. See
[docs/19_prior_art.md](docs/19_prior_art.md) for the reasoning.

- **Replacing JaCoCo.** Different question, different tool. Use both.
- **Production observability.** Reqover records every method entry in the
  packages you name and samples nothing. That is affordable in development, QA,
  and staging, and it is the wrong shape for permanent production use — an APM
  is the right tool there.
- **A hosted backend or shared dashboard service.** A local standalone HTML
  dashboard is in scope; hosted or multi-user services are not. Storage, access
  control and operating costs would require a separate project decision.
- **Claiming a change is safe.** Impact analysis reports observed execution,
  which is a lower bound. A file it cannot match means "not seen", never "not
  affected", and no amount of product polish will change that.

## How this list changes

We revise it when a release ships or when someone makes a better argument than
the one holding an item's place. It is not a commitment, and nothing here
obliges either maintainer to keep working on Reqover — see
[GOVERNANCE.md](GOVERNANCE.md) for what happens if we stop.
