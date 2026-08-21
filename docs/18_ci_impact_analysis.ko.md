**한국어** | [English](18_ci_impact_analysis.md)

# 18. CI에서 영향도 분석하기

Reqover는 어떤 엔드포인트가 어떤 메서드를 실행했는지 기록합니다. 그 기록에 diff를 넘겨주면, 리뷰어가 실제로 궁금해하는 질문에 답이 나옵니다.

> 이 파일들을 고쳤습니다. **어떤 API를 다시 테스트해야 하나요?**

이 문서는 그 흐름 전체를 다룹니다. 테스트 실행에서 리포트를 뽑아내고, 변경에 대해 물어보고, Pull Request에 연결하는 것까지입니다.

## 무엇을 알 수 있고, 무엇을 알 수 없나

한계를 먼저 읽으세요. 결과가 나에게 쓸모 있는지 없는지가 여기서 갈립니다.

**알 수 있는 것:** 관측된 이 엔드포인트들이, 내가 고친 파일 안의 코드를 실행했다는 사실.

**알 수 없는 것:** 나머지가 영향받지 않는다는 사실. 영향도 분석은 리포트를 기록하는 동안 *실행되는 것이 관측된* 코드만 압니다. 바뀐 파일이 리포트에 나타나지 않으면 "관측된 실행 기록 없음"(no observed coverage) 목록으로 들어가는데, 이는 Reqover가 구분할 수 없는 두 가지 중 하나라는 뜻입니다.

- 아무도 그 코드를 호출하지 않거나,
- 그 리포트를 만든 실행에서 아무도 그 코드를 지나가지 않았거나.

그래서 답의 품질은 곧 기록의 품질입니다. 통합 테스트를 촘촘히 돌려서 만든 리포트는 믿고 움직일 만하지만, 수동으로 한 번 클릭해서 만든 리포트는 아닙니다. 결과는 **어디부터 봐야 하는지**로 쓰고, 나머지가 안전하다는 증거로는 절대 쓰지 마세요.

## 1단계 — 테스트 실행에서 리포트 뽑아내기

리포트는 원래 메모리에만 있다가 JVM과 함께 사라집니다. starter는 애플리케이션 컨텍스트가 닫힐 때 리포트를 파일로 쓸 수 있습니다.

```properties
reqover.report.export.json-path=build/reqover-report.json
reqover.report.export.html-path=build/reqover-report.html
```

agent를 붙인 채로 애플리케이션을 실행하고, 통합 테스트를 그 위로 흘려보낸 다음, 정상적으로 종료시킵니다.

```bash
java -javaagent:reqover-agent-0.2.0.jar=include=com.example \
  -jar build/libs/your-app.jar \
  --reqover.report.export.json-path=build/reqover-report.json
```

알아둘 것 두 가지:

- 내보내기는 **컨텍스트가 정상적으로 종료될 때** 실행됩니다. `SIGKILL`로 죽인 프로세스는 아무것도 쓰지 않습니다. CI에서는 `SIGTERM`(기본 `kill`)으로 애플리케이션을 멈추고, 프로세스가 끝날 때까지 기다리세요.
- 내보내기가 실패하면 로그만 남기고 삼킵니다. Reqover 때문에 종료가 실패하는 일은 없게 만들었는데, 뒤집어 말하면 파일이 안 생겨도 조용히 넘어간다는 뜻입니다 — 분석하기 전에 파일이 실제로 있는지 확인하세요.

원한다면 HTTP 엔드포인트가 내려주는 것을 저장해도 됩니다. 바이트 단위로 같은 문서입니다.

```bash
curl -sf http://127.0.0.1:8080/reqover/report > build/reqover-report.json
```

파일로 쓴 JSON은 그 자체로 완결되어 있습니다 — 클래스 이름과 메서드 이름이 문서 안에 다 들어 있어서, 다시 읽을 때 기록을 만든 JVM이 필요하지 않습니다.

## 2단계 — 변경이 무엇에 영향을 주는지 묻기

```bash
git diff --name-only origin/main...HEAD \
  | java -jar reqover-cli-0.2.0.jar impact \
      --report build/reqover-report.json \
      --changed-files -
```

