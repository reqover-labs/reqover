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

package io.reqover.spring.mvc;

import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.SnapshotEvictionPolicy;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings for the Spring MVC adapter, bound from {@code reqover.mvc.*}.
 */
@ConfigurationProperties(prefix = "reqover.mvc")
public class ReqoverMvcProperties {
    private boolean enabled = true;
    private List<String> includePathPatterns = new ArrayList<>(List.of("/**"));
    private List<String> excludePathPatterns =
            new ArrayList<>(List.of("/reqover", "/reqover/**", "/error"));
    private int maxSnapshots = InMemoryCoverageStore.DEFAULT_MAX_SNAPSHOTS;
    private SnapshotEvictionPolicy snapshotEviction = SnapshotEvictionPolicy.OLDEST_FIRST;

    /** Whether request attribution is installed at all. */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Request paths the interceptor attributes. Defaults to everything. */
    public List<String> getIncludePathPatterns() {
        return includePathPatterns;
    }

    public void setIncludePathPatterns(List<String> includePathPatterns) {
        this.includePathPatterns = includePathPatterns;
    }

    /**
     * Paths excluded from attribution. Keep the report endpoint excluded, or
     * reading the report becomes a request that appears in the next one.
     */
    public List<String> getExcludePathPatterns() {
        return excludePathPatterns;
    }

    public void setExcludePathPatterns(List<String> excludePathPatterns) {
        this.excludePathPatterns = excludePathPatterns;
    }

    /**
     * How many finished requests the default in-memory store retains before
     * applying {@link #getSnapshotEviction()}. Ignored when the application
     * supplies its own {@link io.reqover.core.CoverageStore} bean.
     */
    public int getMaxSnapshots() {
        return maxSnapshots;
    }

    public void setMaxSnapshots(int maxSnapshots) {
        this.maxSnapshots = maxSnapshots;
    }

    /**
     * What the default in-memory store does once {@link #getMaxSnapshots()} is
     * reached: drop the oldest snapshot, or reject new ones. Ignored when the
     * application supplies its own {@link io.reqover.core.CoverageStore} bean.
     */
    public SnapshotEvictionPolicy getSnapshotEviction() {
        return snapshotEviction;
    }

    public void setSnapshotEviction(SnapshotEvictionPolicy snapshotEviction) {
        this.snapshotEviction = snapshotEviction == null
                ? SnapshotEvictionPolicy.OLDEST_FIRST
                : snapshotEviction;
    }
}
