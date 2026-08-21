package io.reqover.core;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mutable hit store for one request or logical unit of work.
 *
 * <p>All methods are safe to call from multiple threads; a bucket may receive
 * hits from several worker threads while a reactive request hops schedulers.
 */
public final class CoverageBucket {
    private static final int UNFINISHED_STATUS = CoverageBucketSnapshot.UNFINISHED_STATUS;

    /**
     * Status recorded for a unit of work that finished but has no HTTP status —
     * a scheduled job or a test case, for example.
     */
    public static final int NO_STATUS = 0;

    private final AtomicReference<UnitInfo> unitInfo;
    private final Clock clock;
    private final Instant startedAt;
    private final ConcurrentMap<Integer, Set<Integer>> hitsByClass = new ConcurrentHashMap<>();
    private final Set<String> threadNames = ConcurrentHashMap.newKeySet();
    private final AtomicReference<Instant> endedAt = new AtomicReference<>();
    private final AtomicInteger statusCode = new AtomicInteger(UNFINISHED_STATUS);

    public CoverageBucket(UnitInfo unitInfo) {
        this(unitInfo, Clock.systemUTC());
    }

    CoverageBucket(UnitInfo unitInfo, Clock clock) {
        this.unitInfo = new AtomicReference<>(Objects.requireNonNull(unitInfo, "unitInfo"));
        this.clock = Objects.requireNonNull(clock, "clock");
        this.startedAt = Instant.now(clock);
    }

    public static CoverageBucket global() {
        return new CoverageBucket(UnitInfo.global());
    }

    public UnitInfo unitInfo() {
        return unitInfo.get();
    }

    public void updateUnitInfo(UnitInfo unitInfo) {
        this.unitInfo.set(Objects.requireNonNull(unitInfo, "unitInfo"));
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

    /**
     * Marks this bucket finished. Only the first call wins; later calls keep
     * the original end time and status code.
     *
     * @return {@code true} if this call is the one that finished the bucket,
     * which makes it the caller responsible for flushing it exactly once
     */
    public boolean finish(int statusCode) {
        if (endedAt.compareAndSet(null, Instant.now(clock))) {
            this.statusCode.set(statusCode);
            return true;
        }
        return false;
    }

    public CoverageBucketSnapshot snapshot() {
        return new CoverageBucketSnapshot(
                unitInfo.get(),
                startedAt,
                endedAt.get(),
                statusCode.get(),
                hitsByClass,
                threadNames
        );
    }
}
