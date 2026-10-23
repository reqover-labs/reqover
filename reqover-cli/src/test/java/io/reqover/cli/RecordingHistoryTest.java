/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.cli;
import io.reqover.report.RecordingArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class RecordingHistoryTest {
    @TempDir Path root;
    @Test void savesNamedRecordingsAndStripsUnknownCredentials() throws Exception {
        String input="{\"schemaVersion\":1,\"generatedAt\":\"2026-10-12T00:00:00Z\",\"completedRequestCount\":0,\"endpoints\":[],\"reverseIndex\":[],\"headers\":{\"Authorization\":\"not-real\"}}";
        var entry=RecordingArchive.create(input,"QA snapshot","local","rev1");assertFalse(entry.json().contains("not-real"));
        Path saved=RecordingHistory.save(root.resolve("recordings"),entry);assertTrue(Files.exists(saved));assertTrue(RecordingHistory.list(root.resolve("recordings")).contains("QA snapshot"));
        Path source=root.resolve("coverage.json");Files.writeString(source,input);
        var out=new java.io.ByteArrayOutputStream();var err=new java.io.ByteArrayOutputStream();
        assertEquals(0,ReqoverCli.run(new String[]{"recording-save","--report",source.toString(),"--name","Named","--history-dir",root.resolve("cli-recordings").toString()},new java.io.PrintStream(out),new java.io.PrintStream(err),java.io.InputStream.nullInputStream()));
        assertEquals(0,ReqoverCli.run(new String[]{"recordings","--history-dir",root.resolve("cli-recordings").toString()},new java.io.PrintStream(out),new java.io.PrintStream(err),java.io.InputStream.nullInputStream()));
        assertTrue(out.toString().contains("reqover-recording-history"));
    }
}
