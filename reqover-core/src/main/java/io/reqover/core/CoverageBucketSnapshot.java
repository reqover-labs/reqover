package io.reqover.core;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable point-in-time view of a {@link CoverageBucket}.
 *
 * <p>{@code endedAt} is {@code null} and {@code statusCode} is
 * {@link #UNFINISHED_STATUS} while the owning unit of work has not finished;
 * use {@link #finished()} to distinguish the two states.
 */
public record CoverageBucketSnapshot(
        UnitInfo unitInfo,
        Instant startedAt,
        Instant endedAt,
        int statusCode,
        Map<Integer, Set<Integer>> hitsByClass,
        Set<String> threadNames
) {
    /** Status code reported before {@link CoverageBucket#finish(int)} was called. */
    public static final int UNFINISHED_STATUS = -1;

    public CoverageBucketSnapshot {
        Objects.requireNonNull(unitInfo, "unitInfo");
        Objects.requireNonNull(startedAt, "startedAt");
        hitsByClass = deepCopy(hitsByClass);
        threadNames = Set.copyOf(Objects.requireNonNull(threadNames, "threadNames"));
    }

    public boolean hasHit(int classId, int probeId) {
        Set<Integer> probes = hitsByClass.get(classId);
        return probes != null && probes.contains(probeId);
    }

    /** Whether the owning unit of work had finished when this snapshot was taken. */
    public boolean finished() {
        return endedAt != null;
    }

    private static Map<Integer, Set<Integer>> deepCopy(Map<Integer, Set<Integer>> source) {
        Objects.requireNonNull(source, "hitsByClass");
        Map<Integer, Set<Integer>> copy = new HashMap<>();
        source.forEach((classId, probes) -> copy.put(classId, Set.copyOf(probes)));
        return Map.copyOf(copy);
    }
}
