# Getting help

Reqover is maintained by two people as a side project. We read everything, and
we aim to reply within a week. Picking the right place below gets you an answer
faster.

## Where to go

| You want to | Go here |
| --- | --- |
| Ask how to do something, or whether Reqover fits your case | [Discussions → Q&A](https://github.com/reqover-labs/reqover/discussions/categories/q-a) |
| Report something broken | [Bug report](https://github.com/reqover-labs/reqover/issues/new/choose) |
| Say the demo didn't run | [Demo reproduction issue](https://github.com/reqover-labs/reqover/issues/new/choose) — this is the most useful report we can get |
| Propose a feature | [Open an issue](https://github.com/reqover-labs/reqover/issues/new/choose) before writing code, so nobody's work gets thrown away |
| Report a vulnerability | [Private advisory](https://github.com/reqover-labs/reqover/security/advisories/new) — **never** a public issue |
| Show what you built with it | [Discussions → Show and tell](https://github.com/reqover-labs/reqover/discussions/categories/show-and-tell) |

Not sure which? Open an issue. Being in the wrong place is not a problem — we
will move it.

## Read these first

Most questions are answered in one of these:

- [Try it in 5 minutes](README.md#try-it-in-5-minutes) — the demo, start to finish
- [Spring integration guide](docs/17_integration_guide.md) · [한국어](docs/17_integration_guide.ko.md) — wiring it into your own application, including a troubleshooting section
- [Impact analysis in CI](docs/18_ci_impact_analysis.md) · [한국어](docs/18_ci_impact_analysis.ko.md) — getting a report out of a test run and asking what a change affects
- [What works / what doesn't](README.md#what-works--what-doesnt) — the limits, before you find them the hard way
- [Prior art](docs/19_prior_art.md) · [한국어](docs/19_prior_art.ko.md) — whether Reqover or a different tool is what you actually want

## What makes a report we can act on

The single most common reason we cannot help is a report we cannot reproduce.
Please include:

- OS and JDK version (`java -version`)
- Spring Boot version, and whether the application is MVC or WebFlux
- the full command, including the entire `-javaagent:` option
- **every line starting with `[reqover]` on standard error** — the agent
  reports configuration problems there, and it is usually the fastest path to
  the cause
- what you expected, and what happened instead

If the documentation was what confused you, that is a bug in the documentation.
Report it the same way.

## Language

Issues and pull requests are written in English so contributors anywhere can
follow the history. **한국어로 질문하셔도 됩니다** — 영어 요약만 한 줄 붙여 주시면
다른 사람도 스레드를 따라올 수 있습니다.

## What we cannot help with

- Debugging your application's own logic. Reqover reports what ran; why it ran
  is your code's question.
- Production incidents. Reqover is a development, QA, and staging tool, and
  running it permanently in production is not a supported configuration.
- Guarantees that a change is safe to ship. Impact analysis reports observed
  execution, which is a lower bound and never a proof.
