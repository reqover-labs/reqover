package io.reqover.report;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** The request diagnostics sections of the standalone coverage report. */
final class RequestDiagnosticsHtmlRenderer {
    private static final int MAX_DETAIL_ROWS = 100;

    static final String STYLES = """
            .report-nav { display: flex; gap: 24px; flex-wrap: wrap; padding: 16px 0; border-bottom: 1px solid var(--rule); }
            .report-nav a { color: var(--ink-2); text-decoration: none; font-size: 13px; }
            .report-nav a:hover { color: var(--verb-get); text-decoration: underline; }
            .diagnostics-title { margin: 0 0 20px; font-size: 20px; letter-spacing: 0; }
            .metrics { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 20px; padding-bottom: 22px; border-bottom: 1px solid var(--rule); }
            .metric-label { color: var(--ink-2); font-size: 12px; display: block; }
            .metric-value { font-size: 25px; font-weight: 650; font-variant-numeric: tabular-nums; display: block; margin: 6px 0; overflow-wrap: anywhere; }
            .metric-detail { color: var(--ink-3); font-size: 11px; }
            .http-failure { color: var(--verb-delete); }
            .http-success { color: var(--verb-get); }
            .diagnostic-scroll { overflow-x: auto; }
            .diagnostic-table { min-width: 640px; font-variant-numeric: tabular-nums; }
            .diagnostic-table th, .diagnostic-table td { width: auto; }
            .diagnostic-table th:first-child, .diagnostic-table td:first-child { width: 32%; }
            .request-tools { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; margin-bottom: 16px; }
            .request-tools label { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--ink-2); }
            .request-tools select, .request-tools input { font: inherit; color: var(--ink); background: var(--bg); border: 1px solid var(--rule-strong); padding: 7px; border-radius: 4px; }
            .request-tools input { width: 90px; }
            .request-detail { border-top: 1px solid var(--rule); }
            .request-detail summary { cursor: pointer; padding: 15px 0; display: flex; align-items: center; flex-wrap: wrap; gap: 12px; }
            .request-detail summary::before { content: '+'; color: var(--ink-3); width: 12px; flex-shrink: 0; }
            .request-detail[open] summary::before { content: '-'; }
            .request-name { flex: 1 1 220px; min-width: 0; }
            .request-id { color: var(--ink-3); flex: 0 1 120px; }
            .request-duration { min-width: 90px; text-align: right; font-variant-numeric: tabular-nums; }
            .request-status { min-width: 90px; text-align: right; font-variant-numeric: tabular-nums; }
            .request-fields { display: grid; grid-template-columns: 90px minmax(0, 1fr); gap: 7px 16px; margin: 0 0 18px 24px; font-size: 12px; }
            .request-fields dt { color: var(--ink-3); }
            .request-fields dd { margin: 0; overflow-wrap: anywhere; }
            .request-detail .request-code { margin-left: 24px; margin-bottom: 20px; font-size: 12px; }
            .request-code p { margin: 7px 0; }
            .request-detail summary:focus-visible { outline: 2px solid var(--verb-patch); outline-offset: 2px; }
            @media (max-width: 640px) {
              .metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
              .request-id { flex-basis: 100%; margin-left: 24px; }
              .request-duration, .request-status { text-align: left; min-width: 80px; }
              .request-fields { margin-left: 0; grid-template-columns: 70px minmax(0, 1fr); }
              .request-detail .request-code { margin-left: 0; }
              .topbar .wrap { height: auto; min-height: 56px; flex-wrap: wrap; padding-top: 12px; padding-bottom: 12px; }
              .stamp { overflow-wrap: anywhere; }
              .wrap { padding-left: 16px; padding-right: 16px; }
            }
            """;

    private RequestDiagnosticsHtmlRenderer() {
    }

