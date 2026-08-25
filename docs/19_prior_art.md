**English** | [한국어](19_prior_art.ko.md)

# 19. Prior Art and Where Reqover Sits

"Nobody else does this" is a claim that gets weaker the more you look, so this
document does the looking. It lists the tools that already occupy the space
around Reqover, states what each one answers, and says plainly where Reqover
is not the right choice.

The short version: several mature tools connect **code to tests**. Reqover
connects **code to observed HTTP requests**. That is one word of difference and
it changes what you can do with the result.

## The question each tool answers

| Tool | Attribution unit | Granularity | Answers |
| --- | --- | --- | --- |
| [JaCoCo](https://www.jacoco.org/jacoco/) | none (aggregate) | line, branch | Was this line executed at all, by anything? |
| [OpenClover](https://openclover.org/) | test case | line | Which **tests** executed this line? |
| [Datadog Test Impact Analysis](https://docs.datadoghq.com/tests/test_impact_analysis/) | test case | file | Which **tests** can be skipped for this commit? |
| Commercial quality-intelligence suites | test case | line/method | Which tests cover this change; where are the test gaps? |
| APM / distributed tracing | request (trace) | instrumented boundaries | Where did this request spend its time, and what failed? |
| Traffic replay tools | recorded request | request/response | Does the new build still answer the old traffic the same way? |
| **Reqover** | **observed HTTP request** | **method entry** | **Which endpoints executed this code?** |

## The closest neighbours, in detail

### OpenClover — the nearest relative

OpenClover records coverage per test case and can answer "given a line, which
tests ran on it", and its manual-test recorder can even open a named recording
around interactive work. It is open source, mature, and line-level, which is
finer than Reqover's method entry.

The unit is still a **test**. A recording has to be started and named by
someone. Nothing binds a recording to `GET /orders/{id}` on its own, and
nothing carries a recording across a Reactor thread hop, because attributing
concurrent in-flight requests separately was never the problem it set out to
solve. Its instrumentation is source-level, so it wants your build; Reqover
attaches to a built JAR.

If your question is "which of my JUnit tests touch this class", use OpenClover.
It is better at that than Reqover will ever be.

### Datadog Test Impact Analysis and commercial quality-intelligence suites

These are the tools that made "test impact analysis" a category. They collect
per-test coverage, store it in a backend, and use it operationally — skipping
tests that a commit cannot affect. That is a stronger product than Reqover, and
it is closed source and priced accordingly.

The overlap with Reqover is the *reverse index* idea. The difference is again
the unit and where the data comes from: their index is built from **your test
suite**, so its value is bounded by test coverage. Reqover's index is built
from **traffic somebody actually sent** — a QA session, a staging soak, an
integration test run — so it can say something about code paths that no
automated test covers yet. That is also its weakness, stated below.

### APM and distributed tracing (Pinpoint, Jaeger, Elastic APM, Datadog APM)

This is the comparison to make honestly, because tracing is also per-request
and also uses a Java agent, and the resemblance is real.

Tracing instruments **boundaries** — an incoming request, a database call, an
outbound HTTP call — and produces spans meant to be read as a timeline. It is
usually sampled, because keeping every trace from production is expensive. The
product question is "why is this slow, and what failed".

Reqover instruments **every method entry in the packages you name**, keeps no
timeline, and samples nothing within a recording. The output is a set: the
methods this request walked through. Sets can be inverted, which is the whole
point — a timeline cannot be inverted into "which endpoints touch this class".
Reqover is also built for development, QA, and staging rather than permanent
production use, which is what makes recording everything affordable.

If you want to know why an endpoint is slow, use tracing. Reqover will not tell
you.

### Traffic replay (Arex, Diffy, GoReplay)

Replay tools capture production traffic and re-run it against a new build to
diff the responses. They share Reqover's instinct that real traffic is better
evidence than a test suite, but they answer a different question: *did the
behaviour change*, not *what should I look at*. The two compose rather than
compete — a replay run is exactly the kind of traffic worth recording with
Reqover.

## What is actually new here

Stripping out everything that already exists, three things are left:

1. **The attribution unit is a live HTTP request, not a test case.** This is
   the load-bearing difference. It makes the reverse index reflect what the
   system does, not what the suite checks, and it produces a list of
   *endpoints* — which is what a reviewer, a QA engineer, or an API test plan
   actually consumes.
2. **Attribution survives reactive thread hops.** Everything above that does
   per-unit coverage assumes the unit owns a thread. WebFlux breaks that
   assumption. Reqover carries the bucket through the Reactor `Context` with
   Micrometer context propagation, so a request that lands on `boundedElastic`
   still records into its own bucket. We have not found an open-source tool
   that does per-request coverage attribution under WebFlux.
3. **The index is answerable from a file, in CI, with no JVM.** An exported
   report resolves class and method names inside the document, so
   `reqover impact` reads it in a pipeline and turns a diff into a list of
   endpoints to retest — as a pull request comment or an exit code.

The novelty is the combination. Every individual ingredient — bytecode
instrumentation, per-unit coverage buckets, context propagation, reverse
indexing — is established work, and the project would be worse if it pretended
otherwise.

## Where you should not use Reqover

- **You want line or branch precision.** Reqover records method entry. Use
  JaCoCo, and use it alongside Reqover rather than instead of it.
- **Your question is about tests, not endpoints.** OpenClover and the
  commercial test-impact products are built for that and are better at it.
- **You want production observability.** Use an APM. Reqover is a development,
  QA, and staging tool.
- **You need a guarantee that a change is safe.** Reqover reports a *lower
  bound* from observed execution. A file it cannot match is reported as "no
  observed execution", which means "not seen", never "not affected". A static
  call graph makes claims about code that never ran; Reqover deliberately does
  not.

## Open questions we would like help with

- Is there an open-source tool doing per-request coverage attribution under a
  reactive stack that we missed? Tell us in an issue — we would rather cite it
  than claim a gap that isn't there.
- Does the reverse index stay useful on a codebase far larger than our samples?
  We do not know yet, and only contact with real repositories will answer it.

## Sources

- JaCoCo documentation — <https://www.jacoco.org/jacoco/>
- OpenClover, per-test coverage for manual tests —
  <https://openclover.org/doc/manual/latest/hacking--measuring-per-test-coverage-for-manual-tests.html>
- Datadog, how Test Impact Analysis works —
  <https://docs.datadoghq.com/tests/test_impact_analysis/how_it_works/>
- Micrometer Context Propagation —
  <https://docs.micrometer.io/context-propagation/reference/>
