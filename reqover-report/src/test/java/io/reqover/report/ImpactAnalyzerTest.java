package io.reqover.report;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpactAnalyzerTest {
    private static final String ORDER_SERVICE =
            "src/main/java/com/example/order/OrderService.java";
    private static final String SHARED_VALIDATOR =
            "src/main/java/com/example/SharedValidator.java";

    @Test
    void namesTheEndpointThatRanTheChangedCode() {
        ImpactReport impact = ImpactAnalyzer.analyze(
                ReportFixtures.twoEndpointReport(), List.of(ORDER_SERVICE));

        assertEquals(List.of(ReportFixtures.ORDERS), impact.endpointNames());
        assertTrue(impact.hasImpact());
    }

    @Test
    void namesEveryEndpointThatRanSharedCode() {
        ImpactReport impact = ImpactAnalyzer.analyze(
                ReportFixtures.twoEndpointReport(), List.of(SHARED_VALIDATOR));

        assertEquals(List.of(ReportFixtures.ORDERS, ReportFixtures.PAYMENTS), impact.endpointNames());
    }

    @Test
    void reportsTheChangedMethodEachEndpointRan() {
        ImpactReport impact = ImpactAnalyzer.analyze(
                ReportFixtures.twoEndpointReport(), List.of(ORDER_SERVICE));

        List<CodeRef> code = impact.endpoints().get(0).changedCode();
        assertEquals(1, code.size());
        assertEquals("com.example.order.OrderService", code.get(0).className());
        assertEquals("find", code.get(0).methodName());
    }

    @Test
    void separatesPathsWithNoObservedCoverageFromMatchedOnes() {
        ImpactReport impact = ImpactAnalyzer.analyze(
                ReportFixtures.twoEndpointReport(),
                List.of(ORDER_SERVICE, "src/main/java/com/example/Unobserved.java")
        );

        assertEquals(List.of(ORDER_SERVICE), impact.matchedPaths());
        assertEquals(List.of("src/main/java/com/example/Unobserved.java"), impact.unmatchedPaths());
    }

    @Test
    void findsNothingWhenTheChangeTouchesNoObservedCode() {
        ImpactReport impact = ImpactAnalyzer.analyze(
                ReportFixtures.twoEndpointReport(), List.of("README.md"));

        assertFalse(impact.hasImpact());
        assertEquals(List.of("README.md"), impact.unmatchedPaths());
    }

    @Test
    void matchesOnlyAtADirectoryBoundary() {
        ImpactReport impact = ImpactAnalyzer.analyze(
                ReportFixtures.twoEndpointReport(),
                List.of("src/main/java/com/example/notorder/OrderService.java")
        );

        assertFalse(impact.hasImpact());
    }

    @Test
    void toleratesWindowsSeparatorsAndDotSlashPrefixes() {
        ImpactReport impact = ImpactAnalyzer.analyze(
                ReportFixtures.twoEndpointReport(),
                List.of(".\\src\\main\\java\\com\\example\\order\\OrderService.java")
        );

        assertEquals(List.of(ReportFixtures.ORDERS), impact.endpointNames());
    }

    @Test
    void ignoresBlankLinesFromAPipedDiff() {
        ImpactReport impact = ImpactAnalyzer.analyze(
                ReportFixtures.twoEndpointReport(), List.of("", "   ", ORDER_SERVICE));

        assertEquals(List.of(ORDER_SERVICE), impact.changedPaths());
    }

    @Test
    void matchesANestedClassThroughItsDeclaringFile() {
        CoverageReport report = new CoverageReport(
                ReportFixtures.twoEndpointReport().generatedAt(),
                1,
                List.of(),
                List.of(new CodeEndpointCoverage(
                        "com.example.order.OrderService$Row", "map", "()V", List.of(ReportFixtures.ORDERS)))
        );

        ImpactReport impact = ImpactAnalyzer.analyze(report, List.of(ORDER_SERVICE));

        assertEquals(List.of(ReportFixtures.ORDERS), impact.endpointNames());
    }

    @Test
    void matchesAKotlinSourceFile() {
        CoverageReport report = new CoverageReport(
                ReportFixtures.twoEndpointReport().generatedAt(),
                1,
                List.of(),
                List.of(new CodeEndpointCoverage(
                        "com.example.order.OrderService", "find", "()V", List.of(ReportFixtures.ORDERS)))
        );

        ImpactReport impact = ImpactAnalyzer.analyze(
                report, List.of("src/main/kotlin/com/example/order/OrderService.kt"));

        assertEquals(List.of(ReportFixtures.ORDERS), impact.endpointNames());
    }
}
