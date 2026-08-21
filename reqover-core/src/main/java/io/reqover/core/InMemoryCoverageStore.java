package io.reqover.core;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe {@link CoverageStore} that retains snapshots in heap.
 *
 * <p>The store keeps at most {@code maxSnapshots} entries; once the bound is
 * reached, the oldest snapshots are evicted so a long-running application does
 * not grow memory without limit. Eviction is best-effort under heavy
 * concurrency, but the size stays close to the configured bound.
 */
public final class InMemoryCoverageStore implements CoverageStore {
    /** Default retention bound, sized for local development and demo traffic. */
    public static final int DEFAULT_MAX_SNAPSHOTS = 10_000;

    private final ConcurrentLinkedQueue<CoverageBucketSnapshot> completed = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger();
    private final int maxSnapshots;

    public InMemoryCoverageStore() {
        this(DEFAULT_MAX_SNAPSHOTS);
    }

    public InMemoryCoverageStore(int maxSnapshots) {
        if (maxSnapshots <= 0) {
            throw new IllegalArgumentException("maxSnapshots must be positive: " + maxSnapshots);
        }
        this.maxSnapshots = maxSnapshots;
    }

    /** The retention bound this store was built with. */
    public int maxSnapshots() {
        return maxSnapshots;
    }

    @Override
    public void flush(CoverageBucket bucket) {
        completed.add(bucket.snapshot());
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
