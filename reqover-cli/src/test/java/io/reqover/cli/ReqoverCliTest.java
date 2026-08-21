package io.reqover.cli;

import io.reqover.report.ClassCoverage;
import io.reqover.report.CodeEndpointCoverage;
import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportJson;
import io.reqover.report.EndpointCoverage;
import io.reqover.report.MethodCoverage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverCliTest {
    private static final String ORDERS = "GET /orders/{id}";
    private static final String PAYMENTS = "POST /payments";
    private static final String ORDER_SERVICE_PATH = "src/main/java/com/example/OrderService.java";

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    @TempDir
    Path workspace;

    @Test
    void printsUsageWithoutArguments() {
        assertEquals(ReqoverCli.EXIT_USAGE, run());

        assertTrue(stdout().contains("reqover render"), stdout());
    }

    @Test
    void rejectsAnUnknownCommand() {
        assertEquals(ReqoverCli.EXIT_USAGE, run("summarise"));

        assertTrue(stderr().contains("unknown command: summarise"), stderr());
    }

    @Test
    void rejectsAnUnknownOptionInsteadOfIgnoringIt() {
        Path report = writeReport("report.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_USAGE, run("impact", "--report", report.toString(), "--fail-on-impacts"));

        assertTrue(stderr().contains("unknown option: --fail-on-impacts"), stderr());
    }

    @Test
    void reportsAFileThatIsNotAReport() throws IOException {
        Path broken = workspace.resolve("broken.json");
        Files.writeString(broken, "{oops}");

        assertEquals(ReqoverCli.EXIT_USAGE, run("impact", "--report", broken.toString(), "--changed", "a.java"));

        assertTrue(stderr().contains("is not a Reqover report"), stderr());
    }

    @Test
    void reportsAMissingFile() {
        Path missing = workspace.resolve("absent.json");

        assertEquals(ReqoverCli.EXIT_USAGE, run("render", "--report", missing.toString()));

        assertTrue(stderr().contains("cannot read"), stderr());
    }

    @Test
    void rendersAReportToStandaloneHtml() {
        Path report = writeReport("report.json", twoEndpointReport());
        Path html = workspace.resolve("out/report.html");

        assertEquals(ReqoverCli.EXIT_OK, run("render", "--report", report.toString(), "--out", html.toString()));

        String rendered = read(html);
        assertTrue(rendered.startsWith("<!doctype html>"), rendered.substring(0, 40));
        assertTrue(rendered.contains(ORDERS), ORDERS);
    }

    @Test
    void rendersToStandardOutputWithoutAnOutFile() {
        Path report = writeReport("report.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_OK, run("render", "--report", report.toString()));

        assertTrue(stdout().startsWith("<!doctype html>"), stdout().substring(0, 40));
    }

    @Test
    void namesTheEndpointsToRetestForAChangedFile() {
        Path report = writeReport("report.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_OK, run(
                "impact", "--report", report.toString(), "--changed", ORDER_SERVICE_PATH));

        assertTrue(stdout().contains(ORDERS), stdout());
        assertFalse(stdout().contains(PAYMENTS), stdout());
    }

    @Test
    void readsChangedPathsFromStandardInput() {
        Path report = writeReport("report.json", twoEndpointReport());
        InputStream stdin = new ByteArrayInputStream(
                (ORDER_SERVICE_PATH + "\nREADME.md\n").getBytes(StandardCharsets.UTF_8));

        assertEquals(ReqoverCli.EXIT_OK, run(
                stdin, "impact", "--report", report.toString(), "--changed-files", "-"));

        assertTrue(stdout().contains(ORDERS), stdout());
        assertTrue(stdout().contains("README.md"), stdout());
    }

    @Test
    void readsChangedPathsFromAFile() {
        Path report = writeReport("report.json", twoEndpointReport());
        Path changed = write("changed.txt", ORDER_SERVICE_PATH + "\n");

        assertEquals(ReqoverCli.EXIT_OK, run(
                "impact", "--report", report.toString(), "--changed-files", changed.toString()));

        assertTrue(stdout().contains(ORDERS), stdout());
    }

    @Test
    void requiresExactlyOneSourceOfChangedPaths() {
        Path report = writeReport("report.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_USAGE, run("impact", "--report", report.toString()));
        assertTrue(stderr().contains("exactly one of --changed-files or --changed"), stderr());
    }

    @Test
    void failsTheBuildOnImpactOnlyWhenAsked() {
        Path report = writeReport("report.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_OK, run(
                "impact", "--report", report.toString(), "--changed", ORDER_SERVICE_PATH));
        assertEquals(ReqoverCli.EXIT_GATE_FAILED, run(
                "impact", "--report", report.toString(), "--changed", ORDER_SERVICE_PATH, "--fail-on-impact"));
    }

    @Test
    void passesTheGateWhenNothingIsImpacted() {
        Path report = writeReport("report.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_OK, run(
                "impact", "--report", report.toString(), "--changed", "README.md", "--fail-on-impact"));
    }

    @Test
    void writesMarkdownSuitableForAPullRequestComment() {
        Path report = writeReport("report.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_OK, run(
                "impact", "--report", report.toString(),
                "--changed", ORDER_SERVICE_PATH, "--format", "markdown"));

        assertTrue(stdout().contains("### Reqover — endpoints to retest"), stdout());
        assertTrue(stdout().contains("| Endpoint |"), stdout());
    }

    @Test
    void rejectsAnUnsupportedFormat() {
        Path report = writeReport("report.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_USAGE, run(
                "impact", "--report", report.toString(), "--changed", "a.java", "--format", "xml"));

        assertTrue(stderr().contains("--format must be text, markdown, or json"), stderr());
    }

    @Test
    void diffsTwoReports() {
        Path baseline = writeReport("baseline.json", oneEndpointReport());
        Path current = writeReport("current.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_OK, run(
                "diff", "--baseline", baseline.toString(), "--current", current.toString()));

        assertTrue(stdout().contains(PAYMENTS), stdout());
    }

    @Test
    void failsTheBuildOnADiffOnlyWhenAsked() {
        Path baseline = writeReport("baseline.json", oneEndpointReport());
        Path current = writeReport("current.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_GATE_FAILED, run(
                "diff", "--baseline", baseline.toString(), "--current", current.toString(), "--fail-on-change"));
        assertEquals(ReqoverCli.EXIT_OK, run(
                "diff", "--baseline", current.toString(), "--current", current.toString(), "--fail-on-change"));
    }

    @Test
    void acceptsOptionsWrittenWithAnEqualsSign() {
        Path report = writeReport("report.json", twoEndpointReport());

        assertEquals(ReqoverCli.EXIT_OK, run("impact", "--report=" + report, "--changed=" + ORDER_SERVICE_PATH));

        assertTrue(stdout().contains(ORDERS), stdout());
    }

    @Test
    void printsItsVersion() {
        assertEquals(ReqoverCli.EXIT_OK, run("version"));

        assertTrue(stdout().startsWith("reqover "), stdout());
    }

    // --- helpers ---

    private int run(String... args) {
        return run(InputStream.nullInputStream(), args);
    }

    private int run(InputStream stdin, String... args) {
        out.reset();
        err.reset();
        return ReqoverCli.run(
                args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8),
                stdin
        );
    }

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    private Path writeReport(String name, CoverageReport report) {
        return write(name, CoverageReportJson.write(report));
    }

    private Path write(String name, String content) {
        try {
            Path path = workspace.resolve(name);
            Files.writeString(path, content, StandardCharsets.UTF_8);
            return path;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static CoverageReport twoEndpointReport() {
        return new CoverageReport(
                Instant.parse("2026-08-21T09:00:00Z"),
                2,
                List.of(endpoint(ORDERS, "com.example.OrderService", "find"),
                        endpoint(PAYMENTS, "com.example.PaymentService", "pay")),
                List.of(
                        new CodeEndpointCoverage("com.example.OrderService", "find", "()V", List.of(ORDERS)),
                        new CodeEndpointCoverage("com.example.PaymentService", "pay", "()V", List.of(PAYMENTS))
                )
        );
    }

    private static CoverageReport oneEndpointReport() {
        return new CoverageReport(
                Instant.parse("2026-08-20T09:00:00Z"),
                1,
                List.of(endpoint(ORDERS, "com.example.OrderService", "find")),
                List.of(new CodeEndpointCoverage("com.example.OrderService", "find", "()V", List.of(ORDERS)))
        );
    }

    private static EndpointCoverage endpoint(String name, String className, String methodName) {
        return new EndpointCoverage(
                name,
                1,
                List.of("req-1"),
                List.of("main"),
                List.of(new ClassCoverage(
                        className.hashCode() & Integer.MAX_VALUE,
                        className,
                        Set.of(1),
                        List.of(new MethodCoverage(1, methodName, "()V", 10))
                ))
        );
    }
}
