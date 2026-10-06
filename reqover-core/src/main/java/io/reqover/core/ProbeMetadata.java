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

public record ProbeMetadata(
        int classId,
        int probeId,
        String className,
        String methodName,
        String descriptor,
        Integer lineNumber
) {
    public ProbeMetadata {
        if (classId < 0) {
            throw new IllegalArgumentException("classId must be non-negative");
        }
        if (probeId < 0) {
            throw new IllegalArgumentException("probeId must be non-negative");
        }
        className = requireText(className, "className");
        methodName = requireText(methodName, "methodName");
        descriptor = Objects.requireNonNullElse(descriptor, "");
    }

    public String codeLocationKey() {
        return className + "#" + methodName + descriptor;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

