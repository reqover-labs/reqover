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

package io.reqover.instrumentation;

/**
 * Derives a stable, non-negative class id from a class name using a 31-bit
 * FNV-1a hash.
 *
 * <p>The id is deterministic across JVM runs, but it is a hash: two distinct
 * class names can collide, in which case their probes share an id space and
 * {@code ProbeRegistry} reports the collision at registration time.
 */
public final class StableClassId {
    private StableClassId() {
    }

    public static int of(String className) {
        int hash = 0x811c9dc5;
        for (int i = 0; i < className.length(); i++) {
            hash ^= className.charAt(i);
            hash *= 0x01000193;
        }
        return hash & 0x7fffffff;
    }
}
