package io.reqover.spring.boot;

import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Writes the report to disk when the application context closes.
 *
 * <p>This is what turns an integration test run into a file the CLI can read:
 * boot the application with the agent attached, drive traffic through it, let
 * it shut down, then analyse the exported JSON.
 *
 * <p>A test run usually holds several application contexts at once: Spring's
 * test context cache keeps one per distinct configuration and closes them all
 * when the JVM exits. Each would overwrite the others' file, so the report a
 * CI job reads would cover only whichever closed last. Instead, every export
 * to a path writes everything exported to that path earlier in the same JVM
 * plus its own requests. The first export in a JVM still replaces the file,
 * so a report from a previous run never leaks in. What was exported is kept
 * until the JVM exits: one context in production exports once at shutdown, and
 * a test JVM ends with the run, so the cost is one copy of each context's
 * already-bounded snapshot window plus its per-endpoint aggregates.
 *
 * <p>Export failures are reported on {@code System.err} and swallowed. A
 * measurement tool must not be the reason a shutdown fails.
 */
public class ReqoverReportExporter implements DisposableBean {
    /** Guarded by itself: two contexts closing at once must not interleave a read-merge-write. */
    private static final Map<Path, Recording> EXPORTED_IN_THIS_JVM = new HashMap<>();

    private final ReqoverReportService reportService;
    private final ReqoverReportProperties.Export export;

    public ReqoverReportExporter(ReqoverReportService reportService, ReqoverReportProperties.Export export) {
        this.reportService = Objects.requireNonNull(reportService, "reportService");
        this.export = Objects.requireNonNull(export, "export");
    }

    @Override
    public void destroy() {
        Recording own = reportService.recording();
        write(export.getJsonPath(), own, reportService::json, "JSON");
        write(export.getHtmlPath(), own, reportService::html, "HTML");
    }

    private void write(
            String target,
            Recording own,
            Function<Recording, String> render,
            String kind
    ) {
        if (target == null || target.isBlank()) {
            return;
        }
        Path path = Path.of(target);
        Path key = path.toAbsolutePath().normalize();
        synchronized (EXPORTED_IN_THIS_JVM) {
            writeMerged(path, key, own, render, kind);
        }
    }

    private static void writeMerged(
            Path path,
            Path key,
            Recording own,
            Function<Recording, String> render,
            String kind
    ) {
        try {
            Recording merged = EXPORTED_IN_THIS_JVM.getOrDefault(key, Recording.EMPTY).plus(own);
            Path parent = key.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, render.apply(merged), StandardCharsets.UTF_8);
            EXPORTED_IN_THIS_JVM.put(key, merged);
            System.out.println("[reqover] wrote the " + kind + " report to " + key);
        } catch (IOException | RuntimeException e) {
            System.err.println("[reqover] could not write the " + kind + " report to " + path + ": " + e);
        }
    }

    /** Forgets what earlier contexts exported, so each test starts from an empty file. */
    static void resetForTests() {
        synchronized (EXPORTED_IN_THIS_JVM) {
            EXPORTED_IN_THIS_JVM.clear();
        }
    }
}