    static String render(CoverageReport report) {
        List<RequestObservation> requests = report.requests().stream().filter(RequestObservation::isHttp).toList();
        StringBuilder html = new StringBuilder();
        html.append("<nav class=\"report-nav\" aria-label=\"Report sections\"><a href=\"#request-overview\">Overview</a>");
        if (!requests.isEmpty()) {
            html.append("<a href=\"#request-list\">Requests</a>");
        }
        html.append("<a href=\"#endpoint-code\">API to code</a>"
                + "<a href=\"#code-endpoint\">Code to API</a></nav>");
        html.append("<section class=\"section\" id=\"request-overview\"><h2 class=\"diagnostics-title\">Request diagnostics</h2>");
        if (requests.isEmpty()) {
            html.append("<p class=\"section-note\">No per-request diagnostics. Older reports contain endpoint unions only; "
                    + "a new recording supplies individual timing and status.</p></section>");
            return html.toString();
        }

        RequestSummary summary = RequestSummary.from(requests);
        html.append("<div class=\"metrics\">");
        metric(html, "Recorded HTTP requests", Integer.toString(summary.requestCount()), "Retained observations", false);
        metric(html, "HTTP 4xx / 5xx", summary.clientErrorCount() + " / " + summary.serverErrorCount(),
                percent(summary.httpFailurePercent()) + " of " + summary.knownStatusCount() + " known statuses", true);
        metric(html, "Average", time(summary.averageMillis()), summary.timedRequestCount() + " timed samples", false);
        metric(html, "p95", time(summary.p95Millis()), "Nearest-rank over timed samples", false);
        metric(html, "Maximum", time(summary.maximumMillis()), summary.unknownStatusCount() + " unknown statuses", false);
        html.append("</div><p class=\"section-note\">Observed processing time is the server adapter's recorded wall-clock interval. "
                + "It excludes unobserved traffic and is not client response time, CPU time, or TPS. "
                + "Unfinished or negative intervals are not timed. Overview totals stay fixed while filtering.</p>");

        Map<String, List<RequestObservation>> byEndpoint = new TreeMap<>();
        for (RequestObservation request : requests) {
            byEndpoint.computeIfAbsent(request.endpoint(), ignored -> new java.util.ArrayList<>()).add(request);
        }
        html.append("<div class=\"diagnostic-scroll\"><table class=\"diagnostic-table\"><thead><tr>"
                + "<th scope=\"col\">Endpoint</th><th scope=\"col\">Requests</th><th scope=\"col\">4xx / 5xx</th>"
                + "<th scope=\"col\">Average</th><th scope=\"col\">p95</th><th scope=\"col\">Cumulative</th>"
                + "</tr></thead><tbody>");
        Map<String, RequestSummary> endpointSummaries = new TreeMap<>();
        byEndpoint.forEach((name, observations) -> endpointSummaries.put(name, RequestSummary.from(observations)));
        endpointSummaries.entrySet().stream().sorted(Comparator
                .<Map.Entry<String, RequestSummary>>comparingDouble(entry -> entry.getValue().cumulativeMillis()).reversed()
                .thenComparing(Map.Entry::getKey)).forEach(entry -> {
                    RequestSummary item = entry.getValue();
                    html.append("<tr><td><code>").append(escape(entry.getKey())).append("</code></td><td>")
                            .append(item.requestCount()).append("</td><td>").append(item.clientErrorCount())
                            .append(" / ").append(item.serverErrorCount()).append("</td><td>")
                            .append(time(item.averageMillis())).append("</td><td>").append(time(item.p95Millis()))
                            .append("</td><td>").append(time(item.cumulativeMillis())).append("</td></tr>");
                });
        html.append("</tbody></table></div><p class=\"section-note\">Cumulative time ranks retained observations; "
                + "it is not an estimate of system resource consumption.</p></section>");

        html.append("<section class=\"section\" id=\"request-list\"><h2 class=\"diagnostics-title\">Observed requests</h2>");
        html.append("<p class=\"section-note\">Most recent ").append(Math.min(requests.size(), MAX_DETAIL_ROWS))
                .append(" of ").append(requests.size()).append(" retained HTTP observations. "
                        + "Expanded code is this request's method set, not call order or a timed trace.</p>");
        html.append("""
                <div class="request-tools" id="reqover-request-tools" hidden>
                  <label for="reqover-request-mode">Requests
                    <select id="reqover-request-mode"><option value="all">All</option>
                    <option value="failure">HTTP 4xx / 5xx</option><option value="slow">Slow</option>
                    <option value="unknown">Unknown status</option></select>
                  </label>
                  <label for="reqover-slow-ms">Slow threshold (ms)
                    <input id="reqover-slow-ms" type="number" value="1000" min="1" step="100">
                  </label>
                  <span class="filter-count" id="reqover-request-count" aria-live="polite"></span>
                </div>
                """);
        requests.stream().sorted(Comparator.comparing(RequestObservation::startedAt).reversed()
                .thenComparing(RequestObservation::requestId)).limit(MAX_DETAIL_ROWS)
                .forEach(request -> appendRequest(html, request));
        html.append("<p class=\"no-match\" id=\"reqover-no-request\" hidden>No request matches these filters.</p></section>");
        return html.toString();
    }

