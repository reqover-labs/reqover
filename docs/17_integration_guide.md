**English** | [한국어](17_integration_guide.ko.md)

# 17. Integrating Reqover into your Spring application

To see per-request execution records in **your own application** rather than the demo, follow this guide. Expect 20–30 minutes the first time.

If you haven't run the demo yet, we recommend starting with ["Try it in 5 minutes"](../README.md#try-it-in-5-minutes) in the README. Seeing the demo work first makes problems much easier to isolate.

> [!IMPORTANT]
> Reqover `0.1.1` is **not on Maven Central yet.** That is why step 1 installs the libraries into your local Maven repository. It is designed for development, QA, and staging — not for running permanently in production.

---

## The overall flow

Wiring it in takes four steps. **None of them modify your source code** — step 3 only adds one controller so you can view the report.

| Step | What you do | Why it's needed |
| --- | --- | --- |
| 1 | Install the libraries into local Maven | Not on Maven Central |
| 2 | Add two dependencies | The part that links requests to execution records |
| 3 | Create a report endpoint | Reqover does not create one (reason below) |
| 4 | Run with the Java agent attached | The part that actually records execution |

### Decide these first

- **JDK 17 or 21**
- **Spring Boot 3.x** — the samples are verified against 3.5.16
- **MVC or WebFlux** — add **only one** of the two adapters

---

## 1. Install the libraries into your local Maven repository

Check out the `v0.1.1` tag and install:

```bash
git clone --branch v0.1.1 --depth 1 https://github.com/reqover-labs/reqover.git
cd reqover
./gradlew clean publishToMavenLocal
```

On Windows use `.\gradlew.bat clean publishToMavenLocal`.

This installs the library modules under the `io.reqover` group:

| Artifact | Version |
| --- | --- |
| `io.reqover:reqover-core` | `0.1.1` |
| `io.reqover:reqover-instrumentation` | `0.1.1` |
| `io.reqover:reqover-report` | `0.1.1` |
| `io.reqover:reqover-spring-mvc` | `0.1.1` |
| `io.reqover:reqover-spring-webflux` | `0.1.1` |

**`reqover-agent` is not among them.** The agent is a separately shaded JAR (so its dependencies don't collide with yours), which is why it is not published locally — you download it as a file from the GitHub Release → [step 4](#4-run-with-the-java-agent-attached).

Confirm the install:

```bash
ls ~/.m2/repository/io/reqover        # macOS / Linux
```

```powershell
Get-ChildItem "$env:USERPROFILE\.m2\repository\io\reqover"   # Windows
```

---

## 2. Add the dependencies

**One adapter plus the report module.** You need `mavenLocal()` in your repository list, placed before `mavenCentral()` so what you installed in step 1 is found first.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // For a Spring MVC project
    implementation("io.reqover:reqover-spring-mvc:0.1.1")

    // For a Spring WebFlux project (instead of the line above)
    // implementation("io.reqover:reqover-spring-webflux:0.1.1")

    implementation("io.reqover:reqover-report:0.1.1")
}
```

### Gradle (Groovy DSL)

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'io.reqover:reqover-spring-mvc:0.1.1'
    implementation 'io.reqover:reqover-report:0.1.1'
}
```

### Maven

Maven uses the local repository by default, so no repository configuration is needed.

```xml
<dependencies>
  <!-- For a Spring MVC project -->
  <dependency>
    <groupId>io.reqover</groupId>
    <artifactId>reqover-spring-mvc</artifactId>
    <version>0.1.1</version>
  </dependency>

  <!-- For Spring WebFlux, use artifactId reqover-spring-webflux -->

  <dependency>
    <groupId>io.reqover</groupId>
    <artifactId>reqover-report</artifactId>
    <version>0.1.1</version>
  </dependency>
</dependencies>
```

### What this alone does

The adapter's auto-configuration creates an `InMemoryCoverageStore` bean and attaches a record bucket to each incoming request. At this point **records are only being collected.** There is no way to view them yet, and since the agent isn't attached there is nothing to record either.

> [!NOTE]
> **Reqover does not create a report endpoint for you.** A coverage report exposes your internal structure directly, and authentication and network policy differ from environment to environment. Rather than have the library quietly decide where to expose it and how to protect it, that stays an explicit decision for your application.

### If you use WebFlux

The WebFlux adapter **turns on Reactor's automatic context propagation for the whole JVM** so request information can follow thread hops. If you don't want that, disable the adapter entirely before the application starts:

