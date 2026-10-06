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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverProbeTest {
    @BeforeEach
    void setUp() {
        ReqoverProbe.resetGlobalStateForTests();
    }

    @AfterEach
    void tearDown() {
        ReqoverProbe.resetGlobalStateForTests();
    }

    @Test
    void recordsHitIntoCurrentBucket() {
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));

        try (CoverageContext.Scope ignored = CoverageContext.open(bucket)) {
            ReqoverProbe.hit(10, 3);
        }

        assertTrue(bucket.hasHit(10, 3));
        assertFalse(ReqoverProbe.globalSnapshot().hasHit(10, 3));
    }

    @Test
    void recordsHitIntoGlobalBucketWhenNoContextExists() {
        ReqoverProbe.hit(10, 3);

        assertTrue(ReqoverProbe.globalSnapshot().hasHit(10, 3));
    }

    @Test
    void invalidProbeIdsAreIgnoredWithoutDroppedHit() {
        ReqoverProbe.hit(-1, 3);
        ReqoverProbe.hit(10, -3);

        assertFalse(ReqoverProbe.globalSnapshot().hasHit(-1, 3));
        assertEquals(0, ReqoverProbe.droppedHitCount());
    }
}

