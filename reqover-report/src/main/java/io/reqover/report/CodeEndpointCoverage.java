package io.reqover.report;

import java.util.List;

public record CodeEndpointCoverage(
        String className,
        String methodName,
        String descriptor,
        List<String> endpoints
) {
}

