[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# 기록의 핵심 모델

Maven Central 라이브러리 `io.github.reqover-labs:reqover-core:0.4.2`입니다.
starter에 포함되며, 자체 어댑터나 저장소를 만들 때 직접 사용하는 API입니다.

- `CoverageBucket`: 작업의 시각·상태·스레드와 실행 probe 집합.
- `UnitInfo`: HTTP 요청 또는 작업·메시지·테스트의 식별 정보.
- `CoverageContext`·`ReqoverProbe`: 현재 기록함으로 실행 정보를 전달.
- `UnitScope.open`: 작업 수명 관리. `join`은 다른 스레드의 작업을 같은 기록함에
  연결하고, 원래 작업을 끝내거나 flush하지 않음.
- `CoverageStore`: `flush`, `snapshots`, 선택적인 `aggregates`, `clear`.

기본 `InMemoryCoverageStore`의 snapshot 보관 상한은 10,000개이며 `oldest-first`
또는 `reject-when-full` 정책을 사용합니다. 집계 대상으로 받아들인 작업 이름의 호출 수와
코드 합집합은 상세 삭제 후에도 남지만 재시작·`clear()`로 사라집니다.

집계에는 별도의 서로 다른 작업 이름 2,000개 제한이 있으며, 새 이름이 동시에 들어오면
엄격한 원자적 상한은 아닙니다. 집계별 스레드 이름은 최대 64개입니다. 기존 집계는
계속 누적하지만 상한 밖의 새 이름은 보관 snapshot으로만 표시하고, 그 상세도 삭제되면
리포트에서 사라집니다. `maxSnapshots`를 늘려도 이 집계 제한은 바뀌지 않습니다.
probe 집합은 계속 커질 수 있으므로 상세 상한이 전체 메모리 상한은 아닙니다.

자체 저장소는 동시 호출에 안전하고 완료 스레드를 오래 막지 않아야 합니다.
`aggregates()`가 없으면 리포트 집계는 보관 snapshot 기준으로 돌아갑니다.
[계약 테스트](src/test/java/io/reqover/core/CoverageStoreContract.java)를 참고합니다.
core 자체는 Spring 연결, 영속 저장소, 줄/분기, 메서드 시간이나 요청 재실행을 제공하지 않습니다.

[아키텍처](../docs/02_architecture.ko.md) |
[자체 저장소와 UnitScope](../docs/17_integration_guide.ko.md)
