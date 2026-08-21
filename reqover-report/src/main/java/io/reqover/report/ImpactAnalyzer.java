package io.reqover.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Answers "which APIs should I retest after changing these files?" by matching
 * changed source paths against the reverse index of a {@link CoverageReport}.
 *
 * <p>This is the reverse lookup pointed at a diff. Given the files a pull
 * request touches, it names the endpoints observed executing code in those
 * files, so review and QA can start from a list instead of from a guess.
 */
public final class ImpactAnalyzer {
    private ImpactAnalyzer() {
    }

    /**
     * @param report a report generated from earlier traffic
     * @param changedPaths repository-relative paths, as {@code git diff --name-only}
     * prints them; Windows separators and {@code ./} prefixes are tolerated
     */
    public static ImpactReport analyze(CoverageReport report, Collection<String> changedPaths) {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(changedPaths, "changedPaths");

        Set<String> normalizedPaths = new TreeSet<>();
        for (String path : changedPaths) {
            String normalized = normalize(path);
            if (!normalized.isEmpty()) {
                normalizedPaths.add(normalized);
            }
        }

        // Comparing every code entry against every changed path is quadratic and
        // needless: a source file can only match an entry with the same file name.
        Map<String, List<String>> pathsByFileName = new LinkedHashMap<>();
        for (String path : normalizedPaths) {
            pathsByFileName.computeIfAbsent(fileName(path), ignored -> new ArrayList<>()).add(path);
        }

        Map<String, Set<CodeRef>> codeByEndpoint = new TreeMap<>();
        Set<String> matchedPaths = new TreeSet<>();

        for (CodeEndpointCoverage coverage : report.reverseIndex()) {
            CodeRef code = CodeRef.of(coverage);
            boolean matched = false;

            for (String suffix : code.sourceFileSuffixes()) {
                for (String candidate : pathsByFileName.getOrDefault(fileName(suffix), List.of())) {
                    if (matches(candidate, suffix)) {
                        matchedPaths.add(candidate);
                        matched = true;
                    }
                }
            }

            if (matched) {
                for (String endpoint : coverage.endpoints()) {
                    codeByEndpoint.computeIfAbsent(endpoint, ignored -> new LinkedHashSet<>()).add(code);
                }
            }
        }

        List<ImpactedEndpoint> endpoints = codeByEndpoint.entrySet().stream()
                .map(entry -> new ImpactedEndpoint(
                        entry.getKey(),
                        entry.getValue().stream()
                                .sorted(Comparator.comparing(CodeRef::key))
                                .toList()
                ))
                .toList();

        List<String> unmatchedPaths = normalizedPaths.stream()
                .filter(path -> !matchedPaths.contains(path))
                .toList();

        return new ImpactReport(
                List.copyOf(normalizedPaths),
                endpoints,
                List.copyOf(matchedPaths),
                unmatchedPaths
        );
    }

    /**
     * A changed path matches when it ends with the class's source path at a
     * directory boundary, so {@code src/main/java/a/b/C.java} matches
     * {@code a/b/C.java} while {@code src/main/java/xa/b/C.java} does not.
     */
    private static boolean matches(String changedPath, String sourceSuffix) {
        return changedPath.equals(sourceSuffix) || changedPath.endsWith("/" + sourceSuffix);
    }

    private static String normalize(String path) {
        if (path == null) {
            return "";
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private static String fileName(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash < 0 ? path : path.substring(lastSlash + 1);
    }
}
