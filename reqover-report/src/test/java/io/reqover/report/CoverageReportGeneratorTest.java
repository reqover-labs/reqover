package io.reqover.report;

import io.reqover.core.CoverageBucket;
import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.UnitInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoverageReportGeneratorTest {
    @AfterEach
    void tearDown() {
        ProbeRegistry.clear();
    }

    @Test
    void groupsCoverageByEndpoint() {
        ProbeRegistry.register(new ProbeMetadata(10, 1, "sample.OrderService", "find", "()V", 12));
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));
        bucket.record(10, 1);

        CoverageReport report = new CoverageReportGenerator().generate(List.of(bucket.snapshot()));

        assertEquals(1, report.completedRequestCount());
        assertEquals("GET /orders/{id}", report.endpoints().get(0).endpoint());
        assertEquals("sample.OrderService", report.endpoints().get(0).classes().get(0).className());
        assertEquals("sample.OrderService", report.reverseIndex().get(0).className());
        assertEquals(List.of("GET /orders/{id}"), report.reverseIndex().get(0).endpoints());
    }
}
