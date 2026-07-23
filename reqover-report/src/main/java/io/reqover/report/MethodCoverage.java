package io.reqover.report;

public record MethodCoverage(
        int probeId,
        String methodName,
        String descriptor,
        Integer lineNumber
) {
}

