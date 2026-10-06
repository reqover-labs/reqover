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

import java.util.ArrayList;
import java.util.List;

/** HTTP statistics over the retained observations, not total traffic. */
public record RequestSummary(
        int requestCount,
        int knownStatusCount,
        int clientErrorCount,
        int serverErrorCount,
        int timedRequestCount,
        Double averageMillis,
        Double p95Millis,
        Double maximumMillis,
        double cumulativeMillis
) {
    public static RequestSummary from(List<RequestObservation> requests) {
        int count = 0;
        int known = 0;
        int clientErrors = 0;
        int serverErrors = 0;
        List<Double> times = new ArrayList<>();
        for (RequestObservation request : requests) {
            if (!request.isHttp()) {
                continue;
            }
            count++;
            if (request.hasFinalHttpStatus()) {
                known++;
                if (request.statusCode() >= 500) {
                    serverErrors++;
                } else if (request.statusCode() >= 400) {
                    clientErrors++;
                }
            }
            Double elapsed = request.recordedDurationMillis();
            if (elapsed != null) {
                times.add(elapsed);
            }
        }
        times.sort(Double::compare);
        double total = times.stream().mapToDouble(Double::doubleValue).sum();
        return new RequestSummary(count, known, clientErrors, serverErrors, times.size(),
                times.isEmpty() ? null : total / times.size(),
                times.isEmpty() ? null : times.get((int) Math.ceil(times.size() * 0.95) - 1),
                times.isEmpty() ? null : times.get(times.size() - 1), total);
    }

    public int unknownStatusCount() {
        return requestCount - knownStatusCount;
    }

    public Double httpFailurePercent() {
        return knownStatusCount == 0 ? null : (clientErrorCount + serverErrorCount) * 100.0 / knownStatusCount;
    }
}
