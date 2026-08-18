# Contributing to Reqover

Reqover is an experimental Java developer tool for request-scoped coverage attribution.

New here? The [README's contributing section](README.md#contributing) lists good
first tasks. The single most useful contribution right now is a report saying
**"I ran the demo and it didn't work"** — see [Reporting Issues](#reporting-issues).

## Language

- **Issues, pull requests, commit messages, and code comments: English.** This
  keeps the history readable for contributors anywhere.
- **User-facing documentation is bilingual.** Korean is maintained because
  Reqover started as a Korean competition entry; English is the primary version.

Questions in Korean are welcome in issues — just include an English summary so
others can follow the thread.

## Development Setup

Requirements:

- JDK 17 or 21
- Git

Run tests:

```bash
./gradlew clean test --no-daemon
```

Build all artifacts:

```bash
./gradlew build --no-daemon
```

Generate SBOM:

```bash
./gradlew cyclonedxBom
```

On Windows, use `.\gradlew.bat` instead of `./gradlew`.

## Project Structure

- `reqover-core`: coverage bucket model and probe runtime
- `reqover-instrumentation`: ASM bytecode instrumentation
- `reqover-agent`: Java agent packaging and transformer
- `reqover-spring-mvc`: Spring MVC request adapter
- `reqover-spring-webflux`: WebFlux request adapter
- `reqover-report`: report aggregation and HTML rendering
- `examples`: demo applications
- `docs`: planning, architecture, troubleshooting, and competition notes
- `scripts`: demo runners and the SBOM vulnerability check

## Documentation Layout

User-facing docs exist as English/Korean pairs. The `.md` file is English; the
`.ko.md` file is Korean.

| English | Korean |
| --- | --- |
| `README.md` | `README.ko.md` |
| `docs/02_architecture.md` | `docs/02_architecture.ko.md` |
| `docs/17_integration_guide.md` | `docs/17_integration_guide.ko.md` |

Updating both languages is appreciated but **not required.** Change whichever you
can, say so in the pull request, and a maintainer will follow up on the other
one. An out-of-date translation is worse than a missing one, so please don't
guess at a language you aren't comfortable in.

Documents under `docs/` without a `.ko.md` sibling are single-language; those
still Korean-only are marked *(Korean)* in the README's documentation list, and
translations are welcome.

## Coding Rules

- Keep changes scoped to the relevant module.
- Update docs when behavior or commands change.
- Update the README when user-facing setup or demo flow changes — see
  [Documentation Layout](#documentation-layout) for which files that means.
- Add tests for coverage attribution, instrumentation, and report changes.
- Do not commit generated build outputs.
- Do not commit `.env`, API keys, tokens, local credentials, or private certificates.

## Pull Requests

Changes reach `main` through a pull request. CI runs the full test suite on
JDK 17 and 21 plus an OSV dependency scan, and those checks must pass.

Fork the repository, branch, and open the PR against `main`. If you are planning
a large change, open an issue first — work thrown away because the direction
didn't match is the worst outcome for everyone.

### Checklist

- `./gradlew test` passes.
- `./gradlew build` passes.
- New behavior is documented.
- License or dependency changes are reflected in `THIRD_PARTY_NOTICES.md`.
- No secrets are included.

For a documentation-only change, say so and skip the build boxes rather than
ticking something you did not run — CI verifies it on the PR anyway.

## Reporting Issues

When reporting a bug, include:

- OS and JDK version (`java -version`)
- Spring Boot version, and whether the application is MVC or WebFlux
- the full command used, including the entire `-javaagent:` option
- **any line starting with `[reqover]` from standard error** — the agent reports
  configuration problems there, and it is usually the fastest way to find the cause
- expected behavior
- actual behavior
- minimal reproduction steps
- relevant logs

If the demo itself failed, the [demo reproduction issue
template](.github/ISSUE_TEMPLATE/demo_reproduction.md) collects these for you.

Getting stuck because the documentation was unclear counts as a bug. Please
report that too.
