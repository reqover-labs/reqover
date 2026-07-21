package io.reqover.core;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class InMemoryCoverageStore {
    private final ConcurrentLinkedQueue<CoverageBucketSnapshot> completed = new ConcurrentLinkedQueue<>();

    public void flush(CoverageBucket bucket) {
        completed.add(bucket.snapshot());
    }

    public List<CoverageBucketSnapshot> snapshots() {
        return List.copyOf(completed);
    }

    public void clear() {
        completed.clear();
    }
}

