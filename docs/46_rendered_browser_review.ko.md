[English](46_rendered_browser_review.md) | **한국어** | [문서 목차](README.ko.md)

# PR #53 실제 브라우저 검수와 작업 메시지 수정

2026-10-23 (KST). `7f4762b`에서 시작했으며 fetch 후 원격과 일치하고 미커밋 변경은 없었다.
사용자가 이전 재시도 금지를 명시적으로 해제하고 서버 기동·브라우저 검수·문제 수정을 요청했다.
[45번 인계](45_browser_validation_handoff.ko.md)는 그 이전의 미수행 기록이다.

## 실제 접속과 환경

- Windows의 내부 브라우저로 `http://127.0.0.1:18753/small.html`에 직접 접속했다.
  페이지 제목, 접근성 트리, 실제 화면 캡처가 정상적으로 반환됐다. 이번에는 접근 차단 오류가 없었다.
  다른 브라우저나 주소로 우회하지 않았고 Chrome은 별도로 검수하지 않았다.
- 소스 CLI를 빌드해 합성 coverage JSON을 HTML로 렌더링한 뒤, 전용 임시 폴더만
  loopback 정적 서버로 제공했다. 저장소 전체나 개인 기록을 노출하지 않았다.
  실제 대시보드 JS/CSS와 파일 선택·다운로드를 사용했으며 DOM 대역 검사를 화면 검수로 세지 않았다.
- 5 API/15 관측, 35 API/180 관측, 120 API/660 관측을 사용했다.
  49/50 API, 999/1000 관측, 빈 기록, legacy, 긴 경로도 별도 합성 fixture로 확인했다.
- 기본 데스크톱, 1440×900, 1024×480, 390×844, 320×568 크기에서 조작했다.
  라이트/다크 화면을 직접 확인했다. 모바일 수치는 브라우저 viewport이며 실제 휴대폰 검수가 아니다.
- 실행 이력 3개는 화면 검사용 합성 결과다. 실제 HTTP 실행 성공이나 성능 개선의 증거로 사용하지 않는다.
  관측 이력은 합성 coverage 파일에서 CLI로 생성했다.

## 8개 흐름의 실제 확인 결과

| 흐름 | 직접 확인한 내용 | 결과 |
| --- | --- | --- |
| 1. 기록 규모·보기 전환 | 작은 Request/Retest map, 중간 Groups/API list, 큰 Distribution. 49 API와 999 관측은 Groups, 50 API와 1000 관측은 Distribution | 확인 |
| 2. 사이드바·스크롤 | 낮은 창에서 마지막 CI artifacts 메뉴까지 스크롤·Tab·Enter 접근, 모바일 본문/표 스크롤 | 확인. 마지막 메뉴 초점이 480px 창 안에 있고 outline 표시 |
| 3. 히트맵→그룹→API→요청 | 10:10 UTC/500–999ms의 60건→shipping→item-119→fixture-659. 실제 상세는 10:10:59Z/990ms/HTTP400 | 범위와 요청 일치 |
| 3. 상세가 없는 과거 구간 | 10:00 UTC/100ms 미만 48건 선택, 10 API에 사용 가능한 상세 0개 | Request map/Create drafts 비활성, 누락 10개 표시. 범위 밖 요청으로 대체하지 않음 |
| 4. 필터·정렬·선택 | 35 API를 두 페이지에서 선택→검색 결과 0→숨은 선택 35개→Reset→Groups/API list→p95 정렬 | 선택 35/180 관측 유지, 출처 19개/누락 16개 표시 |
| 5. 초안·검토·다운로드 | 경로/상태/client 제한 입력→검토→JUnit 다운로드→경로 수정. 묶음·HTML도 실제 다운로드 | 편집 시 검토 해제·JUnit 비활성. 받은 파일 내용 별도 확인 |
| 6. 시나리오·파일 오류 | 빈 이름, 저장→import→추가 사례 재검토, 잘못된 JSON/kind·schema/1MiB 초과 파일, 빈 관측에서 import→검토 | 오류가 보여도 기존 사례/메타데이터 보존. Postman/JMeter/명령 실제 다운로드 |
| 7. 실행·관측 이력 | 각각 import·비교·기준 해제·검색/추가 import·같은 ID 비교·선택 기록 제거·파일 저장 | 해제 유지, 자기 비교 안내, 대체 자동 선택 없음. 원본 fixture 파일 유지 |
| 8. 긴 경로·빈 상태·키보드·테마 | 긴 API, 빈/legacy, 검색 결과 0, `/`·Esc·Tab, 라이트/다크, 320/390px | 가로 문서 넘침 없음, 보이는 검색란 초점·2px 초점 표시. 아래 메시지 문제 1건 수정 |

