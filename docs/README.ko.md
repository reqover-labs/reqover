[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# 문서 목차

현재 사용법은 **0.4.1** 기준입니다. 라이브러리는 Maven Central의
`io.github.reqover-labs`, agent·CLI 실행 파일은
[GitHub 릴리스](https://github.com/reqover-labs/reqover/releases/tag/v0.4.1)에서 받습니다.
Java 패키지는 `io.reqover.*` 그대로입니다. 루트 빠른 시작은 릴리스 태그를 고정하고,
`main`에는 이후 변경이 들어갈 수 있습니다.

## 먼저 읽을 문서

| 하고 싶은 일 | 안내 |
| --- | --- |
| Spring Boot 앱에 설치하기 | [연동과 설정](17_integration_guide.ko.md) |
| 느리거나 실패한 요청 찾기 | [요청 진단](24_request_diagnostics.ko.md) |
| 실행 관계를 보고 CI 근거 남기기 | [대시보드와 CI](26_dashboard_and_ci.ko.md) |
| 검토한 JSON·비활성화 JUnit 만들기 | [테스트 초안](27_test_case_drafts.ko.md) |
| 보관된 HTTP 시간·상태 요약 비교 | [기록 비교](28_recording_comparison.ko.md) |
| 코드 diff·PR 재테스트 후보 확인 | [CI 영향 분석](18_ci_impact_analysis.ko.md) |
| 오류·데이터 상한 이해 | [트러블슈팅](25_diagnostics_troubleshooting.md) |

CLI `diff`는 코드 관계를, 대시보드 비교는 보관 HTTP 통계를 비교합니다. 부하 시험을
실행하지 않습니다. 초안은 원래 입력값을 재현하지 않으며, 전체 endpoint 집계와
보관 상세·JSON 내보내기 구간은 서로 다른 범위입니다.

## 모듈별 README

| 모듈 | 역할 |
| --- | --- |
| [Agent](../reqover-agent/README.ko.md) | 기록 옵션과 실행 JAR |
| [Starter](../reqover-spring-boot-starter/README.ko.md) | Boot 연결, 선택적 리포트와 내보내기 |
| [MVC](../reqover-spring-mvc/README.ko.md) / [WebFlux](../reqover-spring-webflux/README.ko.md) | 요청 귀속과 측정 경계 |
| [Report](../reqover-report/README.ko.md) | 대시보드·진단·파일 형식·분석 |
| [CLI](../reqover-cli/README.ko.md) / [Action](../.github/actions/impact/README.ko.md) | 오프라인 명령과 CI 기본값 |
| [Core](../reqover-core/README.ko.md) / [Instrumentation](../reqover-instrumentation/README.ko.md) | 확장 API·저장소·ASM 기록 |
| [Examples](../examples/README.ko.md) | 로컬 데모와 합성 지연·실패 요청 |

## 설계와 검증 근거

- [현재 아키텍처](02_architecture.ko.md), [버전·호환성 정책](20_versioning_and_compatibility.ko.md).
- [선행 도구와 차이](19_prior_art.ko.md), [JaCoCo 연동 판단](14_jacoco_interop_decision.md).
- [메서드 진입 측정](15_performance_results.ko.md): 당시 조건의 벤치마크이며 현재 버전 전체 비용은 아님.
- [보안 정책](../SECURITY.md), [SBOM](../sbom/README.md), [OSV 보완 이력](29_osv_dependency_remediation.ko.md). 기한 있는 예외는 라이브러리 패치와 구분.
- [문서 최신화 판단 기록](31_documentation_refresh.md).

## 과거 자료

다음 문서는 당시 판단을 남긴 자료이며 현재 설치·지원 기능의 기준은 아닙니다.

- [최초 기획](00_project_plan.md), [요구사항](01_requirements.md), [Phase 0 계획](03_phase0_spike_plan.md).
- [초기 MVP 결과](08_phase0_mvp_status.md), [초기 E2E](09_agent_e2e_demo.md), [초기 데모](10_demo_script.md), [최초 화면 촬영 기록](16_readme_demo_capture.md).
- [성능 검증 개선 계획](23_performance_validation_plan.ko.md), [리뷰 보완 이력](30_review_corrections.md).
- [대회 기록](competition/README.md): v0.2.0 영상·보고서 초안을 포함하며 당시 버전·측정·날짜를 유지.

현재 동작은 위 사용 가이드와 [CHANGELOG](../CHANGELOG.md)를 기준으로 확인합니다.
