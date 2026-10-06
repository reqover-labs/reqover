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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** An individual recorded unit, resolved independently from the endpoint union. */
public record RequestObservation(
        String requestId,
        String unitType,
        String endpoint,
        Instant startedAt,
        Instant endedAt,
        int statusCode,
        List<String> threadNames,
        List<ClassCoverage> classes
) {
    public RequestObservation {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(unitType, "unitType");
        Objects.requireNonNull(endpoint, "endpoint");
        Objects.requireNonNull(startedAt, "startedAt");
        threadNames = List.copyOf(threadNames);
        classes = List.copyOf(classes);
    }

    public boolean isHttp() {
        return UnitInfo.TYPE_HTTP_REQUEST.equals(unitType);
    }

    /** Wall-clock adapter interval, not network latency or a monotonic timer. */
    public Double recordedDurationMillis() {
        if (endedAt == null || endedAt.isBefore(startedAt)) {
            return null;
        }
        Duration duration = Duration.between(startedAt, endedAt);
        return duration.getSeconds() * 1000.0 + duration.getNano() / 1_000_000.0;
    }

    public boolean hasFinalHttpStatus() {
        return isHttp() && endedAt != null && statusCode >= 200 && statusCode <= 599;
    }

    public boolean isHttpFailure() {
        return hasFinalHttpStatus() && statusCode >= 400;
    }
}
