# Reviewed Test Case Drafts

**Goal:** Turn a selected observed HTTP request into a reviewable regression-test
draft, continuing the mentor's discovery-to-revalidation workflow.

**Architecture:** A small pure JavaScript generator creates a distinct case JSON
and a disabled JUnit 5/Java 17 HttpClient test. An offline editor owns drafts in
memory. It receives selected request metadata from the existing dashboard, with
no collector, instrumentation, coverage schema, CLI, or deployment changes.

**Execution:** Inline, no subagents; continue the existing preview PR without merging.

## Constraints

- A mapped route is not the captured concrete path or original input.
- Never copy an observed failure into the expected status automatically.
- Do not collect, import, persist, or invent headers, body, query values, or credentials.
- GET/HEAD only for JUnit export; mutating requests require a future reset/safety design.
- Require a reviewed concrete path without query/fragment/template placeholders,
  a final expected status, and an explicit review checkbox before JUnit export.
- Optional client-time assertion is manually set, not inferred from adapter timing.
- Every generated test is `@Disabled` and requires an explicit local/QA base URL.
- No network request or test execution occurs in the browser or during export.
- JSON drafts may be incomplete and always declare `replayable: false`.
- Keep native no-script report views and legacy JSON compatibility unchanged.
- Keep `.env`, keys, generated user-specific cases, and private screenshots out of Git.

## Steps

- [x] Read the mentoring roadmap, dashboard, request model, and troubleshooting history.
- [x] Add pure-generator and renderer tests; verify missing behavior fails.
- [x] Implement safe JSON/JUnit generation and the offline Test drafts editor.
- [x] Connect selected requests and native request detail actions to the editor.
- [x] Test unknown status, non-HTTP/mutating methods, incomplete review, hostile
  strings, path validation, multiple drafts, export/reopen, and legacy/no-script views.
- [x] Compile generated Java using the project's actual JUnit API; run Java, JS,
  and desktop/mobile browser regression checks.
- [x] Update README, mentoring plan, feature guide, and troubleshooting notes.
- [x] Review staged files for secrets, push, and update PR #25 without merging.

## Verification

```powershell
node --test scripts/test-case-drafts.test.cjs
.\gradlew.bat :reqover-report:test :reqover-cli:test :reqover-cli:shadowJar
node scripts/verify-dashboard.cjs sample.html legacy.html tmp/dashboard-check
```

Java generated from synthetic fixtures is compiled with JDK 17-compatible syntax
and JUnit 5.12.2. Existing OSV failures are not waived or presented as resolved.

Local verification: 155 Java tests with no failures/errors/skips; ten Node tests
including actual generated Java compilation; Playwright covers the draft workflow
and all prior report behaviors with zero external requests and page errors.
Actionlint passes the build workflow. Only synthetic case data appears in screenshots.
