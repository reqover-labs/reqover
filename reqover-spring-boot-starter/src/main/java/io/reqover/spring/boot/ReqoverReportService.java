package io.reqover.spring.boot;

import io.reqover.core.CoverageStore;
import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportGenerator;
import io.reqover.report.CoverageReportJson;
import io.reqover.report.HtmlCoverageReportRenderer;

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
        return report(recording());
    }

    public String json() {
        return json(recording());
    }

    public String html() {
        return html(recording());
    }

    Recording recording() {
        return new Recording(coverageStore.snapshots(), coverageStore.aggregates());
    }

    CoverageReport report(Recording recording) {
        return generator.generate(recording.snapshots(), recording.aggregates());
    }

    String json(Recording recording) {
        return CoverageReportJson.write(report(recording));
    }

    String html(Recording recording) {
        return htmlRenderer.render(report(recording));
    }
}
