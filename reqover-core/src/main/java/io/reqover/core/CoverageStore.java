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

import java.util.List;

/**
 * Destination for coverage buckets whose unit of work has finished.
 *
 * <p>This is the extension point for retention: {@link InMemoryCoverageStore}
 * keeps a bounded window in heap, but an implementation is free to write
 * snapshots to disk, to a database, or to drop them under a sampling rule.
 *
 * <p>Implementations must be safe for concurrent use. {@link #flush} is called
 * on the thread that completed the unit of work — an HTTP worker in the common
 * case — so it must not block for long.
 */
public interface CoverageStore {
    /**
     * Records a finished bucket. Implementations should call
     * {@link CoverageBucket#snapshot()} immediately, because the bucket may keep
     * receiving hits from stray threads after this call returns.
     */
    void flush(CoverageBucket bucket);

    /**
     * Returns the snapshots currently retained, oldest first. The returned list
     * is a copy and is safe to iterate while other threads flush.
     */
    List<CoverageBucketSnapshot> snapshots();

    /**
     * Per-unit totals over everything flushed since the last {@link #clear()},
     * including units whose snapshots have since been evicted.
     *
     * <p>The report prefers these to {@link #snapshots()} for which endpoints
     * exist, how often they ran and what they executed, so a long recording
     * does not forget an endpoint that was only called early. The default
     * returns an empty list, which tells the report to fall back to the
     * snapshots alone.
     */
    default List<UnitAggregate> aggregates() {
        return List.of();
    }

    /** Discards every retained snapshot and aggregate. */
    void clear();
}
