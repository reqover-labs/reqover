**English** | [한국어](17_integration_guide.ko.md)

# 17. Integrating Reqover into your Spring application

To see per-request execution records in **your own application** rather than the demo, follow this guide. Expect 20–30 minutes the first time.

If you haven't run the demo yet, we recommend starting with ["Try it in 5 minutes"](../README.md#try-it-in-5-minutes) in the README. Seeing the demo work first makes problems much easier to isolate.

> [!IMPORTANT]
> Reqover `0.2.0` is **not on Maven Central yet.** The signed publication pipeline exists (`./gradlew centralBundle`, plus a release job that is inert until the repository opts in), but it has not been run, so there is nothing to resolve from Central today. You either build from source into your local Maven repository or take the jars from a GitHub Release. Reqover is designed for development, QA, and staging — not for running permanently in production.

---

## The overall flow

Wiring it in takes four steps. **None of them modify your source code.**

| Step | What you do | Why it's needed |
| --- | --- | --- |
| 1 | Get the libraries | Not on Maven Central yet |
| 2 | Add one dependency | The part that links requests to execution records |
| 3 | Decide how you read the report | Both ways of reading it are off by default |
| 4 | Run with the Java agent attached | The part that actually records execution |

### Decide these first

- **JDK 17 or 21**
- **Spring Boot 3.x** — the samples are verified against 3.5.16
- **MVC or WebFlux** — the starter carries both adapters and only the matching one activates

---

## 1. Get the libraries

### Option A — build from source (recommended)

Check out the `v0.2.0` tag and install into your local Maven repository:

```bash
git clone --branch v0.2.0 --depth 1 https://github.com/reqover-labs/reqover.git
cd reqover
./gradlew clean publishToMavenLocal
```

On Windows use `.\gradlew.bat clean publishToMavenLocal`.

This installs the library modules under the `io.reqover` group:

| Artifact | Version |
| --- | --- |
| `io.reqover:reqover-core` | `0.2.0` |
| `io.reqover:reqover-instrumentation` | `0.2.0` |
| `io.reqover:reqover-report` | `0.2.0` |
| `io.reqover:reqover-spring-mvc` | `0.2.0` |
| `io.reqover:reqover-spring-webflux` | `0.2.0` |
| `io.reqover:reqover-spring-boot-starter` | `0.2.0` |

**`reqover-agent` and `reqover-cli` are not among them.** Both are shaded executables (so their dependencies don't collide with yours) rather than libraries you compile against, so neither is published. You download them as files from the GitHub Release → [step 4](#4-run-with-the-java-agent-attached).

Confirm the install:

```bash
ls ~/.m2/repository/io/reqover        # macOS / Linux
```

```powershell
Get-ChildItem "$env:USERPROFILE\.m2\repository\io\reqover"   # Windows
```

### Option B — the GitHub Release bundle

The [v0.2.0 release](https://github.com/reqover-labs/reqover/releases/tag/v0.2.0) ships `reqover-0.2.0.zip`, which contains the same library jars under `lib/`, their sources under `sources/`, plus `reqover-agent-0.2.0.jar` and `reqover-cli-0.2.0.jar` at the root. Use this if you cannot build from source; you will have to put the `lib/` jars somewhere your build can resolve them (a flat-dir repository or your internal Nexus/Artifactory).

Either way, verify the download against `reqover-0.2.0-SHA256SUMS.txt` from the same release.

---

## 2. Add the dependency

### The starter (recommended)

**One dependency.** `reqover-spring-boot-starter` brings `reqover-core`, `reqover-report`, and both adapters, and adds the Spring Boot auto-configuration for the report endpoint and the shutdown export.

You need `mavenLocal()` in your repository list, placed before `mavenCentral()` so what you installed in step 1 is found first.

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.reqover:reqover-spring-boot-starter:0.2.0")
}
```

```groovy
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation 'io.reqover:reqover-spring-boot-starter:0.2.0'
}
```

Maven uses the local repository by default, so no repository configuration is needed:

```xml
<dependency>
  <groupId>io.reqover</groupId>
  <artifactId>reqover-spring-boot-starter</artifactId>
  <version>0.2.0</version>
