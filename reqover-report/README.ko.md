[English](README.md) | **한국어** | [프로젝트](../README.ko.md)

# 리포트와 진단 대시보드

Maven Central 라이브러리 `io.github.reqover-labs:reqover-report:0.4.1`입니다.
starter에 이미 포함됩니다. core에 의존하며 Spring이나 JSON 라이브러리를 요구하지 않습니다.

| 기능 | 데이터와 한계 |
| --- | --- |
| API별 코드·역조회 | 기본 저장소의 전체 기록 집계 |
| 요청 진단 | 보관된 HTTP 요청의 시각·최종 상태·스레드·메서드 집합 |
| 관계도 | 관계를 설명하는 애니메이션. 실제 호출 순서·메서드 span은 아님 |
| 검토 가능한 초안 | JSON 또는 비활성화된 GET/HEAD JUnit. 원본 입력 재현은 아님 |
| 기록 비교 | 보관된 시간·상태 요약의 차이. 성능 합격 자동 판정은 아님 |
| CLI 코드 diff·impact | endpoint·코드 차이와 관측된 재테스트 후보 |

`CoverageReportGenerator.generate(snapshots, aggregates)`가 probe를 코드 이름으로
해석합니다. JSON은 schema 1이며 선택적인 `requests` 상세를 기본 100개까지 담고,
생략 수를 `omittedRequestDetails`로 알립니다. endpoint 집계와 역조회는 자르지 않습니다.
상세가 없는 예전 파일도 계속 읽습니다.

실행 중 HTML은 보관 HTTP snapshot 전체로 통계를 계산하고, JSON을 다시 그린
HTML은 그 파일의 상세만 사용합니다. 전체 보관 구간의 시간 기준선은 실행 중 대시보드의
요약 내보내기를 사용합니다. 요약·테스트 초안은 CLI가 받는 coverage JSON이 아닙니다.

HTML은 서버나 외부 에셋 없이 열립니다. 브라우저의 초안·불러온 기준은 메모리에만
남아 새로고침하면 사라집니다. 본문·헤더·인증값을 수집하지 않아도 파일의 코드·API
이름은 내부 정보일 수 있으므로 자동 공개하지 않습니다.

[요청 진단](../docs/24_request_diagnostics.ko.md) |
[대시보드·CI](../docs/26_dashboard_and_ci.ko.md) |
[초안](../docs/27_test_case_drafts.ko.md) |
[기록 비교](../docs/28_recording_comparison.ko.md)
