/*
 * Copyright 2026 Reqover contributors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

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
import io.reqover.report.TestScenario;
import io.reqover.report.ScenarioRun;
import io.reqover.report.RecordingArchive;

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
 * <p>Coverage commands read reports; explicitly confirmed scenario commands
 * perform bounded local checks. The CLI never instruments anything itself.
 * Its CI role is turning a
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
                case "run" -> runScenario(args,out);
                case "runs" -> runs(args,out);
                case "run-report" -> runReport(args,out);
                case "run-compare" -> runCompare(args,out);
                case "recording-save" -> recordingSave(args,out);
                case "recordings" -> recordings(args,out);
                case "recording-report" -> recordingReport(args,out);
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

    private static String readRunInput(String file) {
        try { if(Files.size(Path.of(file))>1_048_576)throw new CliException("scenario/run input exceeds 1 MiB"); }
        catch(IOException e){throw new CliException("cannot read "+file,e);}
        return readFile(Path.of(file));
    }
    private static int runScenario(String[] args,PrintStream out) {
        var options=CliArguments.parse(args,1,List.of("scenario","base-url","history-dir","repeat"),List.of("confirm-local"));
        if(!options.flag("confirm-local"))throw new CliException("--confirm-local is required; use a disposable local target");
        var scenario=TestScenario.read(readRunInput(options.require("scenario")));
        var origin=java.net.URI.create(options.require("base-url"));
        int repeat=Integer.parseInt(options.optional("repeat","1"));
        LocalScenarioRunner.validate(scenario,origin,repeat,true);
        Path directory=Path.of(options.optional("history-dir",".reqover/runs"));
        try {
            Files.createDirectories(directory);
            Path probe=Files.createTempFile(directory,".reqover-write-check-",".tmp");Files.delete(probe);
            var result=LocalScenarioRunner.run(scenario,origin,repeat,true);
            Path saved=RunHistory.save(directory,result);
            out.println("Saved "+saved);out.println(result.passedCount()+" passed / "+(result.attempts().size()-result.passedCount())+" failed / "+(result.plannedAttempts()-result.attempts().size())+" not run");
            return result.passedCount()==result.plannedAttempts()?EXIT_OK:EXIT_GATE_FAILED;
        } catch(IOException e){throw new CliException("cannot write run history; a partial result file may exist",e);}
    }
    private static int runs(String[] args,PrintStream out) {
        var options=CliArguments.parse(args,1,List.of("history-dir","out"),List.of());
        try { emit(options.optional("out",null),RunHistory.list(Path.of(options.optional("history-dir",".reqover/runs"))),out);return EXIT_OK; }
        catch(IOException e){throw new CliException("cannot read run history",e);}
    }
    private static int runReport(String[] args,PrintStream out) {
        var options=CliArguments.parse(args,1,List.of("result","out"),List.of());
        emit(options.optional("out",null),ScenarioRun.read(readRunInput(options.require("result"))).html(),out);return EXIT_OK;
    }
    private static int runCompare(String[] args,PrintStream out) {
        var options=CliArguments.parse(args,1,List.of("baseline","current","out"),List.of());
        emit(options.optional("out",null),ScenarioRun.compare(ScenarioRun.read(readRunInput(options.require("baseline"))),ScenarioRun.read(readRunInput(options.require("current")))),out);return EXIT_OK;
    }
    private static int recordingSave(String[] args,PrintStream out) {
        var options=CliArguments.parse(args,1,List.of("report","name","environment","revision","history-dir"),List.of());
        String file=options.require("report");
        try {if(Files.size(Path.of(file))>10485760)throw new CliException("Recording input exceeds 10 MiB");
            var entry=RecordingArchive.create(readFile(Path.of(file)),options.require("name"),options.optional("environment",""),options.optional("revision",""));
            out.println("Saved "+RecordingHistory.save(Path.of(options.optional("history-dir",".reqover/recordings")),entry));return EXIT_OK;
        } catch(IOException e){throw new CliException("Cannot save recording archive; a partial file may exist",e);}
    }
    private static int recordings(String[] args,PrintStream out) {
        var options=CliArguments.parse(args,1,List.of("history-dir","out"),List.of());
        try {emit(options.optional("out",null),RecordingHistory.list(Path.of(options.optional("history-dir",".reqover/recordings"))),out);return EXIT_OK;}
        catch(IOException e){throw new CliException("Cannot read recording history",e);}
    }
    private static int recordingReport(String[] args,PrintStream out) {
        var options=CliArguments.parse(args,1,List.of("entry","out"),List.of());String file=options.require("entry");
        try {if(Files.size(Path.of(file))>20971520)throw new CliException("Recording archive exceeds 20 MiB");}
        catch(IOException e){throw new CliException("Cannot read recording archive",e);}
        emit(options.optional("out",null),RecordingArchive.read(readFile(Path.of(file))).html(),out);return EXIT_OK;
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
                  reqover recording-save --report <coverage.json> --name <label>
                              [--environment local] [--revision <git SHA>] [--history-dir .reqover/recordings]
                  reqover recordings [--history-dir .reqover/recordings] [--out catalog.json]
                  reqover recording-report --entry <recording.json> [--out recording.html]
                  reqover run --scenario <scenario.json> --base-url <http://127.0.0.1:port>
                              --confirm-local [--repeat 1-10] [--history-dir .reqover/runs]
                  reqover runs [--history-dir .reqover/runs] [--out history.json]
                  reqover run-report --result <run.json> [--out run.html]
                  reqover run-compare --baseline <run.json> --current <run.json> [--out compare.html]
                  reqover render  --report <report.json> [--out <report.html>]
                  reqover diff    --baseline <a.json> --current <b.json>
                                  [--format text|markdown] [--out <file>] [--fail-on-change]
                  reqover impact  --report <report.json> (--changed-files <file>|- | --changed a,b)
                                  [--format text|markdown|json] [--out <file>] [--fail-on-impact]
                  reqover version

                Reports come from a running application: point
                reqover.report.export.json-path at a file, or save the JSON your report
                endpoint serves.

                impact answers "which observed APIs executed the code this change
                touched", which is what makes it useful in a pull request:

                  git diff --name-only origin/main... \\
                    | reqover impact --report build/reqover-report.json \\
                        --changed-files - --format markdown

                Exit codes: 0 success · 1 a --fail-on-* gate tripped or local checks failed/incomplete · 2 bad usage or input.
                """);
    }
}
