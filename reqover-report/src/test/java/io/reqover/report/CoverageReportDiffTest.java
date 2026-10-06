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

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageReportDiffTest {
    @Test
    void reportsNoChangeForTheSameReport() {
        CoverageReport report = ReportFixtures.twoEndpointReport();

        assertTrue(CoverageReportDiff.between(report, report).isEmpty());
    }

    @Test
    void namesAnEndpointObservedOnlyInTheCurrentReport() {
        ReportDiff diff = CoverageReportDiff.between(onlyOrders(), ReportFixtures.twoEndpointReport());

        assertEquals(List.of(ReportFixtures.PAYMENTS), diff.addedEndpoints());
        assertEquals(List.of(), diff.removedEndpoints());
    }

    @Test
    void namesAnEndpointObservedOnlyInTheBaseline() {
        ReportDiff diff = CoverageReportDiff.between(ReportFixtures.twoEndpointReport(), onlyOrders());

        assertEquals(List.of(ReportFixtures.PAYMENTS), diff.removedEndpoints());
        assertEquals(List.of(), diff.addedEndpoints());
    }

    @Test
    void reportsCodeAnEndpointStartedRunning() {
        CoverageReport baseline = new CoverageReport(
                Instant.parse("2026-08-20T09:00:00Z"),
                1,
                List.of(ReportFixtures.endpoint(ReportFixtures.ORDERS, "req-1", "main",
                        ReportFixtures.classCoverage(11, "com.example.order.OrderService",
                                "find", "(J)Lcom/example/OrderResponse;", 24))),
                List.of()
        );

        ReportDiff diff = CoverageReportDiff.between(baseline, ReportFixtures.twoEndpointReport());

        EndpointCodeDiff orders = diff.changedEndpoints().stream()
                .filter(entry -> entry.endpoint().equals(ReportFixtures.ORDERS))
                .findFirst()
                .orElseThrow();
        assertEquals(1, orders.addedCode().size());
        assertEquals("com.example.SharedValidator", orders.addedCode().get(0).className());
        assertEquals(List.of(), orders.removedCode());
    }

    @Test
    void reportsCodeAnEndpointStoppedRunning() {
        CoverageReport current = new CoverageReport(
                Instant.parse("2026-08-21T09:00:00Z"),
                1,
                List.of(ReportFixtures.endpoint(ReportFixtures.ORDERS, "req-1", "main",
                        ReportFixtures.classCoverage(11, "com.example.order.OrderService",
                                "find", "(J)Lcom/example/OrderResponse;", 24))),
                List.of()
        );

        ReportDiff diff = CoverageReportDiff.between(onlyOrders(), current);

        EndpointCodeDiff orders = diff.changedEndpoints().get(0);
        assertEquals(1, orders.removedCode().size());
        assertEquals("com.example.SharedValidator", orders.removedCode().get(0).className());
    }

    private static CoverageReport onlyOrders() {
        CoverageReport full = ReportFixtures.twoEndpointReport();
        return new CoverageReport(
                full.generatedAt(),
                1,
                full.endpoints().stream()
                        .filter(endpoint -> endpoint.endpoint().equals(ReportFixtures.ORDERS))
                        .toList(),
                full.reverseIndex().stream()
                        .filter(entry -> entry.endpoints().contains(ReportFixtures.ORDERS))
                        .toList()
        );
    }
}
