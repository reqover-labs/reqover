# Reqover README 및 조직 프로필 개편 설계

## 상태

- 설계 방향 승인: 2026-07-31
- 대상 저장소:
  - `reqover-labs/reqover`
  - `reqover-labs/.github`
- 게시 방식: 저장소별 전용 브랜치와 Draft PR

## 목표

Reqover를 처음 방문한 개발자가 첫 화면에서 제품의 차별점과 현재 성숙도를 이해하고, 실제 실행 화면을 확인한 뒤, Windows 또는 macOS/Linux에서 검증 가능한 데모를 실행할 수 있도록 README를 개편한다.

조직 프로필은 Reqover Labs의 미션과 대표 프로젝트를 짧게 설명하고, 메인 저장소의 Demo, Docs, Issues, Contributing, Security 경로로 방문자를 안내하는 허브로 만든다.

## 비목표

- Reqover를 Maven Central이나 GitHub Releases에 배포하지 않는다.
- 실제 구현되지 않은 line/branch coverage, 운영 환경 지원, 완전한 변경 영향 분석을 약속하지 않는다.
- 리포트 UI를 새로 디자인하거나 제품 기능을 확장하지 않는다.
- 대회 등록·선정처럼 공개 근거가 없는 상태를 확정적으로 표현하지 않는다.
- README 개편과 무관한 문서 전체를 한꺼번에 정리하지 않는다.

## 참고 README 조사 결과

2026-07-31 15:22 KST GitHub 공개 데이터 기준 전체 스타 상위 5개는 다음과 같다. 스타 수는 조회 시점의 스냅샷이다.

| 순위 | 저장소 | Stars |
| ---: | --- | ---: |
| 1 | `codecrafters-io/build-your-own-x` | 533,147 |
| 2 | `sindresorhus/awesome` | 490,820 |
| 3 | `public-apis/public-apis` | 453,690 |
| 4 | `freeCodeCamp/freeCodeCamp` | 453,266 |
| 5 | `EbookFoundation/free-programming-books` | 393,383 |

상위 저장소는 대부분 교육·큐레이션 카탈로그이므로 구조를 그대로 복제하지 않는다. 다음 패턴만 Reqover에 맞게 적용한다.

- `freeCodeCamp`: 짧은 랜딩 구조, 명확한 가치 제안과 목적성 있는 CTA
- `awesome`과 `build-your-own-x`: 빠르게 훑을 수 있는 일관된 탐색 구조
- `free-programming-books`: 초보 기여자를 위한 문서와 다음 행동 연결
- 공통 패턴: README를 모든 세부사항의 저장소가 아니라 상세 문서로 연결하는 탐색 허브로 사용

광고·후원 블록의 전면 배치, stars/forks 배지, 과도한 배지 벽, 복수 H1, 거대한 단일 목차는 사용하지 않는다.

## 메인 README 정보 구조

`README.md`는 한국어 본문을 유지하되, 첫 화면에 영문 태그라인을 병기한다. 별도의 번역 파일을 만들어 유지보수 범위를 두 배로 늘리지 않는다.

첫 화면은 다음 순서로 구성한다.

1. `# Reqover`
2. 영문 태그라인과 한국어 한 문장 가치 제안
3. build, Apache-2.0, Java 17 target, JDK 21 build 배지
4. `Demo · Quickstart · Documentation · Contributing` 내부 링크
5. 실제 실행에서 촬영한 대표 리포트 화면
6. 현재 상태를 설명하는 간결한 MVP 안내

본문은 다음 사용자 여정을 따른다.

1. **Why Reqover**
   - 전통적인 커버리지가 “어떤 코드가 실행됐는가”를 보여준다면 Reqover는 관측된 HTTP 요청과 실행 메서드의 관계를 보완한다.
   - JaCoCo 대체재가 아니라 요청 단위 귀속을 추가하는 개발·QA 도구로 설명한다.
2. **See It in Action**
   - MVC 요청 분리, WebFlux thread hop, code-to-endpoint 역조회 화면을 설명한다.
