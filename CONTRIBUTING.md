# Contributing to Reqover

Reqover is an experimental Java developer tool for request-scoped coverage attribution.

## Development Setup

Requirements:

- JDK 21
- Git

Run tests:

```bash
./gradlew test
```

Build all artifacts:

```bash
./gradlew build
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

## Coding Rules

- Keep changes scoped to the relevant module.
- Update docs when behavior or commands change.
- Update README when user-facing setup or demo flow changes.
- Add tests for coverage attribution, instrumentation, and report changes.
- Do not commit generated build outputs.
- Do not commit `.env`, API keys, tokens, local credentials, or private certificates.

## Pull Request Checklist

- `./gradlew test` passes.
- `./gradlew build` passes.
- New behavior is documented.
- License or dependency changes are reflected in `THIRD_PARTY_NOTICES.md`.
- No secrets are included.

## Reporting Issues

When reporting a bug, include:

- OS and JDK version
- command used
- expected behavior
- actual behavior
- minimal reproduction steps
- relevant logs
