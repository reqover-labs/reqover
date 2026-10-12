# PR 53 마무리 계획과 검증 기록

2026-10-12. 서브에이전트 없이 기존 작업 브랜치에서 진행한다.

## 목표와 제약

재테스트 워크플로 구현 이후 남은 보안 검사와 회귀 검증을 마무리한다.
Spring Boot 3.5 호환성, 기존 coverage JSON, 수집기, Action 계약을 유지한다.
기능 확장 이슈 #54~#63은 별도 작업이다. 이번에 병합·릴리스하지 않는다.
비밀값·개인 관측 데이터·검증 로그·다운로드한 엔진은 commit하지 않는다.

## 순서

- [x] CI 실패 로그와 동일한 SBOM을 OSV에 재조회하여 실패 재현.
- [x] Spring 공식 공지와 현재 샘플/어댑터의 적용 조건 확인.
- [x] 보안 예외 정책·만료·미등록 취약점 차단·샘플 범위 회귀 테스트 추가.
- [x] 공개 수정판 없는 Spring 6 공지만 제한적·기한부 예외로 문서화.
- [x] 전체 Java/Node 검증, 실제 loopback 실행, 외부 export 계약 재검증.
- [x] 한국어/영어 README와 보안 정책 최신화.
- GitHub 최종 CI 상태는 [PR 53 checks](https://github.com/reqover-labs/reqover/pull/53/checks)에서 해당 commit 기준으로 확인한다.

## 재현된 원인

SBOM 외부 Maven 좌표 104개를 조회한 결과 Spring WebFlux/MVC 6.2.19에서
추가 공지 2개, 구성요소별 3건이 차단됐다. 기존 XsltView 공지 1건은 별도
기한부 예외였다. 패키지 버전·SBOM·검사 severity를 바꾸어 숨기지 않는다.

- [CVE-2026-47892](https://spring.io/security/cve-2026-47892/):
  WebFlux functional endpoint를 DispatcherServlet에 배포하는 조건.
- [CVE-2026-47890](https://spring.io/security/cve-2026-47890/):
  공격자 제어 데이터를 SSE view fragments로 다른 사용자에게 전송하는 조건.

공식 공지는 7.0.9가 OSS 수정판, 6.2.20은 enterprise 지원 전용이라고 설명한다.
Boot 3.5에 일부 Spring 7 JAR만 덮어쓰는 것은 안전한 패치로 취급하지 않는다.
현재 샘플에는 위 조건이 없지만 소비자 애플리케이션까지 안전하다는 뜻은 아니다.
예외로 통과하는 상태를 취약점 0건 또는 패치 완료라고 표현하지 않는다.

## 실제 수행한 검증

- Java 21 전체 build/SBOM 재생성: 90개 task 실행, Java 202개 테스트,
  실패·오류·skip 0. 기존 Gradle deprecation 경고는 별도 정리 대상으로 남긴다.
- Node 모델/controller/생성 Java 검증: 46개 통과. 실제 브라우저 E2E는 아님.
- 보안 정책: 7개 통과. 수정 전 예외 정책 테스트의 실패를 확인하고 수정 후 통과.
  빌드된 published library JAR에 Spring 클래스·중첩 JAR가 없고,
  WebFlux 샘플에는 reactive Netty만 있으며 servlet stack이 없음을 검사.
- SBOM lock은 재생성된 의존성 목록과 일치. 104개 좌표 OSV 재조회:
  미예외 finding 0, 예외 3개 ID/구성요소별 4건. 새 2개 ID는 2026-11-12 만료.
- 실제 CLI 로컬 fixture: 동의 없으면 요청 0건. 실제 8건 호출로 실패 exit 1 →
  수정 후 exit 0, 실행 이력 2개·관측 기록 1개·결과/비교 생성 확인.
- Postman Collection 2.1 공식 schema 통과. 공식 JMeter 5.6.3 headless 검증:
  비활성 계획 0건, 검토 후 활성화한 fixture 2건 중 의도한 assertion 실패 1건.
- 문서 링크/anchor 324개 통과, 기존 릴리스 CLI checksum 및 smoke 9개 통과.

재현 순서:

```bash
./gradlew build cyclonedxBom --rerun-tasks --no-daemon --console=plain
python3 scripts/test-osv-policy.py
python3 scripts/verify-sbom-lock.py build/reports/bom/reqover.cdx.json sbom/reqover.cdx.json
python3 scripts/check-sbom-osv.py sbom/reqover.cdx.json
node --test scripts/*.test.cjs
node scripts/verify-local-scenarios.cjs reqover-cli/build/libs/reqover-cli-0.4.2.jar
```

정책 테스트는 실제 빌드 JAR도 확인하므로 build 뒤 실행한다. 소스 JAR 이름의
0.4.2가 배포된 0.4.2 바이너리에 새 명령이 있다는 뜻은 아니다.
CI는 Java 17/21 및 실제 OSV-Scanner를 별도로 실행한다.

## 화면 검증 경계

최신 화면의 실제 브라우저 E2E·모바일·라이트/다크 시각 검증은 현재 도구의
로컬 파일 접근 제한 때문에 미완료다. 다른 주소/브라우저/간접 실행으로 우회하지 않는다.
모델/controller 대역과 정적 소스 검사는 이 검증을 대체하지 않는다.
PR에 미완료 항목을 유지하고 실제 화면을 검증하기 전 최종 UI 승인으로 표시하지 않는다.
