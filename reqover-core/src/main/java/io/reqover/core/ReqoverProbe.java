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

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Static entry point inserted into instrumented application bytecode.
 */
public final class ReqoverProbe {
    private static final AtomicReference<CoverageBucket> GLOBAL_BUCKET =
            new AtomicReference<>(CoverageBucket.global());
    private static final AtomicLong droppedHits = new AtomicLong();

    private ReqoverProbe() {
    }

    public static void hit(int classId, int probeId) {
        try {
            CoverageBucket bucket = CoverageContext.current();
            if (bucket == null) {
                bucket = GLOBAL_BUCKET.get();
            }
            bucket.record(classId, probeId);
        } catch (Throwable ignored) {
            droppedHits.incrementAndGet();
        }
    }

    public static CoverageBucketSnapshot globalSnapshot() {
        return GLOBAL_BUCKET.get().snapshot();
    }

    public static long droppedHitCount() {
        return droppedHits.get();
    }

    /**
     * Wipes the global bucket, the current thread's context, and all
     * registered probe metadata. Intended for test isolation only; calling
     * this in a running application permanently discards agent-registered
     * metadata.
     */
    public static void resetGlobalStateForTests() {
        GLOBAL_BUCKET.set(CoverageBucket.global());
        droppedHits.set(0);
        CoverageContext.clear();
        ProbeRegistry.clear();
    }
}
