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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Everything a store has seen for one unit name, such as {@code GET /orders/{id}},
 * independent of how many individual snapshots it still retains.
 *
 * <p>A bounded snapshot window forgets an endpoint once its last request is
 * evicted. The aggregate does not: its size grows with the number of endpoints
 * and probes, not with traffic, so it can keep every unit a recording saw.
 *
 * @param unitName    the grouping key, {@link UnitInfo#name()}
 * @param unitType    {@link UnitInfo#unitType()} of the units folded in
 * @param count       how many units with this name were flushed
 * @param hitsByClass union of the probes those units hit
 * @param threadNames union of the threads they ran on
 */
public record UnitAggregate(
        String unitName,
        String unitType,
        long count,
        Map<Integer, Set<Integer>> hitsByClass,
        Set<String> threadNames
) {
    public UnitAggregate {
        Objects.requireNonNull(unitName, "unitName");
        Objects.requireNonNull(unitType, "unitType");
        Objects.requireNonNull(hitsByClass, "hitsByClass");
        Map<Integer, Set<Integer>> copy = new HashMap<>();
        hitsByClass.forEach((classId, probes) -> copy.put(classId, Set.copyOf(probes)));
        hitsByClass = Map.copyOf(copy);
        threadNames = Set.copyOf(Objects.requireNonNull(threadNames, "threadNames"));
    }
}
