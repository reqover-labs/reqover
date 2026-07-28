package io.reqover.core;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Static entry point inserted into instrumented application bytecode.
 */
public final class ReqoverProbe {
    private static final AtomicReference<CoverageBucket> GLOBAL_BUCKET =
            new AtomicReference<>(CoverageBucket.global());
    private static final AtomicLong droppedHits = new AtomicLong();

    private ReqoverProbe() {
    }

    public static void hit(int classId, int probeId) {
        try {
            CoverageBucket bucket = CoverageContext.current();
            if (bucket == null) {
                bucket = GLOBAL_BUCKET.get();
            }
            bucket.record(classId, probeId);
        } catch (Throwable ignored) {
            droppedHits.incrementAndGet();
        }
    }

    public static CoverageBucketSnapshot globalSnapshot() {
        return GLOBAL_BUCKET.get().snapshot();
    }

    public static long droppedHitCount() {
        return droppedHits.get();
    }

    /**
     * Wipes the global bucket, the current thread's context, and all
     * registered probe metadata. Intended for test isolation only; calling
     * this in a running application permanently discards agent-registered
     * metadata.
     */
    public static void resetGlobalStateForTests() {
        GLOBAL_BUCKET.set(CoverageBucket.global());
        droppedHits.set(0);
        CoverageContext.clear();
        ProbeRegistry.clear();
    }
}
