/* Copyright 2026 Reqover contributors. SPDX-License-Identifier: Apache-2.0 */
package io.reqover.cli;

import io.reqover.report.TestScenario;
import io.reqover.report.ScenarioRun;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LocalScenarioRunnerTest {
    @TempDir Path history;
    private static TestScenario scenario(int expected, boolean reviewed) {
        return new TestScenario("Regression", "local", "rev1", "2026-10-12T00:00:00Z",
                List.of(new TestScenario.Case("case-1", "GET", "/ok", expected, 1000, reviewed)));
    }
    @Test void validatesBeforeNetworkAndRequiresExplicitLoopbackConsent() {
        assertThrows(IllegalArgumentException.class, () -> LocalScenarioRunner.run(scenario(200,true), URI.create("https://example.com"),1,true));
        assertThrows(IllegalArgumentException.class, () -> LocalScenarioRunner.run(scenario(200,true), URI.create("http://127.0.0.1:9"),1,false));
        assertThrows(IllegalArgumentException.class, () -> LocalScenarioRunner.run(scenario(200,false), URI.create("http://127.0.0.1:9"),1,true));
        assertThrows(IllegalArgumentException.class, () -> LocalScenarioRunner.run(scenario(200,true), URI.create("http://127.0.0.1:9/path"),1,true));
    }
    @Test void actualLocalChecksAndHistoryRoundtrip() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/ok", exchange -> { exchange.sendResponseHeaders(200,-1); exchange.close(); });
        server.start();
        try {
            URI origin = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
            var passed = LocalScenarioRunner.run(scenario(200,true),origin,2,true);
            assertEquals(2, passed.passedCount());
            var failed = LocalScenarioRunner.run(scenario(201,true),origin,1,true);
            assertEquals(0, failed.passedCount());
            assertEquals("STATUS_MISMATCH",failed.attempts().get(0).outcome());
            var saved = RunHistory.save(history,passed);
            assertTrue(java.nio.file.Files.exists(saved));
            assertEquals(2, ScenarioRun.read(java.nio.file.Files.readString(saved)).passedCount());
            assertTrue(RunHistory.list(history).contains(passed.runId()));
            assertTrue(ScenarioRun.compare(passed,failed).contains("not comparable"));
        } finally { server.stop(0); }
    }
    @Test void canonicalScenarioRejectsSecretsUnsafePathsAndFutureSchema() {
        String valid = scenario(200,true).json();
        assertEquals("/ok",TestScenario.read(valid).cases().get(0).path());
        assertThrows(IllegalArgumentException.class, () -> TestScenario.read(valid.replace("\"schemaVersion\":1", "\"schemaVersion\":2")));
        assertThrows(IllegalArgumentException.class, () -> TestScenario.read(valid.replace("/ok", "//outside.example")));
        assertThrows(IllegalArgumentException.class, () -> TestScenario.read(valid.replace("\"name\":", "\"headers\":{},\"name\":")));
    }
    @Test void cliNeedsConsentAndHistoryIsReadableWithoutNetwork() throws Exception {
        Path file=history.resolve("scenario.json");java.nio.file.Files.writeString(file,scenario(200,true).json());
        var output=new java.io.ByteArrayOutputStream();var errors=new java.io.ByteArrayOutputStream();
        assertEquals(2,ReqoverCli.run(new String[]{"run","--scenario",file.toString(),"--base-url","http://127.0.0.1:9","--history-dir",history.resolve("runs").toString()},new java.io.PrintStream(output),new java.io.PrintStream(errors),java.io.InputStream.nullInputStream()));
        assertTrue(errors.toString().contains("confirm-local"));
        assertEquals(0,ReqoverCli.run(new String[]{"runs","--history-dir",history.resolve("empty").toString()},new java.io.PrintStream(output),new java.io.PrintStream(errors),java.io.InputStream.nullInputStream()));
        assertTrue(output.toString().contains("reqover-run-history"));
    }
    @Test void redirectsAreNotFollowedAndClientTimeLimitsAreRealAssertions() throws Exception {
        var visits=new java.util.concurrent.atomic.AtomicInteger();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/ok",exchange->{visits.incrementAndGet();exchange.sendResponseHeaders(200,-1);exchange.close();});
        server.createContext("/redirect",exchange->{exchange.getResponseHeaders().add("Location","/ok");exchange.sendResponseHeaders(302,-1);exchange.close();});
        server.createContext("/slow",exchange->{try{Thread.sleep(80);}catch(InterruptedException e){Thread.currentThread().interrupt();}exchange.sendResponseHeaders(200,-1);exchange.close();});server.start();
        try {
            URI origin=URI.create("http://127.0.0.1:"+server.getAddress().getPort());
            var s=new TestScenario("Redirect","local","","2026-10-12T00:00:00Z",List.of(new TestScenario.Case("r","GET","/redirect",200,null,true),new TestScenario.Case("s","GET","/slow",200,1,true)));
            var result=LocalScenarioRunner.run(s,origin,1,true);
            assertEquals(0,visits.get());assertEquals(302,result.attempts().get(0).actualStatus());assertEquals("CLIENT_LIMIT_EXCEEDED",result.attempts().get(1).outcome());
        } finally {server.stop(0);}
    }
}
