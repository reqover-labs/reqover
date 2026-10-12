/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.report;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Bounded time/interval aggregates over all retained HTTP observations. */
final class TimeDistribution {
    private static final int LIMIT = 256;
    private static final String[] LABELS = {"< 100 ms", "100-499 ms", "500-999 ms", "1000-1999 ms", ">= 2000 ms", "Not timed"};
    private TimeDistribution() { }

    static int band(RequestObservation request) {
        Double duration = request.recordedDurationMillis();
        if (duration == null) { return 5; }
        if (duration < 100) { return 0; }
        if (duration < 500) { return 1; }
        if (duration < 1000) { return 2; }
        if (duration < 2000) { return 3; }
        return 4;
    }

    static String json(List<RequestObservation> requests) {
        TreeMap<Long, List<RequestObservation>> windows = new TreeMap<>();
        int total = 0, omitted = 0, unsupported = 0;
        for (RequestObservation request : requests) {
            if (!request.isHttp()) { continue; }
            total++;
            long seconds = request.startedAt().getEpochSecond();
            // Leave margin at the native JavaScript Date limits for bucket endpoints.
            if (Math.abs(seconds) > 8_639_999_999_000L) { unsupported++; continue; }
            long start = Math.floorDiv(seconds, 300) * 300;
            windows.computeIfAbsent(start, ignored -> new ArrayList<>()).add(request);
            if (windows.size() > LIMIT) { omitted += windows.pollFirstEntry().getValue().size(); }
        }
        StringBuilder out = new StringBuilder("{\"kind\":\"reqover-time-distribution\",\"schemaVersion\":1,\"intervalSeconds\":300,\"windowLimit\":256,\"totalRetainedCount\":");
        out.append(total).append(",\"displayedCount\":").append(total - omitted - unsupported)
                .append(",\"omittedWindowRequestCount\":").append(omitted)
                .append(",\"unsupportedStartCount\":").append(unsupported).append(",\"windows\":[");
        boolean comma = false;
        for (var entry : windows.entrySet()) {
            if (comma) { out.append(','); } comma = true;
            out.append("{\"startedAt\":"); Json.writeString(out, Instant.ofEpochSecond(entry.getKey()).toString());
            out.append(",\"endedAt\":"); Json.writeString(out, Instant.ofEpochSecond(entry.getKey() + 300).toString());
            out.append(",\"requestCount\":").append(entry.getValue().size()).append(",\"endpoints\":");
            endpoints(out, entry.getValue());
            out.append(",\"bands\":[");
            for (int index = 0; index < LABELS.length; index++) {
                if (index > 0) { out.append(','); }
                final int selectedBand = index;
                var matched = entry.getValue().stream().filter(request -> band(request) == selectedBand).toList();
                out.append("{\"band\":").append(index).append(",\"label\":"); Json.writeString(out, LABELS[index]);
                metrics(out, RequestSummary.from(matched));
                out.append(",\"endpoints\":"); endpoints(out, matched); out.append('}');
            }
            out.append("]}");
        }
        return out.append("]}").toString();
    }

    private static void endpoints(StringBuilder out, List<RequestObservation> requests) {
        Map<String, List<RequestObservation>> groups = new TreeMap<>();
        for (var request : requests) { groups.computeIfAbsent(request.endpoint(), ignored -> new ArrayList<>()).add(request); }
        out.append('['); boolean comma = false;
        for (var entry : groups.entrySet()) {
            if (comma) { out.append(','); } comma = true;
            out.append("{\"endpoint\":"); Json.writeString(out, entry.getKey());
            metrics(out, RequestSummary.from(entry.getValue())); out.append('}');
        }
        out.append(']');
    }

    private static void metrics(StringBuilder out, RequestSummary summary) {
        out.append(",\"requestCount\":").append(summary.requestCount())
                .append(",\"knownStatusCount\":").append(summary.knownStatusCount())
                .append(",\"clientErrorCount\":").append(summary.clientErrorCount())
                .append(",\"serverErrorCount\":").append(summary.serverErrorCount())
                .append(",\"timedRequestCount\":").append(summary.timedRequestCount())
                .append(",\"averageMillis\":").append(summary.averageMillis())
                .append(",\"p95Millis\":").append(summary.p95Millis())
                .append(",\"maximumMillis\":").append(summary.maximumMillis())
                .append(",\"cumulativeMillis\":").append(summary.cumulativeMillis());
    }
}
