package io.reqover.report;

import java.util.List;

/**
 * Renders an {@link ImpactReport} for the three places it gets read: a
 * terminal, a pull request comment, and another program.
 */
public final class ImpactReportRenderer {
    private static final String CAVEAT =
            "Reqover lists what it observed during the recorded run. A file with no observed "
                    + "coverage may simply not have been exercised while recording.";

    private ImpactReportRenderer() {
    }

    /** Plain text for a terminal or a CI log. */
    public static String text(ImpactReport report) {
        StringBuilder out = new StringBuilder();
        out.append("Reqover impact analysis\n");
        out.append("  changed paths analysed: ").append(report.changedPaths().size()).append('\n');
        out.append("  impacted endpoints:     ").append(report.endpoints().size()).append('\n');
        out.append('\n');

        if (report.endpoints().isEmpty()) {
            out.append("No observed endpoint executed the changed code.\n");
        } else {
            out.append("Endpoints to retest:\n");
            for (ImpactedEndpoint endpoint : report.endpoints()) {
                out.append("  ").append(endpoint.endpoint()).append('\n');
                for (CodeRef code : endpoint.changedCode()) {
                    out.append("      via ").append(code.display()).append('\n');
                }
            }
        }

        if (!report.unmatchedPaths().isEmpty()) {
            out.append('\n');
            out.append("Changed paths with no observed coverage (").append(report.unmatchedPaths().size())
                    .append("):\n");
            for (String path : report.unmatchedPaths()) {
                out.append("  ").append(path).append('\n');
            }
        }

        out.append('\n').append(CAVEAT).append('\n');
        return out.toString();
    }

    /** GitHub-flavoured Markdown, sized for a pull request comment. */
    public static String markdown(ImpactReport report) {
        StringBuilder out = new StringBuilder();
        out.append("### Reqover — endpoints to retest\n\n");

        if (report.endpoints().isEmpty()) {
            out.append("No observed endpoint executed the code this change touches.\n\n");
        } else {
            out.append("**").append(report.endpoints().size())
                    .append(report.endpoints().size() == 1 ? " endpoint** was" : " endpoints** were")
                    .append(" observed executing code this change touches.\n\n");
            out.append("| Endpoint | Changed code it ran |\n| --- | --- |\n");
            for (ImpactedEndpoint endpoint : report.endpoints()) {
                out.append("| `").append(escapeCell(endpoint.endpoint())).append("` | ");
                List<CodeRef> code = endpoint.changedCode();
                for (int i = 0; i < code.size(); i++) {
                    if (i > 0) {
                        out.append("<br>");
                    }
                    out.append("`").append(escapeCell(code.get(i).display())).append("`");
                }
                out.append(" |\n");
            }
            out.append('\n');
        }

        if (!report.unmatchedPaths().isEmpty()) {
            out.append("<details><summary>")
                    .append(report.unmatchedPaths().size())
                    .append(report.unmatchedPaths().size() == 1
                            ? " changed path with no observed coverage"
                            : " changed paths with no observed coverage")
                    .append("</summary>\n\n");
            for (String path : report.unmatchedPaths()) {
                out.append("- `").append(escapeCell(path)).append("`\n");
            }
            out.append("\n</details>\n\n");
        }

        out.append("<sub>").append(CAVEAT).append("</sub>\n");
        return out.toString();
    }

    /** Machine-readable output for another tool in the pipeline. */
    public static String json(ImpactReport report) {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"schemaVersion\": ").append(CoverageReportJson.SCHEMA_VERSION).append(",\n");
        out.append("  \"hasImpact\": ").append(report.hasImpact()).append(",\n");
        out.append("  \"changedPaths\": ");
        writeStrings(out, report.changedPaths());
        out.append(",\n");
        out.append("  \"matchedPaths\": ");
        writeStrings(out, report.matchedPaths());
        out.append(",\n");
        out.append("  \"unmatchedPaths\": ");
        writeStrings(out, report.unmatchedPaths());
        out.append(",\n");

        out.append("  \"endpoints\": [");
        if (!report.endpoints().isEmpty()) {
            out.append('\n');
            for (int i = 0; i < report.endpoints().size(); i++) {
                ImpactedEndpoint endpoint = report.endpoints().get(i);
                out.append("    {\n      \"endpoint\": ");
                Json.writeString(out, endpoint.endpoint());
                out.append(",\n      \"changedCode\": [");
                for (int j = 0; j < endpoint.changedCode().size(); j++) {
                    CodeRef code = endpoint.changedCode().get(j);
                    out.append(j == 0 ? "\n" : ",\n");
                    out.append("        {\"className\": ");
                    Json.writeString(out, code.className());
                    out.append(", \"methodName\": ");
                    Json.writeString(out, code.methodName());
                    out.append(", \"descriptor\": ");
                    Json.writeString(out, code.descriptor());
                    out.append("}");
                }
                if (!endpoint.changedCode().isEmpty()) {
                    out.append("\n      ");
                }
                out.append("]\n    }");
                out.append(i == report.endpoints().size() - 1 ? "\n" : ",\n");
            }
            out.append("  ");
        }
        out.append("]\n");
        out.append("}\n");
        return out.toString();
    }

    private static void writeStrings(StringBuilder out, List<String> values) {
        out.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            Json.writeString(out, values.get(i));
        }
        out.append(']');
    }

    /** Keeps a stray pipe or backtick from breaking the surrounding table cell. */
    private static String escapeCell(String value) {
        return value.replace("|", "\\|").replace("`", "'");
    }
}