</dependency>
```

**Carrying both adapters is safe.** Each adapter is conditional on the web application type *and* on the classes it needs, so a servlet-only application gets the MVC adapter and never touches the WebFlux one, and vice versa.

### Individual modules

If you want only one adapter and no starter, depend on the adapter and the report module directly. You give up the report endpoint, the shutdown export, and `ReqoverReportService` — those live in the starter — and you build the report yourself from a `CoverageStore`.

```kotlin
dependencies {
    // For a Spring MVC project
    implementation("io.reqover:reqover-spring-mvc:0.2.0")

    // For a Spring WebFlux project (instead of the line above)
    // implementation("io.reqover:reqover-spring-webflux:0.2.0")

    implementation("io.reqover:reqover-report:0.2.0")
}
```

### What this alone does

The adapter's auto-configuration creates a `CoverageStore` bean and attaches a record bucket to each incoming request. At this point **records are only being collected.** There is no way to view them yet, and since the agent isn't attached there is nothing to record either.

> [!NOTE]
> **Nothing is exposed by default.** The report endpoint ships disabled, and the shutdown export writes nothing until you name a path. A coverage report exposes your internal structure directly, and authentication and network policy differ from environment to environment, so the library does not quietly decide where to expose it. See [step 3](#3-decide-how-you-read-the-report).

### If you use WebFlux

The WebFlux adapter **turns on Reactor's automatic context propagation for the whole JVM** so request information can follow thread hops. This is the one JVM-wide change Reqover makes. If you don't want it, disable the adapter entirely before the application starts:

```properties
reqover.webflux.enabled=false
```

---

## Configuration reference

Every property Reqover reads, with the default that applies when you leave it out.

`reqover.mvc.*` comes from `reqover-spring-mvc`, `reqover.webflux.*` from `reqover-spring-webflux`, and `reqover.report.*` from `reqover-spring-boot-starter`. The starter carries all three.

| Property | Default | What it does |
| --- | --- | --- |
| `reqover.mvc.enabled` | `true` | Whether request attribution is installed at all. `false` keeps the MVC adapter out of the context |
| `reqover.mvc.include-path-patterns` | `/**` | Ant path patterns the interceptor attributes. Defaults to everything |
| `reqover.mvc.exclude-path-patterns` | `/reqover`, `/reqover/**`, `/error` | Paths excluded from attribution. Setting this **replaces** the default list |
| `reqover.mvc.max-snapshots` | `10000` | How many finished requests the default in-memory store retains before applying the eviction policy. Ignored when you supply your own `CoverageStore` bean |
| `reqover.mvc.snapshot-eviction` | `oldest-first` | What happens at the bound: `oldest-first` drops the oldest snapshot (default), `reject-when-full` keeps the existing window and ignores new flushes. Ignored when you supply your own `CoverageStore` bean |
| `reqover.webflux.enabled` | `true` | Whether the WebFlux adapter is installed. `false` also skips enabling Reactor's automatic context propagation |
| `reqover.webflux.exclude-path-prefixes` | `/reqover` | Paths excluded from attribution, matched as **prefixes** (not Ant patterns). Setting this replaces the default list |
| `reqover.webflux.max-snapshots` | `10000` | Same as `reqover.mvc.max-snapshots`, for reactive applications |
| `reqover.webflux.snapshot-eviction` | `oldest-first` | Same as `reqover.mvc.snapshot-eviction`, for reactive applications |
| `reqover.report.endpoint.enabled` | **`false`** | Whether the built-in HTTP report endpoint is registered. Off by default — see [step 3](#3-decide-how-you-read-the-report) |
| `reqover.report.endpoint.path` | `/reqover/report` | Base path for the endpoint. JSON is served here, and the HTML report at the same path with `.html` appended |
| `reqover.report.export.json-path` | *unset* | Where to write the JSON report when the application context closes. Unset or blank means no JSON export |
| `reqover.report.export.html-path` | *unset* | The same for the HTML report. Unset or blank means no HTML export |

Two things the table cannot show:

- The two adapters never both activate. Which set of properties applies is decided by whether your application is servlet or reactive.
- `reqover.report.*` only takes effect when a `CoverageStore` bean exists — that is, when an adapter is active. Disabling both adapters disables report generation with them.

---

## 3. Decide how you read the report

There are two ways to get the report out, and **both are off by default.** Turn on whichever fits: the HTTP endpoint for looking at it while the application runs, the shutdown export for getting a file out of a CI run. They can be on at the same time, and they produce the same document.

### 3-1. The HTTP report endpoint

```properties
reqover.report.endpoint.enabled=true
reqover.report.endpoint.path=/reqover/report
```

With that, the report is served at:

| URL | Content type |
| --- | --- |
| `/reqover/report` | `application/json` |
| `/reqover/report.html` | `text/html` |

The `.html` suffix is appended to whatever `path` you set — it is not separately configurable.

The endpoint is registered for MVC and for WebFlux alike; the starter picks the implementation that matches your web application type. Both demos turn it on this way — see [`examples/mvc-sample/src/main/resources/application.properties`](../examples/mvc-sample/src/main/resources/application.properties).

> [!CAUTION]
> **The default is `false` on purpose, and turning it on is a deployment decision.** The report names your internal classes and methods, and **Reqover ships no authentication of its own** — not for this endpoint, not anywhere. If you enable it, guarding the path is your application's job: put your own security configuration in front of it and keep it inside a development or QA network. The sample applications have no authentication, which is why the demo scripts start them with `--server.address=127.0.0.1`.

#### Keep the path inside the excluded prefix

Both adapters **exclude `/reqover` from recording by default.** MVC excludes `/reqover`, `/reqover/**`, and `/error`; WebFlux skips anything starting with `/reqover`.

The reason is simple: viewing the report is itself a request, so without the exclusion **every time you open the report, a record of opening the report is added to the report.**

If you move the endpoint with `reqover.report.endpoint.path`, add a matching exclusion yourself — and remember that setting the property replaces the default list, so include the defaults you still want:

```properties
reqover.report.endpoint.path=/internal/reqover/report
reqover.mvc.exclude-path-patterns=/internal/reqover,/internal/reqover/**,/error
# WebFlux:
# reqover.webflux.exclude-path-prefixes=/internal/reqover
```

#### Writing your own controller still works

The guidance from `0.1.1` — expose the report yourself — is still supported, and it is the only option if you skipped the starter. Every bean the starter contributes is annotated `@ConditionalOnMissingBean`, so defining your own makes Reqover's back off:

| Your bean | What backs off |
| --- | --- |
| Any `ReqoverReportService` | The starter's report service |
| A `ReqoverMvcReportEndpoint` | The built-in servlet endpoint |
| A `RouterFunction<ServerResponse>` named `reqoverReportRoutes` | The built-in reactive routes |
| Any `ReqoverReportExporter` | The shutdown export |

With the starter, injecting `ReqoverReportService` is the least code:

```java
package com.example.app.internal;

import io.reqover.spring.boot.ReqoverReportService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalReqoverReportController {
    private final ReqoverReportService reportService;

    public InternalReqoverReportController(ReqoverReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reqover/report")
    ResponseEntity<String> report() {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(reportService.json());
    }

    @GetMapping("/reqover/report.html")
    ResponseEntity<String> htmlReport() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(reportService.html());
    }
}
```

Without the starter, inject `CoverageStore` and build the report yourself with `CoverageReportGenerator` and `HtmlCoverageReportRenderer` from `reqover-report`.

> [!IMPORTANT]
> **Inject `CoverageStore`, not `InMemoryCoverageStore`.** This changed in `0.2.0` — see [Replacing the store](#replacing-the-store).

Complete working examples are in [`examples/mvc-sample`](../examples/mvc-sample) and [`examples/webflux-sample`](../examples/webflux-sample).

### 3-2. Exporting the report at shutdown

```properties
reqover.report.export.json-path=build/reqover-report.json
reqover.report.export.html-path=build/reqover-report.html
```

Either property on its own is enough; setting neither disables the export entirely. Missing parent directories are created. On success Reqover prints one line per file to standard output:

```text
[reqover] wrote the JSON report to /abs/path/build/reqover-report.json
```

**This is how a CI job gets a report file out of an integration test run.** Boot the application with the agent attached, drive your tests through it, let it stop, and the file is there:

```bash
java -javaagent:reqover-agent-0.2.0.jar=include=com.example \
  -jar build/libs/your-app.jar \
  --reqover.report.export.json-path=build/reqover-report.json
```

Three things to know:

- **The export runs when the application context closes.** A process killed with `SIGKILL` writes nothing. In CI, stop the application with `SIGTERM` (plain `kill`) and wait for it to exit.
- **Export failures are logged to standard error and swallowed.** A measurement tool must not be the reason a shutdown fails — which also means a missing file is a quiet failure. Check that the file exists before you analyse it.
- **The exported document is byte-for-byte what the endpoint would have served.** Both go through the same `ReqoverReportService`.

The full loop — exporting a report, asking it which endpoints a diff affects, and commenting the answer on a pull request — is in [Impact analysis in CI](18_ci_impact_analysis.md).

---

## 4. Run with the Java agent attached

This is the part that actually records execution.

### 4-1. Download the agent JAR

Download `reqover-agent-0.2.0.jar` from the [v0.2.0 release](https://github.com/reqover-labs/reqover/releases/tag/v0.2.0). You can verify the file with `reqover-0.2.0-SHA256SUMS.txt` from the same release.

```bash
shasum -a 256 reqover-agent-0.2.0.jar          # macOS
sha256sum reqover-agent-0.2.0.jar              # Linux
```

```powershell
Get-FileHash reqover-agent-0.2.0.jar -Algorithm SHA256   # Windows
```

### 4-2. Run

**You must name your application packages with `include=`.**

```bash
java \
  -javaagent:reqover-agent-0.2.0.jar=include=com.example.orders \
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
4. Fetch `/reqover/report` (with the endpoint enabled), or stop the application and open the exported file.
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
| `/reqover/report` returns 404 | **The endpoint is disabled by default** | Set `reqover.report.endpoint.enabled=true`, or write your own controller. This is the expected out-of-the-box behaviour |
| Endpoint enabled but still 404 | No adapter is active, so there is no `CoverageStore` and no report service | Confirm the application is a web application and that you didn't set `reqover.mvc.enabled=false` / `reqover.webflux.enabled=false` |
| Only report reads accumulate | The endpoint path is outside the excluded prefix | Keep the path under `/reqover`, or add it to `reqover.mvc.exclude-path-patterns` / `reqover.webflux.exclude-path-prefixes` |
| Setting `exclude-path-patterns` made `/error` show up | Setting the property **replaces** the default list | Include the defaults you still want in your own value |
| No file after shutdown | The process was killed with `SIGKILL`, or the write failed | Stop it with `SIGTERM` and wait. Look for `[reqover] wrote the` on stdout and `[reqover] could not write` on stderr |
| `InMemoryCoverageStore` injection fails after upgrading from `0.1.1` | The adapters now contribute a `CoverageStore` bean | Change the injection point to `CoverageStore`. See [Replacing the store](#replacing-the-store) |
| `CoverageStore` injection fails | The adapter dependency is missing, or the web type doesn't match | Use the starter, or confirm `reqover-spring-mvc` in an MVC app and `reqover-spring-webflux` in a WebFlux app |
| Dependency not found (`Could not find io.reqover:...`) | Step 1 wasn't done, or `mavenLocal()` is missing | Re-run `publishToMavenLocal`, and check `mavenLocal()` comes before `mavenCentral()` |
| Old records disappear after a while | The retention cap (10,000 by default) was reached | This is expected. See [Adjusting retention](#adjusting-retention) |

---

## Things you can adjust

### Adjusting retention

Records live in memory only, with a default cap of 10,000 entries. Beyond that the store either drops the oldest snapshot (`oldest-first`, the default) or keeps the existing window and ignores new flushes (`reject-when-full`). Both the bound and the policy are properties — no bean needed:

```properties
reqover.mvc.max-snapshots=50000
reqover.mvc.snapshot-eviction=oldest-first
# Keep the first N for a long QA session instead of rolling:
# reqover.mvc.snapshot-eviction=reject-when-full
# WebFlux:
# reqover.webflux.max-snapshots=50000
# reqover.webflux.snapshot-eviction=reject-when-full
```

A second `CoverageStore` implementation can pin the same behaviour with the abstract JUnit contract in `reqover-core` tests (`CoverageStoreContract`): extend it, return your store from `newStore()`, and run the suite.

### Replacing the store

`CoverageStore` is the SPI for where records go. Define a bean of that type and the adapters back off — both contribute their store with `@ConditionalOnMissingBean(CoverageStore.class)` — so you can write snapshots to disk, to a database, or drop them under a sampling rule:

```java
@Bean
CoverageStore reqoverCoverageStore() {
    return new SamplingCoverageStore(0.1);   // your implementation
}
```

The interface is three methods:

| Method | Contract |
| --- | --- |
| `flush(CoverageBucket)` | Records a finished bucket. Call `bucket.snapshot()` immediately — the bucket may keep receiving hits from stray threads after the call returns |
| `snapshots()` | The retained snapshots, oldest first. The returned list is a copy and is safe to iterate while other threads flush |
| `clear()` | Discards every retained snapshot |

Implementations must be safe for concurrent use, and `flush` is called on the thread that completed the unit of work — an HTTP worker in the common case — so it must not block for long.

Supplying your own store **makes `max-snapshots` irrelevant**: retention is entirely yours at that point.

> [!IMPORTANT]
> **Breaking change from `0.1.1`.** The adapters used to contribute an `InMemoryCoverageStore` bean; they now contribute a `CoverageStore` bean, with `InMemoryCoverageStore` as one implementation of it rather than the only option. An application that injects the concrete type must change the injection point to `CoverageStore`.

To keep the in-memory store but configure it yourself, the bean form still works:

```java
@Bean
CoverageStore reqoverCoverageStore() {
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

### Attributing work that isn't an HTTP request

The adapters attribute HTTP requests automatically. `UnitScope` is how anything else gets the same treatment — a scheduled job, a message listener, a single test case. Everything executed inside the try-with-resources block is attributed to that unit and flushed to the store when the block ends:

```java
import io.reqover.core.CoverageStore;
import io.reqover.core.UnitInfo;
import io.reqover.core.UnitScope;

@Component
class NightlySettlementJob {
    private final CoverageStore coverageStore;
    private final SettlementService settlement;

    NightlySettlementJob(CoverageStore coverageStore, SettlementService settlement) {
        this.coverageStore = coverageStore;
        this.settlement = settlement;
    }

    @Scheduled(cron = "0 0 3 * * *")
    void run() {
        String runId = UUID.randomUUID().toString();
        try (UnitScope scope = UnitScope.open(coverageStore, UnitInfo.scheduledJob(runId, "nightly-settlement"))) {
            settlement.settleYesterday();
        }
    }
}
```

The report groups by the unit's name, so `nightly-settlement` appears alongside your endpoints. `UnitInfo` has factories for the common cases — `scheduledJob(runId, jobName)`, `message(messageId, destination)`, `test(runId, testName)` — and `of(unitId, unitType, name)` for anything else. Use a name that identifies the job, topic, or test rather than the individual run; that name is what the report groups by.

Closing the scope finishes the bucket and flushes it exactly once, so leaving the block early or by exception is safe, and closing twice is a no-op. To record a status of your own, call `scope.bucket().finish(status)` before the block ends.

> [!IMPORTANT]
> **Attribution follows the thread that opened the scope.** Work handed to another thread inside the block is *not* recorded under this unit. That thread has to opt in by opening `UnitScope.join(scope.bucket())` around its own work — closing a `join` scope restores the previous context without finishing or flushing, which stays with the `open` scope that created the bucket.

---

## Limits to know before wiring it in

- **Method granularity.** You cannot see which lines ran. If you need line and branch precision, use JaCoCo alongside it.
- **Records live in memory only.** They are lost on restart, and with multiple instances each holds only its own records. `CoverageStore` is the extension point for storing them elsewhere, but Reqover ships no persistent implementation — export the report to a file instead.
- **MVC async sections are not linked automatically.** Work handed to a separate thread is not recorded; attribution resumes when request handling returns. The same applies inside a `UnitScope` unless the other thread opens a `join` scope.
- **Compiler-generated methods are not recorded.**
- **The shutdown export only runs on a normal context close.** `SIGKILL` writes nothing, and a failed write is logged and swallowed rather than raised.
- **Report endpoint authentication is your responsibility.** Reqover ships none.
- **The report only shows what was actually observed.** Absence from the report is not evidence that a relationship doesn't exist — you may simply not have called that API yet. For the same reason, the reverse lookup is a "start looking here" hint, not a complete change-impact analysis.

The scope and limits of the performance measurements are in [measured agent overhead](15_performance_results.md) — about 24 ns per instrumented method entry, measured over alternating rounds, with the things it does not cover listed.

---

## If you get stuck

[Open an issue](https://github.com/reqover-labs/reqover/issues/new/choose). Including the following narrows it down much faster:

- Your OS and the output of `java -version`
- Your Spring Boot version, and whether it's MVC or WebFlux
- The full `-javaagent:` option you used
- Your `reqover.*` properties
- Any `[reqover]` lines from stderr
- What you expected and what actually happened

**Where people get stuck is the most valuable information this project can receive right now.** Getting stuck because the documentation was unclear counts as a bug too.

Related: [System architecture](02_architecture.md) · [Impact analysis in CI](18_ci_impact_analysis.md) · [Agent E2E Demo](09_agent_e2e_demo.md) · [Demo script](10_demo_script.md)