3. **Quickstart**
   - JDK 21, Git, 사용 가능한 포트를 전제 조건으로 명시한다.
   - Java agent 자동 계측 데모를 먼저 배치한다.
   - Windows PowerShell과 macOS/Linux 명령을 각각 복사 가능한 형태로 제공한다.
   - 성공 기준으로 endpoint pattern, 클래스명, thread 이름, reverse index를 명시한다.
4. **How It Works**
   - Instrumentation → Attribution → Report의 세 단계와 기존 Mermaid 아키텍처 문서를 연결한다.
5. **Compatibility and Project Status**
   - 빌드 JDK 21, Java 17 bytecode target, Spring Boot 3.3.5, CI JDK 21을 표로 구분한다.
   - 외부 artifact가 배포되지 않은 source-built MVP임을 명시한다.
6. **Modules and Documentation**
   - 모듈 설명을 짧은 표로 바꾸고 상세 설계 문서로 연결한다.
7. **Limits and Safe Use**
   - method-entry 기준, in-memory snapshot, 관측 결과의 하한 의미, 미관측이 무영향을 뜻하지 않음을 설명한다.
   - sample report endpoint에는 인증이 없으므로 공개 네트워크에 노출하지 말라고 경고한다.
   - 현재 개발·QA·staging 사용을 우선한다고 명시한다.
8. **Contributing, Team, License**
   - Issues, CONTRIBUTING, Code of Conduct, Security Policy, 팀 링크와 정확한 라이선스 범위를 연결한다.

## 실행 화면 설계

이미지는 생성형 그래픽이 아니라 로컬에서 Reqover를 직접 실행한 브라우저 화면만 사용한다. 모든 자산은 `docs/assets/`에 저장하고 설명적인 alt text를 제공한다.

| 파일 | 보여줄 내용 | 목적 |
| --- | --- | --- |
| `docs/assets/reqover-mvc-request-attribution.png` | `GET /orders/{id}`와 `POST /payments` 카드, 양쪽에 나타나는 `SharedValidator` | 요청별 코드 경로 분리 |
| `docs/assets/reqover-webflux-thread-hop.png` | reactive endpoint와 여러 thread chip | thread hop 이후에도 유지되는 귀속 |
| `docs/assets/reqover-code-to-endpoint-index.png` | `SharedValidator`에서 두 endpoint로 이어지는 reverse index | 변경 후 우선 재검증할 관측 API 탐색 |

촬영 기준은 다음과 같다.

- JDK: `JAVA_HOME`으로 선택한 JDK 21
- 브라우저 viewport: 1440px 너비
- GitHub에서 읽기 쉬운 범위로 필요한 섹션만 촬영
- 개인 정보, 로컬 절대경로, 토큰, 환경 변수 값은 화면에 포함하지 않음
- 동일한 요청 데이터로 JSON 성공 조건과 HTML 화면을 함께 검증
- MVC와 WebFlux 프로세스는 서로 다른 포트를 사용하고 촬영 후 종료

실행 및 촬영 과정을 `docs/16_readme_demo_capture.md`에 기록한다. 이 문서에는 환경, 명령, 호출한 endpoint, 기대 결과, 포트 충돌·JDK·프로세스 종료 문제의 해결 방법을 포함한다.

## 조직 프로필 설계

`reqover-labs/.github/profile/README.md`는 메인 README를 복제하지 않고 다음 내용만 유지한다.

1. Reqover Labs 미션과 영문 한 줄 태그라인
2. 2026 오픈소스 개발자대회 출품을 목표로 개발 중인 MVP라는 상태 안내
3. 요청별 귀속, Java agent, 양방향 리포트, WebFlux context 전파의 네 가지 핵심 기능
4. 대표 프로젝트 상태와 `README · Demo · Docs · Issues` 링크
5. 기여 가이드, Code of Conduct, Security Policy
6. Maintainers의 GitHub와 이름으로 표시되는 LinkedIn 링크
7. Reqover 자체 작성 코드에 한정한 Apache-2.0 라이선스 안내

