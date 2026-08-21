package io.reqover.report;

import io.reqover.core.CoverageBucket;
import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.UnitInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlCoverageReportRendererTest {
    @AfterEach
    void tearDown() {
        ProbeRegistry.clear();
    }

    @Test
    void rendersEndpointAndClassNames() {
        ProbeRegistry.register(new ProbeMetadata(10, 1, "sample.OrderService", "find", "()V", null));
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));
        bucket.record(10, 1);
        CoverageReport report = new CoverageReportGenerator().generate(List.of(bucket.snapshot()));

        String html = new HtmlCoverageReportRenderer().render(report);

        assertTrue(html.contains("GET /orders/{id}"));
        assertTrue(html.contains("OrderService"));
        assertTrue(html.contains("sample"));
        assertTrue(html.contains("Reqover Coverage Report"));
    }

    @Test
    void staysASingleSelfContainedPage() {
        String html = new HtmlCoverageReportRenderer().render(ReportFixtures.twoEndpointReport());

        assertFalse(html.contains("src=\"http"), "the report must not fetch anything");
        assertFalse(html.contains("href=\"http"), "the report must not fetch anything");
        assertTrue(html.startsWith("<!doctype html>"));
        assertTrue(html.trim().endsWith("</html>"));
    }

    @Test
    void shipsTheFilterHiddenSoItIsNeverDeadWithoutScripting() {
        String html = new HtmlCoverageReportRenderer().render(ReportFixtures.twoEndpointReport());

        assertTrue(html.contains("<div class=\"filter\" id=\"reqover-filter-box\" hidden>"), html);
        assertTrue(html.contains("box.hidden = false;"), "the script is what reveals the filter");
    }

    @Test
    void indexesEveryEndpointCardForFiltering() {
        String html = new HtmlCoverageReportRenderer().render(ReportFixtures.twoEndpointReport());

        assertEquals(2, countOccurrences(html, "<article class=\"endpoint\""));
        assertEquals(2, countOccurrences(html, "data-endpoint=\""));
        // The card is matched on its own classes and methods too, not just its name.
        assertTrue(html.contains("data-search=\"get /orders/{id} com.example.order.orderservice"), html);
        assertTrue(html.contains("data-search=\"post /payments com.example.payment.paymentservice"), html);
    }

    @Test
    void indexesTheReverseLookupRowsForFiltering() {
        String html = new HtmlCoverageReportRenderer().render(ReportFixtures.twoEndpointReport());

        assertEquals(3, countOccurrences(html, "<tr class=\"shared\" data-search=\"")
                + countOccurrences(html, "<tr data-search=\""));
    }

    @Test
    void makesADescriptorFindableByItsReadableForm() {
        String html = new HtmlCoverageReportRenderer().render(ReportFixtures.twoEndpointReport());

        // The search index carries both spellings so that typing either the JVM
        // descriptor or the readable signature finds the same method.
        assertTrue(html.contains("(j)lcom/example/orderresponse;"), html);
        assertTrue(html.contains("(long): orderresponse"), html);
    }

    @Test
    void escapesValuesThatWouldOtherwiseCloseAnAttribute() {
        CoverageReport report = new CoverageReport(
                ReportFixtures.twoEndpointReport().generatedAt(),
                1,
                List.of(ReportFixtures.endpoint("GET /\"><script>alert(1)</script>", "req-1", "main")),
                List.of()
        );

        String html = new HtmlCoverageReportRenderer().render(report);

        assertFalse(html.contains("<script>alert(1)</script>"), html);
        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"), html);
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int at = haystack.indexOf(needle, from);
            if (at < 0) {
                return count;
            }
            count++;
            from = at + needle.length();
        }
    }
}
