# 2026 오픈소스 개발자대회 공식 결과보고서 양식

이 디렉터리에는 공식 양식 DOCX를 **두지 않습니다.** 대회 운영사무국이 배포한
저작물이라 Apache-2.0으로 공개하는 이 저장소가 재배포할 대상이 아닙니다.
아래 출처에서 직접 내려받아 이 디렉터리에 두면 생성기가 그대로 동작합니다.

## 내려받기

- 공식 제출 안내: https://osscontest.kr/notice/39
- 공식 양식 ZIP: https://api.osscontest.kr/static/uploads/46414fba-c473-4dae-b595-7214d635b494.zip
- 확인일: 2026-08-10

ZIP 안의 `2026 오픈소스 개발자대회 결과보고서_접수번호(팀명).docx`를 이 디렉터리에
그대로 둡니다. 생성기는 파일이 없으면 명확한 오류로 멈추고, 있으면 아래 해시와
대조해 원본이 맞는지 먼저 확인합니다.

```bash
shasum -a 256 "docs/competition/templates/2026 오픈소스 개발자대회 결과보고서_접수번호(팀명).docx"
# 937679bac40cbfaced3457530c232c9d190a74f6b5d67c58b4bc33014a579195
```

다른 위치에 두었다면 `--template` 으로 경로를 넘깁니다.

## 생성

생성기는 원본을 수정하지 않고 작업 사본을 만듭니다. 공식 A4 용지·여백·표 스타일을
유지하면서 안내 페이지를 제거하고, 본문과 필수 SBOM 붙임을 채웁니다. Reqover에는
AI 모델이 탑재되지 않으므로 해당 시 작성하는 AI 모델 붙임은 제외하며, 개발 보조도구
활용 범위는 본문에 한 문장으로 기재합니다.

```bash
python3 -m venv .venv-report
. .venv-report/bin/activate
python -m pip install -r scripts/requirements-report.txt
python scripts/build-result-report-docx.py --template "<내려받은 양식 경로>"
```
