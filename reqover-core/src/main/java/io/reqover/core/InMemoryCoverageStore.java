package io.reqover.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe {@link CoverageStore} that retains snapshots in heap.
 *
 * <p>The store keeps at most {@code maxSnapshots} entries. What happens once
 * that bound is reached is controlled by {@link SnapshotEvictionPolicy}:
 * {@link SnapshotEvictionPolicy#OLDEST_FIRST} drops the oldest snapshot
 * (the historical default), while {@link SnapshotEvictionPolicy#REJECT_WHEN_FULL}
 * keeps the existing window and ignores further flushes. Eviction is
 * best-effort under heavy concurrency, but the size stays close to the
 * configured bound.
 *
 * <p>Independently of that window, every flush is folded into a per-unit
 * {@link UnitAggregate}, including flushes the window then evicts or rejects.
 * The aggregates are what keep an endpoint in the report after its last
 * request has left the window.
 *
 * <p>Aggregates are bounded too, by distinct unit names rather than traffic:
 * at most {@link #MAX_AGGREGATES} names (an un-templated URL makes a new name
 * per request) and {@link #MAX_THREAD_NAMES} thread names each. A name past
 * the bound is still retained in the snapshot window, just not totalled.
 */
public final class InMemoryCoverageStore implements CoverageStore {
    /** Default retention bound, sized for local development and demo traffic. */
    public static final int DEFAULT_MAX_SNAPSHOTS = 10_000;
    /** Distinct unit names totalled; an endpoint is a name, so real services stay far below it. */
    public static final int MAX_AGGREGATES = 2_000;
    /** Thread names kept per aggregate; enough to show a request hopping pools. */
    public static final int MAX_THREAD_NAMES = 64;

    private final ConcurrentLinkedQueue<CoverageBucketSnapshot> completed = new ConcurrentLinkedQueue<>();
    private final Map<String, Accumulator> aggregates = new ConcurrentHashMap<>();
    /** Flushes share it; clear() takes it exclusively so window and aggregates reset together. */
    private final ReadWriteLock clearLock = new ReentrantReadWriteLock();
    private final AtomicInteger size = new AtomicInteger();
    private final int maxSnapshots;
    private final SnapshotEvictionPolicy evictionPolicy;

    public InMemoryCoverageStore() {
        this(DEFAULT_MAX_SNAPSHOTS, SnapshotEvictionPolicy.OLDEST_FIRST);
    }

    public InMemoryCoverageStore(int maxSnapshots) {
        this(maxSnapshots, SnapshotEvictionPolicy.OLDEST_FIRST);
    }

    public InMemoryCoverageStore(int maxSnapshots, SnapshotEvictionPolicy evictionPolicy) {
        if (maxSnapshots <= 0) {
            throw new IllegalArgumentException("maxSnapshots must be positive: " + maxSnapshots);
        }
        this.maxSnapshots = maxSnapshots;
        this.evictionPolicy = Objects.requireNonNull(evictionPolicy, "evictionPolicy");
    }

    /** The retention bound this store was built with. */
    public int maxSnapshots() {
        return maxSnapshots;
    }

    /** Policy applied when {@link #maxSnapshots()} is reached. */
    public SnapshotEvictionPolicy evictionPolicy() {
        return evictionPolicy;
    }

    @Override
    public void flush(CoverageBucket bucket) {
        CoverageBucketSnapshot snapshot = bucket.snapshot();
        clearLock.readLock().lock();
        try {
            aggregate(snapshot);
            retain(snapshot);
        } finally {
            clearLock.readLock().unlock();
        }
    }

    private void aggregate(CoverageBucketSnapshot snapshot) {
        String name = snapshot.unitInfo().name();
        Accumulator accumulator = aggregates.get(name);
        if (accumulator == null) {
            if (aggregates.size() >= MAX_AGGREGATES) {
                return;
            }
            accumulator = aggregates.computeIfAbsent(name, ignored -> new Accumulator(name, snapshot.unitInfo().unitType()));
        }
        accumulator.add(snapshot);
    }

    private void retain(CoverageBucketSnapshot snapshot) {
        if (evictionPolicy == SnapshotEvictionPolicy.REJECT_WHEN_FULL) {
            // Reserve a slot first so concurrent flushes cannot overshoot the bound.
            int current = size.get();
            while (current < maxSnapshots) {
                if (size.compareAndSet(current, current + 1)) {
                    completed.add(snapshot);
                    return;
                }
                current = size.get();
            }
            // Store is full — drop the new snapshot (already taken so the bucket
            // cannot mutate what was retained earlier).
            return;
        }

        completed.add(snapshot);
        if (size.incrementAndGet() > maxSnapshots && completed.poll() != null) {
            size.decrementAndGet();
        }
    }

    @Override
    public List<CoverageBucketSnapshot> snapshots() {
        return List.copyOf(completed);
    }

    @Override
    public List<UnitAggregate> aggregates() {
        return aggregates.values().stream().map(Accumulator::toAggregate).toList();
    }

    @Override
    public void clear() {
        clearLock.writeLock().lock();
        try {
            completed.clear();
            size.set(0);
            aggregates.clear();
        } finally {
            clearLock.writeLock().unlock();
        }
    }

    private static final class Accumulator {
        private final String unitName;
        private final String unitType;
        private long count;
        private final Map<Integer, Set<Integer>> hitsByClass = new HashMap<>();
        private final Set<String> threadNames = new HashSet<>();

        private Accumulator(String unitName, String unitType) {
            this.unitName = unitName;
            this.unitType = unitType;
        }

        private synchronized void add(CoverageBucketSnapshot snapshot) {
            count++;
            snapshot.hitsByClass().forEach((classId, probes) ->
                    hitsByClass.computeIfAbsent(classId, ignored -> new HashSet<>()).addAll(probes));
            for (String threadName : snapshot.threadNames()) {
                if (threadNames.size() >= MAX_THREAD_NAMES) {
                    break;
                }
                threadNames.add(threadName);
            }
        }

        private synchronized UnitAggregate toAggregate() {
            return new UnitAggregate(unitName, unitType, count, hitsByClass, threadNames);
        }
    }
}
