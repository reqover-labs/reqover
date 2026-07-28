# Security Policy

## Supported Versions

Reqover is currently in MVP development. Security fixes are handled on the `main` branch until the first stable release tag is created.

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

## Current Security Scope

Reqover instruments application bytecode through `-javaagent`. Only use agent include prefixes for code you own or explicitly intend to inspect.

The MVP stores coverage data in memory and exposes reports through sample endpoints. Do not expose sample report endpoints on a public network without authentication.

