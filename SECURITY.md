# Security Policy

## Supported Versions

Reqover is an initial developer/QA release. Security fixes are handled on the latest `0.1.x` release and the `main` branch; older snapshots are unsupported.

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
| GHSA-pc63-qcmh-9cmg (CVE-2026-47884) | `spring-webmvc` 6.2.19 | No open-source 6.2.x fix; only Spring Framework 7.0.9 (Spring Boot 4) is fixed. Reqover's published modules declare Spring `compileOnly` and ship none; the affected copy is the MVC sample and test runtime, which do not use `XsltView`. **If your application is on Spring Boot 3.5, it is affected too** — check whether you use `XsltView`. | 2026-12-31 |

## Current Security Scope

Reqover instruments application bytecode through `-javaagent`. Only use agent include prefixes for code you own or explicitly intend to inspect. JDK, ASM, and Reqover runtime packages are always excluded from instrumentation.

The MVP stores coverage data in memory and exposes reports through sample endpoints. Do not expose sample report endpoints on a public network without authentication.

The WebFlux adapter enables JVM-wide Reactor automatic context propagation. Set `reqover.webflux.enabled=false` before application startup if that global hook is not acceptable for the application.
