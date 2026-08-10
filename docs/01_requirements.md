# 01. 요구사항

## 기능 요구사항

### F1. 요청 경계 인식

Reqover는 HTTP 요청의 시작과 끝을 인식해야 합니다.

Spring MVC에서는 다음 후보를 사용합니다.

- Servlet Filter
- HandlerInterceptor

Spring WebFlux에서는 다음 후보를 사용합니다.

- WebFilter
- Reactor Context
- ThreadLocalAccessor 또는 동등한 context propagation hook

### F2. Coverage bucket 생성

요청이 시작되면 해당 요청을 대표하는 coverage bucket을 생성합니다.

bucket에는 최소한 다음 정보가 있어야 합니다.

- request id
- HTTP method
- normalized endpoint pattern
- start time
- end time
- 관측된 thread 이름 집합
- class id -> probe id set

### F3. Probe hit routing

계측된 코드가 실행되면 probe hit이 발생해야 합니다.

초기 PoC에서는 다음 형태의 호출 삽입을 목표로 합니다.

```java
ReqoverProbe.hit(classId, probeId);
```

`ReqoverProbe.hit`은 현재 context에서 활성 bucket을 찾고, 존재하면 해당 bucket에 hit을 기록합니다. 활성 bucket이 없으면 global bucket에 기록합니다.

### F4. 동시 요청 분리

동시에 여러 요청이 같은 코드를 실행해도 bucket이 섞이면 안 됩니다.

검증 시나리오:

- `GET /orders/{id}`와 `POST /payments`를 동시에 호출합니다.
- 두 요청이 일부 공통 코드를 실행합니다.
- 각 요청 고유 코드가 다른 bucket에 잘 분리되는지 확인합니다.

### F5. WebFlux thread hop 유지

WebFlux 시나리오에서는 요청 처리 중 thread가 바뀌어도 같은 bucket으로 hit이 기록되어야 합니다.

검증 시나리오:

- controller -> service -> reactive operator chain을 구성합니다.
- `publishOn` 또는 `subscribeOn`으로 thread 전환을 유도합니다.
- thread name을 함께 기록해 실제 thread hop을 증명합니다.
- hop 이후 발생한 probe hit이 원래 요청 bucket에 기록되는지 확인합니다.

### F6. 리포트 생성

MVP 리포트는 JSON을 우선합니다.

최소 JSON 예시:

```json
{
  "endpoint": "GET /orders/{id}",
  "requests": 12,
  "classes": [
    {
      "name": "com.example.OrderService",
      "probes": [1, 2, 5, 8]
    }
  ]
}
```

대회 데모용으로는 endpoint 카드와 역방향 index를 포함한 standalone HTML report를 제공합니다.

### F7. 역방향 조회

특정 class와 method가 어떤 endpoint에서 관측되었는지 조회할 수 있어야 합니다. 현재 버전은 source line 실행률이나 branch coverage를 제공하지 않습니다.

## 비기능 요구사항

### N1. 낮은 오버헤드

Reqover는 운영 상시 사용보다 개발, QA, staging, 데모 환경을 우선합니다. 그래도 요청 처리 시간을 과도하게 늘리면 안 됩니다.

최종 release candidate에서 동일 endpoint를 agent 미사용/사용 조건으로 반복 측정하고, 실행 환경·원시 표본·한계를 함께 공개합니다. 이 로컬 순차 측정은 production 성능 보장을 뜻하지 않습니다.

### N2. 안정성

Reqover가 실패해도 애플리케이션의 원래 요청 처리를 깨뜨리면 안 됩니다.

원칙:

- probe 기록 실패는 애플리케이션 예외로 전파하지 않습니다.
- bucket이 없으면 global bucket으로 기록합니다.
- context 복원 실패 시 오귀속보다 미귀속을 선택합니다.

### N3. 명확한 지원 범위

초기 버전은 지원 범위를 좁게 선언합니다.

지원:

- Java 17+
- Spring Boot 3.5.16 sample 검증
- Spring MVC
- Spring WebFlux 기본 Reactor chain

제한:

- custom scheduler
- 직접 생성한 raw thread
- fire-and-forget 작업
- Kotlin coroutine
- 복수 Java agent 충돌 상황

### N4. 라이선스 관리

JaCoCo를 의존성으로만 쓰는 경우와 내부 수정/fork를 하는 경우의 라이선스 영향이 다릅니다.

Phase 0 이후 다음을 결정합니다.

- 전체 저장소 기본 라이선스
- JaCoCo 연동 모듈 라이선스
- fork 또는 수정 파일 분리 여부
- NOTICE 파일 구성

## 품질 기준

### 테스트

MVP에는 다음 테스트가 필요합니다.

- bucket 생성/종료 unit test
- ThreadLocal context unit test
- 동시 요청 integration test
- WebFlux thread hop integration test
- report serialization test

### 데모

대회 발표용 데모는 코드 설명보다 먼저 보여줄 수 있어야 합니다.

필수 데모:

- 전역 coverage와 요청별 coverage 차이
- 동시 요청에서 bucket 분리
- WebFlux thread hop에서 bucket 유지

## 용어

- probe: 계측된 코드 위치에서 실행 여부를 기록하는 지점
- hit: probe가 실행되었다는 신호
- bucket: 특정 요청 또는 작업 단위에 귀속된 hit 저장소
- attribution: hit을 특정 요청, endpoint, trace, job 등에 연결하는 행위
- endpoint: HTTP method와 path pattern의 조합
- global bucket: 활성 요청이 없을 때 기록하는 fallback 저장소
