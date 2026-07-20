# 03. Phase 0 PoC 계획

## 목적

Phase 0의 목적은 제품을 완성하는 것이 아니라 가장 위험한 가정을 빠르게 검증하는 것입니다.

검증할 핵심 질문:

1. bytecode instrumentation으로 probe hit 호출을 안정적으로 삽입할 수 있는가?
2. hit 발생 시 현재 요청 bucket으로 routing할 수 있는가?
3. 동시에 들어온 요청의 hit이 섞이지 않는가?
4. WebFlux thread hop 이후에도 같은 bucket에 기록되는가?
5. 이 방식으로 대회 데모를 만들 만큼 충분한 시각적 차이가 나오는가?

## 결론을 내야 하는 결정

Phase 0이 끝나면 다음 결정을 내려야 합니다.

- JaCoCo 내부 확장으로 갈지
- 경량 자체 계측으로 MVP를 밀지
- branch coverage를 MVP에 포함할지
- line coverage까지만 제출하고 branch는 후속 과제로 둘지
- WebFlux를 실제 기능으로 넣을지, 데모 한정 실험으로 둘지

## 실험 A. 최소 instrumentation

### 목표

간단한 Java class에 method entry probe를 삽입합니다.

예시 원본:

```java
class OrderService {
    Order find(long id) {
        return repository.find(id);
    }
}
```

계측 후 개념:

```java
class OrderService {
    Order find(long id) {
        ReqoverProbe.hit(101, 1);
        return repository.find(id);
    }
}
```

### 구현 후보

- ASM ClassVisitor
- Byte Buddy AgentBuilder

### 성공 기준

- sample class 로딩 시 hit 호출이 발생합니다.
- hit count가 classId/probeId 기준으로 기록됩니다.
- 기존 애플리케이션 동작이 깨지지 않습니다.

## 실험 B. Spring MVC bucket routing

### 목표

Spring MVC 요청마다 bucket을 만들고 ThreadLocal로 routing합니다.

### 샘플 엔드포인트

- `GET /orders/{id}`
- `POST /payments`
- `GET /shared`

### 검증 방법

동시 요청을 여러 번 보냅니다.

예상:

- `GET /orders/{id}` bucket에는 order 관련 probe가 기록됩니다.
- `POST /payments` bucket에는 payment 관련 probe가 기록됩니다.
- 공통 utility probe는 양쪽에 기록될 수 있습니다.
- order 전용 probe가 payment bucket에 들어가면 실패입니다.

### 성공 기준

- 100회 이상 동시 요청에서 오귀속이 없습니다.
- 요청 종료 후 ThreadLocal이 clear됩니다.
- context 없는 background hit은 global bucket으로 갑니다.

## 실험 C. WebFlux context propagation

### 목표

Reactor chain 안에서 thread가 바뀌어도 bucket이 유지되는지 확인합니다.

### 샘플 흐름

```java
@GetMapping("/reactive/orders/{id}")
Mono<Order> find(@PathVariable long id) {
    return Mono.just(id)
        .publishOn(Schedulers.boundedElastic())
        .map(service::find)
        .publishOn(Schedulers.parallel())
        .map(this::toResponse);
}
```

### 관찰 항목

- 요청 id
- endpoint
- probe id
- hit 발생 thread name
- Reactor Context bucket id

### 성공 기준

- 최소 2개 이상의 thread name이 같은 request id 아래에 기록됩니다.
- thread hop 이후의 hit이 global bucket으로 빠지지 않습니다.
- 다른 요청 bucket과 섞이지 않습니다.

## 실험 D. 리포트 prototype

### 목표

사람이 바로 볼 수 있는 JSON 리포트를 만듭니다.

### 출력 예시

```json
{
  "generatedAt": "2026-07-01T12:00:00Z",
  "endpoints": [
    {
      "endpoint": "GET /orders/{id}",
      "requestCount": 10,
      "classes": [
        {
          "className": "sample.OrderService",
          "probeCount": 3
        }
      ]
    }
  ]
}
```

### 성공 기준

- 요청별 bucket이 endpoint별로 집계됩니다.
- class별 hit 목록을 볼 수 있습니다.
- 대회 발표용 heatmap으로 확장 가능한 구조입니다.

## 실험 E. 오버헤드 측정

### 목표

정확한 수치가 아니라 대략적인 비용 감각을 얻습니다.

### 측정 항목

- baseline p50/p95 latency
- Reqover enabled p50/p95 latency
- request당 hit count
- request당 bucket allocation
- GC pressure 추정

### 성공 기준

- 오버헤드가 존재한다면 병목 위치를 설명할 수 있습니다.
- 데모와 staging 용도로 수용 가능한지 판단할 수 있습니다.

## Phase 0 산출물

- minimal instrumentation code
- sample MVC app
- sample WebFlux app
- concurrency test
- thread hop demo
- JSON report sample
- Phase 0 decision note

## 예상 폴더 구조

```text
reqover/
  reqover-core/
  reqover-instrumentation/
  reqover-spring-mvc/
  reqover-spring-webflux/
  reqover-report/
  examples/
    mvc-sample/
    webflux-sample/
  docs/
```

## 일정

권장 일정은 2026년 7월 첫째 주 안에 Phase 0 결론을 내는 것입니다.

- Day 1: Gradle multi-module skeleton
- Day 2: minimal instrumentation
- Day 3: core bucket and probe hit API
- Day 4: MVC adapter and concurrency test
- Day 5: WebFlux context experiment
- Day 6: JSON report
- Day 7: decision note and roadmap update

## 중단 기준

다음 상황이면 설계를 바꿉니다.

- instrumentation이 Spring Boot 3 sample에서 안정적으로 동작하지 않습니다.
- WebFlux context propagation이 예상보다 불안정합니다.
- method-level coverage만으로는 데모 가치가 부족합니다.
- JaCoCo와의 결합 없이는 line mapping이 너무 약합니다.

## 다음 단계

Phase 0이 성공하면 바로 Phase 1로 넘어갑니다.

Phase 1 목표:

- Gradle multi-module 정리
- MVC adapter 안정화
- endpoint normalization
- request별 JSON report
- README quickstart 작성

