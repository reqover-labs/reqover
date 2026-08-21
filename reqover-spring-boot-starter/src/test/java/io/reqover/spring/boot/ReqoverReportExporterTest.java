package io.reqover.spring.boot;

import io.reqover.core.CoverageBucket;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.UnitInfo;
import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportJson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
        ProbeRegistry.register(new ProbeMetadata(10, 1, "sample.OrderService", "find", "()V", 12));
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));
        bucket.record(10, 1);
        bucket.finish(200);
        store.flush(bucket);
    }

    @AfterEach
    void clearRegistry() {
        ProbeRegistry.clear();
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

    private ReqoverReportExporter exporter() {
        return new ReqoverReportExporter(reportService, properties.getExport());
    }

    private static String withoutTimestamp(String json) {
        return json.replaceAll("\"generatedAt\": \"[^\"]+\"", "\"generatedAt\": \"<stamp>\"");
    }
}
