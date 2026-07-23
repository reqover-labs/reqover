package io.reqover.report;

import java.util.List;
import java.util.Set;

public record ClassCoverage(
        int classId,
        String className,
        Set<Integer> probeIds,
        List<MethodCoverage> methods
) {
}

