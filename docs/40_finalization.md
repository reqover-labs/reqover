[한국어](40_finalization.ko.md) | [Documentation](README.md)

# PR 53 Finalization

October 12, 2026. This follow-up closes automated regression and dependency-policy
work on the existing unreleased branch. It does not merge, tag or release it.
Expansion issues #54-#63 are separate work; collector, coverage JSON, Action and
Spring Boot 3.5 compatibility remain unchanged.

## Security Decision

The exact locked inventory (104 external Maven coordinates) reproduced the CI
failure: two additional advisories on Spring WebFlux/MVC 6.2.19, across three
component/advisory pairs. The existing XsltView exception is separate.

- [Spring CVE-2026-47892](https://spring.io/security/cve-2026-47892/):
  functional WebFlux endpoints deployed through DispatcherServlet.
- [Spring CVE-2026-47890](https://spring.io/security/cve-2026-47890/):
  attacker-controlled SSE view fragments sent to other users.

The official notices list OSS 7.0.9 and enterprise-only 6.2.20 fixes. Mixing Spring
7 JARs into Boot 3.5 is not a compatibility-preserving patch. Current samples use
reactive Netty or ordinary MVC responses, not SSE view-fragment rendering. The
report route is restricted to reactive applications; published adapters declare
Spring compileOnly, leaving the consumer's runtime version to the application.

Under the existing policy, the two named advisories receive temporary exceptions
until November 12. The SBOM and severity/fail-on-vulnerability behavior are
unchanged. A green gate is **zero unexcepted findings**, not zero advisories or a
patched library. Consumers must assess their own deployment and rendering paths.

`scripts/test-osv-policy.py` guards reviewed IDs/reasons/dates, the SBOM version,
source/build assumptions, expired exceptions, unknown findings and partial scan
responses. Static scope checks require re-review when assumptions change; they
are not proof that every consumer application is unexploitable.

## Verification Status

- Java 21 full build/SBOM regeneration: 90 executed tasks, 202 Java tests;
  zero failures/errors/skips. Existing Gradle deprecations are not hidden.
- Node model/controller/generated-Java checks: 46 passed, not browser E2E.
- Dependency policy: 7 passed after the missing-exception test failed first.
  Built published JARs contain no Spring classes/nested JARs; the reactive sample
  contains Netty and no servlet stack. Run these checks after building.
- Regenerated SBOM matches the lock. OSV query: 104 coordinates, zero unexcepted
  findings, three excepted IDs across four component/advisory pairs.
- Owned-loopback CLI fixture: zero unconsented requests; eight actual requests,
  failing-before exit 1 and fixed-after exit 0, two runs/one recording, report and
  comparison roundtrip.
- Official Postman Collection 2.1 schema passes. JMeter 5.6.3 headless fixture:
  disabled plan sends zero requests; reviewed plan sends two with one deliberate
  assertion failure.
- Documentation links/anchors: 324 passed. Published CLI checksum and nine smoke
  cases passed. Secrets, local archives, logs and engines are not committed.

Reproduce with Java 21:

```bash
./gradlew build cyclonedxBom --rerun-tasks --no-daemon --console=plain
python3 scripts/test-osv-policy.py
python3 scripts/verify-sbom-lock.py build/reports/bom/reqover.cdx.json sbom/reqover.cdx.json
python3 scripts/check-sbom-osv.py sbom/reqover.cdx.json
node --test scripts/*.test.cjs
node scripts/verify-local-scenarios.cjs reqover-cli/build/libs/reqover-cli-0.4.2.jar
```

The source-built JAR's 0.4.2 filename does not mean the published 0.4.2 binary
contains new commands. The final GitHub verdict is commit-specific in
[PR 53 checks](https://github.com/reqover-labs/reqover/pull/53/checks); CI reruns
Java 17/21 and the actual OSV-Scanner separately.

Rendered browser E2E, mobile/light/dark layout QA and fresh screenshots remain
pending because the available tool blocked local-file interaction. No alternate
URL, browser or indirect execution bypass is used. Model/controller doubles and
source inspection are not replacements for real browser verification.
