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

package io.reqover.spring.boot;

import io.reqover.core.CoverageStore;
import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportGenerator;
import io.reqover.report.CoverageReportJson;
import io.reqover.report.HtmlCoverageReportRenderer;

import java.util.Objects;

/**
 * Builds the report on demand from whatever the store currently holds.
 *
 * <p>Both the HTTP endpoint and the shutdown export go through this, so the
 * file a CI job reads is byte-for-byte what the endpoint would have served.
 */
public class ReqoverReportService {
    private final CoverageStore coverageStore;
    private final CoverageReportGenerator generator = new CoverageReportGenerator();
    private final HtmlCoverageReportRenderer htmlRenderer = new HtmlCoverageReportRenderer();

    public ReqoverReportService(CoverageStore coverageStore) {
        this.coverageStore = Objects.requireNonNull(coverageStore, "coverageStore");
    }

    public CoverageReport report() {
        return report(recording());
    }

    public String json() {
        return json(recording());
    }

    public String html() {
        return html(recording());
    }

    Recording recording() {
        return new Recording(coverageStore.snapshots(), coverageStore.aggregates());
    }

    CoverageReport report(Recording recording) {
        return generator.generate(recording.snapshots(), recording.aggregates());
    }

    String json(Recording recording) {
        return CoverageReportJson.write(report(recording));
    }

    String html(Recording recording) {
        return htmlRenderer.render(report(recording));
    }
}
