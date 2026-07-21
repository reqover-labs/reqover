package io.reqover.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CoverageContextTest {
    @AfterEach
    void tearDown() {
        CoverageContext.clear();
    }

    @Test
    void setsAndClearsCurrentBucket() {
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));

        CoverageContext.set(bucket);
        assertSame(bucket, CoverageContext.current());

        CoverageContext.clear();
        assertNull(CoverageContext.current());
    }

    @Test
    void scopeRestoresPreviousBucket() {
        CoverageBucket outer = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));
        CoverageBucket inner = new CoverageBucket(UnitInfo.httpRequest("req-2", "POST", "/payments"));

        CoverageContext.set(outer);
        try (CoverageContext.Scope ignored = CoverageContext.open(inner)) {
            assertSame(inner, CoverageContext.current());
        }

        assertSame(outer, CoverageContext.current());
    }
}

