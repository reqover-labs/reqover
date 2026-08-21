# 19. 3분 시연영상 대본

> 현재 영상은 **v6**입니다. `v0.2.0` 소스로 만들었고, v5에서 두 가지가 바뀌었습니다.
> **CI 영향도 분석 장면(`impact`)이 역방향 조회 뒤에 새로 들어갔고**, 검증 수치가
> **v0.2.0 · 자동화 테스트 121개**로 갱신되었습니다.
>
> 아래 타임라인과 내레이션은 실제로 만들어진 영상에서 그대로 뽑은 것입니다
> (`video/source/narration/script-v6.json`과 정렬 결과 `out/timeline.json`).
> 길이 **2분 32초**, 대회 3분 제한 안에 들어갑니다.

영상 소스와 빌드 파이프라인은 비공개 제출 아카이브에 있습니다. 화면은 1920×1080이고,
슬라이드는 HTML로 작성해 headless Chrome으로 2560×1440 PNG로 렌더합니다.

## 타임라인

| 시간 | 화면 | 내레이션 |
| --- | --- | --- |
| 0:00~0:10 | 제목 슬라이드 | 안녕하세요. Reqover입니다. Spring 애플리케이션에서 어떤 요청이 어떤 코드를 실행했는지 연결해서 보여주는 오픈소스 도구입니다. |
| 0:10~0:26 | 기존 커버리지와의 대비 | 지금까지의 커버리지는 코드가 실행됐다는 사실까지만 알려줬습니다. 그 실행을 만든 요청이 무엇이었는지는 남지 않습니다. 그래서 테스트가 통과해도 어떤 API가 그 코드를 지나갔는지는 따로 찾아야 했습니다. |
| 0:26~0:44 | 세 단계 구조 | Reqover는 이걸 세 단계로 해결합니다. 먼저 Spring adapter가 요청이 들어올 때마다 실행 bucket을 엽니다. Java Agent는 지정한 패키지에만 method-entry probe를 넣습니다. 마지막으로 그 실행 기록을 양방향 report로 모읍니다. |
| 0:44~0:58 | MVC report의 GET/POST 카드 | 실제 결과입니다. GET /orders 요청과 POST /payments 요청이 실행한 코드가 endpoint별로 분리됩니다. 두 요청이 공통으로 실행한 SharedValidator는 양쪽에 모두 관측됩니다. |
| 0:58~1:13 | Code to Endpoint Index | 반대 방향도 됩니다. 이 메서드를 실제로 실행한 endpoint가 무엇인지 코드에서 거꾸로 찾을 수 있습니다. 정적 영향 분석은 아닙니다. 코드를 고친 뒤에 먼저 확인할 API를 좁혀 주는 실행 근거입니다. |
| 1:13~1:31 | PR 코멘트 — 재검증 대상 endpoint | 0.2.0부터는 이 역방향 조회를 CI에서 그대로 씁니다. 기록한 리포트를 파일로 내보내고 변경된 파일 목록을 넘기면 다시 확인할 endpoint를 뽑아 줍니다. 그 결과를 Pull Request 코멘트로 남기기 때문에 리뷰하는 자리에서 바로 보입니다. |
| 1:31~1:52 | WebFlux report의 thread names | WebFlux는 조금 더 까다롭습니다. 요청 처리가 event loop에서 boundedElastic과 parallel 스레드로 옮겨 다니기 때문입니다. Reqover는 Reactor Context를 사용해서 스레드가 바뀌어도 같은 요청 bucket을 유지합니다. 스레드를 옮긴 뒤에 실행된 validate 메서드까지 같은 요청 아래에 그대로 잡힙니다. |
| 1:52~2:02 | terminal의 run-agent-demo | 이 데모에는 손으로 넣은 probe가 없습니다. 실행 스크립트가 샘플과 Java Agent를 함께 빌드하고 지정한 include 패키지만 자동으로 계측합니다. |
| 2:02~2:07 | include 없이 실행한 terminal | include가 비어 있으면 계측을 아예 켜지 않습니다. 기본값이 안전합니다. |
| 2:07~2:19 | 검증 수치 슬라이드 | v0.2.0 태그 소스는 Java 17과 21에서 자동화 테스트 121개를 통과했습니다. 고정한 SBOM을 기준으로 OSV에 알려진 취약 구성요소는 0건이었습니다. |
| 2:19~2:31 | 한계와 저장소 URL | 현재 정밀도는 method-entry이고 개발과 품질 검증을 위한 초기 릴리스입니다. 설치 방법과 한계 · 재현 절차는 GitHub 저장소에 정리해 두었습니다. 감사합니다. |

