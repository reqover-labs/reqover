# Reqover

Reqover는 실행 중인 Spring 애플리케이션에서 코드 커버리지를 전역 값이 아니라 개별 HTTP 요청과 엔드포인트 단위로 귀속하는 개발자 도구입니다.

기존 커버리지 도구는 "어떤 코드가 실행되었는가"를 알려주지만, Reqover는 한 단계 더 들어가 "어느 요청이 그 코드를 실행했는가"를 보여주는 것을 목표로 합니다. 특히 Spring WebFlux처럼 요청 처리 중 thread가 바뀌는 reactive 환경에서도 귀속이 유지되는지를 핵심 차별점으로 둡니다.

## 현재 상태

이 저장소는 2026 오픈소스 개발자대회 제출을 목표로 하는 Phase 0 MVP 단계입니다.

- 백서 원본: `C:\OpenSourceCompetition\docs\Reqover_기술백서.pdf`
- 작업 폴더: `C:\OpenSourceCompetition\reqover`
- 구현 상태: 요청별 bucket routing, MVC/WebFlux sample, JSON/HTML report, code-to-endpoint reverse index, Spring Boot auto-configuration, ASM method-entry instrumentation, Java agent smoke test, agent 기반 Spring E2E 테스트 구현
- 우선순위: JaCoCo 분석 연동 spike, 제출용 데모 정리, 성능 측정

## Build

요구사항:

- JDK 21 권장
- 컴파일 target은 Java 17

테스트 실행:

```powershell
.\gradlew.bat test
```

현재 포함된 모듈:

- `reqover-core`: coverage bucket, ThreadLocal context, probe entry point, in-memory snapshot store
- `reqover-report`: endpoint-level coverage report model
- `reqover-spring-mvc`: Spring MVC request bucket adapter
- `reqover-spring-webflux`: WebFlux/Reactor Context request bucket adapter
- `reqover-instrumentation`: ASM method-entry instrumentation
- `reqover-agent`: Java agent packaging and class transformer
- `examples/mvc-sample`: MVC demo application
- `examples/webflux-sample`: WebFlux thread-hop demo application

## Quick Demo: Manual Probe

MVC sample:

```powershell
.\gradlew.bat :examples:mvc-sample:bootRun
```

Then call:

```text
GET  http://localhost:8080/orders/1
POST http://localhost:8080/payments
GET  http://localhost:8080/reqover/report
GET  http://localhost:8080/reqover/report.html
```

WebFlux sample:

```powershell
.\gradlew.bat :examples:webflux-sample:bootRun
```

Then call:

```text
GET http://localhost:8080/reactive/orders/1
GET http://localhost:8080/reqover/report
GET http://localhost:8080/reqover/report.html
```

Reports include both endpoint-to-code coverage and code-to-endpoint reverse lookup.

## Quick Demo: Java Agent Auto Instrumentation

아래 데모는 샘플 컨트롤러/서비스 코드에 `ReqoverProbe.hit(...)`를 직접 넣지 않고, `-javaagent`가 method entry probe를 삽입하는 흐름입니다.

Build agent and sample jars:

```powershell
.\gradlew.bat :reqover-agent:jar :examples:mvc-sample:bootJar :examples:webflux-sample:bootJar
```

Run MVC sample with the agent:

```powershell
java -javaagent:reqover-agent\build\libs\reqover-agent-0.1.0-SNAPSHOT.jar=include=io.reqover.example.mvc.auto -jar examples\mvc-sample\build\libs\mvc-sample-0.1.0-SNAPSHOT.jar
```

Then call:

```text
GET http://localhost:8080/auto/orders/42
GET http://localhost:8080/reqover/report
GET http://localhost:8080/reqover/report.html
```

Run WebFlux sample with the agent:

```powershell
java -javaagent:reqover-agent\build\libs\reqover-agent-0.1.0-SNAPSHOT.jar=include=io.reqover.example.webflux.auto -jar examples\webflux-sample\build\libs\webflux-sample-0.1.0-SNAPSHOT.jar
```

Then call:

```text
GET http://localhost:8080/auto/reactive/orders/42
GET http://localhost:8080/reqover/report
GET http://localhost:8080/reqover/report.html
```

## 핵심 가치

- 코드 변경 영향 범위를 정적 추측이 아니라 실제 실행 기록으로 확인합니다.
- 엔드포인트별 커버리지 리포트를 제공합니다.
- 운영 또는 시연 트래픽에서 어떤 코드 경로가 실제로 사용되는지 보여줍니다.
- WebFlux의 thread hop 상황에서도 coverage bucket이 요청을 따라가는지 검증합니다.

## 예상 사용자

- Spring 기반 백엔드 개발자
- 테스트 커버리지와 회귀 테스트 범위를 관리하는 팀
- 장애 또는 변경 영향 범위를 빠르게 좁혀야 하는 플랫폼 팀
- JaCoCo, SonarQube, Codecov를 이미 쓰고 있지만 요청 단위 관찰성이 부족한 팀

## 문서 구조

- [00. 프로젝트 기획](docs/00_project_plan.md)
- [01. 요구사항](docs/01_requirements.md)
- [02. 시스템 아키텍처](docs/02_architecture.md)
- [03. Phase 0 PoC 계획](docs/03_phase0_spike_plan.md)
- [04. 대회 로드맵](docs/04_competition_roadmap.md)
- [05. Repository setup](docs/05_repository_setup.md)
- [06. 제출 요구사항 정리](docs/06_submission_requirements.md)
- [07. 결과보고서 작성 초안](docs/07_result_report_outline.md)
- [08. Phase 0 MVP Status](docs/08_phase0_mvp_status.md)
- [09. Agent E2E Demo](docs/09_agent_e2e_demo.md)

## 초기 구현 방향

대회용 MVP는 JaCoCo 내부를 처음부터 깊게 수정하기보다, 경량 bytecode instrumentation으로 `ReqoverProbe.hit(classId, probeId)` 호출을 삽입하고 이 호출을 현재 요청의 coverage bucket으로 routing하는 방식부터 검증합니다.

이후 Phase 0 결과에 따라 두 갈래 중 하나를 선택합니다.

- JaCoCo probe strategy 확장: branch coverage와 리포팅 재사용성이 강하지만 내부 구조와 라이선스 관리가 어렵습니다.
- 경량 자체 계측 유지: 요청별 routing을 설계하기 쉽고 라이선스가 단순하지만 branch 정밀도와 JVM edge case를 직접 책임져야 합니다.

## 대회 제출물 목표

- 동작하는 Java agent 또는 instrumentation PoC
- Spring MVC 예제 앱
- Spring WebFlux 예제 앱
- 동시 요청 분리 데모
- 엔드포인트별 coverage 리포트
- README, 사용법, 아키텍처 문서
- 라이선스와 오픈소스 의존성 정리
- 결과보고서 DOCX/HWPX와 PDF
- SBOM
- YouTube 시연영상 URL

## License

Reqover 자체 작성 코드는 Apache License 2.0을 적용합니다.

JaCoCo fork 또는 내부 수정 파일이 생기는 경우 해당 모듈의 EPL-2.0 영향은 별도 검토합니다.
