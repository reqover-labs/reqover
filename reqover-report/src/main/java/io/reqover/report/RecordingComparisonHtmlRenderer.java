package io.reqover.report;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/** Complete retained summaries for offline descriptive comparison. */
final class RecordingComparisonHtmlRenderer {
    private RecordingComparisonHtmlRenderer() {
    }

    static String styles() {
        return DiagnosticDashboard.asset("recording-comparison.css");
    }

    static String render() {
        return """
                <section class="section" id="recording-comparison" hidden>
                  <h2 class="diagnostics-title">Recording comparison</h2>
                  <div class="comparison-tools">
                    <label class="case-command" for="reqover-baseline-file">
                """ + DiagnosticDashboard.icon("upload") + """
                      <span>Import baseline JSON</span>
                      <input id="reqover-baseline-file" type="file" accept=".json,application/json" class="sr-only">
                    </label>
                    <button type="button" class="case-command" id="reqover-comparison-export">
                """ + DiagnosticDashboard.icon("download") + """
                      <span>Export current summary</span></button>
                    <button type="button" class="icon-button" id="reqover-comparison-reset" aria-label="Clear baseline" title="Clear baseline" disabled>
                """ + DiagnosticDashboard.icon("x") + """
                    </button>
                  </div>
                  <p id="reqover-comparison-error" class="comparison-error" role="alert" hidden></p>
                  <div class="comparison-records">
                    <div><span class="metric-label">Baseline</span><strong id="reqover-baseline-name">Not imported</strong>
                      <small id="reqover-baseline-meta"></small></div>
                    <div><span class="metric-label">Current recording</span><strong id="reqover-current-name"></strong>
                      <small id="reqover-current-meta"></small></div>
                  </div>
                  <label class="case-review" for="reqover-comparison-confirmed">
                    <input id="reqover-comparison-confirmed" type="checkbox">Scenario, environment and capture scope manually checked
                  </label>
                  <p class="comparison-context" id="reqover-comparison-context">Conditions not recorded. No automatic performance verdict.</p>
                  <ul class="comparison-notices" id="reqover-comparison-notices"></ul>
                  <p id="reqover-comparison-empty" class="empty">No baseline imported.</p>
                  <div id="reqover-comparison-results" hidden>
                    <div class="comparison-filter">
                      <label class="sr-only" for="reqover-comparison-filter">Filter compared endpoints</label>
                      <input id="reqover-comparison-filter" type="search" placeholder="Filter endpoints" autocomplete="off">
                      <label class="sr-only" for="reqover-comparison-sort">Sort endpoint changes</label>
                      <select id="reqover-comparison-sort"><option value="p95">Largest p95 increase</option>
                        <option value="errors">Largest HTTP error increase</option><option value="requests">Most current observations</option>
                        <option value="endpoint">Endpoint name</option></select>
                    </div>
                    <div class="comparison-scroll"><table class="comparison-table">
                      <thead><tr><th>Endpoint</th><th>Retained samples</th><th>Average interval</th>
                        <th>p95 interval</th><th>HTTP 4xx/5xx</th><th>Observation scope</th></tr></thead>
                      <tbody id="reqover-comparison-rows"></tbody>
                    </table></div>
                    <p id="reqover-comparison-no-match" class="no-match" hidden>No compared endpoint matches.</p>
                  </div>
                </section>
                """;
    }

    static String script(CoverageReport report) {
        return "<script type=\"application/json\" id=\"reqover-comparison-data\">"
                + DiagnosticDashboard.scriptJson(summaryJson(report)) + "</script>\n"
                + "<script>" + DiagnosticDashboard.asset("recording-comparison.js") + "</script>\n"
                + "<script>" + DiagnosticDashboard.asset("recording-comparison-view.js") + "</script>\n";
    }

    private static String summaryJson(CoverageReport report) {
        Map<String, ArrayList<RequestObservation>> grouped = new TreeMap<>();
        report.requests().stream().filter(RequestObservation::isHttp).forEach(request ->
                grouped.computeIfAbsent(request.endpoint(), ignored -> new ArrayList<>()).add(request));
        StringBuilder out = new StringBuilder("{\"schemaVersion\":1,\"kind\":\"reqover-recorded-summary\",\"generatedAt\":");
        Json.writeString(out, report.generatedAt().toString());
        out.append(",\"available\":").append(!grouped.isEmpty()).append(",\"httpRequestCount\":")
                .append(RequestSummary.from(report.requests()).requestCount()).append(",\"endpoints\":[");
        boolean comma = false;
        for (var entry : grouped.entrySet()) {
            if (comma) { out.append(','); }
            comma = true;
            RequestSummary summary = RequestSummary.from(entry.getValue());
            out.append("{\"endpoint\":"); Json.writeString(out, entry.getKey());
            out.append(",\"requestCount\":").append(summary.requestCount())
                    .append(",\"knownStatusCount\":").append(summary.knownStatusCount())
                    .append(",\"clientErrorCount\":").append(summary.clientErrorCount())
                    .append(",\"serverErrorCount\":").append(summary.serverErrorCount())
                    .append(",\"timedRequestCount\":").append(summary.timedRequestCount())
                    .append(",\"averageMillis\":").append(summary.averageMillis())
                    .append(",\"p95Millis\":").append(summary.p95Millis())
                    .append(",\"maximumMillis\":").append(summary.maximumMillis())
                    .append(",\"cumulativeMillis\":").append(summary.cumulativeMillis()).append('}');
        }
        return out.append("]}").toString();
    }
}
