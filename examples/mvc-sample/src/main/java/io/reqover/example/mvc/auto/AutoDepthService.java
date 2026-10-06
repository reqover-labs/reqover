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

package io.reqover.example.mvc.auto;

import org.springframework.stereotype.Service;

/**
 * Executes a requested number of instrumented method entries.
 *
 * <p>Each recursion level is one probe hit on the same probe, which is the
 * common case in real code: a method is entered many times, and the bucket
 * records a set, so the repeat cost is the lookup and the set add rather than
 * growth. Measuring that path is the point — it is what a request pays per
 * instrumented method entry.
 */
@Service
public class AutoDepthService {
    long walk(int remaining) {
        if (remaining <= 0) {
            return 0L;
        }
        return remaining + walk(remaining - 1);
    }
}
