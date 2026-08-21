package io.reqover.spring.boot;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

/**
 * Serves the report over HTTP in a servlet application.
 *
 * <p>Registered only when {@code reqover.report.endpoint.enabled=true}. The
 * path comes from {@code reqover.report.endpoint.path}; the HTML report is at
 * the same path with {@code .html} appended.
 *
 * <p>There is no authentication here on purpose — see
 * {@link ReqoverReportProperties.Endpoint}.
 */
@RestController
public class ReqoverMvcReportEndpoint {
    private final ReqoverReportService reportService;

    public ReqoverMvcReportEndpoint(ReqoverReportService reportService) {
        this.reportService = Objects.requireNonNull(reportService, "reportService");
    }

    @GetMapping(
            path = "${reqover.report.endpoint.path:/reqover/report}",
            produces = "application/json;charset=UTF-8"
    )
    public String json() {
        return reportService.json();
    }

    @GetMapping(
            path = "${reqover.report.endpoint.path:/reqover/report}.html",
            produces = "text/html;charset=UTF-8"
    )
    public String html() {
        return reportService.html();
    }
}
