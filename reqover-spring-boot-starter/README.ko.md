[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# Spring Boot Starter

Spring Boot 앱에 붙일 때 사용하는 기본 진입점입니다. core·report·MVC/WebFlux
어댑터를 Maven Central 의존성 하나로 가져옵니다. Java 패키지는 `io.reqover.*` 그대로입니다.

```kotlin
implementation("io.github.reqover-labs:reqover-spring-boot-starter:0.4.2")
```

앱 메서드를 기록하려면 [agent](../reqover-agent/README.ko.md)도 붙입니다.
HTTP 리포트와 파일 내보내기는 설정하지 않으면 꺼져 있습니다. 로컬 개발 앱의 예시입니다.

```properties
server.address=127.0.0.1
reqover.report.endpoint.enabled=true
reqover.report.export.json-path=build/reqover-report.json
reqover.report.export.html-path=build/reqover-report.html
```

기본 JSON 주소는 `/reqover/report`, HTML은 `/reqover/report.html`입니다.
자체 인증은 없으므로 앱의 접근 정책을 적용합니다. 파일은 정상적인 context 종료에
내보내며 `SIGKILL`에서는 저장하지 못합니다. 같은 JVM에서 같은 경로로 내보내면
여러 context의 기록을 누적하고, 첫 내보내기는 이전 실행의 파일을 교체합니다.

기본 저장소는 요청 상세를 제한하면서 전체 endpoint 집계를 유지합니다. JSON 상세
상한은 별도입니다. 자체 `CoverageStore` 빈으로 보관 정책을 바꿀 수 있고, 전체
집계를 유지하려면 `aggregates()`도 구현해야 합니다.

[설정과 연동](../docs/17_integration_guide.ko.md) |
[요청 진단](../docs/24_request_diagnostics.ko.md) |
[CI에서 기록 남기기](../docs/18_ci_impact_analysis.ko.md)
