[English](26_dashboard_and_ci.md) | **한국어**

# 대시보드와 CI 연결

이번 기능은 **개발 브랜치의 미리보기**입니다. 배포된 `v0.3.0`에는 없습니다.
새 Action과 현재 소스로 빌드한 CLI를 함께 사용해야 새 대시보드가 나옵니다.
`version: 0.3.0`만 지정하면 예전 배포 CLI를 내려받습니다. 새 화면이나 요청 수집
기능까지 자동으로 설치되는 것은 아닙니다.

## 무엇부터 보나

화면은 현업에서 다시 확인할 만한 것부터 보여줍니다.

- **실패 요청:** 최종 HTTP 5xx, 4xx가 기록된 요청입니다. 예상한 400을 돌려주는
  테스트는 성공일 수도 있으므로, 여기서 보인다고 테스트 실패라는 뜻은 아닙니다.
- **느린 요청:** 설정한 기준 시간을 넘은 관측 요청입니다. 서버 어댑터가 기록한
  구간이지, 사용자가 기다린 전체 시간이나 메서드별 시간은 아닙니다.
- **공통 코드:** 여러 API가 실제 실행한 메서드입니다. 여기를 바꾸면 관측된 API들을
  재테스트 후보로 볼 수 있습니다. 코드 이름만 보고 업무 중요도를 추측하지 않습니다.

`Request map`은 요청이 들어와서 기록된 결과를 남기는 과정과 그 요청의 메서드들을
보여줍니다. 메서드끼리 호출한 순서가 아니라 **그 요청에 속한 메서드들의 집합**입니다.
`Retest map`은 공통 메서드에서 관련 API로 이어집니다. 노드를 누르면 오른쪽에
상태·시간·스레드 또는 재테스트할 API가 나타납니다. API를 누르면 기존 코드 표로 갑니다.

움직이는 점선은 관계를 강조하는 애니메이션입니다. 실제 실행 시간을 재생한 것은
아닙니다. 일시정지·확대·축소·맞춤 버튼이 있고, OS의 동작 줄이기 설정을 존중합니다.
외부 서버나 CDN 없이 HTML 파일 하나로 열립니다. JavaScript를 끄면 기존 표와 요청
상세가 그대로 남습니다.

그래프와 검토 목록은 최근 HTTP 요청 100개를 대상으로 합니다. 요청 그래프는 메서드
12개, 역방향 그래프는 API 16개까지 표시하고 초과 개수를 적습니다. 나머지는 요청
상세와 역조회 표에서 확인합니다. 위쪽 요약 숫자는 전체 보존 요청을 사용합니다.

역조회에는 수동으로 기록한 배치 작업 이름도 들어갈 수 있습니다. 따라서 연결 개수는
HTTP API라고 단정하지 않고 기존 기록 이름인 `endpoints`로 표시합니다. 배치 기록은
HTTP 처리 시간과 오류 통계에는 포함하지 않습니다.

## CI에 붙이는 순서

**통합 테스트 실행 → Reqover JSON 기록 → 변경 코드 분석 → 요약과 HTML 저장**

Action은 이미 있는 JSON을 읽는 도구입니다. 프로젝트에 agent/starter를 설치하거나
서버를 띄우고 테스트를 대신 돌려주지는 않습니다. 그 부분은
[Spring 연동 가이드](17_integration_guide.ko.md)와
[CI 기록 방법](18_ci_impact_analysis.ko.md)을 먼저 적용합니다.
Maven Central에는 아직 배포되지 않았으므로 소스 빌드 또는 배포 JAR가 필요합니다.

Ubuntu runner, Java 17 이상, Python 3, Bash를 준비합니다. 아래 예제는 기존 PR
워크플로에서 **리포트를 기록하고 Java를 설정한 뒤** 붙이는 부분입니다.
`container: eclipse-temurin` 같은 컨테이너에는 Python이 없을 수 있으므로 먼저
Python 3을 설치합니다. 이 조건과 얕은 checkout을 네트워크 작업 전에 확인합니다.

```yaml
# 개발 미리보기. 장기 사용 시 검토한 commit SHA로 고정합니다.
- uses: actions/checkout@3d3c42e5aac5ba805825da76410c181273ba90b1
  with:
    repository: reqover-labs/reqover
    ref: main  # replace with the commit SHA you reviewed
    path: .reqover-tool
    persist-credentials: false

- name: Build the preview CLI
  run: ./.reqover-tool/gradlew -p .reqover-tool :reqover-cli:shadowJar

- name: Retest candidates and dashboard
  id: reqover
  uses: ./.reqover-tool/.github/actions/impact
  with:
    report: build/reqover-report.json
    cli-jar: .reqover-tool/reqover-cli/build/libs/reqover-cli-0.3.0.jar
    comment: "false"
    upload-artifact: "true"
    artifact-name: reqover-${{ github.job }}-${{ strategy.job-index || 'single' }}
```

