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

package io.reqover.spring.webflux;

import io.micrometer.context.ThreadLocalAccessor;
import io.reqover.core.CoverageBucket;
import io.reqover.core.CoverageContext;

/**
 * Bridges the coverage bucket between the Reactor {@code Context} and the
 * {@link CoverageContext} ThreadLocal so probe hits recorded on any scheduler
 * thread land in the bucket of the request being processed.
 */
public final class ReqoverThreadLocalAccessor implements ThreadLocalAccessor<CoverageBucket> {
    public static final String KEY = "io.reqover.coverageBucket";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public CoverageBucket getValue() {
        return CoverageContext.current();
    }

    @Override
    public void setValue(CoverageBucket value) {
        if (value == null) {
            CoverageContext.clear();
        } else {
            CoverageContext.set(value);
        }
    }

    @Override
    public void setValue() {
        CoverageContext.clear();
    }
}

