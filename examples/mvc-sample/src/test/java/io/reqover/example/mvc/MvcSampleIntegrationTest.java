package io.reqover.example.mvc;

import io.reqover.core.CoverageStore;
import io.reqover.report.CoverageReport;
import io.reqover.report.EndpointCoverage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MvcSampleIntegrationTest {
    @Autowired
    TestRestTemplate rest;

    @Autowired
    CoverageStore coverageStore;

    @BeforeEach
    void clearCoverage() {
        coverageStore.clear();
    }

    @Test
    void recordsEndpointBucketsSeparately() {
        ResponseEntity<String> orderResponse = rest.getForEntity("/orders/1", String.class);
        ResponseEntity<String> paymentResponse = rest.postForEntity("/payments", null, String.class);
        assertEquals(200, orderResponse.getStatusCode().value(), orderResponse.getBody());
        assertEquals(200, paymentResponse.getStatusCode().value(), paymentResponse.getBody());

        ResponseEntity<CoverageReport> response = rest.getForEntity("/reqover/report", CoverageReport.class);
        assertEquals(200, response.getStatusCode().value());
        CoverageReport report = response.getBody();
        assertNotNull(report);

        assertEquals(2, report.completedRequestCount());
        EndpointCoverage orders = endpoint(report, "GET /orders/{id}");
        EndpointCoverage payments = endpoint(report, "POST /payments");

        assertTrue(hasClass(orders, "OrderService"));
        assertTrue(hasClass(orders, "SharedValidator"));
        assertTrue(hasClass(payments, "PaymentService"));
        assertTrue(hasClass(payments, "SharedValidator"));
        assertTrue(report.reverseIndex().stream()
                .anyMatch(item -> item.className().equals("SharedValidator") && item.endpoints().size() == 2));

        ResponseEntity<String> htmlResponse = rest.getForEntity("/reqover/report.html", String.class);
        assertEquals(200, htmlResponse.getStatusCode().value());
        String html = htmlResponse.getBody();
        assertNotNull(html);
        assertTrue(html.contains("Reqover Coverage Report"));
        assertTrue(html.contains("GET /orders/{id}"));
        assertTrue(html.contains("Code to Endpoint Index"));
    }

    @Test
    void keepsBucketsSeparateForConcurrentRequests() throws Exception {
        int iterations = 10;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < iterations; i++) {
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);

                Future<?> order = executor.submit(() -> {
                    await(ready, start);
                    ResponseEntity<String> response = rest.getForEntity("/orders/1", String.class);
                    assertEquals(200, response.getStatusCode().value(), response.getBody());
                });
                Future<?> payment = executor.submit(() -> {
                    await(ready, start);
                    ResponseEntity<String> response = rest.postForEntity("/payments", null, String.class);
                    assertEquals(200, response.getStatusCode().value(), response.getBody());
                });

                assertTrue(ready.await(5, TimeUnit.SECONDS));
                start.countDown();
                order.get(10, TimeUnit.SECONDS);
                payment.get(10, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        ResponseEntity<CoverageReport> response = rest.getForEntity("/reqover/report", CoverageReport.class);
        assertEquals(200, response.getStatusCode().value());
        CoverageReport report = response.getBody();
        assertNotNull(report);
        EndpointCoverage orders = endpoint(report, "GET /orders/{id}");
        EndpointCoverage payments = endpoint(report, "POST /payments");

        assertEquals(iterations, orders.requestCount());
        assertEquals(iterations, payments.requestCount());
        assertTrue(hasClass(orders, "OrderService"));
        assertTrue(hasClass(payments, "PaymentService"));
        assertFalse(hasClass(orders, "PaymentService"));
        assertFalse(hasClass(payments, "OrderService"));
    }

    private static EndpointCoverage endpoint(CoverageReport report, String endpoint) {
        return report.endpoints().stream()
                .filter(value -> value.endpoint().equals(endpoint))
                .findFirst()
                .orElseThrow();
    }

    private static boolean hasClass(EndpointCoverage endpoint, String className) {
        return endpoint.classes().stream().anyMatch(value -> value.className().equals(className));
    }

    private static void await(CountDownLatch ready, CountDownLatch start) {
        try {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }
}
