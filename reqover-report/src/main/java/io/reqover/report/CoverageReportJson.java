package io.reqover.report;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reads and writes {@link CoverageReport} as JSON.
 *
 * <p>This is what makes a report outlive the JVM that produced it. A report
 * written here is fully resolved — class and method names are already in the
 * document — so the CLI can render, diff, and analyse it without the
 * {@code ProbeRegistry} that produced it.
 *
 * <p>Output is pretty-printed with sorted collections so that two runs over the
 * same traffic produce byte-identical files apart from {@code generatedAt}.
 * That is deliberate: baseline reports are meant to be committed and diffed.
 */
public final class CoverageReportJson {
    /** Version of the document shape, raised when a field changes meaning. */
    public static final int SCHEMA_VERSION = 1;

    private CoverageReportJson() {
    }

    public static String write(CoverageReport report) {
        StringBuilder out = new StringBuilder(4096);
        out.append("{\n");
        out.append("  \"schemaVersion\": ").append(SCHEMA_VERSION).append(",\n");
        out.append("  \"generatedAt\": ");
        Json.writeString(out, report.generatedAt().toString());
        out.append(",\n");
        out.append("  \"completedRequestCount\": ").append(report.completedRequestCount()).append(",\n");

        out.append("  \"endpoints\": [");
        writeJoined(out, report.endpoints(), 2, (item, indent) -> writeEndpoint(out, item, indent));
        out.append("],\n");

        out.append("  \"reverseIndex\": [");
        writeJoined(out, report.reverseIndex(), 2, (item, indent) -> writeReverseEntry(out, item, indent));
        out.append("]\n");

        out.append("}\n");
        return out.toString();
    }

