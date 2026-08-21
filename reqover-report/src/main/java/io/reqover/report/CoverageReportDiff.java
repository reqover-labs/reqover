package io.reqover.report;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * Compares two {@link CoverageReport}s.
 *
 * <p>The intended use is a committed baseline report against one produced by
 * the current build: run the same scenario on both sides and the diff shows
 * whether a change moved which code an endpoint reaches.
 */
public final class CoverageReportDiff {
    private CoverageReportDiff() {
    }

    public static ReportDiff between(CoverageReport baseline, CoverageReport current) {
        Objects.requireNonNull(baseline, "baseline");
        Objects.requireNonNull(current, "current");

        Map<String, Set<CodeRef>> before = codeByEndpoint(baseline);
        Map<String, Set<CodeRef>> after = codeByEndpoint(current);

        List<String> added = after.keySet().stream().filter(name -> !before.containsKey(name)).sorted().toList();
        List<String> removed = before.keySet().stream().filter(name -> !after.containsKey(name)).sorted().toList();

        List<EndpointCodeDiff> changed = new ArrayList<>();
        for (Map.Entry<String, Set<CodeRef>> entry : new TreeMap<>(after).entrySet()) {
            Set<CodeRef> baselineCode = before.get(entry.getKey());
            if (baselineCode == null) {
                continue;
            }
            List<CodeRef> addedCode = difference(entry.getValue(), baselineCode);
            List<CodeRef> removedCode = difference(baselineCode, entry.getValue());
            if (!addedCode.isEmpty() || !removedCode.isEmpty()) {
                changed.add(new EndpointCodeDiff(entry.getKey(), addedCode, removedCode));
            }
        }

        return new ReportDiff(added, removed, List.copyOf(changed));
    }

    private static List<CodeRef> difference(Set<CodeRef> from, Set<CodeRef> without) {
        return from.stream()
                .filter(code -> !without.contains(code))
                .sorted(Comparator.comparing(CodeRef::key))
                .toList();
    }

    private static Map<String, Set<CodeRef>> codeByEndpoint(CoverageReport report) {
        Map<String, Set<CodeRef>> byEndpoint = new LinkedHashMap<>();
        for (EndpointCoverage endpoint : report.endpoints()) {
            Set<CodeRef> code = byEndpoint.computeIfAbsent(endpoint.endpoint(), ignored -> new LinkedHashSet<>());
            for (ClassCoverage classCoverage : endpoint.classes()) {
                for (MethodCoverage method : classCoverage.methods()) {
                    code.add(new CodeRef(classCoverage.className(), method.methodName(), method.descriptor()));
                }
            }
        }
        return byEndpoint;
    }
}
