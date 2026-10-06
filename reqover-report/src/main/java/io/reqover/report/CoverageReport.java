package io.reqover.report;

import java.time.Instant;
import java.util.List;

public record CoverageReport(
        Instant generatedAt,
        int completedRequestCount,
        List<EndpointCoverage> endpoints,
        List<CodeEndpointCoverage> reverseIndex
) {
}
