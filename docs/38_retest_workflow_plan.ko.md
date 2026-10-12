# 재테스트 워크플로 구현 계획

2026-10-12. 사용자가 우선 구현 다섯 항목을 승인했다. 서브에이전트 없이 진행한다.

1. 시나리오: 별도 schema 1 파일로 이름·환경·코드 revision·수동 조건 저장/불러오기.
   최대 100개, 파일 1 MiB, 본문·헤더·인증·query 미지원. 불러온 항목은 다시 검토한다.
2. 내보내기: 검토된 GET/HEAD만 Postman Collection 2.1과 비활성 JMeter JMX로 내보낸다.
   변수에는 대상 주소만 두고 인증값을 포함하지 않는다. 원문 request replay라고 주장하지 않는다.
3. 실행: 새로운 CLI run 명령. 실행 동의 플래그와 명시적인 literal loopback origin 필수.
   순차 실행, 반복 1~10회, 총 1000회 이하, redirect 금지, 전체 요청 deadline과 중단 처리.
   GET도 부작용이 있을 수 있어 disposable 로컬 대상에서만 사용한다.
4. 결과: HTTP 기대 상태·선택적 client 시간 조건 판정, 별도 run JSON·정적 HTML 저장.
   응답 본문·헤더·예외 원문 미저장. 서버 어댑터 구간과 client 시간을 혼동하지 않는다.
5. 히스토리: 명시한 로컬 폴더의 실행 결과 목록·환경/revision 표시·기록별 HTML·비교.
   UI는 실행 결과 파일 import 후 현재/기준 선택·제거·묶음 저장. localStorage나 자동 공개 없음.

검증은 테스트 먼저: 입력 상한·schema·review·unsafe path·원격 origin·redirect,
상태/시간 실패·실제 loopback 통신·결과/이력 roundtrip·JS/Java 계약을 확인한다.
외부 도구 엔진 검증과 실제 브라우저 시각 검증은 수행 여부를 별도로 명시한다.
생성된 시나리오/실행 이력은 .reqover/ 또는 ignored tmp/에만 두고 소스만 commit한다.
