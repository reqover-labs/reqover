package io.reqover.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void aggregatesKeepEveryUnitAfterTheWindowEvictsIt() {
        InMemoryCoverageStore store = new InMemoryCoverageStore(2);
        CoverageBucket early = new CoverageBucket(UnitInfo.httpRequest("req-1", "DELETE", "/orders/{id}"));
        early.record(10, 2);
        store.flush(early);
        for (int i = 2; i <= 4; i++) {
            CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-" + i, "GET", "/orders/{id}"));
            bucket.record(10, 1);
            store.flush(bucket);
        }

        assertTrue(store.snapshots().stream().noneMatch(s -> s.unitInfo().name().startsWith("DELETE")));
        Map<String, UnitAggregate> byName = store.aggregates().stream()
                .collect(Collectors.toMap(UnitAggregate::unitName, a -> a));
        assertEquals(1, byName.get("DELETE /orders/{id}").count());
        assertEquals(Set.of(2), byName.get("DELETE /orders/{id}").hitsByClass().get(10));
        assertEquals(3, byName.get("GET /orders/{id}").count());

        store.clear();
        assertTrue(store.aggregates().isEmpty());
    }

    @Test
    void aggregatesCountFlushesARejectingWindowIgnores() {
        InMemoryCoverageStore store = new InMemoryCoverageStore(1, SnapshotEvictionPolicy.REJECT_WHEN_FULL);
        store.flush(new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}")));
        store.flush(new CoverageBucket(UnitInfo.httpRequest("req-2", "GET", "/orders/{id}")));

        assertEquals(1, store.snapshots().size());
        assertEquals(2, store.aggregates().get(0).count());
    }
}
