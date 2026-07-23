package io.reqover.report;

import io.reqover.core.CoverageBucketSnapshot;
import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;

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
        Map<String, EndpointAccumulator> endpoints = new HashMap<>();

        for (CoverageBucketSnapshot snapshot : snapshots) {
            EndpointAccumulator endpoint = endpoints.computeIfAbsent(
                    snapshot.unitInfo().name(),
                    EndpointAccumulator::new
            );
            endpoint.accept(snapshot);
        }

        List<EndpointCoverage> endpointCoverages = endpoints.values().stream()
                .map(EndpointAccumulator::toCoverage)
                .sorted(Comparator.comparing(EndpointCoverage::endpoint))
                .toList();

        return new CoverageReport(Instant.now(clock), snapshots.size(), endpointCoverages);
    }

    private static final class EndpointAccumulator {
        private final String endpoint;
        private int requestCount;
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

        private EndpointCoverage toCoverage() {
            List<ClassCoverage> classes = hitsByClass.entrySet().stream()
                    .map(entry -> classCoverage(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(ClassCoverage::className).thenComparingInt(ClassCoverage::classId))
                    .toList();

            return new EndpointCoverage(
                    endpoint,
                    requestCount,
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

