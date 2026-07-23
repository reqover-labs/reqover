package io.reqover.report;

public final class HtmlCoverageReportRenderer {
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
                    body { font-family: Arial, sans-serif; margin: 32px; color: #202124; background: #f8fafc; }
                    h1 { font-size: 28px; margin: 0 0 8px; }
                    .meta { color: #5f6368; margin-bottom: 24px; }
                    .endpoint { background: #fff; border: 1px solid #dfe3ea; border-radius: 8px; margin: 16px 0; padding: 18px; }
                    .endpoint h2 { font-size: 18px; margin: 0 0 10px; }
                    .chips { display: flex; flex-wrap: wrap; gap: 8px; margin: 10px 0; }
                    .chip { background: #e8f0fe; color: #174ea6; border-radius: 999px; padding: 4px 10px; font-size: 12px; }
                    table { width: 100%; border-collapse: collapse; margin-top: 12px; background: #fff; }
                    th, td { border-top: 1px solid #e5e8ef; text-align: left; padding: 10px; vertical-align: top; }
                    th { color: #3c4043; font-size: 13px; }
                    code { font-family: Consolas, monospace; font-size: 13px; }
                  </style>
                </head>
                <body>
                """);
        html.append("<h1>Reqover Coverage Report</h1>\n");
        html.append("<div class=\"meta\">Generated at ")
                .append(escape(report.generatedAt().toString()))
                .append(" · completed requests ")
                .append(report.completedRequestCount())
                .append("</div>\n");

        for (EndpointCoverage endpoint : report.endpoints()) {
            html.append("<section class=\"endpoint\">\n");
            html.append("<h2><code>").append(escape(endpoint.endpoint())).append("</code></h2>\n");
            html.append("<div class=\"meta\">request count: ").append(endpoint.requestCount()).append("</div>\n");
            html.append("<div class=\"chips\">");
            for (String thread : endpoint.threadNames()) {
                html.append("<span class=\"chip\">").append(escape(thread)).append("</span>");
            }
            html.append("</div>\n");
            html.append("<table><thead><tr><th>Class</th><th>Methods / Probes</th></tr></thead><tbody>\n");
            for (ClassCoverage classCoverage : endpoint.classes()) {
                html.append("<tr><td><code>").append(escape(classCoverage.className())).append("</code></td><td>");
                for (MethodCoverage method : classCoverage.methods()) {
                    html.append("<div><code>")
                            .append(escape(method.methodName()))
                            .append(escape(method.descriptor()))
                            .append("</code> · probe ")
                            .append(method.probeId())
                            .append("</div>");
                }
                html.append("</td></tr>\n");
            }
            html.append("</tbody></table>\n");
            html.append("</section>\n");
        }

        html.append("</body></html>\n");
        return html.toString();
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

