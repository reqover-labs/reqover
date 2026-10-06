package io.reqover.report;

import io.reqover.core.UnitInfo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class DiagnosticDashboardTest {
    @Test
    void providesAnOfflineWorkspaceAndAccessibleControls() {
        String html = new HtmlCoverageReportRenderer().render(report(List.of(request("r1", "GET /orders/{id}"))));
        assertTrue(html.contains("class=\"dashboard-sidebar\""));
        assertTrue(html.contains("id=\"reqover-workspace\" hidden"));
        assertTrue(html.contains("aria-label=\"Pause animation\""));
        assertTrue(html.contains("aria-label=\"Zoom in\""));
        assertTrue(html.contains("id=\"reqover-inspector\""));
        assertTrue(html.contains("Observed associations"));
        assertTrue(html.contains("prefers-reduced-motion"));
        assertTrue(html.contains("Lucide"), "standalone exports must retain the icon license");
        assertFalse(html.contains("src=\"http"));
    }

    @Test
    void embedsIndividualMethodsAndReverseRelationshipsWithoutInventingCallEdges() {
        RequestObservation first = request("r1", "GET /orders/{id}");
        RequestObservation second = new RequestObservation("r2", UnitInfo.TYPE_HTTP_REQUEST,
                "POST /payments", first.startedAt(), first.endedAt(), 200, List.of("worker"),
                List.of(ReportFixtures.classCoverage(2, "sample.PaymentService", "pay", "()V", 8)));
        Map<String, Object> data = graphData(new HtmlCoverageReportRenderer().render(report(List.of(first, second))));
        List<Object> requests = Json.optionalArray(data, "requests");
        assertEquals(2, requests.size());
        Map<String, Object> r1 = Json.object(requests.get(0), "request");
        assertEquals("sample.SharedValidator", Json.string(Json.object(
                Json.optionalArray(r1, "classes").get(0), "class"), "className"));
        assertEquals(1, Json.optionalArray(r1, "classes").size());
        assertFalse(data.containsKey("spans"));
        assertFalse(data.containsKey("callEdges"));
        assertEquals(2, Json.strings(Json.object(Json.optionalArray(data, "reverseIndex").get(0), "code"), "endpoints").size());
    }

    @Test
    void keepsLegacyRetestRelationshipsButDoesNotInventRequests() {
        String html = new HtmlCoverageReportRenderer().render(ReportFixtures.twoEndpointReport());
        Map<String, Object> data = graphData(html);
        assertTrue(Json.optionalArray(data, "requests").isEmpty());
        assertEquals(3, Json.optionalArray(data, "reverseIndex").size());
        assertTrue(html.contains("No per-request diagnostics"));
        assertFalse(html.contains("href=\"#request-list\""));
    }

    @Test
    void boundsRequestGraphDataToTheSameRecentWindowAsDetails() {
        List<RequestObservation> requests = IntStream.range(0, 150).mapToObj(i ->
                new RequestObservation("r" + i, UnitInfo.TYPE_HTTP_REQUEST, "GET /many",
                        Instant.EPOCH.plusSeconds(i), Instant.EPOCH.plusSeconds(i).plusMillis(10),
                        200, List.of(), List.of())).toList();
        Map<String, Object> data = graphData(new HtmlCoverageReportRenderer().render(report(requests)));
        List<Object> retained = Json.optionalArray(data, "requests");
        assertEquals(100, retained.size());
        assertEquals("r149", Json.string(Json.object(retained.get(0), "request"), "requestId"));
        assertEquals("r50", Json.string(Json.object(retained.get(99), "request"), "requestId"));
    }

    @Test
    void preventsScriptTerminationAndPreservesHostileTextInParsedData() {
        String hostile = "</script><img src=x onerror=alert(1)> & \u2028";
        String html = new HtmlCoverageReportRenderer().render(report(List.of(request(hostile, "GET /unsafe"))));
        assertFalse(html.contains(hostile));
        assertEquals(hostile, Json.string(Json.object(Json.optionalArray(graphData(html), "requests").get(0), "request"), "requestId"));
        assertTrue(html.contains("\\u003c/script\\u003e"));
    }

    @Test
    void doesNotMislabelNamedNonHttpScopesAsApis() {
        CoverageReport report = new CoverageReport(Instant.EPOCH, 2, List.of(), List.of(
                new CodeEndpointCoverage("sample.SharedValidator", "validate", "()V",
                        List.of("GET /orders/{id}", "nightly-validation"))),
                List.of(request("http", "GET /orders/{id}"), new RequestObservation(
                        "job", "scheduled-job", "nightly-validation", Instant.EPOCH,
                        Instant.EPOCH.plusMillis(3), 0, List.of("worker"), List.of())));
        String html = new HtmlCoverageReportRenderer().render(report);
        assertTrue(html.contains("Observed in 2+ endpoints"));
        assertFalse(html.contains("['APIs'"), "reverse-index scopes do not carry a reliable HTTP-only label");
        assertEquals(1, Json.optionalArray(graphData(html), "requests").size());
        assertEquals(List.of("GET /orders/{id}", "nightly-validation"), Json.strings(
                Json.object(Json.optionalArray(graphData(html), "reverseIndex").get(0), "code"), "endpoints"));
    }

    private static RequestObservation request(String id, String endpoint) {
        return new RequestObservation(id, UnitInfo.TYPE_HTTP_REQUEST, endpoint,
                Instant.EPOCH, Instant.EPOCH.plusMillis(1200), 503, List.of("worker"),
                List.of(ReportFixtures.classCoverage(1, "sample.SharedValidator", "validate", "()V", 4)));
    }

    private static CoverageReport report(List<RequestObservation> requests) {
        return new CoverageReport(Instant.EPOCH, requests.size(), List.of(), List.of(
                new CodeEndpointCoverage("sample.SharedValidator", "validate", "()V",
                        List.of("GET /orders/{id}", "POST /payments"))), requests);
    }

    private static Map<String, Object> graphData(String html) {
        var match = Pattern.compile("<script type=\"application/json\" id=\"reqover-map-data\">(.*?)</script>", Pattern.DOTALL).matcher(html);
        assertTrue(match.find(), "standalone graph data must be embedded");
        return Json.object(Json.parse(match.group(1)), "graph");
    }
}
