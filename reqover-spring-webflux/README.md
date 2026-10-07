**English** | [한국어](README.ko.md) | [Project](../README.md)

# Spring WebFlux Adapter

Maven Central library: `io.github.reqover-labs:reqover-spring-webflux:0.4.2`.
Use the [starter](../reqover-spring-boot-starter/README.md) and agent for the usual
Spring Boot integration. The adapter itself does not serve a report endpoint.

A WebFilter places the request bucket in Reactor Context. Micrometer context
propagation restores it into the current-thread context for reactive segments,
so observed methods on supported scheduler hops belong to the same request.
Final status and timing use the request's termination, not a Mono factory return.

- Defaults: enabled, 10,000 snapshots, `oldest-first` retention.
- Settings: `reqover.webflux.enabled`, `.max-snapshots`, `.snapshot-eviction`
  and `.exclude-path-prefixes`.
- No raw-thread/fire-and-forget attribution guarantee, method spans or DB timing.
- Catch-all unmapped resources are skipped; 0.4.1 handles Spring's parsed route
  pattern without the former ClassCastException.

Enabling the adapter enables Reactor's JVM-wide automatic context propagation.
Set `reqover.webflux.enabled=false` before startup to disable this adapter;
consider interactions with other context/observability tools in the same JVM.
The default report and sample ports should not be exposed without access control.

[Configuration](../docs/17_integration_guide.md) |
[Architecture](../docs/02_architecture.md)
