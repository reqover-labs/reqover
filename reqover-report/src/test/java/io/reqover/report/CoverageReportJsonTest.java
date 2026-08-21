package io.reqover.report;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageReportJsonTest {
    @Test
    void roundTripsAReportWithoutLosingAnyField() {
        CoverageReport report = ReportFixtures.twoEndpointReport();

        CoverageReport parsed = CoverageReportJson.read(CoverageReportJson.write(report));

        assertEquals(report, parsed);
    }

    @Test
    void roundTripsAnEmptyReport() {
        CoverageReport report = new CoverageReport(
                Instant.parse("2026-08-21T09:00:00Z"), 0, List.of(), List.of());

        assertEquals(report, CoverageReportJson.read(CoverageReportJson.write(report)));
    }

    @Test
    void roundTripsAMissingLineNumber() {
        CoverageReport report = new CoverageReport(
                Instant.parse("2026-08-21T09:00:00Z"),
                1,
                List.of(new EndpointCoverage(
                        "GET /a",
                        1,
                        List.of("req-1"),
                        List.of("main"),
                        List.of(new ClassCoverage(
                                7,
                                "a.B",
                                Set.of(1),
                                List.of(new MethodCoverage(1, "run", "()V", null))
                        ))
                )),
                List.of(new CodeEndpointCoverage("a.B", "run", "()V", List.of("GET /a")))
        );

        CoverageReport parsed = CoverageReportJson.read(CoverageReportJson.write(report));

        assertEquals(report, parsed);
        assertTrue(CoverageReportJson.write(report).contains("\"lineNumber\": null"));
    }

    @Test
    void escapesCharactersThatWouldOtherwiseBreakTheDocument() {
        CoverageReport report = new CoverageReport(
                Instant.parse("2026-08-21T09:00:00Z"),
                1,
                List.of(new EndpointCoverage(
                        "GET /\"quoted\"\\path",
                        1,
                        List.of("req\t1"),
                        List.of("thread\nname"),
                        List.of()
                )),
                List.of()
        );

        CoverageReport parsed = CoverageReportJson.read(CoverageReportJson.write(report));

        assertEquals("GET /\"quoted\"\\path", parsed.endpoints().get(0).endpoint());
        assertEquals(List.of("req\t1"), parsed.endpoints().get(0).requestIds());
        assertEquals(List.of("thread\nname"), parsed.endpoints().get(0).threadNames());
    }

    @Test
    void writesTheSameBytesForTheSameReport() {
        CoverageReport report = ReportFixtures.twoEndpointReport();

        assertEquals(CoverageReportJson.write(report), CoverageReportJson.write(report));
    }

    @Test
    void declaresItsSchemaVersion() {
        String json = CoverageReportJson.write(ReportFixtures.twoEndpointReport());

        assertTrue(json.contains("\"schemaVersion\": " + CoverageReportJson.SCHEMA_VERSION), json);
    }

    @Test
    void rejectsTextThatIsNotJson() {
        assertThrows(IllegalArgumentException.class, () -> CoverageReportJson.read("not json"));
    }

    @Test
    void rejectsAReportWithoutTheFieldsItNeeds() {
        assertThrows(IllegalArgumentException.class, () -> CoverageReportJson.read("{}"));
    }

    @Test
    void rejectsATimestampThatIsNotAnInstant() {
        String json = """
                {"generatedAt": "yesterday", "completedRequestCount": 0,
                 "endpoints": [], "reverseIndex": []}
                """;

        assertThrows(IllegalArgumentException.class, () -> CoverageReportJson.read(json));
    }

    @Test
    void rejectsTrailingContentAfterTheDocument() {
        String json = CoverageReportJson.write(ReportFixtures.twoEndpointReport()) + "{}";

        assertThrows(IllegalArgumentException.class, () -> CoverageReportJson.read(json));
    }

    @Test
    void readsAReportThatOmitsOptionalArrays() {
        String json = """
                {"generatedAt": "2026-08-21T09:00:00Z", "completedRequestCount": 3}
                """;

        CoverageReport report = CoverageReportJson.read(json);

        assertEquals(3, report.completedRequestCount());
        assertEquals(List.of(), report.endpoints());
        assertEquals(List.of(), report.reverseIndex());
    }
}
