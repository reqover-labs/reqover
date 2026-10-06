package io.reqover.report;

import io.reqover.core.CoverageBucket;
import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.UnitAggregate;
import io.reqover.core.UnitInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

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

    @Test
    void keepsEndpointsWhoseRequestsLeftTheSnapshotWindow() {
        ProbeRegistry.register(new ProbeMetadata(10, 1, "sample.OrderService", "find", "()V", 12));
        ProbeRegistry.register(new ProbeMetadata(10, 2, "sample.OrderService", "cancel", "()V", 20));
        CoverageBucket retained = new CoverageBucket(UnitInfo.httpRequest("req-9", "GET", "/orders/{id}"));
        retained.record(10, 1);
        UnitAggregate orders = new UnitAggregate("GET /orders/{id}", UnitInfo.TYPE_HTTP_REQUEST, 7,
                Map.of(10, Set.of(1)), Set.of("main"));
        UnitAggregate cancel = new UnitAggregate("DELETE /orders/{id}", UnitInfo.TYPE_HTTP_REQUEST, 2,
                Map.of(10, Set.of(2)), Set.of("main"));

        CoverageReport report = new CoverageReportGenerator()
                .generate(List.of(retained.snapshot()), List.of(orders, cancel));

        assertEquals(9, report.completedRequestCount());
        EndpointCoverage evicted = report.endpoints().get(0);
        assertEquals("DELETE /orders/{id}", evicted.endpoint());
        assertEquals(2, evicted.requestCount());
        assertEquals(List.of(), evicted.requestIds(), "request ids exist only for retained snapshots");
        assertEquals(List.of("DELETE /orders/{id}"), report.reverseIndex().stream()
                .filter(code -> code.methodName().equals("cancel")).findFirst().orElseThrow().endpoints());
        EndpointCoverage retainedEndpoint = report.endpoints().get(1);
        assertEquals(7, retainedEndpoint.requestCount());
        assertEquals(List.of("req-9"), retainedEndpoint.requestIds());
    }
}
