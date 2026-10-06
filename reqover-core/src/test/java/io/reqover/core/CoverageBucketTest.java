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

package io.reqover.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverageBucketTest {
    @Test
    void recordsProbeHits() {
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));

        bucket.record(10, 3);

        assertTrue(bucket.hasHit(10, 3));
        assertFalse(bucket.hasHit(10, 4));
    }

    @Test
    void ignoresNegativeProbeIds() {
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));

        bucket.record(-1, 3);
        bucket.record(10, -3);

        assertTrue(bucket.isEmpty());
    }

    @Test
    void createsImmutableSnapshot() {
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));
        bucket.record(10, 3);
        bucket.finish(200);

        CoverageBucketSnapshot snapshot = bucket.snapshot();
        bucket.record(10, 4);

        assertTrue(snapshot.hasHit(10, 3));
        assertFalse(snapshot.hasHit(10, 4));
        assertNotNull(snapshot.endedAt());
        assertTrue(snapshot.threadNames().contains(Thread.currentThread().getName()));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.hitsByClass().put(99, java.util.Set.of(1)));
    }
}

