# 05. Repository setup

> 이 문서는 초기 저장소 준비 과정의 기록입니다. 현재 저장소는
> https://github.com/reqover-labs/reqover 에서 관리됩니다.

## 목적

이 문서는 로컬 작업 폴더를 GitHub repository와 연결하기 위한 초기 절차를 기록한 것입니다.

## 권장 repository 이름

권장 이름:

```text
reqover
```

대체 이름:

```text
reqover-java
reqover-coverage
spring-reqover
```

현재 문서와 백서가 모두 Reqover를 기준으로 작성되어 있으므로 `reqover`를 추천합니다.

## 초기화 절차

GitHub repository가 만들어지면 다음 순서로 진행합니다.

```bash
cd <repo-root>
git init
git branch -M main
git remote add origin <GITHUB_REPOSITORY_URL>
git add .
git commit -m "docs: initialize reqover planning"
git push -u origin main
```

이미 repository가 초기화되어 있다면 `git init`은 생략합니다.

## Branch 전략

대회 전까지는 단순하게 운영합니다.

- `main`: 제출 가능한 안정 상태
- `feature/phase0-spike`: Phase 0 실험
- `feature/mvc-mvp`: MVC MVP
- `feature/webflux-context`: WebFlux context propagation
- `docs/submission`: 제출 문서와 발표 자료

개발 속도가 더 중요하므로 초기에는 PR 규칙을 과하게 만들지 않습니다. 다만 1차 제출 직전에는 `main`을 반드시 실행 가능한 상태로 맞춥니다.

## Issue label 초안

GitHub issue를 쓴다면 다음 label이 유용합니다.

- `phase-0`
- `mvc`
- `webflux`
- `instrumentation`
- `report`
- `docs`
- `license`
- `demo`
- `risk`

## 첫 Milestone 제안

### Milestone 1. Phase 0 PoC

목표:

- method entry instrumentation
- `ReqoverProbe.hit`
- MVC bucket routing
- WebFlux context experiment
- JSON report sample

완료 기준:

- `docs/03_phase0_spike_plan.md`의 성공 기준을 만족합니다.
- substrate decision note를 작성합니다.

### Milestone 2. MVC MVP

목표:

- MVC sample app
- endpoint별 coverage bucket
- 동시 요청 테스트
- JSON report

완료 기준:

- README만 보고 MVC 데모를 실행할 수 있습니다.

### Milestone 3. WebFlux headline

목표:

- WebFlux sample app
- thread hop 유지 데모
- context leak 방지 검증

완료 기준:

- thread가 바뀐 기록과 동일 bucket 귀속 기록을 함께 보여줍니다.

### Milestone 4. Submission package

목표:

- README polish
- demo video scenario
- report screenshot
- license check
- result report draft

완료 기준:

- 제출물 압축 전 최종 점검표를 통과합니다.

## Commit message 규칙

간단한 Conventional Commits 스타일을 사용합니다.

예시:

```text
docs: initialize project plan
feat: add coverage bucket model
test: add concurrent mvc request test
chore: configure gradle modules
```

## Repository 첫 화면 체크리스트

GitHub repository를 공개하기 전에 README 첫 화면에서 다음이 보여야 합니다.

- Reqover가 무엇인지
- 왜 필요한지
- 현재 구현 상태
- 빠른 실행 방법
- 데모 이미지 또는 GIF
- 문서 링크
- 라이선스
- 제한 사항

