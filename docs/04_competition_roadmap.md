# 04. 대회 로드맵

## 기준 일정

2026년 6월 30일 기준으로 확인한 2026 오픈소스 개발자대회 일정입니다.

공식 사이트: https://osscontest.kr/

공고 참고: https://www.nipa.kr/home/2-2/16815

- 참가 접수: 2026-06-15 ~ 2026-07-17 18:00
- 오리엔테이션: 2026-07-23
- 출품작 제출: 2026-07-18 ~ 2026-08-27 18:00
- 1차 평가: 2026-09-03 ~ 2026-09-04
- 멘토링: 2026-09-18 ~ 2026-10-09
- 출품작 검증: 2026-10-12 ~ 2026-10-28
- 2차 평가: 2026-11-04 ~ 2026-11-05
- 시상식: 2026-12-04

## 전략

대회 일정상 가장 중요한 것은 8월 27일까지 "설명은 거창하지만 동작은 약한 프로젝트"가 되지 않는 것입니다.

따라서 개발 전략은 다음과 같습니다.

1. 가장 위험한 probe routing을 먼저 검증합니다.
2. MVC 버전으로 확실히 동작하는 기준선을 만듭니다.
3. WebFlux 버전을 차별화 포인트로 얹습니다.
4. 리포트는 처음부터 화려하게 만들지 않고 JSON -> HTML 순서로 확장합니다.
5. 발표에서는 내부 구현보다 "기존 도구와의 차이"를 먼저 보여줍니다.

## Phase 0. 메커니즘 스파이크

기간: 2026-07-01 ~ 2026-07-07

목표:

- method entry probe 삽입
- `ReqoverProbe.hit` 호출 검증
- MVC ThreadLocal bucket routing
- WebFlux Reactor Context 실험
- substrate 결정

완료 조건:

- 최소 샘플 앱에서 요청별 hit 분리가 됩니다.
- Phase 1 구현 방향을 결정할 수 있습니다.

## Phase 1. MVC MVP

기간: 2026-07-08 ~ 2026-07-21

목표:

- Gradle multi-module skeleton
- `reqover-core`
- `reqover-instrumentation`
- `reqover-spring-mvc`
- MVC sample app
- 동시 요청 integration test

완료 조건:

- `GET /orders/{id}`와 `POST /payments` coverage가 분리됩니다.
- JSON 리포트가 생성됩니다.
- README만 보고 실행 가능합니다.

## Phase 2. 리포트와 역조회

기간: 2026-07-22 ~ 2026-08-04

목표:

- endpoint -> code report
- code -> endpoint reverse lookup
- HTML heatmap prototype
- demo data generator

완료 조건:

- 발표 첫 1분에 보여줄 수 있는 시각 자료가 생깁니다.
- "이 코드를 고치면 어느 API를 다시 테스트해야 하는가" 질문에 답할 수 있습니다.

## Phase 3. WebFlux 헤드라인 기능

기간: 2026-08-05 ~ 2026-08-15

목표:

- WebFlux WebFilter
- Reactor Context bucket propagation
- thread hop demo
- ThreadLocal clear invariant 검증

완료 조건:

- 같은 request id에서 여러 thread의 hit이 관측됩니다.
- thread가 바뀌어도 bucket이 유지됩니다.
- 제한 사항이 문서화됩니다.

## Phase 4. 대회 데모 3종

기간: 2026-08-16 ~ 2026-08-22

데모 1: 동시 요청 분리

- 기존 전역 coverage는 결과가 섞입니다.
- Reqover는 endpoint별 bucket으로 나눕니다.

데모 2: 코드 -> API 영향 범위

- 특정 service method를 선택합니다.
- 관측된 endpoint 목록을 보여줍니다.

데모 3: WebFlux thread hop

- 요청 하나가 여러 thread를 건너갑니다.
- coverage bucket은 같은 request id로 유지됩니다.

## Phase 5. 제출 정리

기간: 2026-08-23 ~ 2026-08-27

작업:

- README 보강
- 실행 영상 촬영
- 결과보고서 초안
- 결과보고서 DOCX/HWPX 작성
- 결과보고서 PDF 변환
- YouTube 시연영상 업로드
- SBOM 작성
- AI 모델 미탑재 또는 활용 정보 정리
- 라이선스 정리
- dependency 목록 정리
- known limitations 정리
- release tag 생성

완료 조건:

- 심사위원이 10분 안에 실행 흐름을 이해할 수 있습니다.
- repo 첫 화면에서 가치, 실행법, 데모가 보입니다.
- 라이선스 리스크가 숨겨져 있지 않습니다.
- 결과보고서는 5페이지 이내입니다.
- 결과보고서 파일과 PDF 파일이 모두 준비되어 있습니다.

## 1차 통과 후 멘토링 활용

기간: 2026-09-18 ~ 2026-10-09

집중할 것:

- 사용성 개선
- Gradle/Maven plugin 정리
- HTML report polish
- performance measurement
- 라이선스 및 보안 검증 대응
- 발표 스토리 압축

## 출품작 검증 대응

기간: 2026-10-12 ~ 2026-10-28

집중할 것:

- 기능테스트 재현성 확보
- demo command 단순화
- 라이선스 충돌 확인
- third-party dependency inventory 정리
- sample app 실행 오류 제거
- Windows, macOS, Linux 중 최소 1개 환경에서 확실한 재현 절차 작성
- public repository 유지 조건 확인
- 중복수혜 신고 필요 여부 확인

## 최종 발표 전략

발표는 문제 설명보다 데모를 먼저 보여주는 쪽이 유리합니다.

권장 흐름:

1. 20초: "기존 coverage는 실행 여부만 알지, 어느 요청이 실행했는지는 모릅니다."
2. 40초: 동시 요청 데모를 보여줍니다.
3. 60초: Reqover가 endpoint별로 coverage를 분리하는 화면을 보여줍니다.
4. 60초: WebFlux thread hop에서도 귀속이 유지되는 증거를 보여줍니다.
5. 90초: 아키텍처를 설명합니다.
6. 60초: 오픈소스 사용성과 확장성을 설명합니다.
7. 30초: 한계와 다음 계획을 솔직하게 말합니다.

## 위험 관리

### Probe routing 실패

대응:

- method-level 자체 계측으로 MVP를 축소합니다.
- line/branch coverage는 후속 계획으로 둡니다.

### WebFlux 전파 불안정

대응:

- MVC를 안정 기능으로 제출합니다.
- WebFlux는 experimental headline으로 명확히 표시합니다.

### 데모가 추상적임

대응:

- before/after 화면을 나란히 둡니다.
- endpoint별 색상과 heatmap을 사용합니다.
- "이 코드 수정 시 다시 테스트할 API"라는 실무 질문으로 연결합니다.

### 라이선스 리스크

대응:

- JaCoCo 내부 수정 여부를 빠르게 결정합니다.
- fork한 파일과 자체 작성 파일을 모듈 단위로 분리합니다.
- NOTICE와 LICENSE를 제출 전에 정리합니다.

## 제출 체크리스트

- GitHub repository 공개
- README 작성
- 설치/실행 방법 작성
- 예제 앱 포함
- 데모 영상 포함
- YouTube 시연영상 URL
- 결과보고서 작성
- 결과보고서 PDF 변환본
- SBOM 작성
- AI 모델 활용 정보 또는 미탑재 명시
- 라이선스 명시
- 의존성 목록 명시
- known limitations 명시
- issue template 또는 contribution guide 작성
