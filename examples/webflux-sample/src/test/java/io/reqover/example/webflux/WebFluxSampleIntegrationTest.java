package io.reqover.example.webflux;

import io.reqover.report.CoverageReport;
import io.reqover.report.EndpointCoverage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebFluxSampleIntegrationTest {
    @Autowired
    WebTestClient webTestClient;

    @Test
    void keepsCoverageBucketAcrossThreadHop() {
        webTestClient.get()
                .uri("/reactive/orders/1")
                .exchange()
                .expectStatus().isOk();

        CoverageReport report = webTestClient.get()
                .uri("/reqover/report")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CoverageReport.class)
                .returnResult()
                .getResponseBody();

        EndpointCoverage endpoint = report.endpoints().stream()
                .filter(value -> value.endpoint().equals("GET /reactive/orders/{id}"))
                .findFirst()
                .orElseThrow();

        assertTrue(endpoint.threadNames().size() >= 2, "expected hits from multiple Reactor threads");
        assertTrue(endpoint.classes().stream().anyMatch(value -> value.className().equals("ReactiveOrderService")));
        assertTrue(endpoint.classes().stream().anyMatch(value -> value.className().equals("ReactiveOrderMapper")));
        assertTrue(endpoint.classes().stream().anyMatch(value -> value.className().equals("ReactiveValidator")));
        assertTrue(report.reverseIndex().stream()
                .anyMatch(item -> item.className().equals("ReactiveOrderService")
                        && item.endpoints().contains("GET /reactive/orders/{id}")));

        webTestClient.get()
                .uri("/reqover/report.html")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Reqover Coverage Report")));
    }
}
