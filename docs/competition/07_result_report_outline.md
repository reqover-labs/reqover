# 07. 결과보고서 작성 초안

> 보관 상태: 초기 기획 문서입니다. 구현 완료 범위와 공식 제출 문안은 공식 양식 기반 생성본 및 `13_result_report_draft.md`를 기준으로 검증합니다.

## 목표

결과보고서는 5페이지 이내 제한이 있으므로, 기술 백서 전체를 옮기는 방식이 아니라 심사위원이 빠르게 판단할 수 있는 핵심만 압축합니다.

핵심 메시지:

> Reqover는 Spring MVC/WebFlux에서 HTTP 요청과 관측된 method-entry 실행을 연결하고, 코드에서 관측 endpoint를 역조회하는 오픈소스 개발자 도구입니다.

## 1. 프로젝트 개요

### 프로젝트명

Reqover

### 프로젝트 소개

Spring MVC/WebFlux 애플리케이션에서 개별 HTTP 요청이 실행한 코드를 endpoint별 coverage bucket으로 분리해 보여주는 runtime coverage attribution 도구입니다.

### 공개 저장소 URL

GitHub repository 생성 후 기재합니다.

### 시연영상

YouTube 업로드 후 URL을 기재합니다.

## 2. 개발배경 및 목적

문제:

- 기존 coverage 도구는 전역 기준으로만 실행 여부를 기록합니다.
- 변경 영향 분석은 정적 호출 그래프, 경험, 수동 테스트에 의존하는 경우가 많습니다.
- 동시 요청이 많은 웹 서버에서는 전역 coverage만으로 요청별 실행 코드를 복원할 수 없습니다.
- WebFlux 환경에서는 요청 처리 중 thread가 바뀌므로 단순 ThreadLocal 방식도 깨질 수 있습니다.

목적:

- endpoint별로 실제 실행된 코드 경로를 보여줍니다.
- 코드 변경 시 영향을 받은 API 후보를 관측 기반으로 제시합니다.
- 개발·QA 시연 트래픽에서 관측된 실행 관계를 기록합니다.
- reactive thread hop 상황에서도 coverage attribution이 가능한지 증명합니다.

## 3. 개발환경

예상 개발환경:

- Language: Java 17+
- Build: Gradle
- Framework: Spring Boot 3.x
- Web stack: Spring MVC, Spring WebFlux
- Instrumentation: ASM 또는 Byte Buddy
- Coverage interop: JaCoCo 분석/리포트 연동 검토
- Test: JUnit 5, Spring Boot Test
- Report: JSON, HTML prototype

## 4. 시스템 구성 및 아키텍처

구성:

- `reqover-core`: coverage bucket, context, hit recording
- `reqover-instrumentation`: bytecode instrumentation
- `reqover-spring-mvc`: Servlet Filter/HandlerInterceptor adapter
- `reqover-spring-webflux`: WebFilter/Reactor Context adapter
- `reqover-report`: endpoint별 report 생성
- `examples`: MVC/WebFlux sample application

핵심 흐름:

1. 애플리케이션 class에 probe hit 호출을 삽입합니다.
2. HTTP 요청 시작 시 coverage bucket을 생성합니다.
3. probe hit이 발생하면 현재 요청 bucket으로 기록합니다.
4. 요청 종료 시 bucket을 flush합니다.
5. endpoint별 coverage report와 code -> endpoint 역조회 결과를 생성합니다.

## 5. 주요 기능

### 요청별 coverage bucket

각 HTTP 요청마다 coverage bucket을 생성하고 실행된 probe를 기록합니다.

### endpoint별 coverage report

`GET /orders/{id}`, `POST /payments`처럼 endpoint pattern 기준으로 coverage를 집계합니다.

### 코드 -> API 영향 범위 역조회

특정 class 또는 method가 관측된 endpoint 목록을 보여줍니다.

### 동시 요청 분리

동시에 들어온 여러 요청이 같은 thread pool과 공통 코드를 사용해도 각 요청의 coverage를 분리합니다.

### WebFlux thread hop 대응

Reactor Context 기반으로 요청의 논리 context를 전달해 thread가 바뀌어도 bucket을 유지합니다.

## 6. 구동 및 시연

시연 순서:

1. sample app 실행
2. Reqover agent 또는 instrumentation 활성화
3. MVC endpoint 동시 호출
4. JSON/HTML report 확인
5. WebFlux endpoint 호출 및 thread hop 로그 확인
6. 특정 service class가 어떤 endpoint에서 실행되었는지 역조회

시연에서 보여줄 핵심 화면:

- 요청별 bucket list
- endpoint별 관측 class/method 카드
- endpoint별 관측 thread 이름 집합
- code -> endpoint reverse lookup

## 7. 기대효과 및 활용분야

활용:

- 변경 영향 분석
- 회귀 테스트 범위 선별
- QA 집중 범위 도출
- endpoint별 테스트 갭 확인
- Spring 기반 서비스의 observability 확장

기대효과:

- "어디를 다시 테스트해야 하는가"를 실제 실행 기록으로 좁힙니다.
- 전역 coverage 숫자를 endpoint 중심의 실무 정보로 바꿉니다.
- 기존 JaCoCo/SonarQube 문화 위에 요청 단위 관찰성을 추가합니다.

## 8. 혁신성 및 차별성

차별점:

- 전역 coverage가 아니라 요청 단위 coverage attribution을 목표로 합니다.
- 테스트 케이스가 아니라 라이브 HTTP 요청을 기준으로 합니다.
- WebFlux의 thread hop 상황을 핵심 문제로 다룹니다.
- tracing의 context propagation 개념을 coverage buffer 귀속에 적용합니다.

기술 깊이:

- bytecode instrumentation
- probe routing
- request context lifecycle
- Reactor Context propagation
- coverage report aggregation

## 9. 한계점 및 향후 로드맵

현재 한계:

- Phase 0에서 instrumentation substrate를 결정해야 합니다.
- 초기 버전은 method 또는 line 수준 coverage에 집중할 수 있습니다.
- 모든 custom scheduler, raw thread, coroutine edge case를 즉시 지원하기 어렵습니다.
- 운영 상시 사용보다 개발/QA/staging 사용을 우선합니다.

향후 로드맵:

- JaCoCo 분석 엔진 연동
- branch coverage 강화
- Gradle/Maven plugin 제공
- HTML report polish
- CI/CD 연동
- trace id와 coverage report 연결

## 10. 소감 및 후기 방향

보고서에는 기술적 난도와 학습 내용을 짧게 넣습니다.

강조할 점:

- 기존 coverage 도구의 설계 철학과 한계를 분석했습니다.
- reactive context propagation을 coverage 문제에 적용했습니다.
- 오픈소스 라이선스와 SBOM 정리까지 포함해 실제 공개 가능한 개발자 도구를 목표로 했습니다.

## 5페이지 압축 전략

권장 분량:

- 1페이지: 프로젝트 개요, 개발배경, 목적
- 1페이지: 아키텍처와 핵심 흐름
- 1페이지: 주요 기능과 데모
- 1페이지: 차별성, 기대효과
- 1페이지: 한계, 로드맵, SBOM/라이선스 요약

붙임 SBOM과 AI 모델 정보는 별도 분량 제한이 없으므로 본문에서는 핵심만 언급합니다.
