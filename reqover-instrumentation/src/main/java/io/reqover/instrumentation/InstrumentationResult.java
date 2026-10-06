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

import io.reqover.core.ProbeMetadata;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of instrumenting one class: the (possibly rewritten) bytecode, the
 * probe metadata generated for it, and whether any probe was inserted.
 */
public record InstrumentationResult(
        byte[] bytecode,
        List<ProbeMetadata> metadata,
        boolean instrumented
) {
    public InstrumentationResult {
        bytecode = Objects.requireNonNull(bytecode, "bytecode").clone();
        metadata = List.copyOf(metadata);
    }

    /** Returns a defensive copy; the stored bytecode is never exposed directly. */
    @Override
    public byte[] bytecode() {
        return bytecode.clone();
    }
}
