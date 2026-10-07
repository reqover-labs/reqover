**English** | [한국어](README.ko.md) | [Project](../README.md)

# Reqover Agent

The Java agent instruments included application classes at load time. Use it
with the Spring starter or an explicitly managed `UnitScope`; the agent alone
does not create HTTP request attribution or a report endpoint.

Download `reqover-agent-0.4.2.jar` and the checksum file from the
[v0.4.2 release](https://github.com/reqover-labs/reqover/releases/tag/v0.4.2).
It is a shaded executable, not a Maven Central library. JDK 17+ is required.

```bash
java -javaagent:reqover-agent-0.4.2.jar=include=com.example -jar app.jar
```

| Option | Default | Meaning |
| --- | --- | --- |
| `include` | empty | Dotted class/package prefixes; without it instrumentation is disabled |
| `exclude` | framework prefixes | Narrow recording scope; longest prefix wins, exclude wins ties |
| `accessors` | `skip` | `record` keeps trivial getters/setters/builders for impact analysis |
| `references` | `skip` | `record` observes included interface calls/static-field reads at call sites |

Separate options with commas and prefixes with semicolons. Quote the whole
argument if it contains semicolons. Hard-excluded runtime packages and generated
proxies cannot be enabled. Reference observations are not method-body execution
or measured call order; no line/branch coverage or method timing is collected.

For a source build, run `./gradlew :reqover-agent:shadowJar` at the repository
root; the JAR is under `reqover-agent/build/libs/`.

[Integration and option details](../docs/17_integration_guide.md) |
[Instrumentation](../reqover-instrumentation/README.md)
