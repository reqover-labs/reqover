package io.reqover.report;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Reads and writes {@link CoverageReport} as JSON.
 *
 * <p>This is what makes a report outlive the JVM that produced it. A report
 * written here is fully resolved — class and method names are already in the
 * document — so the CLI can render, diff, and analyse it without the
 * {@code ProbeRegistry} that produced it.
 *
 * <p>Output is pretty-printed and deterministic for a given report. Separate
 * recordings retain their own timestamps and statuses, so their bytes can differ
 * even when the endpoint coverage is identical. Coverage diff compares the code
 * relationships rather than these diagnostic observations.
 */
public final class CoverageReportJson {
    /** Version of the document shape, raised when a field changes meaning. */
    public static final int SCHEMA_VERSION = 1;
    public static final int DEFAULT_REQUEST_DETAILS_LIMIT = 100;

    private CoverageReportJson() {
    }

    public static String write(CoverageReport report) {
        return write(report, DEFAULT_REQUEST_DETAILS_LIMIT);
    }

    /** Explicit larger exports are for trusted local recordings, not the default HTTP response. */
    public static String write(CoverageReport report, int requestDetailsLimit) {
        if (requestDetailsLimit < 0) {
            throw new IllegalArgumentException("requestDetailsLimit must not be negative");
        }
        Comparator<RequestObservation> newest = Comparator.comparing(RequestObservation::startedAt).reversed()
                .thenComparing(RequestObservation::requestId);
        Set<Integer> selected = IntStream.range(0, report.requests().size()).boxed()
                .sorted((a, b) -> newest.compare(report.requests().get(a), report.requests().get(b)))
                .limit(requestDetailsLimit).collect(Collectors.toSet());
        List<RequestObservation> details = IntStream.range(0, report.requests().size())
                .filter(selected::contains).mapToObj(report.requests()::get).toList();
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
        out.append("],\n");
        out.append("  \"omittedRequestDetails\": ").append(report.requests().size() - details.size()).append(",\n");
        out.append("  \"requests\": [");
        writeJoined(out, details, 2, (item, indent) -> writeRequest(out, item, indent));
        out.append("]\n");

        out.append("}\n");
        return out.toString();
    }

    public static CoverageReport read(String json) {
        Map<String, Object> root = Json.object(Json.parse(json), "report");
        requireReadableSchema(root);

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

        List<RequestObservation> requests = new ArrayList<>();
        for (Object item : Json.optionalArray(root, "requests")) {
            Map<String, Object> request = Json.object(item, "requests[]");
            requests.add(new RequestObservation(
                    Json.string(request, "requestId"), Json.string(request, "unitType"),
                    Json.string(request, "endpoint"), readInstant(request, "startedAt"),
                    request.get("endedAt") == null ? null : readInstant(request, "endedAt"),
                    Json.integer(request, "statusCode"), Json.strings(request, "threadNames"), readClasses(request)));
        }

        return new CoverageReport(
                generatedAt,
                Json.integer(root, "completedRequestCount"),
                List.copyOf(endpoints),
                List.copyOf(reverseIndex),
                List.copyOf(requests)
        );
    }

    /**
     * A report is written by one JVM and read by another — often a CI job
     * running a different Reqover version than the one that recorded it. A
     * document from a newer schema has to fail with an answer rather than
     * parse into something quietly wrong.
     */
    private static void requireReadableSchema(Map<String, Object> root) {
        // Absent means version 1: that is the only shape that has ever
        // existed, so a document without the field is readable rather than
        // suspect. Rejecting it would only break hand-written documents,
        // which is not the hazard this guard is for.
        if (!root.containsKey("schemaVersion")) {
            return;
        }
        int schemaVersion = Json.integer(root, "schemaVersion");
        if (schemaVersion > SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "report schema version " + schemaVersion + " is newer than this build understands ("
                            + SCHEMA_VERSION + "); upgrade Reqover to read it");
        }
        if (schemaVersion < 1) {
            throw new IllegalArgumentException(
                    "field 'schemaVersion' must be a positive integer, was " + schemaVersion);
        }
    }

    private static EndpointCoverage readEndpoint(Map<String, Object> node) {
        return new EndpointCoverage(
                Json.string(node, "endpoint"),
                Json.integer(node, "requestCount"),
                Json.strings(node, "requestIds"),
                Json.strings(node, "threadNames"),
                readClasses(node)
        );
    }

    private static List<ClassCoverage> readClasses(Map<String, Object> node) {
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

        return List.copyOf(classes);
    }

    private static Instant readInstant(Map<String, Object> node, String field) {
        try {
            return Instant.parse(Json.string(node, field));
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("field '" + field + "' must be an ISO-8601 instant", error);
        }
    }

    private static void writeRequest(StringBuilder out, RequestObservation request, String indent) {
        String inner = indent + "  ";
        out.append(indent).append("{\n");
        out.append(inner).append("\"requestId\": ");
        Json.writeString(out, request.requestId());
        out.append(",\n").append(inner).append("\"unitType\": ");
        Json.writeString(out, request.unitType());
        out.append(",\n").append(inner).append("\"endpoint\": ");
        Json.writeString(out, request.endpoint());
        out.append(",\n").append(inner).append("\"startedAt\": ");
        Json.writeString(out, request.startedAt().toString());
        out.append(",\n").append(inner).append("\"endedAt\": ");
        if (request.endedAt() == null) {
            out.append("null");
        } else {
            Json.writeString(out, request.endedAt().toString());
        }
        out.append(",\n").append(inner).append("\"statusCode\": ").append(request.statusCode());
        out.append(",\n").append(inner).append("\"threadNames\": ");
        writeStringArray(out, request.threadNames());
        out.append(",\n").append(inner).append("\"classes\": [");
        writeJoined(out, request.classes(), inner.length(), (item, childIndent) -> writeClass(out, item, childIndent));
        out.append("]\n").append(indent).append("}");
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
