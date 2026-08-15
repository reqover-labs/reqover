package io.reqover.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Renders a {@link CoverageReport} as a standalone HTML page. All dynamic
 * values are HTML-escaped.
 *
 * <p>The page is deliberately shaped like an engineering report rather than a
 * dashboard: one accent dimension (the HTTP verb), hairline rules instead of
 * stacked cards, and a single derived figure — how many methods are reached by
 * more than one endpoint — because that is the number a reader acts on.
 */
public final class HtmlCoverageReportRenderer {
    private static final int MAX_IDS = 12;

    public String render(CoverageReport report) {
        Set<String> sharedCode = sharedCodeKeys(report);

        StringBuilder html = new StringBuilder();
        html.append("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Reqover Coverage Report</title>
                  <style>
                    :root {
                      color-scheme: light dark;
                      --bg: #ffffff;
                      --bg-soft: #f6f7f9;
                      --ink: #12161a;
                      --ink-2: #57616b;
                      --ink-3: #878f98;
                      --rule: #e7eaed;
                      --rule-strong: #d2d8de;
                      --verb-get: #16704a;
                      --verb-post: #8a5300;
                      --verb-put: #5b45a8;
                      --verb-delete: #a52016;
                      --verb-patch: #0b6285;
                      --verb-other: #57616b;
                    }
                    @media (prefers-color-scheme: dark) {
                      :root {
                        --bg: #0f1216;
                        --bg-soft: #171b21;
                        --ink: #e6e9ec;
                        --ink-2: #a3adb7;
                        --ink-3: #78828c;
                        --rule: #242a31;
                        --rule-strong: #333b44;
                        --verb-get: #5cc394;
                        --verb-post: #d9a13c;
                        --verb-put: #a78fe6;
                        --verb-delete: #e2796e;
                        --verb-patch: #5fb2d9;
                        --verb-other: #a3adb7;
                      }
                    }
                    * { box-sizing: border-box; }
                    html { -webkit-text-size-adjust: 100%; }
                    body {
                      margin: 0;
                      background: var(--bg);
                      color: var(--ink);
                      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Helvetica Neue", Arial, sans-serif;
                      font-size: 14px;
                      line-height: 1.5;
                    }
                    code, .mono {
                      font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, "Liberation Mono", monospace;
                      font-size: 13px;
                      overflow-wrap: anywhere;
                    }
                    /* Java descriptors are long; a narrower column forces the
                       method cell to wrap on almost every row. */
                    .wrap { max-width: 1200px; margin: 0 auto; padding: 0 32px; }

                    .topbar { border-bottom: 1px solid var(--rule); }
                    .topbar .wrap {
                      display: flex;
                      align-items: baseline;
                      gap: 12px;
                      height: 56px;
                    }
                    .wordmark { font-size: 15px; font-weight: 700; letter-spacing: -0.01em; }
                    .doctype { color: var(--ink-3); font-size: 13px; }
                    .stamp { margin-left: auto; color: var(--ink-3); font-size: 12px; }

                    .digest {
                      padding: 24px 0 32px;
                      color: var(--ink-2);
                      font-size: 14px;
                      border-bottom: 1px solid var(--rule);
                    }
                    .digest b { color: var(--ink); font-weight: 650; font-variant-numeric: tabular-nums; }
                    .digest .sep { color: var(--rule-strong); margin: 0 8px; }

                    .section { padding: 40px 0 8px; }
                    .section-label {
                      margin: 0 0 4px;
                      font-size: 12px;
                      font-weight: 650;
                      letter-spacing: 0.06em;
                      text-transform: uppercase;
                      color: var(--ink-3);
                    }
                    .section-note { margin: 0 0 24px; color: var(--ink-2); font-size: 13px; }

                    .endpoint { padding: 24px 0; border-top: 1px solid var(--rule); }
                    .endpoint:first-of-type { border-top: none; padding-top: 8px; }
                    .ep-head { display: flex; align-items: baseline; gap: 16px; flex-wrap: wrap; }
                    .ep-name {
                      margin: 0;
                      font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
                      font-size: 19px;
                      font-weight: 650;
                      letter-spacing: -0.01em;
                    }
                    .verb { font-weight: 700; }
                    .verb-get { color: var(--verb-get); }
                    .verb-post { color: var(--verb-post); }
                    .verb-put { color: var(--verb-put); }
                    .verb-delete { color: var(--verb-delete); }
                    .verb-patch { color: var(--verb-patch); }
                    .verb-other { color: var(--verb-other); }
                    .ep-count {
                      margin-left: auto;
                      color: var(--ink-2);
                      font-size: 13px;
                      font-variant-numeric: tabular-nums;
                      white-space: nowrap;
                    }
                    .ep-meta { margin: 6px 0 0; color: var(--ink-2); font-size: 13px; }
                    .ep-ids { margin: 4px 0 0; color: var(--ink-3); font-size: 12px; }
                    .ep-ids .sep { margin: 0 6px; color: var(--rule-strong); }

                    table { width: 100%; border-collapse: collapse; margin-top: 16px; table-layout: fixed; }
                    th {
                      padding: 0 12px 8px 0;
                      text-align: left;
                      font-size: 11px;
                      font-weight: 650;
                      letter-spacing: 0.06em;
                      text-transform: uppercase;
                      color: var(--ink-3);
                      border-bottom: 1px solid var(--rule-strong);
                    }
                    td {
                      padding: 12px 12px 12px 0;
                      vertical-align: top;
                      border-bottom: 1px solid var(--rule);
                    }
                    tr:last-child td { border-bottom: none; }
                    th:first-child, td:first-child { width: 40%; }
                    th:last-child, td:last-child { padding-right: 0; }

                    tr.shared td { background: var(--bg-soft); }
                    tr.shared td:first-child { box-shadow: inset 2px 0 0 var(--rule-strong); padding-left: 12px; }

                    .type-name { display: block; font-weight: 650; font-size: 13px; }
                    .type-package { display: block; margin-top: 2px; color: var(--ink-3); font-size: 12px; }
                    .member { margin: 0 0 6px; }
                    .member:last-child { margin-bottom: 0; }
                    .member .at { color: var(--ink-3); font-size: 12px; margin-left: 8px; }
                    .fanout {
                      display: inline-block;
                      margin-top: 4px;
                      font-size: 11px;
                      font-weight: 650;
                      letter-spacing: 0.04em;
                      text-transform: uppercase;
                      color: var(--ink-2);
                    }
                    .empty {
                      margin: 24px 0;
                      padding: 24px;
                      color: var(--ink-2);
                      background: var(--bg-soft);
                      border: 1px solid var(--rule);
                      border-radius: 6px;
                    }
                    footer { padding: 48px 0 64px; color: var(--ink-3); font-size: 12px; }
                  </style>
                </head>
                <body>
                <header class="topbar">
                  <div class="wrap">
                    <span class="wordmark">Reqover</span>
                    <span class="doctype">coverage report</span>
                """);
        html.append("    <span class=\"stamp\">")
                .append(escape(report.generatedAt().toString()))
                .append("</span>\n");
        html.append("""
                  </div>
                </header>
                <main class="wrap">
                """);

        html.append("<p class=\"digest\">");
        appendCount(html, report.completedRequestCount(), "completed request", "completed requests");
        html.append("<span class=\"sep\">·</span>");
        appendCount(html, report.endpoints().size(), "observed endpoint", "observed endpoints");
        html.append("<span class=\"sep\">·</span>");
        appendCount(html, sharedCode.size(), "method reached by 2+ endpoints", "methods reached by 2+ endpoints");
        html.append("</p>\n");

        html.append("<section class=\"section\">\n");
        html.append("<h2 class=\"section-label\">Endpoint to Code</h2>\n");
        html.append("<p class=\"section-note\">What each observed request actually executed.</p>\n");

        if (report.endpoints().isEmpty()) {
            html.append("<div class=\"empty\">No completed request buckets have been recorded yet.</div>\n");
        }
        for (EndpointCoverage endpoint : report.endpoints()) {
            appendEndpoint(html, endpoint, sharedCode);
        }
        html.append("</section>\n");

        html.append("<section class=\"section\">\n");
        html.append("<h2 class=\"section-label\">Code to Endpoint Index</h2>\n");
        html.append("<p class=\"section-note\">Which observed APIs to retest after a code change. "
                + "Highlighted rows are reached by more than one endpoint.</p>\n");
        html.append("<table><thead><tr><th scope=\"col\">Code</th>"
                + "<th scope=\"col\">Observed endpoints</th></tr></thead><tbody>\n");
        for (CodeEndpointCoverage item : report.reverseIndex()) {
            boolean shared = item.endpoints().size() > 1;
            html.append(shared ? "<tr class=\"shared\"><td>" : "<tr><td>");
            appendTypeName(html, item.className());
            html.append("<div class=\"member\"><code>")
                    .append(escape(item.methodName()))
                    .append(escape(readableSignature(item.descriptor())))
                    .append("</code></div>");
            html.append("</td><td>");
            for (String endpoint : item.endpoints()) {
                html.append("<div class=\"member\"><code>");
                appendEndpointName(html, endpoint);
                html.append("</code></div>");
            }
            if (shared) {
                html.append("<span class=\"fanout\">")
                        .append(item.endpoints().size())
                        .append(" endpoints</span>");
            }
            html.append("</td></tr>\n");
        }
        html.append("</tbody></table>\n");
        html.append("</section>\n");

        html.append("<footer>Reqover records method-entry hits per observed request. "
                + "It reports what ran, not line or branch coverage.</footer>\n");
        html.append("</main></body></html>\n");
        return html.toString();
    }

    private void appendEndpoint(StringBuilder html, EndpointCoverage endpoint, Set<String> sharedCode) {
        List<TypeGroup> types = groupByType(endpoint);
        int methods = 0;
        for (TypeGroup group : types) {
            methods += group.members().size();
        }

        html.append("<article class=\"endpoint\" data-endpoint=\"")
                .append(escape(endpoint.endpoint()))
                .append("\">\n");
        html.append("<div class=\"ep-head\"><h3 class=\"ep-name\">");
        appendEndpointName(html, endpoint.endpoint());
        html.append("</h3><span class=\"ep-count\">");
        html.append(endpoint.requestCount()).append(endpoint.requestCount() == 1 ? " request" : " requests");
        html.append("</span></div>\n");

        html.append("<p class=\"ep-meta\">");
        html.append(types.size()).append(types.size() == 1 ? " class" : " classes");
        html.append(" · ").append(methods).append(methods == 1 ? " method" : " methods");
        html.append(" · ").append(endpoint.threadNames().size())
                .append(endpoint.threadNames().size() == 1 ? " thread" : " threads");
        html.append("</p>\n");

        html.append("<p class=\"ep-ids\">");
        appendIds(html, endpoint.requestIds());
        if (!endpoint.requestIds().isEmpty() && !endpoint.threadNames().isEmpty()) {
            html.append("<span class=\"sep\">·</span>");
        }
        appendIds(html, endpoint.threadNames());
        html.append("</p>\n");

        html.append("<table><thead><tr><th scope=\"col\">Class</th>"
                + "<th scope=\"col\">Methods</th></tr></thead><tbody>\n");
        for (TypeGroup group : types) {
            boolean shared = false;
            for (MethodCoverage method : group.members()) {
                if (sharedCode.contains(codeKey(group.className(), method))) {
                    shared = true;
                    break;
                }
            }
            html.append(shared ? "<tr class=\"shared\"><td>" : "<tr><td>");
            appendTypeName(html, group.className());
            html.append("</td><td>");
            for (MethodCoverage method : group.members()) {
                html.append("<div class=\"member\"><code>")
                        .append(escape(method.methodName()))
                        .append(escape(readableSignature(method.descriptor())))
                        .append("</code><span class=\"at\">probe ")
                        .append(method.probeId());
                if (method.lineNumber() != null) {
                    html.append(" · line ").append(method.lineNumber());
                }
                html.append("</span></div>");
            }
            html.append("</td></tr>\n");
        }
        html.append("</tbody></table>\n");
        html.append("</article>\n");
    }

    /**
     * Groups probes by class name so that a type instrumented through more than
     * one registration path is shown once instead of repeated per class id.
     */
    private static List<TypeGroup> groupByType(EndpointCoverage endpoint) {
        Map<String, List<MethodCoverage>> byName = new LinkedHashMap<>();
        for (ClassCoverage classCoverage : endpoint.classes()) {
            byName.computeIfAbsent(classCoverage.className(), ignored -> new ArrayList<>())
                    .addAll(classCoverage.methods());
        }
        List<TypeGroup> groups = new ArrayList<>(byName.size());
        for (Map.Entry<String, List<MethodCoverage>> entry : byName.entrySet()) {
            Set<String> seen = new LinkedHashSet<>();
            List<MethodCoverage> members = new ArrayList<>();
            for (MethodCoverage method : entry.getValue()) {
                if (seen.add(method.methodName() + method.descriptor())) {
                    members.add(method);
                }
            }
            groups.add(new TypeGroup(entry.getKey(), List.copyOf(members)));
        }
        return List.copyOf(groups);
    }

    private static Set<String> sharedCodeKeys(CoverageReport report) {
        Set<String> shared = new LinkedHashSet<>();
        for (CodeEndpointCoverage item : report.reverseIndex()) {
            if (item.endpoints().size() > 1) {
                shared.add(item.className() + "#" + item.methodName() + item.descriptor());
            }
        }
        return shared;
    }

    private static String codeKey(String className, MethodCoverage method) {
        return className + "#" + method.methodName() + method.descriptor();
    }

    private static void appendCount(StringBuilder html, int value, String singular, String plural) {
        html.append("<b>").append(value).append("</b> ").append(value == 1 ? singular : plural);
    }

    private static void appendIds(StringBuilder html, Collection<String> values) {
        int shown = 0;
        for (String value : values) {
            if (shown == MAX_IDS) {
                html.append("<span class=\"sep\">·</span>")
                        .append("+").append(values.size() - MAX_IDS).append(" more");
                break;
            }
            if (shown > 0) {
                html.append("<span class=\"sep\">·</span>");
            }
            html.append("<span class=\"mono\">").append(escape(value)).append("</span>");
            shown++;
        }
    }

    /**
     * Renders {@code GET /orders/{id}} with the verb carrying the only colour on
     * the page. The full endpoint string stays contiguous in the text content.
     */
    private static void appendEndpointName(StringBuilder html, String endpoint) {
        int space = endpoint.indexOf(' ');
        if (space <= 0) {
            html.append(escape(endpoint));
            return;
        }
        String verb = endpoint.substring(0, space);
        html.append("<span class=\"verb ").append(verbClass(verb)).append("\">")
                .append(escape(verb))
                .append("</span> ")
                .append(escape(endpoint.substring(space + 1)));
    }

    private static String verbClass(String verb) {
        return switch (verb) {
            case "GET" -> "verb-get";
            case "POST" -> "verb-post";
            case "PUT" -> "verb-put";
            case "DELETE" -> "verb-delete";
            case "PATCH" -> "verb-patch";
            default -> "verb-other";
        };
    }

    private static void appendTypeName(StringBuilder html, String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot < 0) {
            html.append("<code class=\"type-name\">").append(escape(className)).append("</code>");
            return;
        }
        html.append("<code class=\"type-name\">")
                .append(escape(className.substring(lastDot + 1)))
                .append("</code>");
        html.append("<span class=\"type-package\">")
                .append(escape(className.substring(0, lastDot)))
                .append("</span>");
    }

    /**
     * Turns a JVM descriptor such as {@code (J)Lcom/example/OrderResponse;} into
     * {@code (long): OrderResponse}. Unparseable input is returned unchanged so
     * the report never hides what the agent actually recorded.
     */
    static String readableSignature(String descriptor) {
        if (descriptor == null || descriptor.isEmpty()) {
            return "";
        }
        if (descriptor.charAt(0) != '(') {
            return descriptor;
        }
        int close = descriptor.indexOf(')');
        if (close < 0) {
            return descriptor;
        }
        StringBuilder params = new StringBuilder();
        int cursor = 1;
        while (cursor < close) {
            int next = typeEnd(descriptor, cursor, close);
            if (next < 0) {
                return descriptor;
            }
            if (params.length() > 0) {
                params.append(", ");
            }
            params.append(typeName(descriptor.substring(cursor, next)));
            cursor = next;
        }
        String returns = descriptor.substring(close + 1);
        StringBuilder out = new StringBuilder("(").append(params).append(")");
        if (!returns.isEmpty() && !"V".equals(returns)) {
            int end = typeEnd(returns, 0, returns.length());
            if (end != returns.length()) {
                return descriptor;
            }
            out.append(": ").append(typeName(returns));
        }
        return out.toString();
    }

    private static int typeEnd(String descriptor, int start, int limit) {
        int cursor = start;
        while (cursor < limit && descriptor.charAt(cursor) == '[') {
            cursor++;
        }
        if (cursor >= limit) {
            return -1;
        }
        char kind = descriptor.charAt(cursor);
        if (kind == 'L') {
            int semicolon = descriptor.indexOf(';', cursor);
            return (semicolon < 0 || semicolon >= limit) ? -1 : semicolon + 1;
        }
        return "BCDFIJSZV".indexOf(kind) < 0 ? -1 : cursor + 1;
    }

    private static String typeName(String type) {
        int arrays = 0;
        while (arrays < type.length() && type.charAt(arrays) == '[') {
            arrays++;
        }
        String base = type.substring(arrays);
        String name = switch (base.isEmpty() ? ' ' : base.charAt(0)) {
            case 'B' -> "byte";
            case 'C' -> "char";
            case 'D' -> "double";
            case 'F' -> "float";
            case 'I' -> "int";
            case 'J' -> "long";
            case 'S' -> "short";
            case 'Z' -> "boolean";
            case 'V' -> "void";
            case 'L' -> simpleName(base.substring(1, base.length() - 1));
            default -> base;
        };
        return name + "[]".repeat(arrays);
    }

    private static String simpleName(String internalName) {
        String name = internalName.replace('/', '.');
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? name : name.substring(lastDot + 1);
    }

    private static String escape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record TypeGroup(String className, List<MethodCoverage> members) {
    }
}