```
Reqover impact analysis
  changed paths analysed: 3
  impacted endpoints:     2

Endpoints to retest:
  GET /orders/{id}
      via com.example.order.OrderService#find(long): OrderResponse
  POST /payments
      via com.example.SharedValidator#validate(String)

Changed paths with no observed coverage (1):
  README.md
```

`origin/main...HEAD`는 점 세 개로 씁니다. 내 브랜치가 바꾼 것만 나오고, 갈라져 나온 뒤 base 브랜치에서 움직인 것은 빠집니다.

`--format markdown`은 Pull Request 코멘트 크기에 맞춘 표를 만들고, `--format json`은 파이프라인의 다음 단계가 읽을 수 있는 형태로 출력합니다.

## 3단계 — Pull Request에 붙이기

저장소에 composite action이 들어 있습니다.

```yaml
name: reqover

on: pull_request

permissions:
  contents: read
  pull-requests: write

jobs:
  impact:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v5
        with:
          fetch-depth: 0

      - uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version: 21

      # Whatever produces build/reqover-report.json for your project:
      # boot the app with the agent, run the integration suite, shut it down.
      - name: Record a report
        run: ./scripts/record-reqover-report.sh

      - uses: reqover-labs/reqover/.github/actions/impact@v0.2.0
        with:
          report: build/reqover-report.json
```

`fetch-depth: 0`은 빼면 안 됩니다 — action이 base와 diff를 뜨려면 히스토리가 필요합니다.

action의 입력값:

| 입력값            | 기본값     | 하는 일                                                          |
| ---------------- | --------- | ---------------------------------------------------------------- |
| `report`         | *필수*     | 기록된 리포트 JSON 파일 경로                                        |
| `version`        | `0.2.0`   | CLI를 받아올 릴리스                                                |
| `base-ref`       | PR의 base | diff 기준이 되는 Git ref. Pull Request 밖에서는 필수                 |
| `fail-on-impact` | `false`   | 관측된 엔드포인트가 바뀐 코드를 실행하면 스텝을 실패시킴                 |
| `comment`        | `true`    | 분석 결과를 Pull Request 코멘트로 남김                              |

분석 결과는 job summary에 쓰이고, `markdown` 출력으로도 노출됩니다. 푸시할 때마다 새 코멘트를 다는 대신 이전 코멘트를 갱신합니다.

> `fail-on-impact: true`는 **변경이 커버되었을 때** 빌드를 실패시킵니다. 보통 원하는 것과 반대입니다. API 표면에서 도달할 수 없어야 하는 모듈을 지키는, 좁은 용도로 만든 옵션입니다. 평범한 리뷰라면 켜지 말고 코멘트를 읽으세요.

## 명령 레퍼런스

아래에서 `reqover`는 `java -jar reqover-cli-0.2.0.jar`를 뜻합니다.

### `render`

기록된 리포트를 단독 실행 HTML 페이지로 만듭니다.

```bash
reqover render --report build/reqover-report.json --out build/reqover-report.html
```

애플리케이션을 계속 띄워둘 필요 없이 리포트를 CI 아티팩트로 남기고 싶을 때 씁니다.

### `impact`

```bash
reqover impact --report <file> (--changed-files <file>|- | --changed a,b) \
               [--format text|markdown|json] [--out <file>] [--fail-on-impact]
```

바뀐 경로는 줄바꿈으로 구분된 파일로 주거나, `-`로 표준 입력에서 받거나, `--changed a.java,b.java`처럼 직접 나열합니다. 두 형태 중 정확히 하나는 반드시 있어야 합니다.

### `diff`

```bash
reqover diff --baseline <file> --current <file> \
             [--format text|markdown] [--out <file>] [--fail-on-change]
```

두 기록을 비교합니다. 한쪽에만 있는 엔드포인트, 그리고 어떤 엔드포인트가 새로 실행하기 시작했거나 더 이상 실행하지 않게 된 코드를 보여줍니다. **기준선을 커밋해 두는** 방식과 함께 쓸 때 가장 쓸모 있습니다 — 고정된 시나리오로 리포트를 한 번 기록해서 커밋해 두고, CI가 매 빌드를 그것과 비교하게 하는 것입니다. 리포트를 정렬해서, 보기 좋게 들여쓴 형태로 파일에 쓰는 것도 이 때문입니다. 그래야 git에서 diff가 깔끔하게 나옵니다.

