[English](39_retest_workflow.md) | **한국어** | [문서 목차](README.ko.md)

# 시나리오 저장·실행·이력 관리 (미출시)

현재 브랜치의 소스 기능이며 배포된 0.4.2 JAR에는 없습니다. 이 브랜치에서 CLI를
빌드해야 새 명령이 동작합니다. 기존 수집기·coverage JSON·Action 계약은 유지합니다.

## 다섯 기능의 연결

1. Test drafts에서 실제 경로·기대 상태·선택적 client 시간 조건을 입력하고 검토합니다.
   필요 없는 초안이나 POST 같은 항목은 Remove selected draft로 제거합니다.
2. Scenarios and runs의 Scenario 보기에서 이름·환경·코드 revision을 입력하고 저장합니다.
   최대 100개 사례를 저장합니다. 파일은 schema 1이며 원래 본문·헤더·인증·query를 담지 않습니다.
3. 저장 파일을 불러오면 기존 편집은 보존하고 사례를 추가합니다. 새로 불러온 것은
   다시 검토해야 합니다. 잘못된 파일은 기존 상태를 덮어쓰지 않습니다.
4. 검토된 GET/HEAD만 Postman Collection 2.1 또는 비활성 JMeter 계획으로 내보냅니다.
   Postman baseUrl은 비어 있고, JMeter는 host/port를 직접 설정하고 계획을 활성화해야 합니다.
   다운로드만으로 실행하지 않으며 ${...}가 들어간 이름은 JMeter 함수로 해석되지 않게 처리합니다.
5. 로컬 CLI로 실행해 JSON·HTML을 남기고 Run history 또는 Recordings에서 이력을 확인합니다.
   브라우저 페이지 자체는 요청을 재실행하지 않습니다.

페이지 상태는 메모리이고 새로고침하면 사라집니다. 저장 파일과 로컬 폴더가 영구 이력입니다.
기존 HTML 다운로드는 원래 관측 리포트만 저장하므로 불러온 사례/결과는 따로 저장합니다.

## 실제 로컬 실행

```bash
./gradlew :reqover-cli:shadowJar
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar run \
  --scenario scenario.json --base-url http://127.0.0.1:8080 \
  --confirm-local --repeat 2 --history-dir .reqover/runs
```

명시한 literal loopback `127.0.0.1` 또는 `[::1]`만 허용합니다. 원격 QA 서버는 아직
지원하지 않습니다. origin에 인증·query·fragment·추가 base path를 넣을 수 없습니다.
GET도 부작용이 있을 수 있어 disposable 로컬 앱에서만 사용합니다. 운영에 사용하지 않습니다.
proxy·redirect·인증·cookie·본문을 설정하지 않습니다.

반복 1~10회·사례 최대 100개·계획된 시도 최대 1000회이며 순차 실행입니다.
연결 5초, 본문 종료까지 요청 10초, 전체 실행 60초 상한을 둡니다. 중단하면 추가 요청을
보내지 않고 대기 중인 요청을 취소합니다. 서버에서 이미 수행한 작업까지 되돌리지는 못합니다.
실행 전 이력 폴더 쓰기를 확인하며 파일 이름은 고유하고 기존 파일을 덮어쓰지 않습니다.
저장 오류가 발생하면 JSON·HTML 쌍 일부만 남을 수 있습니다.

종료 코드 0은 계획한 모든 시도가 검토한 기대 상태·시간 조건을 만족했다는 뜻입니다.
1은 실패나 미실행, 2는 입력/사용/저장 오류입니다. 예상한 4xx·5xx도 테스트는 통과할 수 있습니다.
시간은 System.nanoTime 기반 client 시도 시간입니다. 서버 어댑터 시간과 다르고,
소수 시도의 p95를 처리 용량이나 성능 안정성 증거로 해석하지 않습니다.

## 실행 결과·이력

자동 저장하는 .run.json에는 시도별 case ID, 실제 상태 또는 null, client 시간,
결과 코드·완료 상태·계획 수·시나리오 조건·환경·revision·origin을 담습니다.
응답 본문·헤더·예외 원문은 저장하지 않습니다. 함께 생성하는 정적 HTML로 결과를 봅니다.

```bash
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar runs --out history.json
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar run-report --result run.json --out run.html
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar run-compare --baseline before.run.json --current after.run.json --out comparison.html
```

CLI는 폴더 최대 1000개 파일 중 최근 20개 목록을 내보내고 생략 수를 알립니다.
UI Run history도 최대 20개를 불러와 메타데이터 검색·현재/기준 선택·페이지에서 제거·
묶음 저장을 제공합니다. 제거는 원본 디스크 파일을 지우지 않습니다. 상세는 처음 100개,
통계는 해당 실행의 모든 시도를 사용합니다. 조건·origin·환경을 비교하고 revision은
달라도 됩니다. 표본 수·계획 수·미완료에 경고하며 자동 성능 합격을 판정하지 않습니다.
불러온 파일이 실제 실행을 증명하는 인증된 자료라는 의미도 아닙니다.

## 요청 관측 이력은 별도로 관리

```bash
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar recording-save \
  --report coverage.json --name "Before change" --environment local --revision abc
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar recordings --out recording-catalog.json
java -jar reqover-cli/build/libs/reqover-cli-0.4.2.jar recording-report --entry snapshot.recording.json --out restored.html
```

.reqover/recordings에 이름·환경·revision·정규화된 coverage JSON·HTML을 저장합니다.
알 수 없는 입력 필드는 복사하지 않고, 원본에서 상세가 생략된 수를 별도로 남겨
복원한 HTML에서 표시합니다. 입력 최대 10 MiB·상세 10000개, 저장 파일 최대 20 MiB,
목록 최대 1000개 스캔·최근 20개 표시입니다. 잘린 입력으로 사라진 요청을 복구하지 않습니다.

UI Recordings는 저장 기록/목록을 불러오되 raw 상세 문자열은 보관하지 않고 요약과
메타데이터만 사용합니다. 서버 어댑터 시간·HTTP 상태를 기존 비교 규칙으로 비교합니다.
Run history의 client 시간·테스트 판정과 혼동하지 않습니다. UI에서 저장하는 목록은
휴대 가능한 요약 이력이며 디스크 원본 archive를 대체하지 않습니다.

## 검증과 남은 범위

Node 모델/controller 대역: 저장·불러오기·재검토·입력 실패 보존·이력 상한·잘못된
조건·인증 필드 차단·비교를 검증합니다. Java는 직접 띄운 loopback 서버로 동의·상태·
시간 실패·redirect 거부·파일 이력을 확인합니다. CLI 전체 흐름은 8건의 실제 로컬 요청으로
실패 전 exit 1 → 수정 후 exit 0 → 이력 2개 → 보고서·비교 생성까지 확인합니다.
Postman은 공식 스키마, JMeter 5.6.3은 checksum 검증한 공식 바이너리로 headless 실행해
상태 assertion과 비활성 계획의 무요청을 확인합니다.

앱 브라우저의 최신 시각 검증·E2E·모바일/다크 배치는 아직 미완료입니다. 제한을
우회하지 않았습니다. 원격 실행·원래 트래픽 재현·로그인/인증·응답 변수 연결·동시 부하·
정기 실행·메서드/DB span·자동 공개는 추가하지 않았습니다. 파일에는 내부 경로·이름이
있을 수 있으므로 .reqover/와 검증 파일을 자동 commit하지 않습니다.

공식 형식 참고: [Postman schemas](https://schema.postman.com/),
[JMeter HTTP sampler](https://jmeter.apache.org/usermanual/component_reference.html#HTTP_Request).
