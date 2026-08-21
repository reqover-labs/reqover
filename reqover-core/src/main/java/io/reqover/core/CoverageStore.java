package io.reqover.core;

import java.util.List;

/**
 * Destination for coverage buckets whose unit of work has finished.
 *
 * <p>This is the extension point for retention: {@link InMemoryCoverageStore}
 * keeps a bounded window in heap, but an implementation is free to write
 * snapshots to disk, to a database, or to drop them under a sampling rule.
 *
 * <p>Implementations must be safe for concurrent use. {@link #flush} is called
 * on the thread that completed the unit of work — an HTTP worker in the common
 * case — so it must not block for long.
 */
public interface CoverageStore {
    /**
     * Records a finished bucket. Implementations should call
     * {@link CoverageBucket#snapshot()} immediately, because the bucket may keep
     * receiving hits from stray threads after this call returns.
     */
    void flush(CoverageBucket bucket);

    /**
     * Returns the snapshots currently retained, oldest first. The returned list
     * is a copy and is safe to iterate while other threads flush.
     */
    List<CoverageBucketSnapshot> snapshots();

    /** Discards every retained snapshot. */
    void clear();
}
