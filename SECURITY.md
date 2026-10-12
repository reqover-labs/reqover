# Security Policy

## Supported Versions

Reqover is an early developer/QA release. Security fixes are handled on the latest published release (currently `0.4.2`) and the `main` branch; older snapshots are unsupported. Unreleased PR features are not part of the published binaries.

## Reporting a Vulnerability

Do not open a public issue for vulnerabilities that expose secrets, allow arbitrary code execution, or disclose private data.

Report vulnerabilities privately through GitHub private vulnerability reporting: on the repository page, open the Security tab and choose "Report a vulnerability". Include:

- affected version or commit
- reproduction steps
- impact
- suggested mitigation, if known

## Secret Handling

Never commit:

- `.env`
- API keys
- access tokens
- passwords
- private certificates
- local credential files

If a secret is committed by mistake:

1. Rotate the secret immediately.
2. Remove it from the repository.
3. Document the incident in private project notes.

## Dependency advisories

CI fails on any known vulnerability in the dependency inventory (`sbom/reqover.cdx.json`, checked with OSV-Scanner), and so does `scripts/check-sbom-osv.py` locally. The fix is to upgrade. An advisory is excepted in `osv-scanner.toml` only when no fixed release can be used today. Every exception states why and expires within three months, after which the gate fails again. Current exceptions:

| Advisory | Component | Why it is excepted | Until |
| --- | --- | --- | --- |
| GHSA-9qf2-26p9-2q2q (CVE-2026-47892) | `spring-webflux` 6.2.19 | [Spring's notice](https://spring.io/security/cve-2026-47892/) lists 6.2.20 as enterprise-only and 7.0.9 as the OSS fix. The sample runs on reactive Netty, not DispatcherServlet; report routes are reactive-only and do not use header predicates. Published adapters use `compileOnly`. **Consumer applications must assess their own deployment.** | 2026-11-12 |
| GHSA-j9f9-w8pj-32f8 (CVE-2026-47890) | `spring-webmvc` and `spring-webflux` 6.2.19 | [Spring's notice](https://spring.io/security/cve-2026-47890/) has the same fixed-version constraint. Samples/report routes do not send view fragments over SSE. Published adapters use `compileOnly`. **Consumers using SSE view fragments must assess and patch their application.** | 2026-11-12 |
| GHSA-pc63-qcmh-9cmg (CVE-2026-47884) | `spring-webmvc` 6.2.19 | No open-source 6.2.x fix; only Spring Framework 7.0.9 (Spring Boot 4) is fixed. Reqover's published modules declare Spring `compileOnly` and ship none; the affected copy is the MVC sample and test runtime, which do not use `XsltView`. **If your application is on Spring Boot 3.5, it is affected too** — check whether you use `XsltView`. | 2026-12-31 |

The October 12 review adds only the two named advisories above. The inventory,
severity threshold and `fail-on-vuln: true` remain unchanged. A passing gate
under these exceptions means **zero unexcepted findings**, not zero advisories
or patched Spring libraries. `scripts/test-osv-policy.py` checks expiry/reasons,
the reviewed SBOM versions, current source preconditions and unknown/partial
scan failures. These source guards flag scope changes for review; they are not
a complete exploitability analysis. Expired exceptions are not honoured.

## Current Security Scope

Reqover instruments application bytecode through `-javaagent`. Only use agent include prefixes for code you own or explicitly intend to inspect. JDK, ASM, and Reqover runtime packages are always excluded from instrumentation.

The MVP stores coverage data in memory and exposes reports through sample endpoints. Do not expose sample report endpoints on a public network without authentication.

The WebFlux adapter enables JVM-wide Reactor automatic context propagation. Set `reqover.webflux.enabled=false` before application startup if that global hook is not acceptable for the application.
