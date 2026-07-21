package io.reqover.core;

import java.util.Objects;

/**
 * Thread-bound coverage context used by probes on the hot path.
 */
public final class CoverageContext {
    private static final ThreadLocal<CoverageBucket> CURRENT = new ThreadLocal<>();

    private CoverageContext() {
    }

    public static CoverageBucket current() {
        return CURRENT.get();
    }

    public static void set(CoverageBucket bucket) {
        CURRENT.set(Objects.requireNonNull(bucket, "bucket"));
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Scope open(CoverageBucket bucket) {
        CoverageBucket previous = CURRENT.get();
        set(bucket);
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {
        private final CoverageBucket previous;
        private boolean closed;

        private Scope(CoverageBucket previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}

