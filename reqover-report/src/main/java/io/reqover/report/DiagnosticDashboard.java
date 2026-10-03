package io.reqover.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;

/** Offline visual relationships over the same data exposed by the report tables. */
final class DiagnosticDashboard {
    private static final String ROOT = "/io/reqover/report/dashboard/";
    private static final String STYLES = asset("dashboard.css");
    private static final String SCRIPT = asset("dashboard.js");

    private DiagnosticDashboard() {
    }

    static String styles() {
        return STYLES + TestCaseDraftHtmlRenderer.styles();
    }

    static String navigation(boolean hasRequests) {
        StringBuilder out = new StringBuilder("<nav class=\"dashboard-sidebar\" aria-label=\"Report sections\">");
        out.append("<a class=\"dashboard-brand\" href=\"#request-overview\">")
                .append(asset("reqover-mark.svg").replace("<svg ", "<svg aria-hidden=\"true\" "))
                .append("<span>Reqover<small>Validation report</small></span></a>");
        nav(out, "request-overview", "layout-dashboard", "Overview");
        if (hasRequests) {
            nav(out, "request-list", "clock", "Requests");
            nav(out, "test-case-drafts", "flask-conical", "Test drafts");
        }
        nav(out, "endpoint-code", "network", "API to code");
        nav(out, "code-endpoint", "git-pull-request", "Retest candidates");
        nav(out, "ci-report", "download", "CI artifacts");
        out.append("<div class=\"sidebar-meta\"><span class=\"record-dot\"></span>Recorded snapshot<small>Local / QA / staging</small></div></nav>");
        return out.toString();
    }

    static String workspace() {
        return """
                <div id="reqover-workspace" hidden>
                  <div class="workspace-heading"><h3>Execution relationships</h3>
                    <span class="graph-legend">Observed associations · not call order</span></div>
                  <div class="workspace-tools">
                    <div class="map-modes" role="group" aria-label="Relationship view">
                      <button type="button" data-map-mode="request" aria-pressed="true">Request map</button>
                      <button type="button" data-map-mode="retest" aria-pressed="false">Retest map</button>
                    </div>
                    <label class="sr-only" for="reqover-map-selection">Recorded request or method</label>
                    <select id="reqover-map-selection"></select>
                    <div class="map-controls">
                """
                + control("reqover-animation", "pause", "Pause animation")
                + control("reqover-zoom-out", "zoom-out", "Zoom out")
                + control("reqover-zoom-in", "zoom-in", "Zoom in")
                + control("reqover-fit", "maximize", "Fit graph")
                + """
                    </div>
                  </div>
                  <div class="workspace-body">
                    <div class="graph-stage" id="reqover-graph-stage" tabindex="0" aria-label="Execution relationship map">
                      <div id="reqover-graph-size"><div id="reqover-graph-plane">
                        <svg id="reqover-graph-lines" aria-hidden="true"></svg>
                        <div id="reqover-graph-nodes"></div>
                      </div></div>
                    </div>
                    <aside id="reqover-inspector" aria-label="Selected observation"></aside>
                  </div>
                  <div class="attention-heading"><h3>Review queue</h3><span id="reqover-queue-count"></span></div>
                  <div id="reqover-attention" class="attention-list"></div>
                </div>
                """;
    }

    static String artifacts(CoverageReport report) {
        return "<section class=\"section\" id=\"ci-report\"><h2 class=\"diagnostics-title\">CI artifacts</h2>"
                + "<dl class=\"artifact-fields\"><dt>Report format</dt><dd>Standalone HTML + JSON</dd>"
                + "<dt>Observed endpoints</dt><dd>" + report.endpoints().size() + "</dd>"
                + "<dt>Retained HTTP observations</dt><dd>" + RequestSummary.from(report.requests()).requestCount() + "</dd>"
                + "<dt>Change impact</dt><dd>Not attached to this recording</dd>"
                + "<dt>Test verdict</dt><dd>Not determined by recorded coverage</dd></dl>"
                + "<div class=\"artifact-row\">" + icon("layout-dashboard")
                + "<span><strong>report.html</strong><small>Request diagnostics and observed relationships</small></span>"
                + control("reqover-download", "download", "Download HTML report") + "</div>"
                + "<div class=\"artifact-row\">" + icon("git-pull-request")
                + "<span><strong>impact.md / impact.json</strong><small>Generated separately by the CI Action from the Git diff</small></span>"
                + "<span class=\"artifact-status\">Not attached</span></div></section>";
    }

    static String script(CoverageReport report) {
        List<RequestObservation> recent = report.requests().stream().filter(RequestObservation::isHttp)
                .sorted(Comparator.comparing(RequestObservation::startedAt).reversed()
                        .thenComparing(RequestObservation::requestId)).limit(100).toList();
        CoverageReport graph = new CoverageReport(report.generatedAt(), report.completedRequestCount(),
                report.endpoints(), report.reverseIndex(), recent);
        // JSON is inside a raw-text script element; HTML escaping would corrupt it.
        String json = CoverageReportJson.write(graph).replace("&", "\\u0026")
                .replace("<", "\\u003c").replace(">", "\\u003e")
                .replace("\u2028", "\\u2028").replace("\u2029", "\\u2029");
        return "<script type=\"application/json\" id=\"reqover-map-data\">" + json + "</script>\n"
                + "<template id=\"reqover-play-icon\">" + icon("play") + "</template>"
                + "<template id=\"reqover-pause-icon\">" + icon("pause") + "</template>"
                + "<template id=\"reqover-draft-icon\">" + icon("flask-conical") + "</template>"
                + TestCaseDraftHtmlRenderer.script()
                + "<script>" + SCRIPT + "</script>\n"
                + "<!-- Lucide / Feather icon licenses:\n" + asset("icons/LICENSE").replace("--", "- -") + "\n-->";
    }

    private static void nav(StringBuilder out, String id, String icon, String label) {
        out.append("<a class=\"dashboard-link\" href=\"#").append(id).append("\">")
                .append(icon(icon)).append("<span>").append(label).append("</span></a>");
    }

    private static String control(String id, String name, String label) {
        return "<button type=\"button\" class=\"icon-button\" id=\"" + id
                + "\" aria-label=\"" + label + "\" title=\"" + label + "\">" + icon(name) + "</button>";
    }

    static String icon(String name) {
        return asset("icons/" + name + ".svg").replace("<svg ", "<svg class=\"ui-icon\" aria-hidden=\"true\" ");
    }

    static String asset(String name) {
        try (var input = DiagnosticDashboard.class.getResourceAsStream(ROOT + name)) {
            if (input == null) {
                throw new IllegalStateException("Missing dashboard resource: " + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read dashboard resource: " + name, error);
        }
    }
}
