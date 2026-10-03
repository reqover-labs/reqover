package io.reqover.report;

import java.util.ArrayList;
import java.util.List;

/** HTTP statistics over the retained observations, not total traffic. */
public record RequestSummary(
        int requestCount,
        int knownStatusCount,
        int clientErrorCount,
        int serverErrorCount,
        int timedRequestCount,
        Double averageMillis,
        Double p95Millis,
        Double maximumMillis,
        double cumulativeMillis
) {
    public static RequestSummary from(List<RequestObservation> requests) {
        int count = 0;
        int known = 0;
        int clientErrors = 0;
        int serverErrors = 0;
        List<Double> times = new ArrayList<>();
        for (RequestObservation request : requests) {
            if (!request.isHttp()) {
                continue;
            }
            count++;
            if (request.hasFinalHttpStatus()) {
                known++;
                if (request.statusCode() >= 500) {
                    serverErrors++;
                } else if (request.statusCode() >= 400) {
                    clientErrors++;
                }
            }
            Double elapsed = request.recordedDurationMillis();
            if (elapsed != null) {
                times.add(elapsed);
            }
        }
        times.sort(Double::compare);
        double total = times.stream().mapToDouble(Double::doubleValue).sum();
        return new RequestSummary(count, known, clientErrors, serverErrors, times.size(),
                times.isEmpty() ? null : total / times.size(),
                times.isEmpty() ? null : times.get((int) Math.ceil(times.size() * 0.95) - 1),
                times.isEmpty() ? null : times.get(times.size() - 1), total);
    }

    public int unknownStatusCount() {
        return requestCount - knownStatusCount;
    }

    public Double httpFailurePercent() {
        return knownStatusCount == 0 ? null : (clientErrorCount + serverErrorCount) * 100.0 / knownStatusCount;
    }
}
