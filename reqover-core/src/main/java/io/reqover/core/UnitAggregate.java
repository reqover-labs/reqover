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
