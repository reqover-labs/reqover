package io.reqover.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverProbeTest {
    @BeforeEach
    void setUp() {
        ReqoverProbe.resetGlobalStateForTests();
    }

    @AfterEach
    void tearDown() {
        ReqoverProbe.resetGlobalStateForTests();
    }

    @Test
    void recordsHitIntoCurrentBucket() {
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));

        try (CoverageContext.Scope ignored = CoverageContext.open(bucket)) {
            ReqoverProbe.hit(10, 3);
        }

        assertTrue(bucket.hasHit(10, 3));
        assertFalse(ReqoverProbe.globalSnapshot().hasHit(10, 3));
    }

    @Test
    void recordsHitIntoGlobalBucketWhenNoContextExists() {
        ReqoverProbe.hit(10, 3);

        assertTrue(ReqoverProbe.globalSnapshot().hasHit(10, 3));
    }

    @Test
    void invalidProbeIdsAreIgnoredWithoutDroppedHit() {
        ReqoverProbe.hit(-1, 3);
        ReqoverProbe.hit(10, -3);

        assertFalse(ReqoverProbe.globalSnapshot().hasHit(-1, 3));
        assertEquals(0, ReqoverProbe.droppedHitCount());
    }
}