## 그대로 읽을 수 있는 내레이션

안녕하세요. Reqover입니다. Spring 애플리케이션에서 어떤 요청이 어떤 코드를 실행했는지 연결해서 보여주는 오픈소스 도구입니다.

지금까지의 커버리지는 코드가 실행됐다는 사실까지만 알려줬습니다. 그 실행을 만든 요청이 무엇이었는지는 남지 않습니다. 그래서 테스트가 통과해도 어떤 API가 그 코드를 지나갔는지는 따로 찾아야 했습니다.

Reqover는 이걸 세 단계로 해결합니다. 먼저 Spring adapter가 요청이 들어올 때마다 실행 bucket을 엽니다. Java Agent는 지정한 패키지에만 method-entry probe를 넣습니다. 마지막으로 그 실행 기록을 양방향 report로 모읍니다.

실제 결과입니다. GET /orders 요청과 POST /payments 요청이 실행한 코드가 endpoint별로 분리됩니다. 두 요청이 공통으로 실행한 SharedValidator는 양쪽에 모두 관측됩니다.

반대 방향도 됩니다. 이 메서드를 실제로 실행한 endpoint가 무엇인지 코드에서 거꾸로 찾을 수 있습니다. 정적 영향 분석은 아닙니다. 코드를 고친 뒤에 먼저 확인할 API를 좁혀 주는 실행 근거입니다.

0.2.0부터는 이 역방향 조회를 CI에서 그대로 씁니다. 기록한 리포트를 파일로 내보내고 변경된 파일 목록을 넘기면 다시 확인할 endpoint를 뽑아 줍니다. 그 결과를 Pull Request 코멘트로 남기기 때문에 리뷰하는 자리에서 바로 보입니다.

WebFlux는 조금 더 까다롭습니다. 요청 처리가 event loop에서 boundedElastic과 parallel 스레드로 옮겨 다니기 때문입니다. Reqover는 Reactor Context를 사용해서 스레드가 바뀌어도 같은 요청 bucket을 유지합니다. 스레드를 옮긴 뒤에 실행된 validate 메서드까지 같은 요청 아래에 그대로 잡힙니다.

이 데모에는 손으로 넣은 probe가 없습니다. 실행 스크립트가 샘플과 Java Agent를 함께 빌드하고 지정한 include 패키지만 자동으로 계측합니다.

include가 비어 있으면 계측을 아예 켜지 않습니다. 기본값이 안전합니다.

v0.2.0 태그 소스는 Java 17과 21에서 자동화 테스트 121개를 통과했습니다. 고정한 SBOM을 기준으로 OSV에 알려진 취약 구성요소는 0건이었습니다.

현재 정밀도는 method-entry이고 개발과 품질 검증을 위한 초기 릴리스입니다. 설치 방법과 한계 · 재현 절차는 GitHub 저장소에 정리해 두었습니다. 감사합니다.

## 촬영 명령

MVC 자동 계측:

```bash
./scripts/run-agent-demo.sh mvc 18080
```

WebFlux 자동 계측:

```bash
./scripts/run-agent-demo.sh webflux 18081
```

자동 계측에 수동 probe가 없음을 보여줄 파일:

```text
examples/mvc-sample/src/main/java/io/reqover/example/mvc/auto/AutoOrderService.java
examples/webflux-sample/src/main/java/io/reqover/example/webflux/auto/AutoReactiveOrderService.java
```

## 편집 체크

- 시작 10초 안에 프로젝트명과 문제를 모두 보여줍니다.
- terminal build 대기 시간은 잘라내고 실제 명령과 성공 결과는 남깁니다.
- 개인정보, local 절대경로, token, 알림 popup을 가립니다.
- thread 이름과 `validate(J)J`는 확대 또는 강조 상자로 표시합니다.
- 결과보고서·README와 동일하게 “method-entry 실행 귀속”이라고 표현합니다.
- 재업로드하면 YouTube URL이 바뀝니다. 결과보고서를 `--video-url`로 다시 생성하고 PDF도 다시 내보냅니다.
- YouTube 업로드 후 로그아웃 상태에서 영상·자막·설명란 GitHub 링크를 확인합니다.

## 실패 대비 백업

- 최종 MVC·WebFlux report HTML을 각각 standalone 파일로 저장합니다.
- 같은 장면의 무음 화면 녹화를 별도로 보관합니다.
- live 실행이 실패하면 저장된 report를 보여주되, 영상 설명란에 재현 명령과 최종 tag를 연결합니다.
