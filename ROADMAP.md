# Roadmap

What we intend to do next, in the order we intend to do it, and why. There are
no dates — this is a side project maintained by two people, and a date we
cannot keep is worse than no date.

Everything here is open to argument. If something below matters to you, or the
order looks wrong for how you would use Reqover, say so in the issue or in
[Discussions](https://github.com/reqover-labs/reqover/discussions) — that is
the fastest way to change what we work on.

## Now

**October mentoring track: make problem requests actionable.**
The [request diagnostics preview](docs/24_request_diagnostics.md) surfaces retained
request timing and status next to the request's method set. The
offline dashboard adds animated observed relationships, shared-code retest
candidates, and a review queue. The updated CI Action saves its results as
explicit artifacts; see [dashboard and CI setup](docs/26_dashboard_and_ci.md). The
[dated mentoring plan](docs/23_performance_validation_plan.ko.md) (Korean) targets
October 7 for the first demonstration and November 4–5 for the presentation.
Replay-case drafts, selective input capture and a k6 runner are planned stages,
not released capabilities. Validate the single-request discovery/retest workflow
before expanding to an APM-sized service.

**Make Reqover installable.**
[#4](https://github.com/reqover-labs/reqover/issues/4) · Maven Central
publication

Today nobody can depend on Reqover by coordinate. The pipeline is built and
signs on demand; what remains is a namespace, a key, and a smoke test from
outside this build. Everything else on this list matters less than this,
because a tool nobody can install has no users, and a tool with no users cannot
learn what is wrong with it.

The compatibility promises this requires are written down in
[docs/20_versioning_and_compatibility.md](docs/20_versioning_and_compatibility.md);
Central is permanent, so those had to exist first.

**Hold the storage SPI to a contract.**
[#14](https://github.com/reqover-labs/reqover/issues/14) · `CoverageStore`
contract test and eviction policy

`CoverageStore` shipped as an extension point without a way for an
implementation to check it behaves like the built-in one. A reusable contract
test fixes that, and a configurable bound policy makes the in-memory store
usable for a long QA session rather than only a rolling window.

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
- **A hosted backend.** The current diagnostic dashboard is still a local,
  standalone file. Replay and load-test screens will first be validated locally;
  running a shared hosted service requires a separate storage, access and
  operating-cost decision.
- **Claiming a change is safe.** Impact analysis reports observed execution,
  which is a lower bound. A file it cannot match means "not seen", never "not
  affected", and no amount of product polish will change that.

## How this list changes

We revise it when a release ships or when someone makes a better argument than
the one holding an item's place. It is not a commitment, and nothing here
obliges either maintainer to keep working on Reqover — see
[GOVERNANCE.md](GOVERNANCE.md) for what happens if we stop.