    public static CoverageReport read(String json) {
        Map<String, Object> root = Json.object(Json.parse(json), "report");

        Instant generatedAt;
        try {
            generatedAt = Instant.parse(Json.string(root, "generatedAt"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("field 'generatedAt' must be an ISO-8601 instant", e);
        }

        List<EndpointCoverage> endpoints = new ArrayList<>();
        for (Object node : Json.optionalArray(root, "endpoints")) {
            endpoints.add(readEndpoint(Json.object(node, "endpoints[]")));
        }

        List<CodeEndpointCoverage> reverseIndex = new ArrayList<>();
        for (Object node : Json.optionalArray(root, "reverseIndex")) {
            Map<String, Object> entry = Json.object(node, "reverseIndex[]");
            reverseIndex.add(new CodeEndpointCoverage(
                    Json.string(entry, "className"),
                    Json.string(entry, "methodName"),
                    Json.string(entry, "descriptor"),
                    Json.strings(entry, "endpoints")
            ));
        }

        return new CoverageReport(
                generatedAt,
                Json.integer(root, "completedRequestCount"),
                List.copyOf(endpoints),
                List.copyOf(reverseIndex)
        );
    }

    private static EndpointCoverage readEndpoint(Map<String, Object> node) {
        List<ClassCoverage> classes = new ArrayList<>();
        for (Object classNode : Json.optionalArray(node, "classes")) {
            Map<String, Object> entry = Json.object(classNode, "classes[]");

            List<MethodCoverage> methods = new ArrayList<>();
            for (Object methodNode : Json.optionalArray(entry, "methods")) {
                Map<String, Object> method = Json.object(methodNode, "methods[]");
                methods.add(new MethodCoverage(
                        Json.integer(method, "probeId"),
                        Json.string(method, "methodName"),
                        Json.string(method, "descriptor"),
                        Json.nullableInteger(method, "lineNumber")
                ));
            }

            Set<Integer> probeIds = new LinkedHashSet<>();
            for (Object probeNode : Json.optionalArray(entry, "probeIds")) {
                probeIds.add(Json.integer(Map.of("probeIds[]", probeNode), "probeIds[]"));
            }

            classes.add(new ClassCoverage(
                    Json.integer(entry, "classId"),
                    Json.string(entry, "className"),
                    Set.copyOf(probeIds),
                    List.copyOf(methods)
            ));
        }

        return new EndpointCoverage(
                Json.string(node, "endpoint"),
                Json.integer(node, "requestCount"),
                Json.strings(node, "requestIds"),
                Json.strings(node, "threadNames"),
                List.copyOf(classes)
        );
    }

    private static void writeEndpoint(StringBuilder out, EndpointCoverage endpoint, String indent) {
        String inner = indent + "  ";
        out.append(indent).append("{\n");
        out.append(inner).append("\"endpoint\": ");
        Json.writeString(out, endpoint.endpoint());
        out.append(",\n");
        out.append(inner).append("\"requestCount\": ").append(endpoint.requestCount()).append(",\n");
        out.append(inner).append("\"requestIds\": ");
        writeStringArray(out, endpoint.requestIds());
        out.append(",\n");
        out.append(inner).append("\"threadNames\": ");
        writeStringArray(out, endpoint.threadNames());
        out.append(",\n");
        out.append(inner).append("\"classes\": [");
        writeJoined(out, endpoint.classes(), inner.length(), (item, childIndent) ->
                writeClass(out, item, childIndent));
        out.append("]\n");
        out.append(indent).append("}");
    }

    private static void writeClass(StringBuilder out, ClassCoverage classCoverage, String indent) {
        String inner = indent + "  ";
        out.append(indent).append("{\n");
        out.append(inner).append("\"classId\": ").append(classCoverage.classId()).append(",\n");
        out.append(inner).append("\"className\": ");
        Json.writeString(out, classCoverage.className());
        out.append(",\n");
        out.append(inner).append("\"probeIds\": [");
        List<Integer> probeIds = classCoverage.probeIds().stream().sorted().toList();
        for (int i = 0; i < probeIds.size(); i++) {
            out.append(i == 0 ? "" : ", ").append(probeIds.get(i));
        }
        out.append("],\n");
        out.append(inner).append("\"methods\": [");
        writeJoined(out, classCoverage.methods(), inner.length(), (item, childIndent) ->
                writeMethod(out, item, childIndent));
        out.append("]\n");
        out.append(indent).append("}");
    }

    private static void writeMethod(StringBuilder out, MethodCoverage method, String indent) {
        out.append(indent).append("{\"probeId\": ").append(method.probeId()).append(", \"methodName\": ");
        Json.writeString(out, method.methodName());
        out.append(", \"descriptor\": ");
        Json.writeString(out, method.descriptor());
        out.append(", \"lineNumber\": ").append(method.lineNumber() == null ? "null" : method.lineNumber());
        out.append("}");
    }

    private static void writeReverseEntry(StringBuilder out, CodeEndpointCoverage item, String indent) {
        String inner = indent + "  ";
        out.append(indent).append("{\n");
        out.append(inner).append("\"className\": ");
        Json.writeString(out, item.className());
        out.append(",\n");
        out.append(inner).append("\"methodName\": ");
        Json.writeString(out, item.methodName());
        out.append(",\n");
        out.append(inner).append("\"descriptor\": ");
        Json.writeString(out, item.descriptor());
        out.append(",\n");
        out.append(inner).append("\"endpoints\": ");
        writeStringArray(out, item.endpoints());
        out.append("\n");
        out.append(indent).append("}");
    }

    private static void writeStringArray(StringBuilder out, List<String> values) {
        out.append("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            Json.writeString(out, values.get(i));
        }
        out.append("]");
    }

    private static <T> void writeJoined(StringBuilder out, List<T> items, int indentWidth, ItemWriter<T> writer) {
        if (items.isEmpty()) {
            return;
        }
        String indent = " ".repeat(indentWidth + 2);
        out.append("\n");
        for (int i = 0; i < items.size(); i++) {
            writer.write(items.get(i), indent);
            out.append(i == items.size() - 1 ? "\n" : ",\n");
        }
        out.append(" ".repeat(indentWidth));
    }

    @FunctionalInterface
    private interface ItemWriter<T> {
        void write(T item, String indent);
    }
}
