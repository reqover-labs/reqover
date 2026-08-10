# 11. Performance Measurement

## Purpose

Reqover adds work to each instrumented method entry. The MVP does not claim production-ready overhead numbers, but it should provide a repeatable way to measure baseline versus agent-enabled latency.

## Measurement Method

Measure the same endpoint in two modes:

1. Baseline sample application without `-javaagent`.
2. Sample application with Reqover `-javaagent`.

Use the same machine, same JVM, same endpoint, same request count, and no other heavy background work.

The release-candidate helper automates the baseline/agent restart, measures the
same MVC auto endpoint, and stores raw samples, environment metadata, and the
commit SHA. It intentionally refuses a dirty worktree:

```bash
./scripts/capture-performance-evidence.sh 18180 50 300
```

Use this command for submission evidence. The manual commands below are useful
for troubleshooting the individual steps.

## Baseline Run

Start MVC sample without agent:

```bash
./gradlew :examples:mvc-sample:bootRun --args='--server.address=127.0.0.1 --server.port=8080'
```

Measure:

```bash
pwsh scripts/measure-demo-latency.ps1 -Url http://127.0.0.1:8080/auto/orders/1 -WarmupRequests 50 -MeasuredRequests 300
```

Save the JSON output.

On macOS/Linux the equivalent command can preserve raw samples:

```bash
./scripts/measure-demo-latency.sh \
  http://127.0.0.1:8080/auto/orders/1 50 300 baseline-samples-ms.txt
```

## Agent Run

Start MVC sample with agent:

```bash
./gradlew :reqover-agent:shadowJar :examples:mvc-sample:bootJar
java -javaagent:reqover-agent/build/libs/reqover-agent-0.1.0.jar=include=io.reqover.example.mvc.auto \
  -jar examples/mvc-sample/build/libs/mvc-sample-0.1.0.jar \
  --server.address=127.0.0.1 --server.port=8080
```

Measure the auto endpoint:

```bash
pwsh scripts/measure-demo-latency.ps1 -Url http://127.0.0.1:8080/auto/orders/1 -WarmupRequests 50 -MeasuredRequests 300
```

Save the JSON output.

```bash
./scripts/measure-demo-latency.sh \
  http://127.0.0.1:8080/auto/orders/1 50 300 agent-samples-ms.txt
```

## WebFlux Run

Start WebFlux sample with agent:

```bash
./gradlew :reqover-agent:shadowJar :examples:webflux-sample:bootJar
java -javaagent:reqover-agent/build/libs/reqover-agent-0.1.0.jar=include=io.reqover.example.webflux.auto \
  -jar examples/webflux-sample/build/libs/webflux-sample-0.1.0.jar \
  --server.address=127.0.0.1 --server.port=8080
```

Measure:

```bash
pwsh scripts/measure-demo-latency.ps1 -Url http://127.0.0.1:8080/auto/reactive/orders/1 -WarmupRequests 50 -MeasuredRequests 300
```

## Interpreting Results

The MVP result report should include:

- machine and OS
- JDK version
- endpoint
- warmup request count
- measured request count
- average latency
- p50 latency
- p95 latency
- p99 latency
- known caveats

Do not overclaim the result. The script uses sequential local HTTP requests, not a full load generator. It is useful for comparing local demo modes and spotting obvious overhead regressions.
