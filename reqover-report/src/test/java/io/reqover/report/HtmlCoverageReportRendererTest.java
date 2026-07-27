package io.reqover.report;

import io.reqover.core.CoverageBucket;
import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.UnitInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlCoverageReportRendererTest {
    @AfterEach
    void tearDown() {
        ProbeRegistry.clear();
    }

    @Test
    void rendersEndpointAndClassNames() {
        ProbeRegistry.register(new ProbeMetadata(10, 1, "sample.OrderService", "find", "()V", null));
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));
        bucket.record(10, 1);
        CoverageReport report = new CoverageReportGenerator().generate(List.of(bucket.snapshot()));

        String html = new HtmlCoverageReportRenderer().render(report);

        assertTrue(html.contains("GET /orders/{id}"));
        assertTrue(html.contains("OrderService"));
        assertTrue(html.contains("sample"));
        assertTrue(html.contains("Reqover Coverage Report"));
    }
}
