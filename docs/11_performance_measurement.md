# 11. Performance Measurement

## Purpose

Reqover adds work to each instrumented method entry. The MVP does not claim production-ready overhead numbers, but it should provide a repeatable way to measure baseline versus agent-enabled latency.

## Measurement Method

Measure the same endpoint in two modes:

1. Baseline sample application without `-javaagent`.
2. Sample application with Reqover `-javaagent`.

Use the same machine, same JVM, same endpoint, same request count, and no other heavy background work.

## Baseline Run

Start MVC sample without agent:

```bash
./gradlew :examples:mvc-sample:bootRun
```

Measure:

```bash
pwsh scripts/measure-demo-latency.ps1 -Url http://localhost:8080/orders/1 -WarmupRequests 30 -MeasuredRequests 300
```

Save the JSON output.

## Agent Run

Start MVC sample with agent:

```bash
./gradlew :reqover-agent:jar :examples:mvc-sample:bootJar
java -javaagent:reqover-agent/build/libs/reqover-agent-0.1.0-SNAPSHOT.jar=include=io.reqover.example.mvc.auto -jar examples/mvc-sample/build/libs/mvc-sample-0.1.0-SNAPSHOT.jar
```

Measure the auto endpoint:

```bash
pwsh scripts/measure-demo-latency.ps1 -Url http://localhost:8080/auto/orders/1 -WarmupRequests 30 -MeasuredRequests 300
```

Save the JSON output.

## WebFlux Run

Start WebFlux sample with agent:

```bash
./gradlew :reqover-agent:jar :examples:webflux-sample:bootJar
java -javaagent:reqover-agent/build/libs/reqover-agent-0.1.0-SNAPSHOT.jar=include=io.reqover.example.webflux.auto -jar examples/webflux-sample/build/libs/webflux-sample-0.1.0-SNAPSHOT.jar
```

Measure:

```bash
pwsh scripts/measure-demo-latency.ps1 -Url http://localhost:8080/auto/reactive/orders/1 -WarmupRequests 30 -MeasuredRequests 300
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

