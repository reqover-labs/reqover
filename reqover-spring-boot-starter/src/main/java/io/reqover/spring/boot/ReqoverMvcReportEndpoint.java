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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Serves the report over HTTP in a servlet application.
 *
 * <p>Registered only when {@code reqover.report.endpoint.enabled=true}. The
 * path comes from {@code reqover.report.endpoint.path}; the HTML report is at
 * the same path with {@code .html} appended.
 *
 * <p>There is no authentication here on purpose — see
 * {@link ReqoverReportProperties.Endpoint}.
 */
@RestController
public class ReqoverMvcReportEndpoint {
    private final ReqoverReportService reportService;

    public ReqoverMvcReportEndpoint(ReqoverReportService reportService) {
        this.reportService = Objects.requireNonNull(reportService, "reportService");
    }

    @GetMapping(
            path = "${reqover.report.endpoint.path:/reqover/report}",
            produces = "application/json;charset=UTF-8"
    )
    public String json() {
        return reportService.json();
    }

    @GetMapping(
            path = "${reqover.report.endpoint.path:/reqover/report}.html",
            produces = "text/html;charset=UTF-8"
    )
    public String html() {
        return reportService.html();
    }
}
