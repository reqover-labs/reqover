# 16. README Demo Capture

## Purpose

This document records how the screenshots in the root README were generated from a local Reqover run. The images are product output, not mockups.

## Verified Environment

- OS: Windows
- Date: 2026-07-31
- JDK: 21.0.10
- Gradle: 9.5.1 wrapper
- MVC port: 18082
- WebFlux port: 18083

Reqover builds with JDK 21 and emits Java 17-compatible bytecode.

## Build

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.10'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean test --no-daemon --console=plain
.\gradlew.bat :reqover-agent:jar :examples:mvc-sample:bootJar :examples:webflux-sample:bootJar cyclonedxBom --no-daemon --console=plain
```

## MVC Request Attribution

Start `mvc-sample` on port `18082`, then call:

```powershell
Invoke-RestMethod 'http://localhost:18082/orders/1'
Invoke-RestMethod -Method Post 'http://localhost:18082/payments'
Invoke-RestMethod 'http://localhost:18082/reqover/report'
```

Verified results:

- `completedRequestCount` is `2`.
- `GET /orders/{id}` and `POST /payments` are separate endpoint entries.
- `SharedValidator` maps back to both observed endpoints. The current manual-probe metadata deliberately reports the simple class name rather than the fully qualified name.

Assets:

- `docs/assets/reqover-mvc-request-attribution.png`
- `docs/assets/reqover-code-to-endpoint-index.png`

## WebFlux Java-Agent Attribution

Run the WebFlux jar with:

```powershell
java "-javaagent:reqover-agent/build/libs/reqover-agent-0.1.0-SNAPSHOT.jar=include=io.reqover.example.webflux.auto" `
  -jar examples/webflux-sample/build/libs/webflux-sample-0.1.0-SNAPSHOT.jar `
  --server.port=18083 `
  --spring.main.banner-mode=off
```

Then call:

```powershell
Invoke-RestMethod 'http://localhost:18083/auto/reactive/orders/42'
Invoke-RestMethod 'http://localhost:18083/reqover/report'
```

Verified results:

- `GET /auto/reactive/orders/{id}` is present.
- `AutoReactiveOrderController` and `AutoReactiveOrderService` were inserted by the Java agent.
- The request bucket contains multiple thread names from the reactive execution.

Asset:

- `docs/assets/reqover-webflux-thread-hop.png`

## Interpretation Boundary

Reqover reports execution relationships observed in these requests. An absent relationship does not prove that a code path can never affect an endpoint.

## Troubleshooting

- Confirm `java -version` and `.\gradlew.bat --version` both use JDK 21.
- Check ports `18082` and `18083` before starting a sample.
- Call a business endpoint before opening the report.
- Confirm the Java-agent `include=` prefix matches the sample package.
- Stop the sample JVM after capture; do not commit process logs or build outputs.
