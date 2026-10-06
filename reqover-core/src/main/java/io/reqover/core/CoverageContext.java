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

import java.util.Objects;

/**
 * Thread-bound coverage context used by probes on the hot path.
 */
public final class CoverageContext {
    private static final ThreadLocal<CoverageBucket> CURRENT = new ThreadLocal<>();

    private CoverageContext() {
    }

    public static CoverageBucket current() {
        return CURRENT.get();
    }

    public static void set(CoverageBucket bucket) {
        CURRENT.set(Objects.requireNonNull(bucket, "bucket"));
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Scope open(CoverageBucket bucket) {
        CoverageBucket previous = CURRENT.get();
        set(bucket);
        return new Scope(previous);
    }

    public static final class Scope implements AutoCloseable {
        private final CoverageBucket previous;
        private boolean closed;

        private Scope(CoverageBucket previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}