`.reqover-tool`은 도구를 빌드하려고 별도로 받은 폴더입니다. **우리 앱을 처음
checkout하는 단계**에는 `fetch-depth: 0`을 넣어야 변경 기준과 공통 조상을 찾습니다.
Reqover 저장소 자체에서는 로컬 Action과 로컬 CLI를 바로 쓰며,
[실제 CI 설정](../.github/workflows/build.yml)에 같은 흐름을 넣었습니다.

처음에는 `contents: read`, `comment: "false"`로 시작하면 됩니다. 같은 저장소 PR에
댓글도 남기려면 `pull-requests: write`를 주고 댓글을 켭니다. fork PR에는 댓글을
쓰지 않지만 요약과 파일은 남깁니다. 댓글 권한이 부족해도 분석 결과가 없어지지
않습니다. 쓰기 권한을 얻으려고 `pull_request_target`에서 외부 PR 코드를 실행하면
안 됩니다.

## 어떤 결과가 남나

GitHub Actions 실행의 Summary에 관련 API와 관측되지 않은 변경 파일을 보여줍니다.
업로드는 기본적으로 끄고 명시적으로 켭니다. 켰을 때 Artifacts에는
`report.html`, `impact.md`, `impact.json` 세 파일만 7일간 저장합니다.
워크스페이스 전체를 묶거나 원본 JSON을 그대로 올리지 않습니다.
다운로드한 `report.html`은 서버 없이 열 수 있습니다.

기본 리포트 경로는 `build/reqover-report.json`입니다. `cli-jar`로 개발 CLI를 지정하고,
PR이 아닌 push·수동 실행에서는 `base-ref`를 직접 지정합니다. 매트릭스에서는
`artifact-name`을 각각 다르게 줍니다. 파일 업로드는 `upload-artifact: "false"`로
끄는 것이 기본값이며 `upload-artifact: "true"`로 켭니다. 같은 job에서 여러 번
호출한다면 각 호출의 `artifact-name`도 다르게 지정합니다.

한 PR에서 분석을 여러 번 돌린다면(매트릭스, 서비스 여러 개) 각각 `analysis-name`을 다르게 줘야 서로의 댓글을 덮어쓰지 않습니다. 이름이 같으면 PR 댓글은 한 작업에서만 작성해 같은 댓글의 동시 수정을 피합니다.

| 출력 | 의미 |
| --- | --- |
| `has-impact` | 관측된 관련 API가 있는지 |
| `impacted-endpoint-count` | 재테스트 후보 API 수 |
| `unmatched-path-count` | 실행 기록과 매칭되지 않은 변경 경로 수 |
| `markdown` / `markdown-path` | 영향 분석 설명과 파일 경로 |
| `html-path` | 현재 runner의 HTML 경로 |
| `json-path` | 영향 분석 JSON 경로. 원본 요청 JSON이 아님 |
| `artifact-url` | 업로드한 결과 링크. 업로드를 끄면 비어 있음 |

`fail-on-impact`는 기본적으로 끕니다. 켜면 **바꾼 코드가 관측 API에 연결되었을 때**
실패하므로, 일반적인 테스트 실패 판정과 다릅니다. 이 설정으로 성능 회귀가 없다고
보장할 수는 없습니다. 관련 API가 0개인 경우에도 미관측 실행 경로가 있을 수 있습니다.

## 보안과 다음 범위

`.env`, API 키, 인증 헤더, 요청 본문을 수집하거나 함께 올리지 않습니다. 다만 HTML에는
코드 이름·요청 ID·스레드 이름이 있으므로 공개해도 되는 QA 데이터만 사용합니다.
임의의 리포트 필드에 비밀값을 넣으면 Action이 자동으로 지워주지는 않습니다.

테스트 초안 생성, 완전한 재현, 자동 실행, 입력값 수집, 실제 호출 순서·메서드 시간, DB 구간, TPS와 k6는
아직 후속 단계입니다. 이번 목표는 **문제 요청을 찾고 관련 코드·재테스트 범위를
확인한 뒤 CI에서도 같은 근거를 남기는 흐름**입니다.

재현 명령과 검증 범위는 [영문 안내](26_dashboard_and_ci.md)에 있습니다.
기존 OSV 보안 검사는 그대로 유지합니다. 이전 Jackson/Tomcat 경고는
[의존성 패치와 SBOM 갱신](../CHANGELOG.md)으로 보완합니다.
기능 테스트만 통과했다고 보안 검사까지 해결된 것으로 판단하지 않습니다.
