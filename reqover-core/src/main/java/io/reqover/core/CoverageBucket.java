package io.reqover.core;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable hit store for one request or logical unit of work.
 */
public final class CoverageBucket {
    private static final int UNFINISHED_STATUS = -1;

    private final UnitInfo unitInfo;
    private final Instant startedAt;
    private final ConcurrentMap<Integer, Set<Integer>> hitsByClass = new ConcurrentHashMap<>();
    private final Set<String> threadNames = ConcurrentHashMap.newKeySet();
    private final AtomicReference<Instant> endedAt = new AtomicReference<>();
    private final AtomicInteger statusCode = new AtomicInteger(UNFINISHED_STATUS);

    public CoverageBucket(UnitInfo unitInfo) {
        this(unitInfo, Clock.systemUTC());
    }

    CoverageBucket(UnitInfo unitInfo, Clock clock) {
        this.unitInfo = java.util.Objects.requireNonNull(unitInfo, "unitInfo");
        this.startedAt = Instant.now(clock);
    }

    public static CoverageBucket global() {
        return new CoverageBucket(UnitInfo.global());
    }

    public UnitInfo unitInfo() {
        return unitInfo;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public void record(int classId, int probeId) {
        if (classId < 0 || probeId < 0) {
            return;
        }

        hitsByClass
                .computeIfAbsent(classId, ignored -> ConcurrentHashMap.newKeySet())
                .add(probeId);
        threadNames.add(Thread.currentThread().getName());
    }

    public boolean hasHit(int classId, int probeId) {
        Set<Integer> probes = hitsByClass.get(classId);
        return probes != null && probes.contains(probeId);
    }

    public boolean isEmpty() {
        return hitsByClass.isEmpty();
    }

    public void finish(int statusCode) {
        this.statusCode.set(statusCode);
        this.endedAt.compareAndSet(null, Instant.now());
    }

    public CoverageBucketSnapshot snapshot() {
        Map<Integer, Set<Integer>> hitCopy = new HashMap<>();
        hitsByClass.forEach((classId, probes) -> hitCopy.put(classId, Set.copyOf(probes)));

        return new CoverageBucketSnapshot(
                unitInfo,
                startedAt,
                endedAt.get(),
                statusCode.get(),
                Map.copyOf(hitCopy),
                Set.copyOf(threadNames)
        );
    }
}

