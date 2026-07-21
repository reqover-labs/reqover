package io.reqover.core;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record CoverageBucketSnapshot(
        UnitInfo unitInfo,
        Instant startedAt,
        Instant endedAt,
        int statusCode,
        Map<Integer, Set<Integer>> hitsByClass,
        Set<String> threadNames
) {
    public CoverageBucketSnapshot {
        java.util.Objects.requireNonNull(unitInfo, "unitInfo");
        java.util.Objects.requireNonNull(startedAt, "startedAt");
        hitsByClass = deepCopy(hitsByClass);
        threadNames = Set.copyOf(java.util.Objects.requireNonNull(threadNames, "threadNames"));
    }

    public boolean hasHit(int classId, int probeId) {
        Set<Integer> probes = hitsByClass.get(classId);
        return probes != null && probes.contains(probeId);
    }

    private static Map<Integer, Set<Integer>> deepCopy(Map<Integer, Set<Integer>> source) {
        java.util.Objects.requireNonNull(source, "hitsByClass");
        Map<Integer, Set<Integer>> copy = new java.util.HashMap<>();
        source.forEach((classId, probes) -> copy.put(classId, Set.copyOf(probes)));
        return Map.copyOf(copy);
    }
}

