# 16. README Demo Capture

## Purpose

This document records how the screenshots in the root README were generated
from local Reqover runs. The images are product output, not mockups.

## Verified Environment

- OS: macOS 15.7.3, Apple M1, 16 GB RAM
- Date: 2026-08-14 (0.1.1 assets; 0.1.0 assets were taken on 2026-08-10)
- JDK: OpenJDK 21.0.11
- Browser capture viewport: 1440 × 1000 at device scale factor 2
- MVC port: 18200
- WebFlux port: 18201

Reqover emits Java 17-compatible bytecode and the full build passes on both
JDK 17 and JDK 21.

## Capture Method

The images are element-scoped screenshots of the saved report page, taken with
Playwright against a CSS selector rather than a pixel rectangle, so a layout
change cannot silently shift what the image shows.

| Asset | Selector |
| --- | --- |
| `reqover-mvc-request-attribution.png` | first `section.section` (Endpoint to Code) |
| `reqover-code-to-endpoint-index.png` | second `section.section` (Code to Endpoint Index) |
| `reqover-webflux-thread-hop.png` | first `article.endpoint` |
| `reqover-webflux-report.png` | `body` (whole report) |

## Build

```bash
./gradlew clean test --no-daemon --console=plain
./gradlew :reqover-agent:shadowJar \
  :examples:mvc-sample:bootJar \
  :examples:webflux-sample:bootJar \
  cyclonedxBom --no-daemon --console=plain
```

## MVC Request Attribution

> [!WARNING]
> The sample HTML report has no authentication. Restrict it to loopback access
> and never expose its port to a public or untrusted network.

Start the MVC sample on loopback:

```bash
java -jar examples/mvc-sample/build/libs/mvc-sample-0.1.1.jar \
  --server.address=127.0.0.1 \
  --server.port=18200 \
  --spring.main.banner-mode=off
```

Call two business endpoints, then save the standalone HTML before opening it in
a browser:

```bash
curl --fail http://127.0.0.1:18200/orders/42
curl --fail --request POST http://127.0.0.1:18200/payments
curl --fail http://127.0.0.1:18200/reqover/report.html \
  --output /tmp/reqover-final-mvc-report.html
```

Verified results:

- `completedRequestCount` is `2`.
- `GET /orders/{id}` and `POST /payments` are separate endpoint entries.
- `SharedValidator` appears in both endpoint cards and maps back to both
  observed endpoints.
- The manual-probe sample deliberately reports simple class names.

Assets:

- `docs/assets/reqover-mvc-request-attribution.png`
- `docs/assets/reqover-code-to-endpoint-index.png`

## WebFlux Java-Agent Attribution

Start the WebFlux sample with the shaded agent and a narrow include prefix:

```bash
java \
  -javaagent:reqover-agent/build/libs/reqover-agent-0.1.1.jar=include=io.reqover.example.webflux.auto \
  -jar examples/webflux-sample/build/libs/webflux-sample-0.1.1.jar \
  --server.address=127.0.0.1 \
  --server.port=18201 \
  --spring.main.banner-mode=off
```

Call the automatic-instrumentation endpoint and save the report:

```bash
curl --fail http://127.0.0.1:18201/auto/reactive/orders/42
curl --fail http://127.0.0.1:18201/reqover/report.html \
  --output /tmp/reqover-final-webflux-report.html
```

Verified results:

- `GET /auto/reactive/orders/{id}` is present.
- `AutoReactiveOrderController` and `AutoReactiveOrderService` are instrumented
  without manual probe calls.
- `reactor-http-nio-2`, `boundedElastic-1`, and `parallel-1` are retained in the
  same request bucket.
- `AutoReactiveOrderService#validate(J)J`, which runs after the scheduler hop,
  is attributed to the same endpoint. The report renders it as
  `validate(long): long`.

Assets:

- `docs/assets/reqover-webflux-thread-hop.png`
- `docs/assets/reqover-webflux-report.png`

The renderer emits a standalone document without external scripts or styles.
Saving it before browser navigation keeps incidental browser requests such as
`/favicon.ico` out of the captured request set.

## Interpretation Boundary

Observed code-to-endpoint relationships are a lower bound: Reqover reports only
execution relationships seen in the captured requests, so an absent relationship
does not prove that a code path can never affect an endpoint.

Reqover complements aggregate coverage tools such as JaCoCo by adding request
attribution. It does not replace JaCoCo or aggregate coverage reporting.

## Troubleshooting

- Confirm `java -version` and `./gradlew --version` use JDK 17 or 21.
- Check ports `18200` and `18201` before starting a sample.
- Call a business endpoint before opening the report.
- Confirm the Java-agent `include=` prefix matches the sample package.
- Stop the sample JVM after capture; do not commit process logs or build outputs.
