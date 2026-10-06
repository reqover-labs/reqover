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
import io.reqover.report.EndpointCoverage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WebFluxSampleIntegrationTest {
    @Autowired
    WebTestClient webTestClient;

    @Autowired
    CoverageStore coverageStore;

    @Test
    void doesNotRecordRequestsThatMatchNoController() {
        coverageStore.clear();

        webTestClient.get()
                .uri("/no-such-endpoint")
                .exchange()
                .expectStatus().isNotFound();

        assertTrue(coverageStore.snapshots().isEmpty(), coverageStore.snapshots().toString());
    }

    @Test
    void keepsCoverageBucketAcrossThreadHop() {
        webTestClient.get()
                .uri("/reactive/orders/1")
                .exchange()
                .expectStatus().isOk();

        CoverageReport report = webTestClient.get()
                .uri("/reqover/report")
                .exchange()
                .expectStatus().isOk()
                .expectBody(CoverageReport.class)
                .returnResult()
                .getResponseBody();

        EndpointCoverage endpoint = report.endpoints().stream()
                .filter(value -> value.endpoint().equals("GET /reactive/orders/{id}"))
                .findFirst()
                .orElseThrow();

        assertTrue(endpoint.threadNames().size() >= 2, "expected hits from multiple Reactor threads");
        assertTrue(endpoint.classes().stream().anyMatch(value -> value.className().equals("ReactiveOrderService")));
        assertTrue(endpoint.classes().stream().anyMatch(value -> value.className().equals("ReactiveOrderMapper")));
        assertTrue(endpoint.classes().stream().anyMatch(value -> value.className().equals("ReactiveValidator")));
        assertTrue(report.reverseIndex().stream()
                .anyMatch(item -> item.className().equals("ReactiveOrderService")
                        && item.endpoints().contains("GET /reactive/orders/{id}")));

        webTestClient.get()
                .uri("/reqover/report.html")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(html -> assertTrue(html.contains("Reqover Coverage Report")));
    }
}
