[English](ROADMAP.md) | **한국어**

# 앞으로의 방향

현재 사용법은 [문서 목차](docs/README.ko.md), 릴리스별 변경은
[CHANGELOG](CHANGELOG.md)를 기준으로 확인합니다. 아래는 우선순위이며 출시 약속이 아닙니다.

## 지금 집중할 일

0.4.0부터 요청 진단·오프라인 대시보드·검토 가능한 테스트 초안·기록 비교를 제공합니다.
다음은 허용 입력의 마스킹, 메서드별 시간, k6 실행·결과 가져오기 흐름을 검토합니다.
원래 요청의 완전한 재현이나 운영 APM을 이미 지원한다고 소개하지 않습니다.

라이브러리는 `io.github.reqover-labs`로 Maven Central에 배포했고 agent·CLI는
GitHub Release에서 받습니다. 이제 예제 밖의 실제 앱에서 설치와 효용을 검증하고
사용 가이드를 유지합니다. [호환성 정책](docs/20_versioning_and_compatibility.ko.md)이 적용됩니다.

저장소 SPI 계약 테스트, 보관 정책과 전체 endpoint 집계는 구현돼 있습니다.
서로 다른 endpoint·probe가 늘 때의 메모리 비용, 자체 저장소, 전체 코드 관계와
보관된 시간 구간의 차이를 큰 기록에서 확인하는 것이 다음 과제입니다.

## 현재 릴리스에 들어간 것

- Maven Central 라이브러리 6개와 별도 agent·CLI 실행 파일.
- 선택적 접근자·참조 기록, 여러 context의 파일 내보내기 누적과 전체 endpoint 집계.
- 대시보드, 비활성화된 테스트 초안, 보관 요약 비교와 opt-in CI artifact.
- 0.4.1의 WebFlux catch-all route 패턴 처리 수정.
- 0.4.2의 텍스트 입력 중 `/` 검색 단축키 포커스 간섭 수정.

## 다음 후보

- 동시성·GC·시작 시간·참조 probe·집계 비용을 포함하는 실제 앱 오버헤드 측정.
- 기록과 분석을 묶는 Gradle/Maven 플러그인.
- Servlet async worker의 자동 요청 귀속. 현재는 미지원 구간.
- 재시작 이후에도 기록을 유지하는 `CoverageStore`. 현재 기본 구현은 메모리 저장소.

## 이후 검토할 일

- [JaCoCo 상호운용](docs/14_jacoco_interop_decision.md)과 라이선스·정밀도 판단.
- 줄·분기 단위 정밀도. probe 비용을 먼저 검증.
- `UnitScope`로 가능한 비HTTP 작업에 자동 어댑터 제공.

## 목표가 아닌 것

- JaCoCo의 대체 또는 운영 환경의 상시 APM.
- 호스팅·여러 사용자용 대시보드 서비스. 로컬 HTML 대시보드는 지원.
- 후보가 없다는 이유로 변경이 안전하다고 보증하기. 관측되지 않은 코드는 미확인.

우선순위와 의견은 [Discussions](https://github.com/reqover-labs/reqover/discussions)에서
논의합니다. 유지관리 규칙은 [GOVERNANCE](GOVERNANCE.md)를 참고합니다.
