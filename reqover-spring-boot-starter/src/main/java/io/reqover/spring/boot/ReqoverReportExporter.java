package io.reqover.spring.boot;

import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Writes the report to disk when the application context closes.
 *
 * <p>This is what turns an integration test run into a file the CLI can read:
 * boot the application with the agent attached, drive traffic through it, let
 * it shut down, then analyse the exported JSON.
 *
 * <p>Export failures are reported on {@code System.err} and swallowed. A
 * measurement tool must not be the reason a shutdown fails.
 */
public class ReqoverReportExporter implements DisposableBean {
    private final ReqoverReportService reportService;
    private final ReqoverReportProperties.Export export;

    public ReqoverReportExporter(ReqoverReportService reportService, ReqoverReportProperties.Export export) {
        this.reportService = Objects.requireNonNull(reportService, "reportService");
        this.export = Objects.requireNonNull(export, "export");
    }

    @Override
    public void destroy() {
        write(export.getJsonPath(), reportService::json, "JSON");
        write(export.getHtmlPath(), reportService::html, "HTML");
    }

    private void write(String target, ContentSupplier content, String kind) {
        if (target == null || target.isBlank()) {
            return;
        }
        Path path = Path.of(target);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content.get(), StandardCharsets.UTF_8);
            System.out.println("[reqover] wrote the " + kind + " report to " + path.toAbsolutePath());
        } catch (IOException | RuntimeException e) {
            System.err.println("[reqover] could not write the " + kind + " report to " + path + ": " + e);
        }
    }

    @FunctionalInterface
    private interface ContentSupplier {
        String get();
    }
}
