**English** | [한국어](README.ko.md) | [Project](../README.md)

# Spring MVC Adapter

Maven Central library: `io.github.reqover-labs:reqover-spring-mvc:0.4.1`.
For normal Spring Boot use, install the [starter](../reqover-spring-boot-starter/README.md)
and attach the agent rather than wiring this adapter yourself.

The interceptor creates a bucket after handler mapping, binds it to the request
thread, and finishes it with the response status. Routes use patterns such as
`GET /orders/{id}`. Request scopes keep concurrent calls separate.

- Defaults: enabled, 10,000 snapshots, `oldest-first` retention.
- Settings: `reqover.mvc.enabled`, `.max-snapshots`, `.snapshot-eviction`,
  `.include-path-patterns` and `.exclude-path-patterns`.
- Unmapped catch-all static-resource requests and default excluded report/error
  paths are not recorded. No body, query or authorization capture is performed.
- Servlet async redispatch reuses the bucket; work on an unmanaged async worker
  before redispatch is not automatically attributed.

The recorded interval starts after handler mapping, not at network arrival. It
is wall-clock adapter time, not client latency, method timing, CPU or database time.
The adapter does not publish report endpoints on its own.

[Configuration and limitations](../docs/17_integration_guide.md) |
[Request diagnostics](../docs/24_request_diagnostics.md)
