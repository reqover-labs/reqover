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

import java.time.Instant;
import java.util.List;

public record CoverageReport(
        Instant generatedAt,
        int completedRequestCount,
        List<EndpointCoverage> endpoints,
        List<CodeEndpointCoverage> reverseIndex,
        List<RequestObservation> requests,
        int omittedRequestDetails
) {
    /**
     * @param requests              retained request details, possibly truncated
     * @param omittedRequestDetails details left out of {@code requests} by an
     *                              earlier export, so a report read back from
     *                              JSON still knows how many it does not show
     */
    public CoverageReport {
        requests = requests == null ? List.of() : List.copyOf(requests);
        if (omittedRequestDetails < 0) {
            throw new IllegalArgumentException("omittedRequestDetails must not be negative");
        }
    }

    /** Preserves the existing four-argument constructor for compiled callers and fixtures. */
    public CoverageReport(Instant generatedAt, int completedRequestCount,
                          List<EndpointCoverage> endpoints, List<CodeEndpointCoverage> reverseIndex) {
        this(generatedAt, completedRequestCount, endpoints, reverseIndex, List.of(), 0);
    }

    public CoverageReport(Instant generatedAt, int completedRequestCount, List<EndpointCoverage> endpoints,
                          List<CodeEndpointCoverage> reverseIndex, List<RequestObservation> requests) {
        this(generatedAt, completedRequestCount, endpoints, reverseIndex, requests, 0);
    }
}
