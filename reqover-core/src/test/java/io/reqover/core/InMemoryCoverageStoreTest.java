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

    @Test
    void evictsOldestSnapshotsBeyondCapacity() {
        InMemoryCoverageStore store = new InMemoryCoverageStore(2);

        for (int i = 1; i <= 3; i++) {
            store.flush(new CoverageBucket(UnitInfo.httpRequest("req-" + i, "GET", "/orders/{id}")));
        }

        assertEquals(2, store.snapshots().size());
        assertEquals("req-2", store.snapshots().get(0).unitInfo().unitId());
        assertEquals("req-3", store.snapshots().get(1).unitInfo().unitId());
    }

    @Test
    void rejectsNonPositiveCapacity() {
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new InMemoryCoverageStore(0)
        );
    }
}

