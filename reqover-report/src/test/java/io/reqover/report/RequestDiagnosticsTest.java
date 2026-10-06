package io.reqover.report;

import io.reqover.core.CoverageBucketSnapshot;
import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.UnitInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class RequestDiagnosticsTest {
    private static final Instant START = Instant.parse("2026-10-03T00:00:00Z");

    @AfterEach
    void clearRegistry() {
        ProbeRegistry.clear();
    }

    @Test
    void exportsRequestTimingStatusAndIndividualCodeInsteadOfOnlyTheEndpointUnion() {
        ProbeRegistry.register(new ProbeMetadata(1, 0, "sample.FastService", "run", "()V", null));
        ProbeRegistry.register(new ProbeMetadata(2, 0, "sample.SlowService", "run", "()V", null));
        CoverageReport report = new CoverageReportGenerator().generate(List.of(
                snapshot("req-fast", 20, 200, 1), snapshot("req-slow", 1200, 500, 2)));

        Map<String, Object> root = Json.object(Json.parse(CoverageReportJson.write(report)), "report");
        List<Object> requests = Json.optionalArray(root, "requests");
        assertEquals(2, requests.size(), "individual request observations must survive export");
        Map<String, Object> first = Json.object(requests.get(0), "request");
        assertEquals("req-fast", Json.string(first, "requestId"));
        assertEquals(200, Json.integer(first, "statusCode"));
        assertEquals(START.plusMillis(20).toString(), Json.string(first, "endedAt"));
        assertEquals(1, Json.optionalArray(first, "classes").size());
        assertEquals("sample.FastService",
                Json.string(Json.object(Json.optionalArray(first, "classes").get(0), "class"), "className"));
        assertEquals(2, report.endpoints().get(0).classes().size());
    }

    @Test
    void rendersRealRequestOverviewAndNativeDetails() {
        CoverageReport report = new CoverageReportGenerator().generate(List.of(snapshot("slow-1", 1200, 500, 1)));
        String html = new HtmlCoverageReportRenderer().render(report);

        assertTrue(html.contains("Recorded HTTP requests"));
        assertTrue(html.contains("Observed processing time"));
        assertTrue(html.contains("data-request-status=\"500\""));
        assertTrue(html.contains("data-request-duration=\"1200.0\""));
        assertTrue(html.contains("<details class=\"request-detail\""));
        assertTrue(html.contains("slow-1"));
        assertTrue(html.contains("Endpoint to Code"));
    }

    @Test
    void rendersAnExplicitUnknownStateForOldFiles() {
        CoverageReport legacy = ReportFixtures.twoEndpointReport();
        String html = new HtmlCoverageReportRenderer().render(legacy);
        assertTrue(html.contains("No per-request diagnostics"));
        assertTrue(html.contains("OrderService"));
        assertFalse(html.contains("href=\"#request-list\""), "legacy reports must not link to a missing request section");
    }

    @Test
    void diagnosticsSurviveJsonRoundTripAndIgnoreUnrelatedFutureFields() {
        CoverageReport report = new CoverageReportGenerator().generate(List.of(snapshot("req-1", 150, 404, 1)));
        String json = CoverageReportJson.write(report);
        assertTrue(json.contains("\"requests\""));
        assertEquals(report, CoverageReportJson.read(json));
        assertEquals(report, CoverageReportJson.read(json.replace("\"statusCode\": 404", "\"future\": true, \"statusCode\": 404")));
    }

    @Test
    void escapesIndividualRequestIdsAndThreadNames() {
        CoverageBucketSnapshot snapshot = new CoverageBucketSnapshot(
                UnitInfo.httpRequest("<script>request</script>", "GET", "/orders/{id}"),
                START, START.plusMillis(10), 200, Map.of(), Set.of("\"><img src=x onerror=alert(1)>"));
        String html = new HtmlCoverageReportRenderer().render(new CoverageReportGenerator().generate(List.of(snapshot)));
        assertFalse(html.contains("<script>request</script>"));
        assertFalse(html.contains("<img src=x"));
        assertTrue(html.contains("&lt;script&gt;request&lt;/script&gt;"));
    }

    @Test
    void limitsHtmlDetailsWithoutTruncatingTheJsonOrOverview() {
        List<CoverageBucketSnapshot> observations = IntStream.range(0, 150).mapToObj(i ->
                new CoverageBucketSnapshot(UnitInfo.httpRequest("request-" + i, "GET", "/many"),
                        START.plusSeconds(i), START.plusSeconds(i).plusMillis(10), 200,
                        Map.<Integer, Set<Integer>>of(), Set.of("worker"))).toList();
        CoverageReport report = new CoverageReportGenerator().generate(observations);
        String html = new HtmlCoverageReportRenderer().render(report);
        assertEquals(100, java.util.regex.Pattern.compile("<details class=\"request-detail\"").matcher(html).results().count());
        assertTrue(html.contains("Most recent 100 of 150 retained HTTP observations"));
        assertTrue(html.contains("request-149"));
        assertFalse(html.contains("<code class=\"request-id\">request-0</code>"));
        assertEquals(100, CoverageReportJson.read(CoverageReportJson.write(report)).requests().size());
        assertEquals(150, CoverageReportJson.read(CoverageReportJson.write(report, 150)).requests().size());
        assertEquals(150, RequestSummary.from(report.requests()).requestCount());
    }

    static CoverageBucketSnapshot snapshot(String id, long millis, int status, int classId) {
        return new CoverageBucketSnapshot(UnitInfo.httpRequest(id, "GET", "/orders/{id}"),
                START, START.plusMillis(millis), status, Map.of(classId, Set.of(0)), Set.of("worker-1"));
    }

    @Test
    void boundsDefaultJsonDetailsWhilePreservingEndpointUnions() {
        List<CoverageBucketSnapshot> observations = IntStream.range(0, 150).mapToObj(i ->
                new CoverageBucketSnapshot(UnitInfo.httpRequest("r" + i, "GET", "/bounded"),
                        START.plusSeconds(i), START.plusSeconds(i).plusMillis(1), 200,
                        Map.<Integer, Set<Integer>>of(), Set.of())).toList();
        CoverageReport report = new CoverageReportGenerator().generate(observations);
        Map<String, Object> root = Json.object(Json.parse(CoverageReportJson.write(report)), "report");
        List<Object> details = Json.optionalArray(root, "requests");
        assertEquals(100, details.size());
        assertEquals("r50", Json.string(Json.object(details.get(0), "request"), "requestId"));
        assertEquals(150, Json.integer(root, "completedRequestCount"));
        assertEquals(150, report.endpoints().get(0).requestCount());
        assertEquals(50, Json.integer(root, "omittedRequestDetails"));
    }
}
