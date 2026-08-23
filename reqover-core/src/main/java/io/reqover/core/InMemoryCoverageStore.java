package io.reqover.core;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

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
 */
public final class InMemoryCoverageStore implements CoverageStore {
    /** Default retention bound, sized for local development and demo traffic. */
    public static final int DEFAULT_MAX_SNAPSHOTS = 10_000;

    private final ConcurrentLinkedQueue<CoverageBucketSnapshot> completed = new ConcurrentLinkedQueue<>();
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
    public void clear() {
        completed.clear();
        size.set(0);
    }
}
