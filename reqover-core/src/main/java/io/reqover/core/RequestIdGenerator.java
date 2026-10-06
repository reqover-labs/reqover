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

/**
 * Generates monotonically increasing, human-readable request ids such as
 * {@code req-1}. Thread-safe.
 *
 * <p>The sequence is shared by every generator in the JVM, so ids stay unique
 * when a test run holds several application contexts whose reports are
 * exported to one file.
 */
public final class RequestIdGenerator {
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private final String prefix;

    public RequestIdGenerator() {
        this("req");
    }

    public RequestIdGenerator(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        this.prefix = prefix;
    }

    public String nextId() {
        return prefix + "-" + SEQUENCE.incrementAndGet();
    }
}

