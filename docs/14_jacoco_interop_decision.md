# 14. JaCoCo Interop Decision

## Decision

For the competition MVP, Reqover will not fork or modify JaCoCo internals.

Reqover will submit with its own lightweight method-entry instrumentation and request-scoped attribution model.

JaCoCo interoperability remains future work.

## Reasoning

### Why Not Fork JaCoCo Now

JaCoCo provides mature line and branch coverage, but extending its probe strategy safely requires deeper internal integration and EPL-2.0 license handling. This is too risky for the MVP timeline.

The competition demo needs to prove request attribution more than branch precision. Reqover already proves:

- automatic method-entry instrumentation
- request bucket routing
- MVC endpoint attribution
- WebFlux thread-hop attribution
- endpoint-to-code report
- code-to-endpoint reverse index

### Why Lightweight Instrumentation Is Enough for MVP

The core technical claim is:

```text
When code executes during an HTTP request, Reqover can attribute that execution to the request endpoint.
```

Method-entry probes are sufficient to prove this claim.

## Future Interop Options

### Option A. Import JaCoCo XML Reports

Reqover can read JaCoCo XML reports after tests or staging traffic and enrich them with endpoint attribution data captured separately.

Pros:

- lower license and maintenance risk
- no JaCoCo fork
- compatible with existing CI pipelines

Cons:

- harder to map runtime request buckets to exact branch data
- may require stable class/method identity mapping

### Option B. JaCoCo Agent Extension

Reqover can investigate whether JaCoCo agent hooks can be extended to route probe hits into request buckets.

Pros:

- preserves mature line/branch instrumentation
- better compatibility with existing coverage expectations

Cons:

- higher implementation complexity
- higher licensing review cost
- stronger dependency on JaCoCo internals

### Option C. Keep Independent Instrumentation

Reqover can keep its own instrumentation and improve method/line mapping gradually.

Pros:

- simple mental model
- request attribution is directly controlled
- easier MVP packaging

Cons:

- line/branch precision must be built or approximated
- more bytecode edge cases must be handled over time

## Current MVP Position

Reqover should describe itself as:

```text
Request-scoped method coverage attribution for Spring applications.
```

It should not describe itself as:

```text
A full replacement for JaCoCo line and branch coverage.
```

