# 18. 2026 오픈소스 개발자대회 최종 제출 계획

기준일은 2026-08-10, 공식 제출 마감은 2026-08-27 18:00(KST)입니다. 내부 마감은 장애 대응 여유를 위해 2026-08-26 18:00로 둡니다.

## 제출 범위 고정

- 대표 프로젝트: `reqover-labs/reqover`
- 한 문장: Spring MVC/WebFlux 요청 단위 method-entry 실행 귀속 도구
- 핵심 증거: Java Agent 자동 계측, MVC 요청 분리, WebFlux thread-hop 귀속, endpoint-to-code 및 code-to-endpoint report
- 제외: 과거 OpenAPI/npm Reqover CLI와 기존 `reqover` 조직의 legacy 저장소
- 주장하지 않는 범위: line/branch coverage, 완전한 정적 영향 분석, production 상시 운영 적합성

## 일정과 완료 조건

| 기간 | 작업 | 완료 조건 |
| --- | --- | --- |
| 8/10~8/11 | 코드·패키징 안전성 보강 | JDK 17/21 clean build, agent E2E, 충돌·기본 옵션·port 실패 경로 검증 |
| 8/11~8/12 | SBOM·라이선스·보안 | CycloneDX JSON 고정, OSV 0건, agent JAR 고지 포함, secret scan 확인 |
| 8/12~8/13 | 문서·도입 경로 | README 5분 Quickstart, integration guide, 한계·지원표·재현 명령 일치 |
| 8/13~8/14 | release candidate | GitHub PR CI green, `v0.1.0` tag와 Release, checksum·SBOM 첨부 |
| 8/14~8/17 | 공식 결과보고서 | 공식 A4 양식, 본문 5쪽 이내, 필수 SBOM 붙임, DOCX/PDF 전 페이지 검수 |
| 8/17~8/20 | 3분 영상 | 최종 tag 소스로 촬영, 3분 이내, 자막·음성·코드 글자 가독성 확인 |
| 8/20~8/22 | 제3 환경 재현 | 새 clone/JDK 17·21에서 Quickstart와 report 재현, 링크 비로그인 확인 |
| 8/23~8/25 | 제출 dry run | 파일명·URL·중복수혜·팀 기여 입력, 포털 입력값 교차검수 |
| 8/26 18:00 | 내부 제출 마감 | 제출 완료 화면과 확인 메일 보관, 제출본 hash 기록 |
| 8/27 18:00 | 공식 마감 | 긴급 수정 외 작업 금지 |

## 엔지니어링 체크

- [x] no-option agent는 애플리케이션을 깨지 않고 계측을 비활성화한다.
- [x] JDK/ASM/Reqover runtime package는 include로도 계측할 수 없다.
- [x] class ID 충돌 시 기존 metadata를 보존하고 충돌 class 계측을 중단한다.
- [x] MVC 동시 요청과 WebFlux 동시 요청이 서로 오귀속되지 않는다.
- [x] WebFlux 자동 context propagation의 JVM 전역 효과와 opt-out을 문서화한다.
- [x] demo script는 loopback만 사용하고 port 충돌·child 조기 종료를 명시적으로 실패시킨다.
- [x] release workflow는 JDK 17/21과 OSV 검사를 통과한 현재 `main` tag만 배포한다.

## 오픈소스·공급망 체크

- [x] `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md`와 agent JAR 내 고지가 일치한다.
- [x] ASM BSD-3-Clause 전문을 저장소·agent JAR·release bundle에 포함한다.
- [x] 최종 `sbom/reqover.cdx.json`을 schema 검증하고 공식 SBOM 표와 일치시킨다.
- [x] dependency vulnerability scan 결과와 실행 시각을 증거로 남긴다.
- [x] Dependabot alerts/updates, private vulnerability reporting, secret scanning을 가능한 범위에서 활성화한다.
- [x] changelog, source JAR, checksum, GitHub Release가 최종 tag와 일치한다.

## 결과보고서 체크

- [x] 공식 안내 페이지를 제거하고 공식 표·A4·여백을 보존한다.
- [x] 결과보고서 본문은 최대 5쪽이다.
- [x] SBOM 붙임에는 OSS명, 버전, 라이선스, 공식 URL, 사용 목적이 있다.
- [x] 성능 수치는 최종 runtime candidate, 동일 endpoint, 원시 표본과 환경이 있을 때만 사용한다.
- [ ] 팀원별 역할과 실제 의사결정을 확인받는다.
- [x] AI 모델 붙임은 제외하고 개발 과정의 Codex 보조 범위만 본문 한 문장으로 기재한다.
- [ ] Word/맑은고딕 환경에서 DOCX와 PDF를 열어 한글·표·그림·페이지를 전부 확인한다.

## 3분 영상 구성

| 구간 | 화면과 메시지 |
| --- | --- |
| 0:00~0:15 | 일반 집계 coverage만으로는 어떤 요청이 코드를 실행했는지 알기 어렵다는 문제 |
| 0:15~0:45 | 완성된 endpoint-to-code report를 먼저 제시 |
| 0:45~1:15 | MVC 두 endpoint와 공유 method가 요청별로 분리되는 장면 |
| 1:15~1:40 | code-to-endpoint 역방향 index |
| 1:40~2:15 | WebFlux의 여러 thread에서도 한 endpoint bucket으로 유지되는 장면 |
| 2:15~2:40 | `-javaagent` 명령과 manual probe가 없는 sample 코드 |
| 2:40~3:00 | 테스트·Apache-2.0·SBOM·현재 method-entry 한계와 GitHub URL |

## 팀 확인이 필요한 입력

다음 정보는 코드나 Git 기록만으로 확정하지 않습니다.

- 접수번호와 팀명
- 학생/일반 참가부문, 자유/지정 과제유형
- 접수 당시 프로젝트명·소개와 현재 Java 프로젝트의 일치 여부
- 팀원별 역할·기여 최종 확인
- 정부지원·정부 대회 수상·개발지원금 등 중복수혜 여부
- 최종 YouTube URL

## AI 보조 표기

본문에는 다음 문장만 사용합니다. 사용 비율을 임의로 만들지 않습니다.

> 상용 생성형 AI(Codex)는 일부 코드·테스트·문서의 초안 작성 및 검토 보조에 활용했으며, 팀이 요구사항을 결정하고 결과를 검증·수정하여 최종 반영했다. 출품작에는 AI 모델이나 외부 추론 API가 포함되지 않는다.
