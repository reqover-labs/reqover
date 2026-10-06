package io.reqover.spring.boot;

import io.reqover.core.CoverageBucket;
import io.reqover.core.CoverageBucketSnapshot;
import io.reqover.core.CoverageStore;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.UnitInfo;
import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportJson;
import io.reqover.report.EndpointCoverage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverReportExporterTest {
    private final InMemoryCoverageStore store = new InMemoryCoverageStore();
    private final ReqoverReportService reportService = new ReqoverReportService(store);
    private final ReqoverReportProperties properties = new ReqoverReportProperties();

    @TempDir
    Path workspace;

    @BeforeEach
    void recordOneRequest() {
        ReqoverReportExporter.resetForTests();
        ProbeRegistry.register(new ProbeMetadata(10, 1, "sample.OrderService", "find", "()V", 12));
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));
        bucket.record(10, 1);
        bucket.finish(200);
        store.flush(bucket);
    }

    @AfterEach
    void clearRegistry() {
        ProbeRegistry.clear();
        ReqoverReportExporter.resetForTests();
    }

    @Test
    void writesNothingWhenNoPathIsConfigured() throws Exception {
        exporter().destroy();

        try (var entries = Files.list(workspace)) {
            assertEquals(0, entries.count());
        }
    }

    @Test
    void writesTheJsonReportOnShutdown() throws Exception {
        Path json = workspace.resolve("nested/report.json");
        properties.getExport().setJsonPath(json.toString());

        exporter().destroy();

        assertTrue(Files.exists(json), "the export creates missing parent directories");
        CoverageReport report = CoverageReportJson.read(Files.readString(json, StandardCharsets.UTF_8));
        assertEquals(1, report.completedRequestCount());
        assertEquals("GET /orders/{id}", report.endpoints().get(0).endpoint());
    }

    @Test
    void writesTheHtmlReportOnShutdown() throws Exception {
        Path html = workspace.resolve("report.html");
        properties.getExport().setHtmlPath(html.toString());

        exporter().destroy();

        String rendered = Files.readString(html, StandardCharsets.UTF_8);
        assertTrue(rendered.startsWith("<!doctype html>"), rendered.substring(0, 40));
        assertTrue(rendered.contains("GET /orders/{id}"));
    }

    @Test
    void servesTheSameJsonThroughTheEndpointAndTheExport() throws Exception {
        Path json = workspace.resolve("report.json");
        properties.getExport().setJsonPath(json.toString());

        exporter().destroy();

        // Identical apart from the timestamp, which is stamped per generation.
        String exported = Files.readString(json, StandardCharsets.UTF_8);
        assertEquals(
                withoutTimestamp(exported),
                withoutTimestamp(new ReqoverMvcReportEndpoint(reportService).json())
        );
    }

    @Test
    void doesNotFailShutdownWhenThePathCannotBeWritten() throws Exception {
        // A regular file where a directory would have to be: creating the
        // parent directory fails, and shutdown must survive that.
        Path blocker = Files.createFile(workspace.resolve("blocker"));
        Path unwritable = blocker.resolve("report.json");
        properties.getExport().setJsonPath(unwritable.toString());

        exporter().destroy();

        assertFalse(Files.exists(unwritable));
    }

    @Test
    void keepsEveryContextsRequestsWhenSeveralExportToOnePath() throws Exception {
        // Spring's test context cache closes several contexts at JVM exit; the
        // file must hold all of them, not just whichever closed last.
        Path json = workspace.resolve("report.json");
        properties.getExport().setJsonPath(json.toString());
        InMemoryCoverageStore otherStore = new InMemoryCoverageStore();
        CoverageBucket payment = new CoverageBucket(UnitInfo.httpRequest("req-2", "POST", "/payments"));
        payment.record(10, 1);
        payment.finish(201);
        otherStore.flush(payment);

        exporter().destroy();
        new ReqoverReportExporter(new ReqoverReportService(otherStore), properties.getExport()).destroy();

        CoverageReport report = CoverageReportJson.read(Files.readString(json, StandardCharsets.UTF_8));
        assertEquals(2, report.completedRequestCount());
        assertEquals(
                List.of("GET /orders/{id}", "POST /payments"),
                report.endpoints().stream().map(EndpointCoverage::endpoint).sorted().toList()
        );
    }

    @Test
    void sumsOneEndpointRecordedByTwoContexts() throws Exception {
        Path json = workspace.resolve("report.json");
        properties.getExport().setJsonPath(json.toString());
        InMemoryCoverageStore otherStore = new InMemoryCoverageStore();
        CoverageBucket again = new CoverageBucket(UnitInfo.httpRequest("req-2", "GET", "/orders/{id}"));
        again.finish(200);
        otherStore.flush(again);

        exporter().destroy();
        new ReqoverReportExporter(new ReqoverReportService(otherStore), properties.getExport()).destroy();

        CoverageReport report = CoverageReportJson.read(Files.readString(json, StandardCharsets.UTF_8));
        assertEquals(1, report.endpoints().size());
        assertEquals(2, report.endpoints().get(0).requestCount());
        assertEquals(2, report.completedRequestCount());
    }

    @Test
    void replacesAReportLeftByAnEarlierRun() throws Exception {
        Path json = workspace.resolve("report.json");
        Files.writeString(json, "stale");
        properties.getExport().setJsonPath(json.toString());

        exporter().destroy();

        assertEquals(1, CoverageReportJson.read(Files.readString(json, StandardCharsets.UTF_8)).completedRequestCount());
    }

    @Test
    void doesNotMixReportsExportedToDifferentPaths() throws Exception {
        Path first = workspace.resolve("first.json");
        Path second = workspace.resolve("second.json");
        properties.getExport().setJsonPath(first.toString());
        exporter().destroy();

        ReqoverReportProperties other = new ReqoverReportProperties();
        other.getExport().setJsonPath(second.toString());
        new ReqoverReportExporter(reportService, other.getExport()).destroy();

        assertEquals(1, CoverageReportJson.read(Files.readString(second, StandardCharsets.UTF_8)).completedRequestCount());
    }

    private ReqoverReportExporter exporter() {
        return new ReqoverReportExporter(reportService, properties.getExport());
    }

    private static String withoutTimestamp(String json) {
        return json.replaceAll("\"generatedAt\": \"[^\"]+\"", "\"generatedAt\": \"<stamp>\"");
    }

    @Test
    void countsEveryRequestWhenAStoreWithoutAggregatesIsMerged() throws Exception {
        Path json = workspace.resolve("report.json");
        properties.getExport().setJsonPath(json.toString());
        InMemoryCoverageStore windowed = new InMemoryCoverageStore(1);
        for (int i = 0; i < 5; i++) {
            CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("w-" + i, "GET", "/orders/{id}"));
            bucket.finish(200);
            windowed.flush(bucket);
        }
        CoverageStore windowOnly = new CoverageStore() {
            private final InMemoryCoverageStore delegate = new InMemoryCoverageStore();

            @Override
            public void flush(CoverageBucket bucket) {
                delegate.flush(bucket);
            }

            @Override
            public List<CoverageBucketSnapshot> snapshots() {
                return delegate.snapshots();
            }

            @Override
            public void clear() {
                delegate.clear();
            }
        };
        for (int i = 0; i < 2; i++) {
            CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("c-" + i, "GET", "/orders/{id}"));
            bucket.finish(200);
            windowOnly.flush(bucket);
        }

        new ReqoverReportExporter(new ReqoverReportService(windowed), properties.getExport()).destroy();
        new ReqoverReportExporter(new ReqoverReportService(windowOnly), properties.getExport()).destroy();

        CoverageReport report = CoverageReportJson.read(Files.readString(json, StandardCharsets.UTF_8));
        assertEquals(7, report.completedRequestCount());
        assertEquals(7, report.endpoints().get(0).requestCount());
    }
}
