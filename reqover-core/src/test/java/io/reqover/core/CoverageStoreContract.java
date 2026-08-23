package io.reqover.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reusable contract tests for {@link CoverageStore} implementations.
 *
 * <p>Subclass this and implement {@link #newStore()} to pin a second store
 * against the same behaviour the in-memory implementation is held to.
 */
public abstract class CoverageStoreContract {

    /** Fresh empty store under test. */
    protected abstract CoverageStore newStore();

    protected CoverageBucket bucket(String unitId) {
        return new CoverageBucket(UnitInfo.httpRequest(unitId, "GET", "/orders/{id}"));
    }

    @Test
    void flushedBucketAppearsInSnapshots() {
        CoverageStore store = newStore();
        CoverageBucket bucket = bucket("req-1");
        bucket.record(10, 3);

        store.flush(bucket);

        assertEquals(1, store.snapshots().size());
        assertTrue(store.snapshots().get(0).hasHit(10, 3));
        assertEquals("req-1", store.snapshots().get(0).unitInfo().unitId());
    }

    @Test
    void snapshotsReturnsCopySafeToIterateWhileAnotherThreadFlushes() throws Exception {
        CoverageStore store = newStore();
        store.flush(bucket("seed"));

        List<CoverageBucketSnapshot> view = store.snapshots();
        assertEquals(1, view.size());

        int readers = 4;
        int flushes = 40;
        ExecutorService pool = Executors.newFixedThreadPool(readers + 1);
        CyclicBarrier start = new CyclicBarrier(readers + 1);
        CountDownLatch done = new CountDownLatch(readers + 1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Future<?> writer = pool.submit(() -> {
            try {
                start.await(5, TimeUnit.SECONDS);
                for (int i = 0; i < flushes; i++) {
                    store.flush(bucket("w-" + i));
                }
            } catch (Throwable t) {
                failure.compareAndSet(null, t);
            } finally {
                done.countDown();
            }
        });

        List<Future<?>> readerFutures = new ArrayList<>();
        for (int r = 0; r < readers; r++) {
            readerFutures.add(pool.submit(() -> {
                try {
                    start.await(5, TimeUnit.SECONDS);
                    for (int i = 0; i < 200; i++) {
                        int sum = 0;
                        for (CoverageBucketSnapshot snapshot : view) {
                            sum += snapshot.unitInfo().unitId().length();
                        }
                        assertTrue(sum > 0);
                        // Fresh snapshots() must not be the same list instance.
                        assertNotSame(view, store.snapshots());
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                } finally {
                    done.countDown();
                }
            }));
        }

        assertTrue(done.await(30, TimeUnit.SECONDS), "contract threads timed out");
        writer.get(5, TimeUnit.SECONDS);
        for (Future<?> future : readerFutures) {
            future.get(5, TimeUnit.SECONDS);
        }
        pool.shutdownNow();
        if (failure.get() != null) {
            throw new AssertionError("contract concurrency failure", failure.get());
        }
        assertEquals(1, view.size(), "snapshot list returned earlier must stay stable");
    }

    @Test
    void clearEmptiesTheStore() {
        CoverageStore store = newStore();
        store.flush(bucket("a"));
        store.flush(bucket("b"));
        assertFalse(store.snapshots().isEmpty());

        store.clear();

        assertTrue(store.snapshots().isEmpty());
    }

    @Test
    void hitsAfterFlushDoNotMutateStoredSnapshot() {
        CoverageStore store = newStore();
        CoverageBucket bucket = bucket("req-live");
        bucket.record(1, 1);
        store.flush(bucket);

        bucket.record(2, 2);

        CoverageBucketSnapshot stored = store.snapshots().get(0);
        assertTrue(stored.hasHit(1, 1));
        assertFalse(stored.hasHit(2, 2), "post-flush hits must not mutate the retained snapshot");
        assertTrue(bucket.hasHit(2, 2), "the live bucket may keep receiving hits");
    }
}
