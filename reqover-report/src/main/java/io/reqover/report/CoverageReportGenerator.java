package io.reqover.report;

import io.reqover.core.CoverageBucketSnapshot;
import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.UnitAggregate;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CoverageReportGenerator {
    private final Clock clock;

    public CoverageReportGenerator() {
        this(Clock.systemUTC());
    }

    CoverageReportGenerator(Clock clock) {
        this.clock = clock;
    }

    public CoverageReport generate(List<CoverageBucketSnapshot> snapshots) {
        return generate(snapshots, List.of());
    }

    /**
     * Builds the report from a snapshot window plus the store's per-unit
     * aggregates. Which endpoints exist, how often they ran and what they
     * executed come from the aggregates, so an endpoint whose requests were all
     * evicted still appears. Request ids exist only for retained snapshots, so
     * an endpoint's {@code requestIds} can be fewer than its {@code requestCount}.
     * An aggregate must cover every snapshot of its unit name; a name with no
     * aggregate is counted from its snapshots. With no aggregates this is
     * {@link #generate(List)}.
     */
    public CoverageReport generate(List<CoverageBucketSnapshot> snapshots, List<UnitAggregate> aggregates) {
        Map<String, EndpointAccumulator> endpoints = new HashMap<>();

        for (CoverageBucketSnapshot snapshot : snapshots) {
            EndpointAccumulator endpoint = endpoints.computeIfAbsent(
                    snapshot.unitInfo().name(),
                    EndpointAccumulator::new
            );
            endpoint.accept(snapshot);
        }
        long aggregated = 0;
        Set<String> totalled = new HashSet<>();
        for (UnitAggregate aggregate : aggregates) {
            endpoints.computeIfAbsent(aggregate.unitName(), EndpointAccumulator::new).accept(aggregate);
            aggregated += aggregate.count();
            totalled.add(aggregate.unitName());
        }
        // A name the store did not total is counted from its snapshots alone.
        for (CoverageBucketSnapshot snapshot : snapshots) {
            if (!totalled.contains(snapshot.unitInfo().name())) {
                aggregated++;
            }
        }
        int completed = (int) Math.min(Integer.MAX_VALUE, aggregated);

        List<EndpointCoverage> endpointCoverages = endpoints.values().stream()
                .map(EndpointAccumulator::toCoverage)
                .sorted(Comparator.comparing(EndpointCoverage::endpoint))
                .toList();

        return new CoverageReport(
                Instant.now(clock),
                completed,
                endpointCoverages,
                reverseIndex(endpointCoverages)
        );
    }

    private static List<CodeEndpointCoverage> reverseIndex(List<EndpointCoverage> endpoints) {
        Map<CodeLocation, Set<String>> endpointNamesByCode = new HashMap<>();
        for (EndpointCoverage endpoint : endpoints) {
            for (ClassCoverage classCoverage : endpoint.classes()) {
                for (MethodCoverage method : classCoverage.methods()) {
                    CodeLocation location = new CodeLocation(
                            classCoverage.className(),
                            method.methodName(),
                            method.descriptor()
                    );
                    endpointNamesByCode.computeIfAbsent(location, ignored -> new HashSet<>())
                            .add(endpoint.endpoint());
                }
            }
        }

        return endpointNamesByCode.entrySet().stream()
                .map(entry -> new CodeEndpointCoverage(
                        entry.getKey().className(),
                        entry.getKey().methodName(),
                        entry.getKey().descriptor(),
                        entry.getValue().stream().sorted().toList()
                ))
                .sorted(Comparator
                        .comparing(CodeEndpointCoverage::className)
                        .thenComparing(CodeEndpointCoverage::methodName)
                        .thenComparing(CodeEndpointCoverage::descriptor))
                .toList();
    }

    private record CodeLocation(String className, String methodName, String descriptor) {
    }

    private static final class EndpointAccumulator {
        private final String endpoint;
        private int requestCount;
        private long aggregateCount;
        private final Set<String> requestIds = new HashSet<>();
        private final Set<String> threadNames = new HashSet<>();
        private final Map<Integer, Set<Integer>> hitsByClass = new HashMap<>();

        private EndpointAccumulator(String endpoint) {
            this.endpoint = endpoint;
        }

        private void accept(CoverageBucketSnapshot snapshot) {
            requestCount++;
            requestIds.add(snapshot.unitInfo().unitId());
            threadNames.addAll(snapshot.threadNames());
            snapshot.hitsByClass().forEach((classId, probes) ->
                    hitsByClass.computeIfAbsent(classId, ignored -> new HashSet<>()).addAll(probes)
            );
        }

        private void accept(UnitAggregate aggregate) {
            aggregateCount += aggregate.count();
            threadNames.addAll(aggregate.threadNames());
            aggregate.hitsByClass().forEach((classId, probes) ->
                    hitsByClass.computeIfAbsent(classId, ignored -> new HashSet<>()).addAll(probes)
            );
        }

        private EndpointCoverage toCoverage() {
            List<ClassCoverage> classes = hitsByClass.entrySet().stream()
                    .map(entry -> classCoverage(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(ClassCoverage::className).thenComparingInt(ClassCoverage::classId))
                    .toList();

            return new EndpointCoverage(
                    endpoint,
                    (int) Math.min(Integer.MAX_VALUE, Math.max(requestCount, aggregateCount)),
                    requestIds.stream().sorted().toList(),
                    threadNames.stream().sorted().toList(),
                    classes
            );
        }

        private ClassCoverage classCoverage(int classId, Set<Integer> probes) {
            List<MethodCoverage> methods = new ArrayList<>();
            String className = "class-" + classId;

            for (int probeId : probes) {
                ProbeMetadata metadata = ProbeRegistry.find(classId, probeId).orElse(null);
                if (metadata != null) {
                    className = metadata.className();
                    methods.add(new MethodCoverage(
                            probeId,
                            metadata.methodName(),
                            metadata.descriptor(),
                            metadata.lineNumber()
                    ));
                } else {
                    methods.add(new MethodCoverage(probeId, "probe-" + probeId, "", null));
                }
            }

            methods.sort(Comparator.comparing(MethodCoverage::methodName).thenComparingInt(MethodCoverage::probeId));
            return new ClassCoverage(classId, className, Set.copyOf(probes), List.copyOf(methods));
        }
    }
}
