# 00. 프로젝트 기획

## 한 줄 설명

Reqover는 Spring 애플리케이션에서 "어느 HTTP 요청이 어느 코드를 실행했는지"를 요청 단위로 기록하는 runtime coverage attribution 도구입니다.

## 문제 정의

일반적인 코드 커버리지는 전역 기준입니다. JaCoCo 같은 도구는 특정 line 또는 branch가 실행되었는지를 잘 알려주지만, 그 실행이 어떤 API 요청 때문에 발생했는지는 기록하지 않습니다.

백엔드 팀이 실제로 알고 싶은 질문은 전역 커버리지 숫자보다 더 구체적인 경우가 많습니다.

- `OrderService.calculate()`를 수정하면 어떤 API가 영향을 받는가?
- `POST /payments`는 실제로 어떤 service, repository, branch를 지나가는가?
- 라이브 트래픽 기준으로 어떤 엔드포인트의 분기 경로가 아직 한 번도 실행되지 않았는가?
- 동시 요청이 많은 환경에서 특정 코드 실행을 요청별로 분리해서 볼 수 있는가?

기존 커버리지 도구만으로는 이 질문에 직접 답하기 어렵습니다. 전역 probe 배열에는 "켜졌다"는 사실만 남고 "누가 켰는지"는 사라지기 때문입니다.

## 제안하는 해결책

Reqover는 coverage probe가 점등될 때 그 신호를 전역 저장소에만 남기지 않고, 현재 실행 중인 요청의 coverage bucket으로 routing합니다.

요청이 끝나면 bucket을 집계해 다음 정보를 만듭니다.

- 엔드포인트 -> 실행 코드 목록
- 코드 -> 관측된 엔드포인트 목록
- 엔드포인트별 line coverage
- 엔드포인트별 branch coverage 후보
- 전체 coverage와 요청별 coverage의 비교

## 핵심 차별점

### 1. 요청 단위 귀속

기존 coverage가 "코드 중심"이었다면 Reqover는 "동작 단위 중심"입니다. API, 메시지, job, test method 같은 unit of work를 기준으로 coverage를 나눕니다.

### 2. 라이브 트래픽 기반

단위 테스트나 통합 테스트뿐 아니라, 실제 HTTP 요청 또는 시연 트래픽에서 발생한 coverage를 수집하는 것을 목표로 합니다.

### 3. WebFlux thread hop 대응

Spring MVC에서는 하나의 요청이 보통 하나의 thread에서 처리되므로 ThreadLocal 기반 bucket이 작동합니다. 그러나 WebFlux에서는 요청 처리 도중 thread가 바뀔 수 있습니다.

Reqover의 헤드라인 기능은 Reactor Context와 Micrometer Context Propagation 계열의 장치를 이용해 coverage bucket이 thread가 아니라 요청의 논리 흐름을 따라가게 하는 것입니다.

## 대회 관점의 포지셔닝

Reqover는 완성형 서비스가 아니라 개발자 도구입니다. 오픈소스 개발자대회에서 좋은 평가를 받을 수 있는 지점은 다음과 같습니다.

- 개발자가 직접 가져다 쓸 수 있는 라이브러리/도구 형태입니다.
- 테스트, 검증, 변경 영향 분석이라는 실무 문제를 풉니다.
- bytecode instrumentation, coverage analysis, context propagation이 결합되어 기술 깊이가 있습니다.
- 데모를 잘 구성하면 기존 전역 coverage와의 차이가 빠르게 보입니다.

## MVP 범위

MVP는 다음 질문에 답할 수 있으면 성공입니다.

- 두 개 이상의 HTTP 요청이 동시에 들어와도 coverage bucket이 섞이지 않는가?
- 한 요청이 실행한 method 또는 line을 endpoint 단위로 볼 수 있는가?
- MVC에서는 ThreadLocal 기반 routing이 안정적으로 되는가?
- WebFlux에서는 thread가 바뀌어도 bucket이 유지되는가?
- 결과를 사람이 이해할 수 있는 HTML 또는 JSON 리포트로 볼 수 있는가?

## MVP에서 제외할 것

초기 MVP에서는 다음을 욕심내지 않습니다.

- 모든 JVM 언어와 edge case 완전 지원
- JaCoCo와 동일한 수준의 branch 분석 정확도
- 운영 환경 장기 수집 안정성
- 분산 시스템 전체 trace와의 완전 통합
- SonarQube, Codecov 공식 연동
- MC/DC coverage

## 성공 기준

대회 제출 전 최소 성공 기준은 다음과 같습니다.

- Spring MVC 예제 앱에서 요청별 coverage 분리가 동작합니다.
- Spring WebFlux 예제 앱에서 thread hop이 있는 시나리오를 보여줍니다.
- 동시 요청 데모에서 전역 coverage 방식과 Reqover 방식의 차이를 시각적으로 설명할 수 있습니다.
- 소스코드, README, 실행 방법, 데모 영상 시나리오, 라이선스 문서가 정리되어 있습니다.

## 이름

Reqover는 Request와 Coverage의 합성어입니다.

대체 이름 후보는 남겨두되, 현재 문서와 발표에서는 Reqover를 기본 이름으로 사용합니다.

