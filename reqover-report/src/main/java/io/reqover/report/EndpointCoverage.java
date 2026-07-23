package io.reqover.report;

import java.util.List;

public record EndpointCoverage(
        String endpoint,
        int requestCount,
        List<String> requestIds,
        List<String> threadNames,
        List<ClassCoverage> classes
) {
}

