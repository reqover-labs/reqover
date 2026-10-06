package io.reqover.spring.boot;

import io.reqover.core.CoverageBucketSnapshot;
import io.reqover.core.UnitAggregate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** What a store holds at one moment: its snapshot window and its per-unit aggregates. */
record Recording(List<CoverageBucketSnapshot> snapshots, List<UnitAggregate> aggregates) {
    static final Recording EMPTY = new Recording(List.of(), List.of());

    Recording {
        snapshots = List.copyOf(snapshots);
        aggregates = List.copyOf(aggregates);
    }

    /** Both recordings together, with aggregates of the same unit name summed. */
    Recording plus(Recording other) {
        List<CoverageBucketSnapshot> allSnapshots = new ArrayList<>(snapshots);
        allSnapshots.addAll(other.snapshots);

        Map<String, UnitAggregate> byName = new LinkedHashMap<>();
        for (UnitAggregate aggregate : aggregates) {
            byName.merge(aggregate.unitName(), aggregate, Recording::sum);
        }
        for (UnitAggregate aggregate : other.aggregates) {
            byName.merge(aggregate.unitName(), aggregate, Recording::sum);
        }
        return new Recording(allSnapshots, List.copyOf(byName.values()));
    }

    private static UnitAggregate sum(UnitAggregate left, UnitAggregate right) {
        Map<Integer, Set<Integer>> hits = new HashMap<>();
        left.hitsByClass().forEach((classId, probes) -> hits.computeIfAbsent(classId, ignored -> new HashSet<>()).addAll(probes));
        right.hitsByClass().forEach((classId, probes) -> hits.computeIfAbsent(classId, ignored -> new HashSet<>()).addAll(probes));
        Set<String> threads = new HashSet<>(left.threadNames());
        threads.addAll(right.threadNames());
        return new UnitAggregate(left.unitName(), left.unitType(), left.count() + right.count(), hits, threads);
    }
}
