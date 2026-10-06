package io.reqover.example.mvc;

import io.reqover.core.CoverageStore;
import io.reqover.report.CoverageReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MvcDiagnosticsIntegrationTest {
    @Autowired
    TestRestTemplate rest;

    @Autowired
    CoverageStore coverageStore;

    @BeforeEach
    void clearCoverage() {
        coverageStore.clear();
    }

    @Test
    void slowAndFailureDemoEndpointsProduceRealDiagnostics() {
        assertEquals(200, rest.getForEntity("/auto/diagnostics/delay/20", String.class).getStatusCode().value());
        assertEquals(503, rest.getForEntity("/auto/diagnostics/failure", String.class).getStatusCode().value());
        CoverageReport report = rest.getForObject("/reqover/report", CoverageReport.class);
        assertNotNull(report);
        var delay = report.requests().stream()
                .filter(request -> request.endpoint().equals("GET /auto/diagnostics/delay/{millis}"))
                .findFirst().orElseThrow();
        assertEquals(200, delay.statusCode());
        assertTrue(delay.recordedDurationMillis() >= 10);
        assertTrue(report.requests().stream().anyMatch(request ->
                request.endpoint().equals("GET /auto/diagnostics/failure") && request.statusCode() == 503));
    }

    @Test
    void rejectsDelaysOutsideTheBoundedDemoRange() {
        assertEquals(400, rest.getForEntity("/auto/diagnostics/delay/2001", String.class).getStatusCode().value());
        assertEquals(400, rest.getForEntity("/auto/diagnostics/delay/-1", String.class).getStatusCode().value());
    }
}
