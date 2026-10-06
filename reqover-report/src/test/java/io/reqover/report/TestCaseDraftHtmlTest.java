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

import static org.junit.jupiter.api.Assertions.*;

class TestCaseDraftHtmlTest {
    @Test
    void embedsAnOfflineReviewEditorAndGeneratorBeforeDashboardInitialization() {
        CoverageReport report = new CoverageReport(Instant.EPOCH, 1, List.of(), List.of(), List.of(
                new RequestObservation("r1", UnitInfo.TYPE_HTTP_REQUEST, "GET /orders/{id}",
                        Instant.EPOCH, Instant.EPOCH.plusMillis(1000), 503, List.of(), List.of())));
        String html = new HtmlCoverageReportRenderer().render(report);
        assertTrue(html.contains("<a class=\"dashboard-link\" href=\"#test-case-drafts\""));
        assertTrue(html.contains("id=\"reqover-case-path\""));
        assertTrue(html.contains("id=\"reqover-case-expected-status\""));
        assertTrue(html.contains("id=\"reqover-case-reviewed\""));
        assertTrue(html.contains("id=\"reqover-case-junit\""));
        assertTrue(html.contains("reqover.test.baseUrl"));
        assertTrue(html.indexOf("root.ReqoverTestDrafts") < html.indexOf("var dataElement ="));
        assertFalse(html.contains("localStorage"));
        assertFalse(html.contains("src=\"http"));
    }

    @Test
    void leavesLegacyReportsWithoutADraftNavigationLink() {
        String html = new HtmlCoverageReportRenderer().render(ReportFixtures.twoEndpointReport());
        assertFalse(html.contains("<a class=\"dashboard-link\" href=\"#test-case-drafts\""));
        assertTrue(html.contains("No per-request diagnostics"));
    }
}
