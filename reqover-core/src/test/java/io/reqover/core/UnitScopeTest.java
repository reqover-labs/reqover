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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitScopeTest {
    private final InMemoryCoverageStore store = new InMemoryCoverageStore();

    @AfterEach
    void clearContext() {
        CoverageContext.clear();
    }

    @Test
    void attributesHitsInsideTheBlockToTheUnit() {
        try (UnitScope scope = UnitScope.open(store, UnitInfo.scheduledJob("run-1", "nightly-settlement"))) {
            CoverageContext.current().record(7, 1);
            assertEquals(scope.bucket(), CoverageContext.current());
        }

        assertEquals(1, store.snapshots().size());
        CoverageBucketSnapshot snapshot = store.snapshots().get(0);
        assertEquals("nightly-settlement", snapshot.unitInfo().name());
        assertEquals(UnitInfo.TYPE_SCHEDULED_JOB, snapshot.unitInfo().unitType());
        assertTrue(snapshot.hasHit(7, 1));
    }

    @Test
    void restoresThePreviousContextOnClose() {
        assertNull(CoverageContext.current());

        try (UnitScope ignored = UnitScope.open(store, UnitInfo.test("run-1", "OrderServiceTest.find"))) {
            assertTrue(CoverageContext.current() != null);
        }

        assertNull(CoverageContext.current());
    }

    @Test
    void marksTheUnitFinishedWithNoHttpStatus() {
        try (UnitScope ignored = UnitScope.open(store, UnitInfo.message("msg-1", "orders.created"))) {
            // nothing recorded
        }

        CoverageBucketSnapshot snapshot = store.snapshots().get(0);
        assertTrue(snapshot.finished());
        assertEquals(CoverageBucket.NO_STATUS, snapshot.statusCode());
    }

    @Test
    void keepsAStatusTheCallerSetItself() {
        try (UnitScope scope = UnitScope.open(store, UnitInfo.scheduledJob("run-1", "job"))) {
            scope.bucket().finish(503);
        }

        assertEquals(1, store.snapshots().size(), "the scope still flushes a bucket the caller finished");
        assertEquals(503, store.snapshots().get(0).statusCode());
    }

    @Test
    void flushesOnceWhenClosedTwice() {
        UnitScope scope = UnitScope.open(store, UnitInfo.scheduledJob("run-1", "job"));
        scope.close();
        scope.close();

        assertEquals(1, store.snapshots().size());
    }

    @Test
    void flushesWhenTheBlockThrows() {
        assertThrows(IllegalStateException.class, () -> {
            try (UnitScope ignored = UnitScope.open(store, UnitInfo.scheduledJob("run-1", "job"))) {
                throw new IllegalStateException("boom");
            }
        });

        assertEquals(1, store.snapshots().size());
    }

    @Test
    void letsASecondThreadRecordIntoTheSameUnitWithoutFlushingTwice() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try (UnitScope scope = UnitScope.open(store, UnitInfo.scheduledJob("run-1", "fan-out"))) {
            CoverageBucket bucket = scope.bucket();
            worker.submit(() -> {
                try (UnitScope ignored = UnitScope.join(bucket)) {
                    CoverageContext.current().record(9, 2);
                }
            }).get(5, TimeUnit.SECONDS);
        } finally {
            worker.shutdownNow();
        }

        assertEquals(1, store.snapshots().size());
        assertTrue(store.snapshots().get(0).hasHit(9, 2));
    }
}
