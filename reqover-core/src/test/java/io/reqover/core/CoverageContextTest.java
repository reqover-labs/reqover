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

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CoverageContextTest {
    @AfterEach
    void tearDown() {
        CoverageContext.clear();
    }

    @Test
    void setsAndClearsCurrentBucket() {
        CoverageBucket bucket = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));

        CoverageContext.set(bucket);
        assertSame(bucket, CoverageContext.current());

        CoverageContext.clear();
        assertNull(CoverageContext.current());
    }

    @Test
    void scopeRestoresPreviousBucket() {
        CoverageBucket outer = new CoverageBucket(UnitInfo.httpRequest("req-1", "GET", "/orders/{id}"));
        CoverageBucket inner = new CoverageBucket(UnitInfo.httpRequest("req-2", "POST", "/payments"));

        CoverageContext.set(outer);
        try (CoverageContext.Scope ignored = CoverageContext.open(inner)) {
            assertSame(inner, CoverageContext.current());
        }

        assertSame(outer, CoverageContext.current());
    }
}

