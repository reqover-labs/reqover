[English](README.md) | **한국어** | [프로젝트](../../../README.ko.md)

# 변경 영향 분석 Action

이미 만든 coverage 리포트와 변경 파일을 대조해 작업 요약·PR 댓글을 남기고,
원하면 HTML·영향 분석 파일을 보관합니다. 요청 기록은 먼저 수행해야 하며,
Action이 agent를 설치하거나 앱 테스트를 실행하지는 않습니다.

`pull_request` 작업에서 전체 이력 checkout(`fetch-depth: 0`), Java 준비와 리포트
생성 뒤 `reqover-labs/reqover/.github/actions/impact@v0.4.2`을 사용합니다.
컨테이너 내부를 포함한 실행 환경에 Java 17+, Bash, Git, curl, Python 3이 필요합니다.

```yaml
- uses: reqover-labs/reqover/.github/actions/impact@v0.4.2
  with:
    report: build/reqover-report.json
    comment: "false"
    upload-artifact: "true"
    artifact-name: reqover-${{ github.job }}-${{ strategy.job-index || 'single' }}
    analysis-name: ${{ github.job }}-${{ strategy.job-index || 'single' }}
```

기본값은 CLI `version: 0.4.2`, `comment: true`, `upload-artifact: false`,
`fail-on-impact: false`입니다. 업로드는 `report.html`, `impact.md`, `impact.json`
세 파일만 7일간 보관합니다. matrix·반복 실행은 artifact 이름을 구분하고,
`analysis-name`도 구분하면 분석마다 독립적인 봇 댓글을 갱신합니다.

댓글은 같은 저장소 PR과 `pull-requests: write` 권한이 필요하며 fork는 생략합니다.
`pull_request_target` 권한으로 신뢰하지 않는 PR 코드를 실행하지 않습니다.
PR이 아닌 실행에서는 `base-ref`를 지정합니다. `cli-jar`는 릴리스 내려받기를 대신합니다.

영향 게이트는 근거 저장 후 실행하며 테스트 실패가 아닌 관측된 관련 endpoint 때문에
실패합니다. 매칭 실패나 후보 0개가 안전을 증명하지 않습니다. 리포트 이름·메타데이터도
내부 정보일 수 있으므로 입력 파일이나 비밀값을 임의로 공개하지 않습니다.

[모든 입력·출력](../../../docs/26_dashboard_and_ci.ko.md) |
[전체 기록 흐름](../../../docs/18_ci_impact_analysis.ko.md) |
[설정 원본](action.yml)
