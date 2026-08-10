# Third Party Notices

This file tracks the principal direct and runtime dependencies used by Reqover.
The complete resolved inventory, including transitive and test-only components,
is published as the CycloneDX SBOM at `sbom/reqover.cdx.json`.

| Component | Version | License | URL | Purpose |
| --- | --- | --- | --- | --- |
| Spring Boot | 3.5.16 | Apache-2.0 | https://github.com/spring-projects/spring-boot | MVC/WebFlux sample applications |
| Spring Framework | 6.2.19 | Apache-2.0 | https://github.com/spring-projects/spring-framework | MVC/WebFlux adapter APIs |
| Project Reactor | 3.7.19 | Apache-2.0 | https://github.com/reactor/reactor-core | Reactive request flow and scheduler hops |
| Reactor Netty | 1.2.18 | Apache-2.0 | https://github.com/reactor/reactor-netty | WebFlux sample HTTP runtime |
| Micrometer Context Propagation | 1.1.4 | Apache-2.0 | https://github.com/micrometer-metrics/context-propagation | Reactor Context to ThreadLocal propagation |
| Jakarta Servlet API | 6.0.0 | EPL-2.0 OR GPL-2.0-with-classpath-exception | https://github.com/jakartaee/servlet | Compile-only servlet API for the MVC adapter |
| Jackson | 2.21.5 | Apache-2.0 | https://github.com/FasterXML/jackson | Sample JSON serialization and E2E report parsing |
| Netty | 4.1.136.Final | Apache-2.0 | https://github.com/netty/netty | WebFlux sample network runtime |
| Apache Tomcat | 10.1.55 | Apache-2.0 | https://github.com/apache/tomcat | MVC sample embedded server |
| Logback | 1.5.34 | EPL-2.0 OR LGPL-2.1-only | https://github.com/qos-ch/logback | Sample application logging |
| ASM | 9.10.1 | BSD-3-Clause | https://gitlab.ow2.org/asm/asm | Method-entry bytecode instrumentation; redistributed in the agent JAR |
| JUnit 5 | 5.12.2 | EPL-2.0 | https://github.com/junit-team/junit5 | Unit and integration tests |
| CycloneDX Gradle Plugin | 3.3.0 | Apache-2.0 | https://github.com/CycloneDX/cyclonedx-gradle-plugin | Build-time SBOM generation |
| Shadow Gradle Plugin | 9.6.1 | Apache-2.0 | https://github.com/GradleUp/shadow | Build-time ASM relocation and agent packaging |
| Gradle | 9.5.1 | Apache-2.0 | https://github.com/gradle/gradle | Build system |

The full ASM BSD-3-Clause notice is retained at
`third-party-licenses/ASM-BSD-3-Clause.txt` and embedded in the agent JAR as
`META-INF/LICENSE-ASM`.

JaCoCo is not linked into the implementation. If a future phase modifies or forks JaCoCo internals, EPL-2.0 obligations must be reviewed separately.
