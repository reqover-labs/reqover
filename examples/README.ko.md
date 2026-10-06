[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# 예제 애플리케이션

실서비스가 아닌 읽기 전용·합성 Spring Boot 예제입니다. 수동 probe 예제와 agent로
기록하는 `/auto/` handler가 있으며 DB는 필요하지 않습니다. JDK 17/21과 비어 있는
포트를 준비하고 저장소 루트에서 실행합니다.

```bash
git checkout v0.4.2
./scripts/run-agent-demo.sh mvc 8080
./scripts/run-agent-demo.sh webflux 8081
```

각각 다른 터미널에서 실행하며 Enter로 해당 데모를 종료합니다. 대시보드는
`http://127.0.0.1:<port>/reqover/report.html`입니다. Windows는
`scripts/run-agent-demo.ps1 -App mvc -Port 8080` 또는 `-App webflux -Port 8081`을
사용합니다. 리포트 인증은 없으며 스크립트가 loopback으로만 엽니다.

MVC가 켜진 동안 `GET /auto/diagnostics/delay/1200` 또는
`GET /auto/diagnostics/failure`를 호출한 뒤 새로고침합니다. failure는 의도적으로
503을 반환하고, 지연이 0~2000 ms 밖이면 400입니다. WebFlux의 같은 예제는
`/auto/reactive/diagnostics/`로 시작합니다.

기본 자동 계측 MVC 예제는 AutoOrderController·AutoOrderService를 기록하고
단순 AutoOrderResponse 접근자는 건너뜁니다. 패키지 전체를 계측하면 수동 probe와
agent 기록이 함께 나타날 수 있습니다. 수동 probe는 학습용이며 자기 앱 연동에는 필요하지 않습니다.

`./scripts/run-impact-demo.sh 8080`은 트래픽을 기록하고 JSON·HTML을
`build/reqover-impact-demo/`에 남긴 뒤 CLI 재테스트 후보를 보여줍니다.
`run-agent-demo.sh ... --stop-after-report`는 JSON 출력 후 종료하며 HTML을 저장하지
않습니다. 파일은 내보내기 설정이나 대시보드 내려받기를 사용합니다.

[빠른 시작](../README.ko.md#5분-만에-직접-보기) |
[요청 진단](../docs/24_request_diagnostics.ko.md) |
[CI 기록](../docs/18_ci_impact_analysis.ko.md)
