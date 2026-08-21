package io.reqover.report;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Reports shaped like the ones the demo applications produce, so the tests
 * read the way the tool is actually used.
 */
final class ReportFixtures {
    static final String ORDERS = "GET /orders/{id}";
    static final String PAYMENTS = "POST /payments";

    private ReportFixtures() {
    }

    /**
     * Two endpoints that both run {@code SharedValidator} — the case the whole
     * reverse lookup exists for.
     */
    static CoverageReport twoEndpointReport() {
        return new CoverageReport(
                Instant.parse("2026-08-21T09:00:00Z"),
                2,
                List.of(
                        endpoint(ORDERS, "req-1", "http-nio-8080-exec-1",
                                classCoverage(11, "com.example.order.OrderService", "find", "(J)Lcom/example/OrderResponse;", 24),
                                classCoverage(13, "com.example.SharedValidator", "validate", "(Ljava/lang/String;)V", 9)),
                        endpoint(PAYMENTS, "req-2", "http-nio-8080-exec-2",
                                classCoverage(12, "com.example.payment.PaymentService", "pay", "()V", 31),
                                classCoverage(13, "com.example.SharedValidator", "validate", "(Ljava/lang/String;)V", 9))
                ),
                List.of(
                        new CodeEndpointCoverage("com.example.SharedValidator", "validate",
                                "(Ljava/lang/String;)V", List.of(ORDERS, PAYMENTS)),
                        new CodeEndpointCoverage("com.example.order.OrderService", "find",
                                "(J)Lcom/example/OrderResponse;", List.of(ORDERS)),
                        new CodeEndpointCoverage("com.example.payment.PaymentService", "pay",
                                "()V", List.of(PAYMENTS))
                )
        );
    }

    static EndpointCoverage endpoint(String name, String requestId, String threadName, ClassCoverage... classes) {
        return new EndpointCoverage(name, 1, List.of(requestId), List.of(threadName), List.of(classes));
    }

    static ClassCoverage classCoverage(
            int classId,
            String className,
            String methodName,
            String descriptor,
            Integer lineNumber
    ) {
        return new ClassCoverage(
                classId,
                className,
                Set.of(1),
                List.of(new MethodCoverage(1, methodName, descriptor, lineNumber))
        );
    }
}
