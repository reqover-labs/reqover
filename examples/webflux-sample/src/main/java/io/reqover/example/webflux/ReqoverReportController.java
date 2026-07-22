package io.reqover.example.webflux;

import io.reqover.core.InMemoryCoverageStore;
import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReqoverReportController {
    private final InMemoryCoverageStore coverageStore;
    private final CoverageReportGenerator reportGenerator = new CoverageReportGenerator();

    public ReqoverReportController(InMemoryCoverageStore coverageStore) {
        this.coverageStore = coverageStore;
    }

    @GetMapping("/reqover/report")
    CoverageReport report() {
        return reportGenerator.generate(coverageStore.snapshots());
    }
}

