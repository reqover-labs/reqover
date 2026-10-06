package io.reqover.example.webflux;

import io.reqover.core.CoverageStore;
import io.reqover.report.CoverageReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebFluxDiagnosticsIntegrationTest {
    @Autowired
    WebTestClient client;

    @Autowired
    CoverageStore coverageStore;

    @BeforeEach
    void clearCoverage() {
        coverageStore.clear();
    }

    @Test
    void recordsReactiveDelayAndExplicitHttpFailure() {
        client.get().uri("/auto/reactive/diagnostics/delay/20").exchange().expectStatus().isOk();
        client.get().uri("/auto/reactive/diagnostics/failure").exchange().expectStatus().isEqualTo(503);
        CoverageReport report = client.get().uri("/reqover/report").exchange().expectStatus().isOk()
                .expectBody(CoverageReport.class).returnResult().getResponseBody();
        assertNotNull(report);
        var delay = report.requests().stream()
                .filter(request -> request.endpoint().equals("GET /auto/reactive/diagnostics/delay/{millis}"))
                .findFirst().orElseThrow();
        assertEquals(200, delay.statusCode());
        assertTrue(delay.recordedDurationMillis() >= 10);
        assertTrue(report.requests().stream().anyMatch(request ->
                request.endpoint().equals("GET /auto/reactive/diagnostics/failure") && request.statusCode() == 503));
    }

    @Test
    void rejectsDelaysOutsideTheBoundedDemoRange() {
        client.get().uri("/auto/reactive/diagnostics/delay/2001").exchange().expectStatus().isBadRequest();
        client.get().uri("/auto/reactive/diagnostics/delay/-1").exchange().expectStatus().isBadRequest();
    }
}
