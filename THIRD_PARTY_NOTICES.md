# Third Party Notices

This file tracks the main third-party dependencies used by Reqover.

| Component | Version | License | URL | Purpose |
| --- | --- | --- | --- | --- |
| Spring Boot | 3.3.5 | Apache-2.0 | https://github.com/spring-projects/spring-boot | MVC/WebFlux sample applications |
| Spring Framework | 6.1.14 | Apache-2.0 | https://github.com/spring-projects/spring-framework | MVC/WebFlux adapter APIs |
| Project Reactor | 3.6.11 | Apache-2.0 | https://github.com/reactor/reactor-core | WebFlux reactive context/thread-hop demo |
| Micrometer Context Propagation | 1.1.2 | Apache-2.0 | https://github.com/micrometer-metrics/context-propagation | Reactor Context to ThreadLocal propagation |
| Jakarta Servlet API | 6.0.0 | EPL-2.0 OR GPL-2.0-with-classpath-exception | https://github.com/jakartaee/servlet | Compile-only servlet API for the MVC adapter |
| ASM | 9.7.1 | BSD-3-Clause | https://asm.ow2.io/ | Method-entry bytecode instrumentation |
| JUnit 5 | 5.10.3 | EPL-2.0 | https://github.com/junit-team/junit5 | Unit and integration tests |
| Gradle | 9.5.1 | Apache-2.0 | https://github.com/gradle/gradle | Build system |

JaCoCo is not linked into the implementation. If a future phase modifies or forks JaCoCo internals, EPL-2.0 obligations must be reviewed separately.
