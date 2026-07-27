# 12. Release Checklist

## Pre-Release Verification

Run:

```powershell
.\gradlew.bat clean build cyclonedxBom
```

Required outputs:

```text
build/reports/bom/reqover-sbom.json
reqover-agent/build/libs/reqover-agent-0.1.0-SNAPSHOT.jar
examples/mvc-sample/build/libs/mvc-sample-0.1.0-SNAPSHOT.jar
examples/webflux-sample/build/libs/webflux-sample-0.1.0-SNAPSHOT.jar
docs/submission/Reqover_result_report_draft.docx
```

## Demo Verification

Run:

```powershell
.\scripts\run-agent-demo.ps1 -App mvc -Port 8080 -StopAfterReport
.\scripts\run-agent-demo.ps1 -App webflux -Port 8080 -StopAfterReport
```

Check:

- `/reqover/report` returns JSON.
- `/reqover/report.html` renders in a browser.
- Auto MVC endpoint appears in the report.
- Auto WebFlux endpoint appears in the report.
- WebFlux report includes more than one thread name.

## Security Check

Run:

```powershell
rg -n "(?i)(api[_-]?key|secret|password|token|\\.env)" -S --glob "!**/build/**" --glob "!**/.gradle/**"
```

Rules:

- Do not commit `.env`.
- Do not commit API keys.
- Do not commit access tokens.
- Do not commit local credentials or private certificates.
- If a secret is accidentally committed, rotate it before continuing.

## Documentation Check

Required files:

- `README.md`
- `LICENSE`
- `THIRD_PARTY_NOTICES.md`
- `CONTRIBUTING.md`
- `SECURITY.md`
- `CODE_OF_CONDUCT.md`
- `docs/06_submission_requirements.md`
- `docs/10_demo_script.md`
- `docs/11_performance_measurement.md`
- `docs/12_release_checklist.md`
- `build/reports/bom/reqover-sbom.json`

## Competition Submission Check

Prepare:

- GitHub repository URL
- YouTube demo URL
- result report DOCX/HWPX
- result report PDF
- SBOM JSON
- license summary
- AI model usage statement
- known limitations

Result report must state:

```text
Reqover does not embed an AI model in the submitted software. AI tools were used only as development assistance if applicable.
```

## Suggested Tagging

After all checks pass:

```powershell
git tag v0.1.0-mvp
git push origin v0.1.0-mvp
```

Only tag after the repository is in a reproducible state.

## DOCX/PDF Note

The draft DOCX can be regenerated with:

```powershell
& 'C:\Users\1043t\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe' scripts\build-result-report-docx.py
```

PDF conversion requires a working local Word or LibreOffice installation. In the current local run, LibreOffice was not installed and Word COM PDF export did not complete, so the DOCX draft is prepared but the final PDF should be exported manually from Word after team metadata and video URL are filled.
