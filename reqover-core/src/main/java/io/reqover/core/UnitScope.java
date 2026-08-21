package io.reqover.core;

import java.util.Objects;

/**
 * Attributes everything executed inside a try-with-resources block to one unit
 * of work, then flushes it to a store.
 *
 * <p>The Spring adapters do this for HTTP requests. This class is how anything
 * else gets the same treatment — a scheduled job, a message listener, a single
 * test case:
 *
 * <pre>{@code
 * try (UnitScope scope = UnitScope.open(store, UnitInfo.scheduledJob(runId, "nightly-settlement"))) {
 *     settlementJob.run();
 * }
 * }</pre>
 *
 * <p>Attribution follows the thread that opened the scope. Work handed to
 * another thread inside the block is not recorded under this unit unless that
 * thread opens a {@link #join} scope on the same bucket.
 *
 * <p>Closing the scope returned by {@link #open} finishes the bucket with
 * {@link CoverageBucket#NO_STATUS} and flushes it exactly once. To record a
 * status of your own, call {@link CoverageBucket#finish(int)} on
 * {@link #bucket()} first — the first call wins, and the flush still happens.
 * Closing twice is a no-op, so the block is safe to leave early or by
 * exception.
 */
public final class UnitScope implements AutoCloseable {
    private final CoverageStore store;
    private final CoverageBucket bucket;
    private final CoverageContext.Scope contextScope;
    private final boolean owner;
    private boolean closed;

    private UnitScope(CoverageStore store, CoverageBucket bucket, boolean owner) {
        this.store = store;
        this.bucket = bucket;
        this.owner = owner;
        this.contextScope = CoverageContext.open(bucket);
    }

    /**
     * Opens a scope for a new bucket owned by {@code unitInfo}. Closing it
     * finishes and flushes that bucket.
     */
    public static UnitScope open(CoverageStore store, UnitInfo unitInfo) {
        Objects.requireNonNull(store, "store");
        Objects.requireNonNull(unitInfo, "unitInfo");
        return new UnitScope(store, new CoverageBucket(unitInfo), true);
    }

    /**
     * Opens a scope on an existing bucket so a second thread can record into
     * the same unit of work. Closing it restores the previous context without
     * finishing or flushing — that stays with the {@link #open} scope that
     * created the bucket.
     */
    public static UnitScope join(CoverageBucket bucket) {
        Objects.requireNonNull(bucket, "bucket");
        return new UnitScope(null, bucket, false);
    }

    /** The bucket collecting hits for this unit of work. */
    public CoverageBucket bucket() {
        return bucket;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (owner) {
                bucket.finish(CoverageBucket.NO_STATUS);
                store.flush(bucket);
            }
        } finally {
            contextScope.close();
        }
    }
}
