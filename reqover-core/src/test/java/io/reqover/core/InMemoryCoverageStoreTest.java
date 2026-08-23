package io.reqover.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryCoverageStoreTest extends CoverageStoreContract {
    @Override
    protected CoverageStore newStore() {
        return new InMemoryCoverageStore();
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
        assertEquals(SnapshotEvictionPolicy.OLDEST_FIRST, store.evictionPolicy());
    }

    @Test
    void rejectWhenFullKeepsExistingWindow() {
        InMemoryCoverageStore store =
                new InMemoryCoverageStore(2, SnapshotEvictionPolicy.REJECT_WHEN_FULL);

        store.flush(new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}")));
        store.flush(new CoverageBucket(UnitInfo.httpRequest("req-2", "GET", "/orders/{id}")));
        store.flush(new CoverageBucket(UnitInfo.httpRequest("req-3", "GET", "/orders/{id}")));

        assertEquals(2, store.snapshots().size());
        assertEquals("req-1", store.snapshots().get(0).unitInfo().unitId());
        assertEquals("req-2", store.snapshots().get(1).unitInfo().unitId());
        assertEquals(SnapshotEvictionPolicy.REJECT_WHEN_FULL, store.evictionPolicy());
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new InMemoryCoverageStore(0));
    }

    @Test
    void parsesEvictionPolicyTokens() {
        assertEquals(SnapshotEvictionPolicy.OLDEST_FIRST, SnapshotEvictionPolicy.fromProperty("oldest-first"));
        assertEquals(SnapshotEvictionPolicy.REJECT_WHEN_FULL, SnapshotEvictionPolicy.fromProperty("reject-when-full"));
        assertThrows(IllegalArgumentException.class, () -> SnapshotEvictionPolicy.fromProperty("sample"));
    }
}
