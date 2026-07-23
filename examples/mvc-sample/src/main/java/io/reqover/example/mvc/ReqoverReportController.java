package io.reqover.example.mvc;

import io.reqover.core.InMemoryCoverageStore;
import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportGenerator;
import io.reqover.report.HtmlCoverageReportRenderer;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReqoverReportController {
    private final InMemoryCoverageStore coverageStore;
    private final CoverageReportGenerator reportGenerator = new CoverageReportGenerator();
    private final HtmlCoverageReportRenderer htmlRenderer = new HtmlCoverageReportRenderer();

    public ReqoverReportController(InMemoryCoverageStore coverageStore) {
        this.coverageStore = coverageStore;
    }

    @GetMapping("/reqover/report")
    CoverageReport report() {
        return reportGenerator.generate(coverageStore.snapshots());
    }

    @GetMapping("/reqover/report.html")
    ResponseEntity<String> htmlReport() {
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(htmlRenderer.render(reportGenerator.generate(coverageStore.snapshots())));
    }
}
