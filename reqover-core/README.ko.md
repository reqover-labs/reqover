[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# 기록의 핵심 모델

Maven Central 라이브러리 `io.github.reqover-labs:reqover-core:0.4.1`입니다.
starter에 포함되며, 자체 어댑터나 저장소를 만들 때 직접 사용하는 API입니다.

- `CoverageBucket`: 작업의 시각·상태·스레드와 실행 probe 집합.
- `UnitInfo`: HTTP 요청 또는 작업·메시지·테스트의 식별 정보.
- `CoverageContext`·`ReqoverProbe`: 현재 기록함으로 실행 정보를 전달.
- `UnitScope.open`: 작업 수명 관리. `join`은 다른 스레드의 작업을 같은 기록함에
  연결하고, 원래 작업을 끝내거나 flush하지 않음.
- `CoverageStore`: `flush`, `snapshots`, 선택적인 `aggregates`, `clear`.

기본 `InMemoryCoverageStore`는 snapshot을 최대 10,000개 보관하고 `oldest-first`
또는 `reject-when-full` 정책을 사용합니다. 이와 별도로 모든 flush의 작업 수·코드
합집합을 누적합니다. 집계는 상세 삭제 후에도 남지만 재시작·`clear()`로 사라집니다.
서로 다른 작업·probe가 늘면 집계도 커지므로 상세 상한이 전체 메모리 상한은 아닙니다.

자체 저장소는 동시 호출에 안전하고 완료 스레드를 오래 막지 않아야 합니다.
`aggregates()`가 없으면 리포트 집계는 보관 snapshot 기준으로 돌아갑니다.
[계약 테스트](src/test/java/io/reqover/core/CoverageStoreContract.java)를 참고합니다.
core 자체는 Spring 연결, 영속 저장소, 줄/분기, 메서드 시간이나 요청 재실행을 제공하지 않습니다.

[아키텍처](../docs/02_architecture.ko.md) |
[자체 저장소와 UnitScope](../docs/17_integration_guide.ko.md)
