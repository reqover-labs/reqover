package io.reqover.spring.boot;

import io.reqover.core.CoverageBucketSnapshot;
import io.reqover.core.CoverageStore;
import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportGenerator;
import io.reqover.report.CoverageReportJson;
import io.reqover.report.HtmlCoverageReportRenderer;

import java.util.List;
import java.util.Objects;

/**
 * Builds the report on demand from whatever the store currently holds.
 *
 * <p>Both the HTTP endpoint and the shutdown export go through this, so the
 * file a CI job reads is byte-for-byte what the endpoint would have served.
 */
public class ReqoverReportService {
    private final CoverageStore coverageStore;
    private final CoverageReportGenerator generator = new CoverageReportGenerator();
    private final HtmlCoverageReportRenderer htmlRenderer = new HtmlCoverageReportRenderer();

    public ReqoverReportService(CoverageStore coverageStore) {
        this.coverageStore = Objects.requireNonNull(coverageStore, "coverageStore");
    }

    public CoverageReport report() {
        return report(snapshots());
    }

    public String json() {
        return json(snapshots());
    }

    public String html() {
        return html(snapshots());
    }

    List<CoverageBucketSnapshot> snapshots() {
        return coverageStore.snapshots();
    }

    CoverageReport report(List<CoverageBucketSnapshot> snapshots) {
        return generator.generate(snapshots);
    }

    String json(List<CoverageBucketSnapshot> snapshots) {
        return CoverageReportJson.write(report(snapshots));
    }

    String html(List<CoverageBucketSnapshot> snapshots) {
        return htmlRenderer.render(report(snapshots));
    }
}
