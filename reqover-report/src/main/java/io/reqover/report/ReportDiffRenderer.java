package io.reqover.report;

import java.util.List;

/**
 * Renders a {@link ReportDiff} for a terminal or a pull request comment.
 */
public final class ReportDiffRenderer {
    private ReportDiffRenderer() {
    }

    public static String text(ReportDiff diff) {
        if (diff.isEmpty()) {
            return "Reqover diff: both reports observed the same endpoints executing the same code.\n";
        }

        StringBuilder out = new StringBuilder("Reqover diff\n");
        appendNames(out, "Endpoints observed only now", diff.addedEndpoints(), "+");
        appendNames(out, "Endpoints observed only in the baseline", diff.removedEndpoints(), "-");

        if (!diff.changedEndpoints().isEmpty()) {
            out.append('\n').append("Endpoints whose executed code changed:\n");
            for (EndpointCodeDiff endpoint : diff.changedEndpoints()) {
                out.append("  ").append(endpoint.endpoint()).append('\n');
                for (CodeRef code : endpoint.addedCode()) {
                    out.append("    + ").append(code.display()).append('\n');
                }
                for (CodeRef code : endpoint.removedCode()) {
                    out.append("    - ").append(code.display()).append('\n');
                }
            }
        }
        return out.toString();
    }

    public static String markdown(ReportDiff diff) {
        StringBuilder out = new StringBuilder("### Reqover — coverage diff\n\n");
        if (diff.isEmpty()) {
            out.append("Both reports observed the same endpoints executing the same code.\n");
            return out.toString();
        }

        appendMarkdownNames(out, "Endpoints observed only now", diff.addedEndpoints());
        appendMarkdownNames(out, "Endpoints observed only in the baseline", diff.removedEndpoints());

        if (!diff.changedEndpoints().isEmpty()) {
            out.append("**Endpoints whose executed code changed**\n\n");
            for (EndpointCodeDiff endpoint : diff.changedEndpoints()) {
                out.append("- `").append(endpoint.endpoint()).append("`\n");
                for (CodeRef code : endpoint.addedCode()) {
                    out.append("  - added `").append(code.display()).append("`\n");
                }
                for (CodeRef code : endpoint.removedCode()) {
                    out.append("  - removed `").append(code.display()).append("`\n");
                }
            }
            out.append('\n');
        }
        return out.toString();
    }

    private static void appendNames(StringBuilder out, String title, List<String> names, String marker) {
        if (names.isEmpty()) {
            return;
        }
        out.append('\n').append(title).append(":\n");
        for (String name : names) {
            out.append("  ").append(marker).append(' ').append(name).append('\n');
        }
    }

    private static void appendMarkdownNames(StringBuilder out, String title, List<String> names) {
        if (names.isEmpty()) {
            return;
        }
        out.append("**").append(title).append("**\n\n");
        for (String name : names) {
            out.append("- `").append(name).append("`\n");
        }
        out.append('\n');
    }
}
