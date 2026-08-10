# 12. Release and Submission Checklist

이 문서는 `v0.1.0` release candidate와 대회 제출본을 같은 소스로 고정하기 위한 최종 점검표입니다.

## 1. 사용자 입력 확정

- [ ] 접수번호와 팀명
- [ ] 팀 인원
- [ ] 학생/일반 참가부문
- [ ] 자유/지정 과제유형
- [ ] 접수 당시 프로젝트 설명과 현재 Java 프로젝트의 일치 여부
- [ ] 팀원별 역할·기여
- [ ] 정부지원·정부 대회 수상 등 중복수혜 여부
- [ ] 공개 또는 일부 공개 YouTube URL

## 2. Clean Build

JDK 17과 21의 깨끗한 환경에서 각각 실행합니다.

```bash
./gradlew clean build cyclonedxBom --no-daemon --console=plain
```

필수 산출물:

```text
build/reports/bom/reqover.cdx.json
reqover-agent/build/libs/reqover-agent-0.1.0.jar
examples/mvc-sample/build/libs/mvc-sample-0.1.0.jar
examples/webflux-sample/build/libs/webflux-sample-0.1.0.jar
```

- [ ] 전체 test failure 0
- [ ] Java 17 build 성공
- [ ] Java 21 build 성공
- [ ] agent JAR에 `META-INF/LICENSE-REQOVER`, `NOTICE-REQOVER`, `THIRD_PARTY_NOTICES.md`, `LICENSE-ASM` 포함
- [ ] agent JAR에 원래 `org/objectweb/asm` package가 없고 relocated ASM만 포함

## 3. Demo E2E

```bash
./scripts/run-agent-demo.sh mvc 18080 --stop-after-report
./scripts/run-agent-demo.sh webflux 18081 --stop-after-report
```

- [ ] 두 endpoint 모두 HTTP 200
- [ ] MVC report에 controller/service method-entry가 나타남
- [ ] WebFlux report에 transport별 `reactor-http-*`, `boundedElastic`, `parallel` thread가 나타남
- [ ] WebFlux `validate(J)J`가 thread hop 이후에도 같은 endpoint에 귀속됨
- [ ] 사용 중인 port로 실행했을 때 명확히 실패함
- [ ] 인증 없는 report endpoint는 `127.0.0.1`에만 바인딩됨

## 4. SBOM, License, Vulnerability

```bash
mkdir -p sbom
cp build/reports/bom/reqover.cdx.json sbom/reqover.cdx.json
```

- [ ] CycloneDX JSON schema 유효
- [ ] root project와 내부 `io.reqover` 모듈은 Apache-2.0으로 설명됨
- [ ] `THIRD_PARTY_NOTICES.md`의 주요 버전이 resolved dependency와 일치
- [ ] OSV scan 결과 취약점 0건 또는 예외 사유 문서화
- [ ] 공식 결과보고서 SBOM 표와 `sbom/reqover.cdx.json`이 일치
- [ ] source repository, release bundle, agent JAR의 license notice가 일치

## 5. Secret and Repository Security

GitHub의 private vulnerability reporting, Dependabot alerts/updates, secret scanning/push protection을 가능한 범위에서 활성화합니다.

전용 scanner가 있으면 현재 tree뿐 아니라 Git history 전체를 검사하고 결과를 보관합니다. 단순 문자열 검색만으로 “secret 없음”을 증명하지 않습니다.

- [ ] `.env`, private key, access token, 개인 credential 미포함
- [ ] 발견한 credential은 파일 삭제만 하지 않고 먼저 폐기·회전
- [ ] `SECURITY.md`의 신고 경로와 GitHub security 설정 일치
- [ ] Actions dependency는 immutable commit SHA로 pin
- [ ] Gradle wrapper checksum 검증

## 6. Documentation Consistency

- [ ] `README.md`
- [ ] `docs/02_architecture.md`
- [ ] `docs/09_agent_e2e_demo.md`
- [ ] `docs/10_demo_script.md`
- [ ] `docs/11_performance_measurement.md`
- [ ] `docs/15_performance_results.md`
- [ ] `docs/17_integration_guide.md`
- [ ] `LICENSE`, `NOTICE`, `THIRD_PARTY_NOTICES.md`, `CHANGELOG.md`
- [ ] version, JDK, Spring Boot, dependency, command, report 용어가 모두 동일
- [ ] line/branch coverage, 완전한 영향 분석, production-ready 같은 미구현 주장이 없음

## 7. Official Result Report

공식 원본과 hash:

```text
docs/competition/templates/2026 오픈소스 개발자대회 결과보고서_접수번호(팀명).docx
SHA-256 937679bac40cbfaced3457530c232c9d190a74f6b5d67c58b4bc33014a579195
```

최종 SBOM 생성 후 초안을 만듭니다.

```bash
python3 -m venv .venv-report
. .venv-report/bin/activate
python -m pip install -r scripts/requirements-report.txt
python3 scripts/build-result-report-docx.py \
  --team-name "팀명" \
  --team-size "2" \
  --division "학생 또는 일반" \
  --task-type "자유과제 또는 지정과제" \
  --registration-number "접수번호" \
  --video-url "YouTube URL" \
  --development-environment "macOS 15.7.3, Apple M1, 16 GB RAM"
```

- [ ] 공식 안내 페이지 제거
- [ ] 공식 A4·여백·표 구조 유지
- [ ] 본문 최대 5쪽
- [ ] 필수 SBOM 붙임 포함
- [ ] AI 모델 붙임 제외, 개발 과정 보조 범위는 본문 한 문장만 기재
- [ ] `[확인 필요: ...]` placeholder 0개
- [ ] DOCX와 PDF 내용 동일
- [ ] Word/맑은고딕 환경에서 전 페이지 한글·표·그림·페이지 검수
- [ ] 최종 파일명 `2026 오픈소스 개발자대회 결과보고서_접수번호(팀명)`

## 8. Performance Evidence

동일 runtime candidate와 동일 endpoint에서 baseline/agent를 각각 측정합니다.

- [ ] commit SHA, 날짜, OS, CPU, 메모리, JDK 배포판·버전 기록
- [ ] warmup/측정 횟수와 percentile 방식 기록
- [ ] raw sample 보관
- [ ] local sequential sanity check임을 명시
- [ ] final code 변경 후 예전 수치를 재사용하지 않음

## 9. Pull Request and Release

- [ ] 의도한 변경만 commit
- [ ] PR의 Java 17/21 build와 OSV check 모두 green
- [ ] review 후 `main` merge
- [ ] release tag가 현재 `main` commit과 정확히 일치

```bash
git tag -s v0.1.0 -m "Reqover 0.1.0"
git push origin v0.1.0
```

서명 tag를 사용할 수 없는 환경이면 annotated tag를 사용하고 이유를 release 기록에 남깁니다. Tag push 후 release workflow가 checksum, SBOM, JAR, ZIP을 게시하는지 확인합니다.

## 10. Video and Portal

- [ ] 최종 tag 소스로 3분 이내 영상 촬영
- [ ] 음성·자막·코드 글자 가독성 확인
- [ ] 로그아웃/시크릿 창에서 YouTube URL 재생
- [ ] GitHub repository와 Release를 로그아웃 상태에서 열기
- [ ] 포털 입력값과 결과보고서 값 교차검수
- [ ] 제출 완료 화면 캡처 및 확인 메일 보관
- [ ] 내부 마감 2026-08-26 18:00 KST 준수
