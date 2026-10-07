[English](32_current_screenshot_capture.md) | **한국어** | [문서 목차](README.ko.md)

# 최신 README 화면 촬영 기록

2026년 10월 7일 한국 시간 기준으로 **v0.4.2** 예제에 실제 로컬 HTTP 요청을 보내
촬영했습니다. 소스 commit은 `0afe11bb4e2e37054fce36ae25db89421d1363de`입니다.
화면을 생성하거나 측정값을 고친 이미지가 아닌 실제 애플리케이션 출력입니다.

## 환경과 범위

- Windows, Java 21.0.10, Playwright 1.62.1의 Chromium.
- 현재 main이 아닌 정확한 릴리스 태그에서 agent와 두 예제 JAR 빌드.
- 비어 있는 임시 포트를 `127.0.0.1`로만 열고 촬영 후 직접 띄운 두 프로세스만 종료.
- include는 `io.reqover.example.mvc.auto`, `io.reqover.example.webflux.auto`.
- `accessors=skip`, `references=skip` 기본값을 사용해 응답 접근자는 기록하지 않음.
- 실제 리포트 HTML을 저장한 뒤 데이터를 바꾸지 않고 브라우저에서 촬영.
- 밝은 모드, 배율 2. 요청 상세 viewport 1280 x 800, 검색한 후보 1280 x 720,
  멈춘 관계도 1280 x 900.

| 새 이미지 | 화면과 확인 내용 |
| --- | --- |
| `assets/reqover-webflux-request-detail.png` | Requests에서 요청 하나 펼침. 클래스 2개·메서드 4개·스레드 3개 |
| `assets/reqover-retest-candidates.png` | Retest candidates에서 SharedValidator 검색. 공유 메서드 1행과 API 두 개 |
| `assets/reqover-retest-map-current.png` | Overview의 Retest map, 애니메이션 일시정지. 공유 메서드와 관측된 두 후보 연결 |

## 실제 호출한 요청

MVC는 `GET /auto/orders/42`, `GET /orders/42`, `POST /payments`,
`GET /auto/diagnostics/delay/1200`, `GET /auto/diagnostics/failure` 총 5건입니다.
앞의 4건은 200이고 마지막 실패 예제는 의도적으로 503을 반환합니다. 입력 본문이나
인증값은 필요하지 않습니다. 수동 probe 예제는 단순 클래스 이름을 사용하며,
`SharedValidator#validate`에 주문 조회와 결제 API가 함께 연결됩니다.

WebFlux는 `GET /auto/reactive/orders/42` 한 건이며 200을 반환합니다.
`boundedElastic-1`, `parallel-1`, `reactor-http-nio-2`가 같은 요청에 나타납니다.
controller의 `find`와 service의 `find`, `toResponse`, `validate`가 기록되며
AutoReactiveOrderResponse 접근자는 없습니다. 재실행하면 스레드 번호·요청 ID·시각·시간은 달라질 수 있습니다.

## 해석과 안전

시간은 작은 데모의 관측값이지 벤치마크가 아닙니다. 선은 호출 순서가 아닌 관측
관계입니다. 후보 이미지는 일부러 검색한 화면이며, 리포트에 메서드가 하나뿐이라는
뜻은 아닙니다. 실사용자의 요청, 토큰, 원시 로그와 컴퓨터 내부 경로는 공개하지 않습니다.

응답 상태·JSON 요청 수·요청별 코드·스레드·공유 API 관계와 실제 화면 상태를
확인했습니다. 브라우저 오류와 외부 HTTP(S) 에셋 요청은 모두 0건이며 PNG도 직접
확인해 글자나 내용이 잘리지 않는지 검토했습니다.

예전 표 이미지는 [과거 촬영 기록](16_readme_demo_capture.md)용으로 보존하고,
현재 README는 새 파일 이름을 사용합니다.
