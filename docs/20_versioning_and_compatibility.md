**English** | [한국어](20_versioning_and_compatibility.ko.md)

# 20. Versioning, compatibility, and rollback

Publishing to Maven Central is permanent. A released version can never be
changed or withdrawn, so the promises attached to a version number have to
exist *before* the first upload, not after. This document is that prerequisite.

It covers what a version number promises, what counts as public API, how the
supported Java and Spring versions move, and what happens when a release turns
out to be broken.

## Coordinates

All modules publish under the group `io.reqover` and share one version.

| Coordinate | Use it when |
| --- | --- |
| `io.reqover:reqover-spring-boot-starter` | You have a Spring Boot application. Start here; it pulls in what it needs. |
| `io.reqover:reqover-core` | You are implementing a `CoverageStore` or opening a `UnitScope` yourself. |
| `io.reqover:reqover-report` | You are reading, rendering, diffing, or analysing a report programmatically. |
| `io.reqover:reqover-spring-mvc` / `-spring-webflux` | You are wiring an adapter by hand instead of using the starter. |
| `io.reqover:reqover-instrumentation` | You are building on the ASM transformer directly. |
| `io.reqover:reqover-agent` | The `-javaagent` JAR. Usually downloaded, not depended on. |
| `io.reqover:reqover-cli` | Running `render`, `diff`, or `impact` from a build rather than the release JAR. |

Every module ships at the same version. Mixing versions across `io.reqover`
modules is not supported and is not tested.

## What a version number promises

Reqover follows [Semantic Versioning](https://semver.org/) with the pre-1.0
rule spelled out, because "0.x means anything can change" is true in the
specification and useless in practice.

### While the version starts with `0.`

- **Patch** (`0.2.0` → `0.2.1`) — no public API change. Safe to take without
  reading anything. Bug fixes, performance, documentation, dependency bumps.
- **Minor** (`0.2.x` → `0.3.0`) — **may break the public contract.** Every
  break is listed in [CHANGELOG.md](../CHANGELOG.md) under a `Breaking` heading,
  with the migration for each one. If we cannot write the migration line, the
  break does not ship.
- We will not remove something in a minor release without it having been
  deprecated in an earlier release, **unless** it is younger than two minor
  releases or keeping it would mean shipping something we know is wrong. Both
  exceptions get a `Breaking` entry.

### After `1.0.0`

- **Major** for a break, **minor** for additions, **patch** for fixes — the
  ordinary rules.
- Anything removed in a major release will have been deprecated for at least
  one minor release first, with the replacement available at the same time.

`1.0.0` is not a schedule. It is the point at which the public contract below
has survived contact with codebases that are not ours, and we are willing to
stop changing it. Until then the `0.` prefix is doing honest work.

## What counts as public API

Compatibility promises cover exactly this list:

- **`reqover-core`** — `CoverageStore`, `UnitScope`, `UnitInfo`,
  `CoverageBucket`, `CoverageBucketSnapshot`, `CoverageContext`, and
  `ReqoverProbe.hit`
- **Configuration properties** — every documented `reqover.*` property
- **The exported report JSON** — see the schema rule below
- **`reqover-cli`** — command names, flag names, output formats, and exit codes
  (`0` clean, `1` gate tripped, `2` bad usage or input)
- **The agent option string** — `-javaagent:reqover-agent.jar=include=...`
- **The composite GitHub Action** — its input names and their meanings
- **`reqover-report`** — the report model types and the renderers

Everything else is internal and may change in any release, whatever its Java
visibility. If you find yourself depending on something not on this list, open
an issue — that is a signal the list is wrong, and we would rather fix the list
than break you by surprise.

`ReqoverProbe.resetGlobalStateForTests` is public because the agent needs it
across class loaders. It is not public API. Calling it in a running application
permanently discards agent-registered metadata.

### Report JSON schema

The exported report carries a `schemaVersion` field, currently `1`. Reading
rules:

- **New optional fields may appear in any release**, including a patch. A
  reader must ignore fields it does not know.
- **A document with no `schemaVersion` is read as version 1**, which is the
  only shape that has ever existed.
- **A document from a newer schema is rejected with an error naming the
  version**, rather than parsed into something quietly wrong. A report is
  written by one job and read by another, and the two do not upgrade at the
  same moment.
- **A field is never removed or repurposed within a schema version.**
- **A breaking schema change bumps `schemaVersion`**, and the CLI keeps
  reading the previous version for at least one minor release, so a report
  recorded before an upgrade still analyses after it.

## Supported Java and Spring versions

| | Current |
| --- | --- |
| JDK to build | 17, 21 |
| Bytecode target | Java 17 |
| JDK to run | 17 or later |
| Spring Boot verified | 3.5.x |

- **Adding** a JDK or Spring Boot line to CI is a minor release.
- **Dropping** one is a minor release before `1.0.0` and a major release after,
  announced in the changelog either way.
- The bytecode target rises only in a minor (pre-1.0) or major (post-1.0)
  release. It will not move in a patch.

A Spring Boot version we have not verified is not necessarily broken; it is
untested. Reports that it works, or does not, are useful contributions.

## When a release is broken

Maven Central is immutable. There is no unpublish, no yank, and no editing a
released artifact — so "roll back" cannot mean removing anything. What it means
here:

1. **Fix forward, fast.** The broken version stays on Central forever; the
   answer is a patch release that supersedes it, cut with priority over
   anything else in progress.
2. **Mark the broken release.** The GitHub Release for the bad version gets a
   prominent notice naming the problem and the version to use instead, and
   [CHANGELOG.md](../CHANGELOG.md) records it under the fixed version.
3. **Advise, in the open.** A pinned issue describes the symptom, who is
   affected, and the workaround, and stays open until the fix is released.
4. **Add the missing test.** A release that broke in a way CI did not catch
   means the gate had a hole. The patch closes the hole in the same pull
   request as the fix, not "later".
5. **Withdraw only if we must.** If a release contains something that must not
   be distributed — a secret, or code we do not have the right to publish — we
   contact Central support to request removal and say so publicly. This is a
   last resort and it is not a rollback mechanism.

For a version that was published but never announced and has no known
downloads, the same rules still apply. We do not pretend a release did not
happen.

## Before the first Central release

Publishing is deliberately inert until these are true, and this checklist is
the acceptance criteria for
[#4](https://github.com/reqover-labs/reqover/issues/4):

- [x] This document exists and the promises in it are ones we can keep
- [x] The public API list above matches what the code actually exposes
- [x] The report JSON carries `schemaVersion`, and the reader refuses a
      document from a newer schema instead of misparsing it
- [ ] Signed publication pipeline dry-run against a staging bundle
      (`REQOVER_SIGNING_KEY=... ./gradlew centralBundle`) — the pipeline exists
      and stages unsigned bundles, but has never run with a key
- [ ] `PUBLISH_TO_MAVEN_CENTRAL` enabled with the `io.reqover` namespace
      verified on the Central Portal
- [ ] A consumer smoke test: a project outside this repository resolving the
      starter from Central and recording one request

The last one is not paperwork. A starter nobody has resolved from outside this
build is how a permanent mistake gets made.
