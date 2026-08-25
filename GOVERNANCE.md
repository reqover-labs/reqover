# Governance

Reqover is a small project run by two maintainers. This document says who
decides what, how someone else can gain a say, and what happens to the project
if the maintainers stop. It describes what we actually do — if the practice and
this document ever disagree, the document is the bug.

## Maintainers

| Maintainer | GitHub | Area |
| --- | --- | --- |
| TaeHui Kim | [@TaeHuiKKIM](https://github.com/TaeHuiKKIM) | Core, instrumentation, agent, report |
| Sangmin Lee | [@lsmin3388](https://github.com/lsmin3388) | Build, CI, release, Spring adapters, docs |

Both maintainers have write access and are listed in
[`.github/CODEOWNERS`](.github/CODEOWNERS).

## How decisions get made

**Ordinary changes** — a bug fix, a test, a documentation change, a
self-contained feature — need one maintainer approval and passing CI. That is
the whole process.

**Changes to the public contract** need agreement from both maintainers,
recorded in the issue or pull request thread rather than in private. The public
contract is:

- the `CoverageStore` and `UnitScope` SPIs
- `reqover.*` configuration properties
- the exported report JSON schema
- `reqover-cli` command names, flags, output formats, and exit codes
- the `-javaagent` option syntax
- the composite GitHub Action's inputs

**Disagreement** is settled by discussion in the open thread. If the two of us
cannot agree, the change does not land — the tie goes to the status quo. We
would rather leave a good idea unbuilt than ship a contract we disagree about,
because a contract is much harder to withdraw than to postpone.

**Direction and scope** — what the project is for and what it declines to
become — is decided by the maintainers, in public issues. [Prior art and where
Reqover sits](docs/19_prior_art.md) records the current answer, including the
things we deliberately do not do.

## Contributions

Anyone may open an issue or a pull request; see
[CONTRIBUTING.md](CONTRIBUTING.md). Contributions are reviewed by a maintainer
who is not the author. Maintainers' own changes follow the same pull request
and CI path — nobody pushes to `main` directly, and branch protection enforces
it.

We aim to respond to an issue or pull request **within one week**. This is a
side project, so that is an intent, not a service level. If a thread has gone
quiet longer than that, a ping in the thread is welcome and not rude.

A contribution can be declined. If that happens you will get a reason, in the
thread, before it is closed. "Out of scope" is a legitimate reason and we will
say which scope.

## Becoming a maintainer

There is no application form. The path is: a few landed contributions, followed
by an invitation from the existing maintainers, decided by their agreement.
What we are looking for is not volume but judgement — reviews that catch real
problems, issues that are well diagnosed, and changes that leave the project's
constraints intact.

A maintainer who wants to step down should say so in an issue. There is no
expectation of permanence and no hard feelings.

## Releases

Releases are cut by a maintainer from `main` with a version tag. The release
workflow verifies that the tag matches the project version and that it targets
`main`; the build must be green on JDK 17 and 21 with the dependency scan
passing. Version numbers and compatibility promises follow
[docs/20_versioning_and_compatibility.md](docs/20_versioning_and_compatibility.md).

Either maintainer may cut a patch release. A minor or major release needs both
to agree, because those are the ones that change the contract.

## Security

Vulnerability reports go through [GitHub private vulnerability
reporting](https://github.com/reqover-labs/reqover/security/advisories/new), not
public issues. [SECURITY.md](SECURITY.md) has the process and the response
expectations.

## Code of conduct

[CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) applies to every project space.
Enforcement is handled by the maintainers. If a report concerns a maintainer,
send it to the other one.

## If the project stops

Reqover began as an entry for the 2026 Korea Open Source Developer Competition,
so it is fair to ask what happens when a contest project's original reason
expires. Our answers:

- The repository stays public. It is Apache-2.0, so a fork is always available
  to anyone, with or without us.
- If we stop maintaining it, we will say so in the README rather than let the
  repository go quiet and leave people guessing.
- If someone is using Reqover and wants to keep it alive, we would rather hand
  over the repository than archive it. Open an issue and ask.

Nothing here obliges anyone to keep working on Reqover. It exists so that
nobody has to guess what the silence means.
