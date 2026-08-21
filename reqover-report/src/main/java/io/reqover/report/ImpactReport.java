package io.reqover.report;

import java.util.List;

/**
 * Which observed endpoints executed the code a change touched.
 *
 * <p>Read this as "start retesting here", not as a complete change-impact
 * analysis. It can only speak about code that was observed running during the
 * traffic the report was built from: a path lands in {@link #unmatchedPaths()}
 * both when nothing calls it and when nobody exercised it while recording, and
 * the report cannot tell those apart.
 *
 * @param changedPaths every changed path that was analysed, normalised and sorted
 * @param endpoints the impacted endpoints with the changed methods each one ran
 * @param matchedPaths changed paths that map to code present in the report
 * @param unmatchedPaths changed paths with no observed coverage
 */
public record ImpactReport(
        List<String> changedPaths,
        List<ImpactedEndpoint> endpoints,
        List<String> matchedPaths,
        List<String> unmatchedPaths
) {
    /** Whether any observed endpoint ran changed code. */
    public boolean hasImpact() {
        return !endpoints.isEmpty();
    }

    /** The impacted endpoint names alone, in report order. */
    public List<String> endpointNames() {
        return endpoints.stream().map(ImpactedEndpoint::endpoint).toList();
    }
}
