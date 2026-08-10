# 19. 3분 시연영상 대본

최종 영상은 `v0.1.0` tag와 같은 소스로 촬영합니다. 화면은 1920×1080, 브라우저 125~150% 확대, IDE·터미널 글자는 최소 22px를 권장합니다.

## 타임라인

| 시간 | 화면 | 내레이션 핵심 |
| --- | --- | --- |
| 0:00~0:15 | 제목과 문제 한 문장 | 일반 커버리지는 코드 실행 여부를 보여주지만 어떤 HTTP 요청이 실행했는지는 기본 차원으로 남기지 않습니다. |
| 0:15~0:35 | 6개 모듈 아키텍처 한 장 | Reqover는 Java Agent가 넣은 method-entry hit를 현재 요청 bucket에 기록하고 두 방향 report로 집계합니다. |
| 0:35~1:05 | MVC report의 GET/POST 카드 | 서로 다른 두 endpoint의 controller/service가 분리되고, 공유 validator는 두 요청에 각각 관측됩니다. |
| 1:05~1:25 | Code to Endpoint Index 확대 | 특정 method에서 실제로 그 method를 실행한 관측 endpoint를 역으로 확인합니다. 완전한 정적 영향 분석이 아니라 우선 재검증할 후보를 좁히는 신호입니다. |
| 1:25~2:00 | WebFlux report의 thread names와 `validate(J)J` | Reactor가 세 thread로 이동해도 같은 endpoint bucket으로 유지되고, hop 뒤의 자동 계측 method도 함께 귀속됩니다. |
| 2:00~2:25 | terminal의 `run-agent-demo`와 auto sample 코드 | 애플리케이션 코드에 수동 probe 호출을 넣지 않고, 좁은 package를 `include=`로 지정해 agent를 실행합니다. |
| 2:25~2:45 | GitHub Actions, SBOM, LICENSE | Java 17·21에서 35개 테스트를 실행하고 CycloneDX SBOM과 OSV 검사, Apache-2.0·서드파티 고지를 제공합니다. |
| 2:45~3:00 | limitations와 GitHub URL | 현재 정밀도는 method-entry이고 개발·QA용 초기 릴리스입니다. 저장소에서 5분 Quickstart와 재현 절차를 확인할 수 있습니다. |

## 그대로 읽을 수 있는 내레이션

안녕하세요. Reqover는 Spring 애플리케이션에서 어떤 HTTP 요청이 어떤 메서드를 실행했는지 연결하는 오픈소스 개발자 도구입니다.

일반적인 집계 커버리지는 코드가 실행됐다는 사실은 보여주지만, 그 실행을 만든 HTTP 요청을 기본 차원으로 남기지는 않습니다. Reqover의 Java Agent는 선택한 애플리케이션 클래스에 method-entry probe를 넣고, Spring adapter가 만든 현재 요청 bucket으로 hit를 전달합니다.

먼저 MVC 결과입니다. `GET /orders/{id}`와 `POST /payments`가 실행한 controller와 service가 서로 다른 카드에 분리됩니다. 두 요청이 함께 실행한 `SharedValidator`는 양쪽에 각각 관측됩니다.

아래 역방향 index에서는 특정 method를 실제로 실행한 관측 endpoint를 다시 찾을 수 있습니다. 이것은 완전한 정적 영향 분석이 아니라, 코드 변경 뒤 먼저 재검증할 API 후보를 좁히는 실행 근거입니다.

WebFlux에서는 요청 처리가 `reactor-http-*` event loop, `boundedElastic`, `parallel` thread로 이동합니다. Reqover는 Reactor Context와 context propagation을 이용해 세 thread의 hit를 같은 endpoint bucket에 유지합니다. thread hop 뒤 실행된 `validate` method도 자동 계측 결과에 포함됩니다.

자동 시연 endpoint에는 수동 `ReqoverProbe.hit` 호출이 없습니다. `-javaagent`와 명시적인 `include` package만으로 실행하며, include가 없으면 안전하게 계측을 비활성화합니다.

현재 소스는 Java 17과 21에서 35개 테스트로 검증하고, CycloneDX SBOM과 OSV 취약점 검사, Apache-2.0 및 서드파티 고지를 함께 제공합니다. Reqover 0.1.0은 method-entry 정밀도의 개발·QA용 초기 릴리스입니다. 설치와 한계, 전체 재현 절차는 GitHub 저장소에서 확인해 주세요.

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
- YouTube 업로드 후 로그아웃 상태에서 영상·자막·설명란 GitHub 링크를 확인합니다.

## 실패 대비 백업

- 최종 MVC·WebFlux report HTML을 각각 standalone 파일로 저장합니다.
- 같은 장면의 무음 화면 녹화를 별도로 보관합니다.
- live 실행이 실패하면 저장된 report를 보여주되, 영상 설명란에 재현 명령과 최종 tag를 연결합니다.
