/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.report;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimeDistributionTest {
    private static RequestObservation request(Instant start, Long millis, int status) {
        return new RequestObservation("r", "http-request", "GET /orders", start,
                millis == null ? null : start.plusMillis(millis), status, List.of(), List.of());
    }
    @Test void usesAllRetainedRecordsNotTheMapWindow() {
        var requests = IntStream.range(0, 150).mapToObj(i -> request(Instant.EPOCH.plusSeconds(i), 100L, i == 0 ? 503 : 200)).toList();
        var data = Json.object(Json.parse(TimeDistribution.json(requests)), "distribution");
        assertEquals(150, Json.integer(data, "displayedCount"));
        var window = Json.object(Json.optionalArray(data, "windows").get(0), "window");
        var summary = Json.object(Json.optionalArray(window, "endpoints").get(0), "endpoint");
        assertEquals(150, Json.integer(summary, "requestCount"));
        assertEquals(1, Json.integer(summary, "serverErrorCount"));
    }
    @Test void separatesBoundariesAndUnavailableMeasurements() {
        assertEquals(0, TimeDistribution.band(request(Instant.EPOCH, 99L, 200)));
        assertEquals(1, TimeDistribution.band(request(Instant.EPOCH, 100L, 200)));
        assertEquals(2, TimeDistribution.band(request(Instant.EPOCH, 500L, 200)));
        assertEquals(3, TimeDistribution.band(request(Instant.EPOCH, 1000L, 200)));
        assertEquals(4, TimeDistribution.band(request(Instant.EPOCH, 2000L, 200)));
        assertEquals(5, TimeDistribution.band(request(Instant.EPOCH, null, 200)));
        assertEquals(5, TimeDistribution.band(request(Instant.EPOCH, -1L, 503)));
    }
    @Test void nonHttpUnitsAreExcludedAndUtcWindowsAreAligned() {
        var http = request(Instant.EPOCH.plusSeconds(300), 0L, 200);
        var job = new RequestObservation("job", "scheduled-job", "nightly", Instant.EPOCH, null, -1, List.of(), List.of());
        var data = Json.object(Json.parse(TimeDistribution.json(List.of(http, job))), "distribution");
        assertEquals(1, Json.integer(data, "totalRetainedCount"));
        var window = Json.object(Json.optionalArray(data, "windows").get(0), "window");
        assertEquals("1970-01-01T00:05:00Z", Json.string(window, "startedAt"));
    }
    @Test void boundsWindowsAndCountsUnsupportedTimestampsWithoutCrashing() {
        var requests = new java.util.ArrayList<>(IntStream.range(0, 257)
                .mapToObj(i -> request(Instant.EPOCH.plusSeconds(i * 300L), 1L, 200)).toList());
        requests.add(request(Instant.MAX, null, -1));
        var data = Json.object(Json.parse(TimeDistribution.json(requests)), "distribution");
        assertEquals(256, Json.optionalArray(data, "windows").size());
        assertEquals(1, Json.integer(data, "omittedWindowRequestCount"));
        assertEquals(1, Json.integer(data, "unsupportedStartCount"));
        assertEquals(258, Json.integer(data, "totalRetainedCount"));
    }
}