## 브라우저에서 재현한 문제와 원인

1. Scenario에서 잘못된 JSON을 import한다. `Import failed: ...`가 표시된다.
2. Run history로 이동해도 Scenario 오류가 그대로 남는다.
3. 실행 이력을 제거한 뒤 Recordings로 이동하면 `Disk history was not deleted`라는
   실행 이력 메시지가 관측 기록 화면에 남는다. 관측 기록의 자체 안내와 동시에 표시되기도 했다.

원인은 세 하위 보기가 공통 `scenario-message` 영역을 사용하지만 보기 전환 시
그 내용을 정리하지 않는 것이다. 수집기·파일 파서·실행 결과가 실패한 문제가 아니다.
기존 [선택/비교 수정](41_workspace_followup.ko.md)과 [트러블슈팅](25_diagnostics_troubleshooting.md)을
참고해 수정 범위를 작업 메시지의 수명으로 제한했다.

### 수정과 재검수

- 다른 하위 보기로 바뀔 때 공통 작업 메시지만 비운다.
- 같은 보기를 다시 누르면 현재 오류를 유지한다. 입력·초안·이력·Current/Baseline은 그대로 둔다.
- 회귀 테스트를 먼저 추가해 `Scenario import errors must not appear in run history` 실패를 확인했다.
  수정 후 같은 검사와 전체 Node 검사가 통과했다.
- CLI를 재빌드하고 HTML을 다시 생성한 뒤 브라우저를 reload했다.
  잘못된 import→같은 Scenario 재선택에서는 오류 유지, Run history 전환에서는 오류 제거를 확인했다.
  실행 이력 제거→Recordings에서도 이전 제거 메시지가 사라졌다.
- 재현/수정 화면은 로컬 작업 outputs에 보관했다. 개인 기록·원시 로그·다운로드 결과는 commit하지 않는다.

## 다운로드 파일 내용 확인

| 실제 받은 파일 | 확인 |
| --- | --- |
| API draft bundle | 선택 35개, draft 19개, 상세 누락 API 16개 |
| JUnit | 검토한 `/qa/check`와 기대 상태 200, `@Disabled` 포함 |
| 원본 HTML | 대시보드 데이터와 Lucide/Feather 라이선스 고지 유지 |
| 시나리오/Postman/JMeter | 저장한 사례, 빈 baseUrl, 비활성 JMeter 계획 |
| 실행 이력/관측 목록 | 저장 시 각각 2개/3개. 페이지에서 제거해도 디스크의 관측 fixture 3개 유지 |
| 로컬 실행 명령 | `REPLACE_WITH_LOCAL_ORIGIN`과 `--confirm-local` 포함. 다운로드로 실행하지 않음 |

## 자동 검사와 한계

- 수정 후 Java 21: report 79개 + CLI 25개, 실패·오류·skip 0. 변경 리소스를 포함한 CLI 빌드 성공.
- Node 전체: 48개 통과, 실패·skip 0. 기존 controller 검사에 메시지 수명·선택/이력 보존 회귀를 추가했다.
- 내부 브라우저의 캡처된 warning/error 로그는 빈 목록이었다.
- 최신 push의 GitHub 검사 결과는 PR 설명과 checks에 해당 commit 기준으로 기록한다.
  이 자동 CI는 실제 화면 검수와 별개의 증거다.
- 모든 조합의 완전한 E2E 승인이나 실제 모바일 기기/Safari/Firefox 지원 검증은 아니다.
  200% 확대·forced-colors·reduced-motion·256개 시간 구간 한계의 화면 검수는 이번에 수행하지 않았다.
  기존 전체 브라우저 스크립트를 실행한 것으로도 보고하지 않는다.
- 정적 리포트 검수이며 라이브 수집기의 네트워크 흐름이나 운영 APM 성능을 검증한 것이 아니다.
  머지·릴리스는 하지 않는다. 전용 서버를 종료하고 18753 포트가 닫힌 것을 확인했다.
  임시 테마·viewport 설정을 원복하고 검수 탭을 닫았다.
