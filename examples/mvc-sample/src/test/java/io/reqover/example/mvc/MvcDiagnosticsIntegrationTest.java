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

package io.reqover.example.mvc;

import io.reqover.core.CoverageStore;
import io.reqover.report.CoverageReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MvcDiagnosticsIntegrationTest {
    @Autowired
    TestRestTemplate rest;

    @Autowired
    CoverageStore coverageStore;

    @BeforeEach
    void clearCoverage() {
        coverageStore.clear();
    }

    @Test
    void slowAndFailureDemoEndpointsProduceRealDiagnostics() {
        assertEquals(200, rest.getForEntity("/auto/diagnostics/delay/20", String.class).getStatusCode().value());
        assertEquals(503, rest.getForEntity("/auto/diagnostics/failure", String.class).getStatusCode().value());
        CoverageReport report = rest.getForObject("/reqover/report", CoverageReport.class);
        assertNotNull(report);
        var delay = report.requests().stream()
                .filter(request -> request.endpoint().equals("GET /auto/diagnostics/delay/{millis}"))
                .findFirst().orElseThrow();
        assertEquals(200, delay.statusCode());
        assertTrue(delay.recordedDurationMillis() >= 10);
        assertTrue(report.requests().stream().anyMatch(request ->
                request.endpoint().equals("GET /auto/diagnostics/failure") && request.statusCode() == 503));
    }

    @Test
    void rejectsDelaysOutsideTheBoundedDemoRange() {
        assertEquals(400, rest.getForEntity("/auto/diagnostics/delay/2001", String.class).getStatusCode().value());
        assertEquals(400, rest.getForEntity("/auto/diagnostics/delay/-1", String.class).getStatusCode().value());
    }
}