조직 프로필에는 설치 명령, 모듈 목록, 성능 수치, 상세 제한사항을 중복하지 않는다.

## 콘텐츠 정확성 원칙

- “JDK 21 권장” 대신 “빌드에는 JDK 21 필요”라고 쓴다.
- Java 17 배지는 bytecode target이라는 의미를 함께 설명한다.
- release, tag, package, `maven-publish` 설정이 없으므로 설치 가능한 배포 artifact처럼 표현하지 않는다.
- “JaCoCo 대체”가 아니라 “표준 커버리지에 요청 단위 attribution을 보완”한다고 표현한다.
- code-to-endpoint 결과는 관측된 실행 관계이며 완전한 정적 영향 분석이 아니다.
- “모든 코드가 Apache-2.0” 대신 Reqover 자체 작성 코드의 라이선스 범위를 명시한다.
- 대회 등록 또는 선정 근거가 확인되기 전에는 “출품을 목표로 개발 중”이라고 표현한다.

## 기준 테스트에서 발견된 Windows 문제

JDK 21에서 `.\gradlew.bat clean test`를 실행하면 제품 검증 전에 기준 테스트가 깨끗하게 통과하지 않는다. 두 `AgentSpringE2ETest`가 `stop()`에서 임시 로그를 삭제할 때 Windows 파일 잠금으로 `FileSystemException`을 발생시킨다.

README 완료를 검증하려면 이 정리 단계의 플랫폼 안정성 문제를 최소 범위로 수정한다.

- production 코드는 변경하지 않는다.
- Spring sample process 종료를 확인한 뒤 Windows의 지연된 파일 핸들 해제를 견디도록 임시 로그 삭제를 제한적으로 재시도한다.
- 삭제 실패가 영구적이면 원래 예외를 숨기지 않고 테스트를 실패시킨다.
- MVC와 WebFlux의 기존 HTTP 응답 및 report assertion은 그대로 유지한다.
- 수정 후 전체 테스트와 해당 E2E 테스트를 별도로 재실행한다.

이 수정은 README 기능 확장이 아니라 실제 실행 결과를 신뢰할 수 있게 만드는 검증 기반 작업으로 취급한다.

## 검증

메인 저장소는 다음 순서로 검증한다.

1. `.\gradlew.bat clean test --no-daemon --console=plain`
2. `.\gradlew.bat :reqover-agent:test --tests io.reqover.agent.AgentSpringE2ETest --no-daemon --console=plain`
3. `.\gradlew.bat build cyclonedxBom --no-daemon --console=plain`
4. MVC와 WebFlux sample을 실제 Java agent와 함께 실행
5. PowerShell에서 JSON report의 endpoint, 클래스, thread, reverse index 확인
6. 브라우저에서 HTML report를 열어 세 이미지를 촬영
7. README의 모든 로컬 링크와 이미지 경로 검사
8. 민감정보, 로컬 절대경로, 존재하지 않는 release/package 링크가 없는지 검사

조직 프로필은 모든 링크가 공개 접근 가능한지 확인하고, 메인 README의 최종 anchor와 일치하는지 검사한다.

## 커밋과 게시

메인 저장소 커밋은 다음 책임으로 분리한다.

1. 승인된 설계 문서
2. Windows E2E 로그 정리 안정화
3. 재현 문서와 실제 실행 스크린샷
4. 메인 README 개편

조직 프로필은 별도 저장소에서 하나의 문서 커밋으로 만든다.

두 저장소 모두 `agent/readme-product-refresh` 브랜치를 사용하고, 검증 결과와 촬영 근거를 포함한 Draft PR을 생성한다. `.env`, API 키, 토큰, 로컬 로그와 빌드 산출물은 커밋하지 않는다.

모든 새 커밋은 `TaeHuiKKIM <1043tae@naver.com>` 단독 저자로 기록한다. `Claude`, `Anthropic`, AI bot 또는 다른 자동화 도구를 `Co-authored-by` trailer나 contributor 문구로 추가하지 않는다.
