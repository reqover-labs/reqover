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

package io.reqover.example.webflux;

import io.reqover.core.CoverageStore;
import io.reqover.report.CoverageReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebFluxDiagnosticsIntegrationTest {
    @Autowired
    WebTestClient client;

    @Autowired
    CoverageStore coverageStore;

    @BeforeEach
    void clearCoverage() {
        coverageStore.clear();
    }

    @Test
    void recordsReactiveDelayAndExplicitHttpFailure() {
        client.get().uri("/auto/reactive/diagnostics/delay/20").exchange().expectStatus().isOk();
        client.get().uri("/auto/reactive/diagnostics/failure").exchange().expectStatus().isEqualTo(503);
        CoverageReport report = client.get().uri("/reqover/report").exchange().expectStatus().isOk()
                .expectBody(CoverageReport.class).returnResult().getResponseBody();
        assertNotNull(report);
        var delay = report.requests().stream()
                .filter(request -> request.endpoint().equals("GET /auto/reactive/diagnostics/delay/{millis}"))
                .findFirst().orElseThrow();
        assertEquals(200, delay.statusCode());
        assertTrue(delay.recordedDurationMillis() >= 10);
        assertTrue(report.requests().stream().anyMatch(request ->
                request.endpoint().equals("GET /auto/reactive/diagnostics/failure") && request.statusCode() == 503));
    }

    @Test
    void rejectsDelaysOutsideTheBoundedDemoRange() {
        client.get().uri("/auto/reactive/diagnostics/delay/2001").exchange().expectStatus().isBadRequest();
        client.get().uri("/auto/reactive/diagnostics/delay/-1").exchange().expectStatus().isBadRequest();
    }
}
