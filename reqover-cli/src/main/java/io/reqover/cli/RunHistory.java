/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.cli;

import io.reqover.report.ScenarioRun;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Local files only; never uploaded by the existing CI Action. */
final class RunHistory {
    private RunHistory() { }
    static Path save(Path directory,ScenarioRun result) throws IOException {
        Files.createDirectories(directory);
        String name=String.format(java.util.Locale.ROOT,"%019d",java.time.Instant.parse(result.startedAt()).toEpochMilli())+"_"+result.runId();
        Path json=directory.resolve(name+".run.json");
        Files.writeString(json,result.json(),StandardOpenOption.CREATE_NEW);
        Files.writeString(directory.resolve(name+".html"),result.html(),StandardOpenOption.CREATE_NEW);
        return json;
    }
    static String list(Path directory) throws IOException {
        if(!Files.exists(directory))return "{\"kind\":\"reqover-run-history\",\"schemaVersion\":1,\"runs\":[]}";
        var files=new ArrayList<Path>();
        try(var stream=Files.newDirectoryStream(directory,"*.run.json")) {
            for(Path file:stream){if(Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)){files.add(file);if(files.size()>1000)throw new IllegalArgumentException("History exceeds 1000 files; archive old runs manually");}}
        }
        files.sort(Comparator.comparing((Path p)->p.getFileName().toString()).reversed());
        var runs=new ArrayList<String>();
        for(Path file:files.subList(0,Math.min(files.size(),20))){if(Files.size(file)>1_048_576)throw new IllegalArgumentException("History run exceeds 1 MiB");runs.add(ScenarioRun.read(Files.readString(file)).json());}
        return "{\"kind\":\"reqover-run-history\",\"schemaVersion\":1,\"omittedRuns\":"+Math.max(0,files.size()-20)+",\"runs\":["+String.join(",",runs)+"]}";
    }
}