```properties
reqover.webflux.enabled=false
```

---

## 3. Create the report endpoint

Add the controller below. The `import` lines are included so you can paste it as-is.

```java
package com.example.app.internal;

import io.reqover.core.InMemoryCoverageStore;
import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportGenerator;
import io.reqover.report.HtmlCoverageReportRenderer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalReqoverReportController {
    private final InMemoryCoverageStore coverageStore;
    private final CoverageReportGenerator reportGenerator = new CoverageReportGenerator();
    private final HtmlCoverageReportRenderer htmlRenderer = new HtmlCoverageReportRenderer();

    public InternalReqoverReportController(InMemoryCoverageStore coverageStore) {
        this.coverageStore = coverageStore;
    }

    @GetMapping("/reqover/report")
    CoverageReport report() {
        return reportGenerator.generate(coverageStore.snapshots());
    }

    @GetMapping("/reqover/report.html")
    ResponseEntity<String> htmlReport() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlRenderer.render(reportGenerator.generate(coverageStore.snapshots())));
    }
}
```

`InMemoryCoverageStore` is the bean created by the auto-configuration in step 2, so you can just inject it.

Complete working examples are in [`examples/mvc-sample`](../examples/mvc-sample) and [`examples/webflux-sample`](../examples/webflux-sample).

### Keep the path under `/reqover/**`

Both adapters **exclude this prefix from recording.** The MVC interceptor excludes `/reqover`, `/reqover/**`, and `/error`; the WebFlux filter skips the same paths.

The reason is simple: viewing the report is itself a request, so without the exclusion **every time you open the report, a record of opening the report is added to the report.** If you want a different path, add an equivalent exclusion for it before exposing the endpoint.

> [!CAUTION]
> **Do not expose this endpoint to the internet.** The report contains your internal package, class, and method structure. Apply your application's authentication and network access controls, and keep it inside a development or QA network. The sample endpoints have no authentication.

---

## 4. Run with the Java agent attached

This is the part that actually records execution.

### 4-1. Download the agent JAR

