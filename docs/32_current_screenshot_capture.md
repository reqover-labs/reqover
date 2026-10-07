**English** | [한국어](32_current_screenshot_capture.ko.md) | [Documentation](README.md)

# Current README Screenshot Capture

Captured on October 7, 2026 (Asia/Seoul) from actual local HTTP requests to the
**v0.4.2** samples. Source commit:
`0afe11bb4e2e37054fce36ae25db89421d1363de`.
These are actual application screenshots, not generated UI mockups. Recorded
request data is unchanged; the comparison baseline is synthetic as described below.

## Environment and Scope

- Windows capture host, Java 21.0.10, Playwright 1.62.1 Chromium.
- Agent and both sample JARs built from the exact release tag, not current main.
- Services bound to `127.0.0.1` on unused ephemeral ports. Both capture-owned
  processes were stopped afterward; no other running demo was interrupted.
- Agent includes: `io.reqover.example.mvc.auto` and `io.reqover.example.webflux.auto`.
- Default `accessors=skip`, `references=skip`; no response-accessor recording.
- Real report HTML saved before browser capture, with report data unchanged.
- Light mode, device scale 2. Request detail: 1280 x 800 viewport; filtered
  candidates: 1280 x 720; paused retest map: 1280 x 900.
- All current README and feature-guide dashboard screenshots use light mode.
  MVC overview and test draft: 1280 x 900; MVC slow detail: 1280 x 720;
  comparison: 1280 x 1000 with full-page capture. Dark-mode application support
  and the theme-adaptive README wordmark are unchanged.

| Current asset | View and verification |
| --- | --- |
| `assets/reqover-webflux-request-detail.png` | Requests, one expanded `/auto/reactive/orders/{id}` observation; two classes, four methods, three thread names |
| `assets/reqover-retest-candidates.png` | Retest candidates with the ordinary `SharedValidator` text filter; one shared-method row, two API names |
| `assets/reqover-retest-map-current.png` | Overview with Retest map selected and animation paused; shared method connected to two observed endpoints |
| `assets/reqover-request-diagnostics.png` | Five actual MVC requests; delayed `req-4` selected, animation paused, one intentional 503 |
| `assets/reqover-request-detail.png` | Slow filter at 1000 ms, one of five requests shown; expanded delay observation |
| `assets/reqover-test-case-draft.png` | Draft linked to `req-4`; manually entered path, expected 200 and 500 ms limit; disabled Java preview expanded |
| `assets/reqover-recording-comparison.png` | Actual five-request summary compared with `synthetic-baseline.json`; timings doubled only in the imported baseline, conditions unchecked |

## Request Sequence

MVC records five requests: `GET /auto/orders/42`, `GET /orders/42`,
`POST /payments`, `GET /auto/diagnostics/delay/1200`, and
`GET /auto/diagnostics/failure`. The first four return 200 and the final demo
request deliberately returns 503. No original request body or authentication
is needed. The manual-probe examples intentionally use simple class names;
`SharedValidator#validate` maps to `GET /orders/{id}` and `POST /payments`.

WebFlux records one `GET /auto/reactive/orders/42` returning 200. Its observation
contains `boundedElastic-1`, `parallel-1` and `reactor-http-nio-2`, with methods
`AutoReactiveOrderController.find` and `AutoReactiveOrderService.find`,
`toResponse`, `validate`. `AutoReactiveOrderResponse` accessor methods are absent.
Exact thread suffixes, request IDs, timestamps and intervals can vary on rerun.

## Interpretation and Safety

The draft path `/auto/diagnostics/delay/120` differs from the recorded call's
`/1200` intentionally: it demonstrates a manually reviewed test choice, not
captured-input recovery or successful replay. The generated test remains disabled.
For comparison, the actual summary is left unchanged. A separate clone doubles
its average, p95, maximum and cumulative intervals, leaving counts/statuses and
the generation timestamp unchanged. It is explicitly imported as
`synthetic-baseline.json`, not described as another measured run or an improvement.

Times illustrate this small demo, not a benchmark. Graph edges are observed
associations, not measured invocation order. The candidate screenshot is
filtered intentionally; it does not imply only one method exists in the report.
No private traffic, tokens, user recordings, host paths or log files are published.

Checks: actual response statuses and JSON counters, request-specific code,
thread attribution, shared endpoint mapping and visible browser state. Capture
had zero browser errors and zero external HTTP(S) asset requests. All PNGs were
visually inspected for readable, unclipped content.

Original table screenshots remain as historical assets with their
[old provenance](16_readme_demo_capture.md); current READMEs use the new filenames.
