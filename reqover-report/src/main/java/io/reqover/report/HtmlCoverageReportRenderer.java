package io.reqover.report;

import java.util.Collection;

/**
 * Renders a {@link CoverageReport} as a standalone HTML page. All dynamic
 * values are HTML-escaped.
 */
public final class HtmlCoverageReportRenderer {
    private static final int MAX_CHIPS = 20;

    public String render(CoverageReport report) {
        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Reqover Report</title>
                  <style>
                    :root {
                      color-scheme: light;
                      --bg: #f5f7fb;
                      --panel: #ffffff;
                      --ink: #18212f;
                      --muted: #667085;
                      --line: #d9e0ea;
                      --brand: #1463ff;
                      --brand-soft: #eaf1ff;
                      --success: #0f766e;
                      --success-soft: #dff7f3;
                      --warning: #8a5a00;
                      --warning-soft: #fff4d7;
                    }
                    * { box-sizing: border-box; }
                    body {
                      font-family: Arial, sans-serif;
                      margin: 0;
                      color: var(--ink);
                      background: var(--bg);
                    }
                    header {
                      background: #101828;
                      color: #fff;
                      padding: 28px 40px;
                    }
                    main {
                      max-width: 1180px;
                      margin: 0 auto;
                      padding: 28px 24px 48px;
                    }
                    h1 { font-size: 30px; margin: 0 0 8px; letter-spacing: 0; }
                    h2 { font-size: 19px; margin: 0 0 12px; letter-spacing: 0; }
                    h3 { font-size: 15px; margin: 0 0 8px; letter-spacing: 0; }
                    .subtitle { color: #cbd5e1; margin: 0; }
                    .meta { color: var(--muted); font-size: 13px; }
                    .summary {
                      display: grid;
                      grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
                      gap: 12px;
                      margin: 0 0 18px;
                    }
                    .summary-card, .endpoint, .index-panel {
                      background: var(--panel);
                      border: 1px solid var(--line);
                      border-radius: 8px;
                      box-shadow: 0 1px 2px rgba(16, 24, 40, 0.04);
                    }
                    .summary-card { padding: 16px; }
                    .metric { display: block; font-size: 28px; font-weight: 700; margin-top: 4px; }
                    .endpoint, .index-panel { margin: 16px 0; padding: 18px; }
                    .endpoint-head {
                      display: flex;
                      justify-content: space-between;
                      gap: 16px;
                      align-items: flex-start;
                      flex-wrap: wrap;
                    }
                    .badge {
                      display: inline-flex;
                      align-items: center;
                      border-radius: 999px;
                      padding: 5px 10px;
                      font-size: 12px;
                      font-weight: 700;
                      background: var(--success-soft);
                      color: var(--success);
                      white-space: nowrap;
                    }
                    .chips { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0 0; }
                    .chip { background: var(--brand-soft); color: #174ea6; border-radius: 999px; padding: 4px 10px; font-size: 12px; }
                    .chip-muted { background: #eef2f6; color: #475467; }
                    table { width: 100%; border-collapse: collapse; margin-top: 14px; background: #fff; table-layout: fixed; }
                    th, td { border-top: 1px solid #e5e8ef; text-align: left; padding: 11px 10px; vertical-align: top; }
                    th { color: #475467; font-size: 12px; text-transform: uppercase; letter-spacing: .04em; }
                    td:first-child, th:first-child { width: 38%; }
                    code {
                      font-family: Consolas, "Liberation Mono", monospace;
                      font-size: 13px;
                      overflow-wrap: anywhere;
                    }
                    .class-simple { display: block; font-weight: 700; margin-bottom: 3px; }
                    .class-package { color: var(--muted); font-size: 12px; overflow-wrap: anywhere; }
                    .method { margin: 0 0 7px; }
                    .method:last-child { margin-bottom: 0; }
                    .line { color: var(--warning); background: var(--warning-soft); border-radius: 4px; padding: 1px 5px; font-size: 12px; margin-left: 6px; }
                    .empty { padding: 18px; color: var(--muted); background: #fff; border: 1px dashed var(--line); border-radius: 8px; }
                  </style>
                </head>
                <body>
                <header>
                """);
        html.append("<h1>Reqover Coverage Report</h1>\n");
        html.append("<p class=\"subtitle\">Request-scoped method coverage attribution for Spring applications</p>\n");
        html.append("</header>\n<main>\n");
        html.append("<section class=\"summary\">\n");
        html.append("<div class=\"summary-card\"><span class=\"meta\">Completed requests</span><span class=\"metric\">")
                .append(report.completedRequestCount())
                .append("</span></div>\n");
        html.append("<div class=\"summary-card\"><span class=\"meta\">Observed endpoints</span><span class=\"metric\">")
                .append(report.endpoints().size())
                .append("</span></div>\n");
        html.append("<div class=\"summary-card\"><span class=\"meta\">Code impact entries</span><span class=\"metric\">")
                .append(report.reverseIndex().size())
                .append("</span></div>\n");
        html.append("</section>\n");
        html.append("<div class=\"meta\">Generated at ")
                .append(escape(report.generatedAt().toString()))
                .append("</div>\n");

        if (report.endpoints().isEmpty()) {
            html.append("<div class=\"empty\">No completed request buckets have been recorded yet.</div>\n");
        }
        for (EndpointCoverage endpoint : report.endpoints()) {
            html.append("<section class=\"endpoint\">\n");
            html.append("<div class=\"endpoint-head\"><div><h2><code>").append(escape(endpoint.endpoint())).append("</code></h2>\n");
            html.append("<div class=\"meta\">")
                    .append(endpoint.classes().size())
                    .append(" classes · ")
                    .append(methodCount(endpoint))
                    .append(" methods · ")
                    .append(endpoint.threadNames().size())
                    .append(" threads</div></div>\n");
            html.append("<span class=\"badge\">")
                    .append(endpoint.requestCount())
                    .append(" request");
            if (endpoint.requestCount() != 1) {
                html.append("s");
            }
            html.append("</span></div>\n");
            html.append("<div class=\"chips\">");
            appendChips(html, endpoint.requestIds(), "chip chip-muted");
            appendChips(html, endpoint.threadNames(), "chip");
            html.append("</div>\n");
            html.append("<table><thead><tr><th>Class</th><th>Methods / Probes</th></tr></thead><tbody>\n");
            for (ClassCoverage classCoverage : endpoint.classes()) {
                html.append("<tr><td>");
                appendClassName(html, classCoverage.className());
                html.append("</td><td>");
                for (MethodCoverage method : classCoverage.methods()) {
                    html.append("<div class=\"method\"><code>")
                            .append(escape(method.methodName()))
                            .append(escape(method.descriptor()))
                            .append("</code> · probe ")
                            .append(method.probeId());
                    if (method.lineNumber() != null) {
                        html.append("<span class=\"line\">line ")
                                .append(method.lineNumber())
                                .append("</span>");
                    }
                    html.append("</div>");
                }
                html.append("</td></tr>\n");
            }
            html.append("</tbody></table>\n");
            html.append("</section>\n");
        }

        html.append("<section class=\"index-panel\">\n");
        html.append("<h2>Code to Endpoint Index</h2>\n");
        html.append("<div class=\"meta\">Use this section to decide which observed APIs should be retested after a code change.</div>\n");
        html.append("<table><thead><tr><th>Code</th><th>Observed endpoints</th></tr></thead><tbody>\n");
        for (CodeEndpointCoverage item : report.reverseIndex()) {
            html.append("<tr><td>");
            appendClassName(html, item.className());
            html.append("<code>#")
                    .append(escape(item.methodName()))
                    .append(escape(item.descriptor()))
                    .append("</code></td><td>");
            for (String endpoint : item.endpoints()) {
                html.append("<div><code>").append(escape(endpoint)).append("</code></div>");
            }
            html.append("</td></tr>\n");
        }
        html.append("</tbody></table>\n");
        html.append("</section>\n");

        html.append("</main></body></html>\n");
        return html.toString();
    }

    private static void appendChips(StringBuilder html, Collection<String> values, String cssClass) {
        int shown = 0;
        for (String value : values) {
            if (shown == MAX_CHIPS) {
                html.append("<span class=\"chip chip-muted\">+")
                        .append(values.size() - MAX_CHIPS)
                        .append(" more</span>");
                break;
            }
            html.append("<span class=\"").append(cssClass).append("\">").append(escape(value)).append("</span>");
            shown++;
        }
    }

    private static int methodCount(EndpointCoverage endpoint) {
        int count = 0;
        for (ClassCoverage classCoverage : endpoint.classes()) {
            count += classCoverage.methods().size();
        }
        return count;
    }

    private static void appendClassName(StringBuilder html, String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot < 0) {
            html.append("<code class=\"class-simple\">").append(escape(className)).append("</code>");
            return;
        }
        html.append("<code class=\"class-simple\">")
                .append(escape(className.substring(lastDot + 1)))
                .append("</code>");
        html.append("<div class=\"class-package\">")
                .append(escape(className.substring(0, lastDot)))
                .append("</div>");
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