Download `reqover-agent-0.1.1.jar` from the [v0.1.1 release](https://github.com/reqover-labs/reqover/releases/tag/v0.1.1). You can verify the file with `reqover-0.1.1-SHA256SUMS.txt` from the same release.

```bash
shasum -a 256 reqover-agent-0.1.1.jar          # macOS
sha256sum reqover-agent-0.1.1.jar              # Linux
```

```powershell
Get-FileHash reqover-agent-0.1.1.jar -Algorithm SHA256   # Windows
```

### 4-2. Run

**You must name your application packages with `include=`.**

```bash
java \
  -javaagent:reqover-agent-0.1.1.jar=include=com.example.orders \
  -jar app.jar
```

### `include` / `exclude` syntax

```text
include=com.example.orders;com.example.payments,exclude=com.example.orders.generated
```

| Rule | Detail |
| --- | --- |
| Separator between options | comma `,` |
| Separator between packages | semicolon `;` |
| `include` | **Required.** Without it, nothing is instrumented |
| Matching | **Prefix match** on the dotted class name |
| Precedence | Longest prefix wins. **On a tie, `exclude` wins** |

**Excluded by default** — these keep framework internals out so the report stays readable:

```text
org.springframework.   reactor.   io.micrometer.
```

**Always excluded and cannot be enabled by an include** — instrumenting these would break the JVM or Reqover itself:

```text
java.  javax.  jakarta.  jdk.  sun.  com.sun.  org.objectweb.asm.
io.reqover.core.  io.reqover.agent.  io.reqover.instrumentation.
io.reqover.report.  io.reqover.spring.
```

How the precedence rule actually behaves:

```text
include=com.example, exclude=com.example.generated
  → com.example.orders.OrderService   instrumented      (only include matches)
  → com.example.generated.Dto         not instrumented  (exclude is longer)

include=org.springframework.samples
  → org.springframework.samples.Foo   instrumented      (longer than the default exclude, so it wins)
  → org.springframework.boot.Bar      not instrumented  (caught by the default exclude)
```

> [!TIP]
> Prefixes are **plain string prefix matches.** Writing `include=com.example.order` will also catch `com.example.orders`. To match only the package you meant, **end the prefix with a dot**: `include=com.example.order.`

Keep `include` narrow. A broad include makes the report hard to read and increases startup time.

---

## 5. Check that it worked

1. Start the application **only on a development or QA network.**
2. **Look at standard error in the startup log.** Any line beginning with `[reqover]` means something is wrong with the agent configuration (see [Troubleshooting](#troubleshooting)). No warnings means you're fine.
3. Call **one** API you know.
4. Fetch `/reqover/report`.
5. Confirm:
   - the API you just called appears in normalized form (`GET /orders/{id}`)
   - your controller and service classes appear beneath it
   - method names are readable (`find(long): OrderResponse`)

For WebFlux, check one more thing — that a single API's records contain **two or more distinct thread names.** That is the evidence attribution survived the thread hop. (A simple API where no thread switch actually happens may legitimately show only one.)

---

## Troubleshooting

The most common failure is **"the report is empty"**, and the cause is usually `include`.

| Symptom | Cause | What to do |
| --- | --- | --- |
| No endpoints in the report at all | The agent isn't attached, or there is no `include` | Check stderr for `[reqover]` warnings. Verify the `-javaagent:` path points at a real file |
| `[reqover] no include configured` | You didn't pass `include=` | Add `include=your.package`. In this state nothing is instrumented, by design |
| `[reqover] no valid include configured` | You passed `include` but the value was empty | Check there is a value after `include=`, and that you didn't mix up the comma and semicolon |
| `[reqover] ignoring malformed agent option` | Not in `key=value` form | Make sure you included the `=`, as in `include=com.example` |
| `[reqover] ignoring unknown agent option` | A key other than `include`/`exclude` | Check for typos — `includes` and `packages` are not recognized |
| Endpoints appear but the class list is empty | `include` doesn't match your classes | Confirm you passed a **package prefix** rather than a class, and that it isn't caught by a default exclude such as `org.springframework.` |
| Only report reads accumulate | The report is exposed outside `/reqover/**` | Move it under `/reqover/**` or add an exclusion for your path |
| `/reqover/report` returns 404 | You didn't create the controller in step 3 | Reqover does not create the endpoint. Do step 3 |
| `InMemoryCoverageStore` injection fails | The adapter dependency is missing, or the web type doesn't match | Confirm `reqover-spring-mvc` in an MVC app, `reqover-spring-webflux` in a WebFlux app |
| Dependency not found (`Could not find io.reqover:...`) | Step 1 wasn't done, or `mavenLocal()` is missing | Re-run `publishToMavenLocal`, and check `mavenLocal()` comes before `mavenCentral()` |
| Old records disappear after a while | The retention cap (10,000 by default) was reached | This is expected. See [Adjusting retention](#adjusting-retention) |

---

## Things you can adjust

### Adjusting retention

Records live in memory only, with a default cap of 10,000 entries; beyond that the oldest are dropped. The adapter's bean backs off when your application defines its own, so defining your own bean takes effect:

```java
@Bean
InMemoryCoverageStore reqoverCoverageStore() {
    return new InMemoryCoverageStore(50_000);
}
```

### Clearing records between tests

Use `clear()` when you want each test to start from a clean state:

```java
@AfterEach
void resetCoverage() {
    coverageStore.clear();
}
```

---

## Limits to know before wiring it in

- **Method granularity.** You cannot see which lines ran. If you need line and branch precision, use JaCoCo alongside it.
- **Records live in memory only.** They are lost on restart, and with multiple instances each holds only its own records.
- **MVC async sections are not linked automatically.** Work handed to a separate thread is not recorded; attribution resumes when request handling returns.
- **Compiler-generated methods are not recorded.**
- **The report only shows what was actually observed.** Absence from the report is not evidence that a relationship doesn't exist — you may simply not have called that API yet. For the same reason, the reverse lookup is a "start looking here" hint, not a complete change-impact analysis.

The scope and limits of the performance measurements are in [local performance results](15_performance_results.md). It is not a formal benchmark.

---

## If you get stuck

[Open an issue](https://github.com/reqover-labs/reqover/issues/new/choose). Including the following narrows it down much faster:

- Your OS and the output of `java -version`
- Your Spring Boot version, and whether it's MVC or WebFlux
- The full `-javaagent:` option you used
- Any `[reqover]` lines from stderr
- What you expected and what actually happened

**Where people get stuck is the most valuable information this project can receive right now.** Getting stuck because the documentation was unclear counts as a bug too.

Related: [System architecture](02_architecture.md) (Korean) · [Agent E2E Demo](09_agent_e2e_demo.md) · [Demo script](10_demo_script.md)
