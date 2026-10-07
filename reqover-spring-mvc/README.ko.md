[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# Spring MVC 어댑터

Maven Central 라이브러리 `io.github.reqover-labs:reqover-spring-mvc:0.4.2`입니다.
일반 Spring Boot 앱은 이 어댑터를 직접 연결하기보다
[starter](../reqover-spring-boot-starter/README.ko.md)와 agent를 사용합니다.

인터셉터가 handler 매핑 이후 기록함을 만들고 요청 스레드에 연결하며, 응답 상태와
함께 종료합니다. 주소는 `GET /orders/{id}` 같은 route 패턴으로 묶습니다.
동시 요청도 각각의 scope로 분리합니다.

- 기본값: 활성, snapshot 10,000개, `oldest-first` 보관.
- 설정: `reqover.mvc.enabled`, `.max-snapshots`, `.snapshot-eviction`,
  `.include-path-patterns`, `.exclude-path-patterns`.
- 미매핑 catch-all 리소스 요청과 기본 제외 리포트·오류 경로는 기록하지 않습니다.
  본문·query·인증값도 수집하지 않습니다.
- Servlet async 재진입은 기록함을 재사용하지만, 그 전에 다른 worker가 실행한
  작업은 자동으로 연결하지 않습니다.

시간은 네트워크 도착이 아닌 handler 매핑 이후부터의 벽시계 구간입니다.
클라이언트 지연·메서드·CPU·DB 시간이 아닙니다. 어댑터만으로 리포트 주소를 열지는 않습니다.

[설정과 한계](../docs/17_integration_guide.ko.md) |
[요청 진단](../docs/24_request_diagnostics.ko.md)
