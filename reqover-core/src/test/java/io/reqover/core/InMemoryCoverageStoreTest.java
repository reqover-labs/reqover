package io.reqover.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCoverageStoreTest {
    @Test
    void flushesSnapshots() {
        InMemoryCoverageStore store = new InMemoryCoverageStore();
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));
        bucket.record(10, 3);

        store.flush(bucket);

        assertEquals(1, store.snapshots().size());
        assertTrue(store.snapshots().get(0).hasHit(10, 3));
    }
}