    private static void appendRequest(StringBuilder html, RequestObservation request) {
        Double elapsed = request.recordedDurationMillis();
        StringBuilder search = new StringBuilder(request.requestId()).append(' ').append(request.endpoint());
        for (ClassCoverage type : request.classes()) {
            search.append(' ').append(type.className());
            for (MethodCoverage method : type.methods()) {
                search.append(' ').append(method.methodName());
            }
        }
        html.append("<details class=\"request-detail\" data-request-search=\"")
                .append(escape(search.toString().toLowerCase(Locale.ROOT)))
                .append("\" data-request-status=\"").append(request.hasFinalHttpStatus() ? request.statusCode() : -1)
                .append("\" data-request-duration=\"").append(elapsed == null ? "" : elapsed)
                .append("\"><summary><code class=\"request-name\">").append(escape(request.endpoint()))
                .append("</code><span class=\"request-duration\">").append(time(elapsed))
                .append("</span><span class=\"request-status ")
                .append(request.isHttpFailure() ? "http-failure" : request.hasFinalHttpStatus() ? "http-success" : "")
                .append("\">").append(request.hasFinalHttpStatus() ? "HTTP " + request.statusCode() : "Unknown")
                .append("</span><code class=\"request-id\">").append(escape(request.requestId()))
                .append("</code></summary><dl class=\"request-fields\"><dt>Started</dt><dd>")
                .append(escape(request.startedAt().toString())).append("</dd><dt>Ended</dt><dd>")
                .append(request.endedAt() == null ? "Not finished" : escape(request.endedAt().toString()))
                .append("</dd><dt>Threads</dt><dd>").append(escape(String.join(", ", request.threadNames())))
                .append("</dd><dt>Inputs</dt><dd>Not collected</dd><dt>Exceptions</dt><dd>Not collected; HTTP status only</dd>")
                .append("</dl><div class=\"request-code\">");
        if (request.classes().isEmpty()) {
            html.append("<p>No application methods observed.</p>");
        }
        for (ClassCoverage type : request.classes()) {
            for (MethodCoverage method : type.methods()) {
                html.append("<p><code>").append(escape(type.className())).append('#')
                        .append(escape(method.methodName()))
                        .append(escape(HtmlCoverageReportRenderer.readableSignature(method.descriptor())))
                        .append("</code></p>");
            }
        }
        html.append("</div></details>");
    }

    private static void metric(StringBuilder html, String label, String value, String detail, boolean failure) {
        html.append("<div><span class=\"metric-label\">").append(label)
                .append("</span><span class=\"metric-value").append(failure ? " http-failure" : "")
                .append("\">").append(value).append("</span><span class=\"metric-detail\">")
                .append(detail).append("</span></div>");
    }

    private static String time(Double millis) {
        return millis == null ? "Not timed" : String.format(Locale.ROOT, "%.2f ms", millis);
    }

    private static String percent(Double value) {
        return value == null ? "Unknown" : String.format(Locale.ROOT, "%.1f%%", value);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
