**English** | [한국어](README.ko.md) | [Project](../README.md)

# Sample Applications

Read-only/synthetic Spring Boot samples, not production services. They include
manual probe examples and agent-recorded `/auto/` handlers. No database is needed.
Run commands from the repository root with JDK 17/21 and free ports:

```bash
git checkout v0.4.1
./scripts/run-agent-demo.sh mvc 8080
./scripts/run-agent-demo.sh webflux 8081
```

Run one per terminal; each script waits for Enter to stop its own sample. The
dashboard is at `http://127.0.0.1:<port>/reqover/report.html`. On Windows use
`scripts/run-agent-demo.ps1 -App mvc -Port 8080` or `-App webflux -Port 8081`.
Report endpoints have no authentication; the scripts bind to loopback only.

While MVC is running, send `GET /auto/diagnostics/delay/1200` or
`GET /auto/diagnostics/failure` and refresh the dashboard. The latter deliberately
returns 503; delay values outside 0-2000 ms return 400. WebFlux equivalents start
with `/auto/reactive/diagnostics/`.

The default automatic MVC example records AutoOrderController and
AutoOrderService, not trivial AutoOrderResponse accessors. Manual probes may
coexist with agent probes if you instrument the whole sample package; they are
learning examples, not a requirement for integrating your own application.

`./scripts/run-impact-demo.sh 8080` records traffic, exports JSON and HTML under
`build/reqover-impact-demo/`, and demonstrates CLI retest candidates.
`run-agent-demo.sh ... --stop-after-report` prints JSON then stops; it does not
save an offline HTML file by itself. Use export settings or the download button.

[Quickstart](../README.md#try-it-in-5-minutes) |
[Request guide](../docs/24_request_diagnostics.md) |
[CI recording](../docs/18_ci_impact_analysis.md)
