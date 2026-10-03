package io.reqover.report;

import java.time.Instant;
import java.util.List;

public record CoverageReport(
        Instant generatedAt,
        int completedRequestCount,
        List<EndpointCoverage> endpoints,
        List<CodeEndpointCoverage> reverseIndex,
        List<RequestObservation> requests
) {
    public CoverageReport {
        requests = List.copyOf(requests);
    }

    /** Keeps existing callers and older report fixtures source/binary compatible. */
    public CoverageReport(Instant generatedAt, int completedRequestCount,
                          List<EndpointCoverage> endpoints, List<CodeEndpointCoverage> reverseIndex) {
        this(generatedAt, completedRequestCount, endpoints, reverseIndex, List.of());
    }
}
