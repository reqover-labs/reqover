package io.reqover.cli;

import io.reqover.report.CoverageReport;
import io.reqover.report.CoverageReportDiff;
import io.reqover.report.CoverageReportJson;
import io.reqover.report.HtmlCoverageReportRenderer;
import io.reqover.report.ImpactAnalyzer;
import io.reqover.report.ImpactReport;
import io.reqover.report.ImpactReportRenderer;
import io.reqover.report.ReportDiff;
import io.reqover.report.ReportDiffRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line front end for reports that were written to disk.
 *
 * <p>Everything here reads a report produced by a running application; the CLI
 * never instruments anything itself. Its reason to exist is CI: turning a
 * recorded report plus a list of changed files into a reviewable answer, and
 * into an exit code a pipeline can gate on.
 */
public final class ReqoverCli {
    static final int EXIT_OK = 0;
    /** A gate such as {@code --fail-on-impact} tripped. */
    static final int EXIT_GATE_FAILED = 1;
    /** The command line or an input file was wrong. */
    static final int EXIT_USAGE = 2;

    private ReqoverCli() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err, System.in));
    }

    static int run(String[] args, PrintStream out, PrintStream err, InputStream stdin) {
        if (args.length == 0) {
            printUsage(out);
            return EXIT_USAGE;
        }

        String command = args[0];
        try {
            return switch (command) {
                case "render" -> render(args, out);
                case "diff" -> diff(args, out);
                case "impact" -> impact(args, out, stdin);
                case "help", "--help", "-h" -> {
                    printUsage(out);
                    yield EXIT_OK;
                }
                case "version", "--version" -> {
                    out.println("reqover " + version());
                    yield EXIT_OK;
                }
                default -> throw new CliException("unknown command: " + command);
            };
        } catch (CliException e) {
            err.println("reqover: " + e.getMessage());
            err.println("Run 'reqover help' for usage.");
            return EXIT_USAGE;
        } catch (IllegalArgumentException e) {
            err.println("reqover: " + e.getMessage());
            return EXIT_USAGE;
        }
    }

    private static int render(String[] args, PrintStream out) {
        CliArguments arguments = CliArguments.parse(args, 1, List.of("report", "out"), List.of());
        CoverageReport report = readReport(arguments.require("report"));
        String html = new HtmlCoverageReportRenderer().render(report);
        emit(arguments.optional("out", null), html, out);
        return EXIT_OK;
    }

    private static int diff(String[] args, PrintStream out) {
        CliArguments arguments = CliArguments.parse(
                args, 1,
                List.of("baseline", "current", "format", "out"),
                List.of("fail-on-change")
        );

        ReportDiff diff = CoverageReportDiff.between(
                readReport(arguments.require("baseline")),
                readReport(arguments.require("current"))
        );

        String format = arguments.optional("format", "text");
        String rendered = switch (format) {
            case "text" -> ReportDiffRenderer.text(diff);
            case "markdown" -> ReportDiffRenderer.markdown(diff);
            default -> throw new CliException("--format must be text or markdown, got: " + format);
        };

        emit(arguments.optional("out", null), rendered, out);
        return arguments.flag("fail-on-change") && !diff.isEmpty() ? EXIT_GATE_FAILED : EXIT_OK;
    }

    private static int impact(String[] args, PrintStream out, InputStream stdin) {
        CliArguments arguments = CliArguments.parse(
                args, 1,
                List.of("report", "changed-files", "changed", "format", "out"),
                List.of("fail-on-impact")
        );

        CoverageReport report = readReport(arguments.require("report"));
        ImpactReport impact = ImpactAnalyzer.analyze(report, changedPaths(arguments, stdin));

        String format = arguments.optional("format", "text");
        String rendered = switch (format) {
            case "text" -> ImpactReportRenderer.text(impact);
            case "markdown" -> ImpactReportRenderer.markdown(impact);
            case "json" -> ImpactReportRenderer.json(impact);
            default -> throw new CliException("--format must be text, markdown, or json, got: " + format);
        };

        emit(arguments.optional("out", null), rendered, out);
        return arguments.flag("fail-on-impact") && impact.hasImpact() ? EXIT_GATE_FAILED : EXIT_OK;
    }

    private static List<String> changedPaths(CliArguments arguments, InputStream stdin) {
        if (arguments.has("changed-files") == arguments.has("changed")) {
            throw new CliException("pass exactly one of --changed-files or --changed");
        }

        if (arguments.has("changed")) {
            List<String> paths = new ArrayList<>();
            for (String value : arguments.require("changed").split(",")) {
                if (!value.isBlank()) {
                    paths.add(value.trim());
                }
            }
            return paths;
        }

        String source = arguments.require("changed-files");
        String content = "-".equals(source) ? readAll(stdin) : readFile(Path.of(source));
        List<String> paths = new ArrayList<>();
        for (String line : content.split("\\R")) {
            if (!line.isBlank()) {
                paths.add(line.trim());
            }
        }
        return paths;
    }

    private static CoverageReport readReport(String path) {
        String json = readFile(Path.of(path));
        try {
            return CoverageReportJson.read(json);
        } catch (IllegalArgumentException e) {
            throw new CliException(path + " is not a Reqover report: " + e.getMessage(), e);
        }
    }

    private static String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CliException("cannot read " + path + ": " + e.getMessage(), e);
        }
    }

    private static String readAll(InputStream stdin) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            stdin.transferTo(buffer);
            return buffer.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CliException("cannot read standard input: " + e.getMessage(), e);
        }
    }

    private static void emit(String target, String content, PrintStream out) {
        if (target == null) {
            out.print(content);
            return;
        }
        Path path = Path.of(target);
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CliException("cannot write " + path + ": " + e.getMessage(), e);
        }
        out.println("Wrote " + path);
    }

    static String version() {
        String version = ReqoverCli.class.getPackage().getImplementationVersion();
        return version == null ? "(development build)" : version;
    }

    private static void printUsage(PrintStream out) {
        out.print("""
                reqover — read a recorded Reqover report

                Usage:
                  reqover render  --report <report.json> [--out <report.html>]
                  reqover diff    --baseline <a.json> --current <b.json>
                                  [--format text|markdown] [--out <file>] [--fail-on-change]
                  reqover impact  --report <report.json> (--changed-files <file>|- | --changed a,b)
                                  [--format text|markdown|json] [--out <file>] [--fail-on-impact]
                  reqover version

                Reports come from a running application: point
                reqover.report.export.path at a file, or save the JSON your report
                endpoint serves.

                impact answers "which observed APIs executed the code this change
                touched", which is what makes it useful in a pull request:

                  git diff --name-only origin/main... \\
                    | reqover impact --report build/reqover-report.json \\
                        --changed-files - --format markdown

                Exit codes: 0 success · 1 a --fail-on-* gate tripped · 2 bad usage or input.
                """);
    }
}