차이가 무엇을 뜻하는지는 주의해서 읽어야 합니다. 양쪽 모두 *관측된 트래픽*을 기술한 것이라, 차이가 났다는 건 코드가 바뀌었다는 뜻일 수도, 시나리오가 달랐다는 뜻일 수도, 이번에는 그 트래픽이 흐르지 않았다는 뜻일 수도 있습니다. diff는 차이를 보고할 뿐이고, 셋 중 무엇이었는지 판단하는 건 사람이 할 일입니다.

### 종료 코드

| 코드  | 의미                                                  |
| ---- | ----------------------------------------------------- |
| `0`  | 성공. 걸린 게이트 없음                                   |
| `1`  | `--fail-on-impact` 또는 `--fail-on-change` 게이트에 걸림  |
| `2`  | 잘못된 사용법, 읽을 수 없는 파일, 또는 리포트가 아닌 파일      |

CI에서는 이 구분이 중요합니다. `2`는 파이프라인 설정이 잘못됐다는 뜻이고, `1`은 파이프라인은 제대로 돌았고 게이트가 뭔가를 잡았다는 뜻입니다.

## 파일 매칭은 어떻게 동작하나

리포트에는 `com.example.order.OrderService` 같은 바이너리 클래스 이름이 들어 있습니다. 분석기는 이걸 그 클래스가 선언되어 있었을 소스 경로 — `com/example/order/OrderService.java` — 로 바꾼 다음, 바뀐 경로가 그 문자열로 끝나면서 그 지점이 디렉터리 경계와 맞아떨어질 때 매칭으로 봅니다.

그래서 `src/main/java/com/example/order/OrderService.java`는 매칭되고, `src/main/java/com/example/notorder/OrderService.java`는 매칭되지 않습니다. 디렉터리 구조는 상관없습니다. 비교하는 것이 패키지 모양의 뒷부분뿐이라 Gradle이든 Maven이든 멀티 모듈 저장소든 모두 동작합니다.

알아둘 만한 것들:

- **중첩 클래스**는 자신을 선언한 파일을 통해 매칭됩니다. `OrderService$Row`는 `OrderService.java`로 이어집니다.
- **Kotlin**도 매칭됩니다. 각 경로의 `.kt` 버전도 함께 시도합니다.
- **Windows 경로 구분자**와 맨 앞의 `./`는 정규화합니다.
- **파일 이름과 다른 이름을 가진 두 번째 최상위 클래스는 매칭되지 않습니다.** 그 파일은 매칭 실패 목록에 들어갑니다. 놓쳤다는 사실이 조용히 묻히지 않고 눈에 보이도록 한 것입니다.

## 문제 해결

**리포트 파일이 아예 안 생겼습니다.** 애플리케이션이 정상 종료되지 않고 죽었거나, 내보내기 경로를 설정하지 않은 것입니다. 프로세스가 `SIGTERM`으로 종료되는지 확인하고, 표준 출력에 `[reqover] wrote the JSON report to ...` 줄이 있는지 보세요.

**바뀐 경로가 전부 매칭 실패로 나옵니다.** 대개 agent의 `include=`가 내 패키지를 덮지 않아서 애초에 아무것도 기록되지 않은 경우입니다. HTML 리포트를 열어보세요. 엔드포인트가 하나도 없다면 문제는 분석 이전 단계에 있습니다.

**있어야 할 엔드포인트가 없습니다.** 기록하는 동안 그 엔드포인트가 실행되지 않은 것입니다. 영향도 분석이 추론해낼 수 있는 것이 아닙니다.

**`reqover: ... is not a Reqover report`.** 이 도구가 쓴 JSON이 아니라는 뜻입니다 — 다운로드가 중간에 잘렸거나, HTML 페이지를 잘못 저장했거나.

## 관련 문서

- [Spring 애플리케이션 연동 가이드](17_integration_guide.ko.md) — 전체 속성 목록
- [시스템 아키텍처](02_architecture.ko.md) — 실행 귀속이 어떻게 기록되는가
- `scripts/run-impact-demo.sh` — 데모 애플리케이션에 대고 전체 흐름을 명령 하나로 돌려보기
