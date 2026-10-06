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

import io.reqover.core.CoverageBucketSnapshot;
import io.reqover.core.UnitInfo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class RequestSummaryTest {
    private final Instant start = Instant.parse("2026-10-03T00:00:00Z");

    @Test
    void computesNearestRankP95OverValidSamplesAndKeepsZeroDurations() {
        List<RequestObservation> requests = IntStream.range(0, 20)
                .mapToObj(i -> request("req-" + i, "http-request", start.plusMillis(i * 10), 200)).toList();
        RequestSummary summary = RequestSummary.from(requests);
        assertEquals(20, summary.timedRequestCount());
        assertEquals(95.0, summary.averageMillis());
        assertEquals(180.0, summary.p95Millis());
        assertEquals(190.0, summary.maximumMillis());
        assertEquals(1900.0, summary.cumulativeMillis());
    }

    @Test
    void excludesNonHttpAndUnknownStatusFromTheFailureRateDenominator() {
        RequestSummary summary = RequestSummary.from(List.of(
                request("ok", "http-request", start.plusMillis(1), 200),
                request("client", "http-request", start.plusMillis(1), 404),
                request("server", "http-request", start.plusMillis(1), 503),
                request("unknown", "http-request", start.plusMillis(1), -1),
                request("job", "scheduled-job", start.plusMillis(1), 500)));
        assertEquals(4, summary.requestCount());
        assertEquals(3, summary.knownStatusCount());
        assertEquals(1, summary.clientErrorCount());
        assertEquals(1, summary.serverErrorCount());
        assertEquals(1, summary.unknownStatusCount());
        assertEquals(200.0 / 3, summary.httpFailurePercent(), 0.001);
    }

    @Test
    void unfinishedAndReversedIntervalsAreNotFabricatedAsZeroMilliseconds() {
        RequestObservation unfinished = request("unfinished", "http-request", null, 500);
        RequestObservation reversed = request("reversed", "http-request", start.minusMillis(1), 200);
        RequestSummary summary = RequestSummary.from(List.of(unfinished, reversed));
        assertNull(unfinished.recordedDurationMillis());
        assertNull(reversed.recordedDurationMillis());
        assertEquals(0, summary.timedRequestCount());
        assertNull(summary.averageMillis());
        assertNull(summary.p95Millis());
        assertEquals(1, summary.unknownStatusCount());
        assertEquals(0.0, summary.httpFailurePercent());
    }

    @Test
    void preservesSubMillisecondPrecisionAndDoesNotOverflowLongIntervals() {
        assertEquals(0.125, request("short", "http-request", start.plusNanos(125_000), 200).recordedDurationMillis());
        assertTrue(Double.isFinite(request("long", "http-request", Instant.MAX, 200).recordedDurationMillis()));
    }

    @Test
    void emptySummaryHasUnknownDurationsAndFailureRate() {
        RequestSummary summary = RequestSummary.from(List.of());
        assertEquals(0, summary.requestCount());
        assertNull(summary.p95Millis());
        assertNull(summary.httpFailurePercent());
    }

    @Test
    void readsOldFilesWithoutManufacturingRequestObservations() {
        String old = """
                {"schemaVersion": 1, "generatedAt": "2026-10-03T00:00:00Z", "completedRequestCount": 10,
                 "endpoints": [], "reverseIndex": []}
                """;
        assertTrue(CoverageReportJson.read(old).requests().isEmpty());
    }

    @Test
    void roundTripsUnfinishedUnitsAndRejectsMalformedNewTimestamps() {
        CoverageBucketSnapshot snapshot = new CoverageBucketSnapshot(UnitInfo.scheduledJob("job", "batch"),
                start, null, -1, Map.of(), Set.of());
        CoverageReport report = new CoverageReportGenerator().generate(List.of(snapshot));
        String json = CoverageReportJson.write(report);
        assertEquals(report, CoverageReportJson.read(json));
        assertThrows(IllegalArgumentException.class,
                () -> CoverageReportJson.read(json.replace(start.toString(), "not-an-instant")));
    }

    private RequestObservation request(String id, String type, Instant end, int status) {
        return new RequestObservation(id, type, "GET /test", start, end, status, List.of("main"), List.of());
    }
}
