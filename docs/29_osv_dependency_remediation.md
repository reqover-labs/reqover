# OSV Dependency Remediation

Date: October 4, 2026. Applies to the development branch/PR #25, not existing
release binaries. Security checks remain enabled and fail on known findings.

## Cause and Patch

The failed [CI job](https://github.com/reqover-labs/reqover/actions/runs/37136298134/job/111241880086)
scanned the generated CycloneDX inventory successfully, then failed its
vulnerability policy: three components, ten advisories. It was not a token,
checkout, or scanner-startup error. The local OSV query reproduced the findings.

| Component | Before | After | Remediation |
| --- | --- | --- | --- |
| Jackson core | 2.21.5 | 2.21.7 | GHSA-7hhh-6rmp-j9qf, GHSA-p6pp-m3f8-5c89 |
| Jackson databind | 2.21.5 | 2.21.7 | GHSA-cxp5-3px4-pw24, GHSA-gx83-3vf8-gh7j, GHSA-q4xh-88c3-wmh7, GHSA-wjgm-6hv5-3cvf, GHSA-wv8q-qhhj-9h54 |
| Tomcat embedded core | 10.1.55 | 10.1.60 | GHSA-9xv2-5v5q-p794, GHSA-gcx9-497g-6cp6, GHSA-h3x4-894j-xpx5 |

The Jackson BOM stays in its existing 2.21 release line. It aligns the Jackson
modules used by samples, starter tests and agent E2E parsing; annotations are
correctly versioned `2.21`, not `2.21.7`, under that BOM.

Tomcat core, EL and WebSocket all resolve to 10.1.60. Constraints apply only to
the MVC sample and starter's test runtime. They do not add a Tomcat runtime or
server-version constraint to published starter consumers. Spring Boot remains
3.5.16; the agent, attribution logic, probes and report formats do not change.

The advisory metadata mentions 10.1.58, but its release vote failed, so it is not
a downloadable release. 10.1.59 includes those fixes; 10.1.60 also includes the
subsequent published security fixes. Maven Central availability was verified.
The scanner's “0 vulnerabilities can be fixed” summary was not treated as proof
that no patched dependency existed; upstream ranges and released artifacts were
checked directly.

## Verification and Scope

- Before: OSV query of all 104 external Maven coordinates reported three affected
  components and ten advisories.
- After: the complete regenerated inventory resolves Jackson 2.21.7 and Tomcat
  10.1.60 and the same full OSV query reports zero affected components.
- `sbom/reqover.cdx.json` is regenerated from Gradle, including actual versions,
  hashes and dependency relationships. It is not manually relabelled.
- Existing `fail-on-vuln: true` remains; no ignored advisories, severity exclusions,
  removed components or `continue-on-error` are added to the security gate.
- A fresh isolated `clean build` passes 157 Java tests without failures/errors/skips;
  all 20 Node tests, including generated Java compilation, pass with the patch.
  GitHub's actual security job is checked separately before calling the PR green.

Windows `clean` can fail if an old sample JVM holds the agent JAR open. This is
a file-lock issue, not dependency incompatibility. Validate in a separate clean
checkout with only the dependency edits applied instead of disrupting a user's
running demo. Linux CI runs its normal clean build.

Reproduce:

```bash
./gradlew clean build cyclonedxBom
python3 scripts/verify-sbom-lock.py build/reports/bom/reqover.cdx.json sbom/reqover.cdx.json
python3 scripts/check-sbom-osv.py sbom/reqover.cdx.json
```

Zero known findings at the check time is not a blanket security guarantee.
Consumers must still maintain their application dependencies and access policy.

Sources: [Jackson maintainer advisory](https://github.com/FasterXML/jackson-core/security/advisories/GHSA-7hhh-6rmp-j9qf),
[Jackson databind advisory](https://github.com/advisories/GHSA-cxp5-3px4-pw24),
[Apache Tomcat security notices](https://tomcat.apache.org/security-10.html).
