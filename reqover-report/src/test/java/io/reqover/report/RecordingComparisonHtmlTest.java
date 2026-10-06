/*
 * Copyright 2026 Reqover contributors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.reqover.report;

import io.reqover.core.UnitInfo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class RecordingComparisonHtmlTest {
    @Test
    void embedsAllRetainedHttpSummariesInsteadOfTheGraphsLatestHundred() {
        List<RequestObservation> requests = IntStream.range(0, 150).mapToObj(i -> new RequestObservation(
                "r" + i, UnitInfo.TYPE_HTTP_REQUEST, "GET /many", Instant.EPOCH.plusSeconds(i),
                Instant.EPOCH.plusSeconds(i).plusMillis(i), i == 0 ? 503 : 200, List.of(), List.of())).toList();
        String html = new HtmlCoverageReportRenderer().render(new CoverageReport(Instant.EPOCH, 150, List.of(), List.of(), requests));
        Map<String, Object> summary = data(html);
        assertEquals(150, Json.integer(summary, "httpRequestCount"));
        Map<String, Object> row = Json.object(Json.optionalArray(summary, "endpoints").get(0), "endpoint");
        assertEquals(150, Json.integer(row, "requestCount"));
        assertEquals(1, Json.integer(row, "serverErrorCount"));
        assertEquals(74.5, ((Number) row.get("averageMillis")).doubleValue());
        assertEquals(142.0, ((Number) row.get("p95Millis")).doubleValue());
        assertTrue(html.contains("id=\"reqover-baseline-file\""));
    }

    @Test
    void escapesSummaryScriptDataAndKeepsLegacyMeasurementsUnavailable() {
        var request = new RequestObservation("r", UnitInfo.TYPE_HTTP_REQUEST, "GET /</script><img>",
                Instant.EPOCH, Instant.EPOCH.plusNanos(100008), 200, List.of(), List.of());
        String html = new HtmlCoverageReportRenderer().render(new CoverageReport(Instant.EPOCH, 1, List.of(), List.of(), List.of(request)));
        Map<String, Object> row = Json.object(Json.optionalArray(data(html), "endpoints").get(0), "endpoint");
        assertEquals("GET /</script><img>", Json.string(row, "endpoint"));
        assertEquals(0.100008, ((Number) row.get("averageMillis")).doubleValue());
        assertFalse(html.contains("GET /</script><img>"));
        assertEquals(false, data(new HtmlCoverageReportRenderer().render(ReportFixtures.twoEndpointReport())).get("available"));
    }

    private static Map<String, Object> data(String html) {
        var match = Pattern.compile("<script type=\"application/json\" id=\"reqover-comparison-data\">(.*?)</script>", Pattern.DOTALL).matcher(html);
        assertTrue(match.find(), "current summaries must be precomputed over the complete retained window");
        return Json.object(Json.parse(match.group(1)), "summary");
    }
}
