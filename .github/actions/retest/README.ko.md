[English](README.md) | **한국어**

# 저장 시나리오를 GitHub Actions에서 검사하기

미출시 소스 기능이다. 배포된 0.4.2 CLI에는 필요한 run 명령이 없다.
이 Action과 CLI를 같은 소스 revision에서 사용한다.

개발자가 검토한 시나리오 파일을 읽어, runner 안에 테스트 앱을 띄우고 API 검사를 수행한다.
앱 기동 → HTTP 200 준비 확인 → GET/HEAD 순차 실행 → 결과 저장 → 앱 종료 순서다.
기존 impact Action은 변경 코드와 관측 API 관계 분석이고, 이 Action은 기대 상태·시간 검사다.

저장소 내 실제 예제는 [.github/workflows/retest.yml](../../workflows/retest.yml)이다.

```yaml
permissions:
  contents: read
steps:
  # 먼저 checkout·JDK 17+·Python 3.9+ 및 앱/CLI 빌드를 완료한다.
  - uses: ./.github/actions/retest
    with:
      cli-jar: path/to/source-built-reqover-cli.jar
      app-jar: path/to/disposable-spring-app.jar
      scenario: path/to/reviewed-scenario.json
      ready-path: /actuator/health
      confirm-local: 'true'
      upload-artifact: 'true'
      artifact-name: service-a-retest
```

앱 JAR는 Spring의 `--server.address`·`--server.port` 인자를 받아야 한다.
readiness가 200이어도 seed 초기화 등 앱별 준비가 모두 끝났다는 뜻은 아니다.
그런 앱은 실제 업무 준비를 표현하는 readiness endpoint를 제공한다.
같은 job에 여러 Action을 붙이면 port와 artifact-name을 다르게 지정한다.

## 결과와 판정

- `PASS` / 종료 0: 계획한 검사가 기대 조건을 만족.
- `TEST_FAILED` / 종료 1: 상태 불일치·client 시간 조건 초과·네트워크 오류·미실행 등.
- `INPUT_ERROR`, `OUTPUT_ERROR`, `STARTUP_FAILED`, `EXECUTION_ERROR`, `COMPARISON_ERROR` / 종료 2: 실행 준비·입력·저장 등 오류.
- attempt의 `STATUS_MISMATCH`, `CLIENT_LIMIT_EXCEEDED`, `NETWORK_ERROR`, `TIMEOUT`를 요약에서 구분.
- 현재 GitHub SHA는 실행용 시나리오 복사본의 revision에 기록한다. 원래 저장 파일은 수정하지 않는다.

Job Summary와 summary.json/md를 생성한다. result.json/html은 실행 결과가 저장된 경우에 생긴다.
실패해도 opt-in artifact 업로드 후 마지막 gate에서 job을 실패시킨다.
앱 로그·실행용 시나리오 원본·환경변수는 업로드 대상에 넣지 않는다.
결과 JSON에는 검사 경로와 시나리오 메타데이터가 있으므로 공유 범위를 확인한다.

`baseline`에 명시한 이전 result.json을 주면 comparison.html을 만든다.
다른 origin·시나리오 조건·환경은 비교 HTML에서 경고하며 자동 성능 합격 판정은 하지 않는다.
이전 실행 파일을 자동 다운로드하거나 신뢰하지 않는다. 팀이 출처를 확인한 파일을 전달한다.

## 입력과 실행 상한

`confirm-local` 기본 false, `upload-artifact` 기본 false다. literal loopback만 사용한다.
최대 100개 사례·repeat 1~10·1000시도, 요청 10초·실행 60초 상한은 CLI 계약을 따른다.
앱 기동 대기는 60초다. redirect·인증·본문·query·동시 부하는 지원하지 않는다.
정상 종료·실패·취소 시 자신이 시작한 앱 프로세스를 정리한다.
runner가 강제 종료되는 경우까지 보장하지 않는다. workflow에도 timeout을 둔다.
fork PR에서도 contents:read만 사용하고 secrets나 PR 쓰기 권한을 주지 않는다.

## 예약 실행은 팀이 선택

현재 workflow는 PR·main push·수동 실행만 활성화한다.
예약이 필요하면 팀이 관리하는 workflow의 on에 예를 들어 아래를 추가한다. cron은 UTC다.

```yaml
schedule:
  - cron: '0 0 * * 1-5'
```

위 예는 평일 오전 9시 KST다. concurrency와 job timeout을 유지한다.
매번 runner 안에 앱을 새로 띄우므로 운영 서버에 주기적으로 요청하는 기능이 아니다.

## 로컬에서 재현

```bash
python3 scripts/run-retest-ci.py --cli path/to/source-cli.jar \
  --app-jar path/to/test-app.jar --scenario reviewed-scenario.json \
  --confirm-local --ready-path /actuator/health --out build/my-check
```

출력 폴더는 새 경로여야 한다. 생략하면 고유 임시 폴더를 생성한다.
GITHUB_SHA가 없는 환경은 `--revision <commit>`으로 실행 버전을 명시할 수 있다.
