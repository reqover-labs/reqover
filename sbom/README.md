# Software Bill of Materials

`reqover.cdx.json` is the checked-in CycloneDX 1.6 inventory for the current
release candidate. It includes build, test, sample-runtime, and project modules;
it is therefore broader than the dependencies redistributed inside the shaded
Java agent JAR.

Regenerate it from the repository root:

```bash
./gradlew clean build cyclonedxBom --no-daemon --console=plain
cp build/reports/bom/reqover.cdx.json sbom/reqover.cdx.json
./scripts/verify-sbom-lock.py build/reports/bom/reqover.cdx.json sbom/reqover.cdx.json
./scripts/check-sbom-osv.py sbom/reqover.cdx.json
```

Reqover-owned components use Apache-2.0. Third-party license metadata comes from
the resolved Maven components and is summarized for human review in
[`THIRD_PARTY_NOTICES.md`](../THIRD_PARTY_NOTICES.md). The ASM license text is
retained separately because ASM classes are redistributed in the shaded agent.

The lock verifier compares every component, version, license, property, and
dependency edge. It intentionally normalizes only volatile SBOM provenance: the
serial number, generation timestamp, GitHub Actions run URL, and the equivalent
presence or absence of a trailing `.git` in the root VCS URL.
