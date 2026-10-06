[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# Spring WebFlux 어댑터

Maven Central 라이브러리 `io.github.reqover-labs:reqover-spring-webflux:0.4.1`입니다.
일반 Spring Boot 연동은 [starter](../reqover-spring-boot-starter/README.ko.md)와 agent를
사용합니다. 어댑터 자체는 리포트 엔드포인트를 열지 않습니다.

WebFilter가 요청 기록함을 Reactor Context에 넣고, Micrometer context propagation이
reactive 구간의 현재 스레드에 복원합니다. 지원하는 scheduler 전환 이후에도 같은
요청으로 실행을 기록합니다. 최종 상태와 시간은 Mono를 만드는 순간이 아닌 요청 종료 기준입니다.

- 기본값: 활성, snapshot 10,000개, `oldest-first` 보관.
- 설정: `reqover.webflux.enabled`, `.max-snapshots`, `.snapshot-eviction`,
  `.exclude-path-prefixes`.
- 임의 스레드·fire-and-forget 작업의 자동 귀속, 메서드 span·DB 시간은 보장하지 않습니다.
- 미매핑 catch-all 리소스는 건너뜁니다. 0.4.1은 Spring의 파싱된 route 패턴을 처리해
  이전의 ClassCastException을 수정했습니다.

어댑터가 켜지면 Reactor의 JVM 전체 자동 context propagation도 켜집니다.
시작 전에 `reqover.webflux.enabled=false`로 어댑터를 끌 수 있습니다. 같은 JVM의
다른 context·관측 도구와 함께 사용할 때는 상호작용을 확인합니다. 기본 리포트와
데모 포트는 접근 제어 없이 외부에 열지 않습니다.

[설정](../docs/17_integration_guide.ko.md) |
[아키텍처](../docs/02_architecture.ko.md)
