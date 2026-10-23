[English](44_ci_retest.md) | **한국어** | [문서 목차](README.ko.md)

# 사이드 스크롤과 CI 재테스트

2026-10-23. PR #53의 미출시 기능이다. [계획](42_ci_retest_plan.ko.md)과 [출시 준비](43_release_readiness.ko.md)를 따른다.

## 화면 보완

- 기본·다크 테마에서 얇은 스크롤바와 hover 색을 지정했다. 스크롤바를 숨기지 않는다.
- 낮은 창에서도 메뉴가 잘리지 않도록 사이드바에 내부 세로 스크롤을 둔다.
- 선택 메뉴에 3px 표식과 배경색을 함께 사용한다. 메뉴 최소 높이는 44px다.
- 모바일에서는 기존 줄바꿈 메뉴를 유지하고 sidebar 내부 고정 스크롤을 해제한다.
- 동작 줄이기 설정에서는 메뉴 전환 효과를 끄고, 강제 색상 모드에서는 시스템 스크롤 색을 쓴다.

실제 시각 검증은 아직 수행하지 못했다. CSS와 생성 HTML 검사는 화면의 가독성·레이아웃을 대신하지 않는다.

## 저장한 시나리오를 PR에서 다시 검사

새 [retest Action](../.github/actions/retest/README.ko.md)은 source-built CLI와 테스트 앱 JAR를 받는다.
앱 기동·준비 확인·순차 GET/HEAD·상태/시간 조건 판정·JSON/HTML·정리를 한 단계로 연결한다.
기존 impact 분석 Action은 그대로 사용한다.

실행이 실패해도 고정 형식 Job Summary를 남기고, opt-in artifact를 업로드한 다음 실패 gate를 적용한다.
원격 서버·인증정보·동시 부하·자동 재현은 추가하지 않았다.

기준 실행 파일을 명시하면 기존 run-compare로 comparison.html을 생성한다.
다른 환경·origin·검사 조건은 기존 경고로 표시한다. 자동으로 다른 PR의 파일을 내려받지 않는다.

## 실행 확인 기록

- Python 통합 검사: 동의 없음·정상·상태 불일치·client 시간 초과·앱 기동 실패·잘못된 baseline·기존 출력 폴더 보호의 7개 경로.
- 각 경로에서 종료코드·summary.json·Job Summary·Action 출력·서버 종료를 확인한다.
- 현재 GitHub SHA를 시나리오 복사본에 넣어 실행 revision을 남긴다.
- GitHub의 실제 workflow 실행 결과는 완료 후 이 기록 또는 PR 설명에 연결한다.
- Node 48개와 Java report 79개·CLI 25개 회귀 검사 통과.
- 갱신된 runner로 GitHub 출력·Job Summary·revision 복사와 기존 출력 보호를 포함한 7개 통합 경로 통과.
- 저장된 MVC 예제 시나리오 3건도 실행해 PASS 3건·실패/미실행 0건 확인. 기대한 503은 테스트 통과로 처리.
- 최신 CLI로 대규모 예제 HTML을 재생성했다. 실제 화면을 열어 검증한 것은 아니다.
- release workflow에도 Node 전체와 runner 통합 검사를 연결했다. 실제 태그/릴리스 실행은 하지 않았다.

## 예약과 권한

새 workflow는 PR·main push·수동 실행에 연결했다. 예약은 Action README의 UTC cron 예제를 팀이 복사해 활성화한다.
concurrency와 job timeout을 두며 fork PR에 secrets나 PR 쓰기 권한을 제공하지 않는다.
artifact는 예제의 합성 검사 정보만 자동 opt-in하고, 일반 Action 사용은 기본 비활성이다.
서버 로그는 artifact 대상에서 제외한다.
