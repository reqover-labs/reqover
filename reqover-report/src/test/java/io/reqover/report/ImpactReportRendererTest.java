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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImpactReportRendererTest {
    @Test
    void textNamesTheEndpointsAndTheCodeThatReachedThem() {
        String text = ImpactReportRenderer.text(impactOnOrderService());

        assertTrue(text.contains(ReportFixtures.ORDERS), text);
        assertTrue(text.contains("com.example.order.OrderService#find(long): OrderResponse"), text);
    }

    @Test
    void textSaysSoWhenNothingIsImpacted() {
        ImpactReport impact = ImpactAnalyzer.analyze(ReportFixtures.twoEndpointReport(), List.of("README.md"));

        assertTrue(ImpactReportRenderer.text(impact).contains("No observed endpoint executed"), "");
    }

    @Test
    void markdownRendersATableAndKeepsTheCaveat() {
        String markdown = ImpactReportRenderer.markdown(impactOnOrderService());

        assertTrue(markdown.startsWith("### Reqover — endpoints to retest"), markdown);
        assertTrue(markdown.contains("| Endpoint | Changed code it ran |"), markdown);
        assertTrue(markdown.contains("`GET /orders/{id}`"), markdown);
        assertTrue(markdown.contains("may simply not have been exercised"), markdown);
    }

    @Test
    void markdownFoldsUnmatchedPathsIntoADisclosure() {
        ImpactReport impact = ImpactAnalyzer.analyze(
                ReportFixtures.twoEndpointReport(),
                List.of("src/main/java/com/example/order/OrderService.java", "README.md")
        );

        String markdown = ImpactReportRenderer.markdown(impact);

        assertTrue(markdown.contains("<details><summary>1 changed path with no observed coverage"), markdown);
        assertTrue(markdown.contains("- `README.md`"), markdown);
    }

    @Test
    void jsonIsItselfReadableJson() {
        String json = ImpactReportRenderer.json(impactOnOrderService());

        Object parsed = Json.parse(json);
        assertTrue(parsed instanceof java.util.Map, json);
        assertTrue(json.contains("\"hasImpact\": true"), json);
        assertTrue(json.contains("\"endpoint\": \"GET /orders/{id}\""), json);
    }

    @Test
    void jsonStaysValidWithNoImpact() {
        ImpactReport impact = ImpactAnalyzer.analyze(ReportFixtures.twoEndpointReport(), List.of("README.md"));

        String json = ImpactReportRenderer.json(impact);

        Json.parse(json);
        assertTrue(json.contains("\"hasImpact\": false"), json);
    }

    private static ImpactReport impactOnOrderService() {
        return ImpactAnalyzer.analyze(
                ReportFixtures.twoEndpointReport(),
                List.of("src/main/java/com/example/order/OrderService.java")
        );
    }
}
