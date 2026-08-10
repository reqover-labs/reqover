# 2026 오픈소스 개발자대회 공식 결과보고서 양식

이 디렉터리의 DOCX는 대회 운영사무국이 배포한 공식 결과보고서 원본을
그대로 보존한 파일입니다. 제출용 문서는 이 원본을 직접 수정하지 않고
`scripts/build-result-report-docx.py`가 작업 사본을 만들어 생성합니다.

## 출처

- 공식 제출 안내: https://osscontest.kr/notice/39
- 공식 양식 ZIP: https://api.osscontest.kr/static/uploads/46414fba-c473-4dae-b595-7214d635b494.zip
- 확인일: 2026-08-10
- 원본 파일명: `2026 오픈소스 개발자대회 결과보고서_접수번호(팀명).docx`
- SHA-256: `937679bac40cbfaced3457530c232c9d190a74f6b5d67c58b4bc33014a579195`

무결성 확인:

```bash
shasum -a 256 "docs/competition/templates/2026 오픈소스 개발자대회 결과보고서_접수번호(팀명).docx"
```

생성기는 공식 A4 용지·여백·표 스타일을 유지하면서 안내 페이지를 제거하고,
결과보고서 본문과 필수 SBOM 붙임을 채웁니다. Reqover에는 AI 모델이 탑재되지
않으므로 해당 시 작성하는 AI 모델 붙임은 제출 사본에서 제외하며, 개발 보조
도구 활용 범위는 본문에 한 문장으로 기재합니다.

생성 환경:

```bash
python3 -m venv .venv-report
. .venv-report/bin/activate
python -m pip install -r scripts/requirements-report.txt
python scripts/build-result-report-docx.py
```
