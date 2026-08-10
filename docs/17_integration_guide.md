# 17. Integrating Reqover into a Spring Application

Reqover `0.1.0` is an initial developer/QA release. The Java agent is distributed
through GitHub Releases; the library modules can be built from the same tag and
published to a local Maven repository for integration testing.

## 1. Publish the library modules locally

From a checkout of the `v0.1.0` tag:

```bash
./gradlew clean publishToMavenLocal
```

Reqover publishes the core, report, instrumentation, MVC, and WebFlux library
modules under the `io.reqover` group. The standalone shaded agent JAR remains a
GitHub Release artifact.

## 2. Add one Spring adapter and the report module

Spring MVC:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.reqover:reqover-spring-mvc:0.1.0")
    implementation("io.reqover:reqover-report:0.1.0")
}
```

Spring WebFlux:

```kotlin
dependencies {
    implementation("io.reqover:reqover-spring-webflux:0.1.0")
    implementation("io.reqover:reqover-report:0.1.0")
}
```

The adapter auto-configuration creates the request coverage store and binds a
coverage bucket to each observed request. It does not expose a report endpoint.
That endpoint must remain an explicit application decision because authentication
and network policy differ by environment.

The WebFlux adapter enables Reactor automatic context propagation for the JVM so
the request bucket can follow scheduler hops. To disable the WebFlux
auto-configuration and prevent Reqover from installing this global hook, set the
following property before application startup:

```properties
reqover.webflux.enabled=false
```

## 3. Add an internal-only report endpoint

The samples contain complete controllers for JSON and HTML reports. The minimal
shape is:

```java
@RestController
final class InternalReqoverReportController {
    private final InMemoryCoverageStore store;
    private final CoverageReportGenerator reports = new CoverageReportGenerator();
    private final HtmlCoverageReportRenderer html = new HtmlCoverageReportRenderer();

    InternalReqoverReportController(InMemoryCoverageStore store) {
        this.store = store;
    }

    @GetMapping("/reqover/report")
    CoverageReport report() {
        return reports.generate(store.snapshots());
    }

    @GetMapping(value = "/reqover/report.html", produces = "text/html")
    String reportHtml() {
        return html.render(report());
    }
}
```

Keep report endpoints under `/reqover/**`: the MVC interceptor and WebFlux filter
reserve that prefix so report reads are not attributed as application traffic.
If you choose another prefix, add an equivalent exclusion before enabling the
endpoint. Protect these endpoints with the application's authentication and
network access controls. Do not expose the sample endpoint to the public Internet.

## 4. Start with the shaded Java agent

Download `reqover-agent-0.1.0.jar` from the matching GitHub Release, then pass a
narrow application package explicitly:

```bash
java \
  -javaagent:reqover-agent-0.1.0.jar=include=com.example.orders \
  -jar app.jar
```

Multiple prefixes use `;`, and a narrower exclusion can be added:

```text
include=com.example.orders;com.example.payments,exclude=com.example.orders.generated
```

An explicit `include=` is required. Without a valid include, the agent fails
closed and leaves instrumentation inactive. Third-party framework packages should
not be included.

## 5. Validate the integration

1. Start the application only on an approved development/QA network.
2. Call one known endpoint.
3. Fetch the internal JSON report.
4. Confirm the normalized endpoint, application class, and method metadata.
5. For WebFlux, confirm the expected scheduler thread names remain in one endpoint
   bucket.

Reqover reports observed method-entry relationships. Absence from the report is
not proof that a static relationship cannot exist, and the reverse index is not a
complete change-impact analysis.
