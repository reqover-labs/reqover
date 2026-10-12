/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.cli;

import io.reqover.report.RecordingArchive;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

final class RecordingHistory {
    private RecordingHistory() { }
    static Path save(Path directory,RecordingArchive entry) throws IOException {
        Files.createDirectories(directory);String name=String.format(java.util.Locale.ROOT,"%019d",java.time.Instant.parse(entry.savedAt()).toEpochMilli())+"_"+entry.id();
        String json=entry.json();if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>20971520)throw new IllegalArgumentException("Recording archive exceeds 20 MiB");
        Path file=directory.resolve(name+".recording.json");Files.writeString(file,json,StandardOpenOption.CREATE_NEW);Files.writeString(directory.resolve(name+".html"),entry.html(),StandardOpenOption.CREATE_NEW);return file;
    }
    static String list(Path directory)throws IOException {
        var files=new ArrayList<Path>();if(Files.exists(directory))try(var stream=Files.newDirectoryStream(directory,"*.recording.json")){for(var file:stream){if(Files.isRegularFile(file,LinkOption.NOFOLLOW_LINKS)){files.add(file);if(files.size()>1000)throw new IllegalArgumentException("Archive exceeds 1000 recordings; archive old files manually");}}}
        files.sort(Comparator.comparing((Path p)->p.getFileName().toString()).reversed());var entries=new ArrayList<String>();
        for(var file:files.subList(0,Math.min(files.size(),20))){if(Files.size(file)>20971520)throw new IllegalArgumentException("Recording archive exceeds 20 MiB");entries.add(RecordingArchive.read(Files.readString(file)).catalogJson());}
        return "{\"kind\":\"reqover-recording-history\",\"schemaVersion\":1,\"omittedRecordings\":"+Math.max(0,files.size()-20)+",\"recordings\":["+String.join(",",entries)+"]}";
    }
}
