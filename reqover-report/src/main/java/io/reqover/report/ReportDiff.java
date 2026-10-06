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

package io.reqover.report;

import java.util.List;

/**
 * What changed between two coverage reports.
 *
 * <p>Both sides describe observed traffic, so a difference means "the recording
 * changed", which can be a code change, a different scenario, or traffic that
 * simply did not run this time. The diff reports the change; deciding which of
 * those it was is the reader's job.
 *
 * @param addedEndpoints endpoints observed now but not in the baseline
 * @param removedEndpoints endpoints observed in the baseline but not now
 * @param changedEndpoints endpoints in both reports whose executed code differs
 */
public record ReportDiff(
        List<String> addedEndpoints,
        List<String> removedEndpoints,
        List<EndpointCodeDiff> changedEndpoints
) {
    /** Whether the two reports describe the same observed execution. */
    public boolean isEmpty() {
        return addedEndpoints.isEmpty() && removedEndpoints.isEmpty() && changedEndpoints.isEmpty();
    }
}
