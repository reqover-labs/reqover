package io.reqover.agent;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSpringE2ETest {
    private record SampleApp(Process process, Path log) {
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Test
    void instrumentsMvcSampleThroughJavaAgent() throws Exception {
        SampleApp app = startSpringApp(
                System.getProperty("reqover.mvc.sample.jar"),
                "io.reqover.example.mvc.auto",
                18080
        );
        try {
            waitForApp(app, 18080);
            assertStatus(URI.create("http://localhost:18080/auto/orders/42"), 200);

            String report = get(URI.create("http://localhost:18080/reqover/report"));
            assertTrue(report.contains("GET /auto/orders/{id}"), report);
            assertTrue(report.contains("io.reqover.example.mvc.auto.AutoOrderService"), report);
            assertTrue(report.contains("io.reqover.example.mvc.auto.AutoOrderController"), report);
        } finally {
            stop(app);
        }
    }

    @Test
    void instrumentsWebFluxSampleThroughJavaAgentAcrossThreadHop() throws Exception {
        SampleApp app = startSpringApp(
                System.getProperty("reqover.webflux.sample.jar"),
                "io.reqover.example.webflux.auto",
                18081
        );
        try {
            waitForApp(app, 18081);
            assertStatus(URI.create("http://localhost:18081/auto/reactive/orders/42"), 200);

            String report = get(URI.create("http://localhost:18081/reqover/report"));
            assertTrue(report.contains("GET /auto/reactive/orders/{id}"), report);
            assertTrue(report.contains("io.reqover.example.webflux.auto.AutoReactiveOrderService"), report);
            assertTrue(report.contains("io.reqover.example.webflux.auto.AutoReactiveOrderController"), report);
        } finally {
            stop(app);
        }
    }

    private SampleApp startSpringApp(String appJar, String includePackage, int port) throws IOException {
        Path log = Files.createTempFile("reqover-e2e-", ".log");
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        command.add("-javaagent:" + System.getProperty("reqover.agent.jar") + "=include=" + includePackage);
        command.add("-jar");
        command.add(appJar);
        command.add("--server.port=" + port);
        command.add("--spring.main.banner-mode=off");
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        return new SampleApp(process, log);
    }

    private void waitForApp(SampleApp app, int port) throws Exception {
        URI uri = URI.create("http://localhost:" + port + "/reqover/report");
        long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
        Throwable lastFailure = null;
        while (System.nanoTime() < deadline) {
            if (!app.process().isAlive()) {
                throw new AssertionError(
                        "Spring sample exited early with code " + app.process().exitValue() + appLog(app),
                        lastFailure
                );
            }
            try {
                HttpResponse<String> response = http.send(
                        HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(2)).GET().build(),
                        HttpResponse.BodyHandlers.ofString()
                );
                if (response.statusCode() == 200) {
                    return;
                }
            } catch (Throwable error) {
                lastFailure = error;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Spring sample did not start on port " + port + appLog(app), lastFailure);
    }

    private void assertStatus(URI uri, int status) throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(status, response.statusCode(), response.body());
    }

    private String get(URI uri) throws Exception {
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(200, response.statusCode(), response.body());
        return response.body();
    }

    private void stop(SampleApp app) throws Exception {
        try {
            app.process().destroy();
            if (!app.process().waitFor(10, TimeUnit.SECONDS)) {
                app.process().destroyForcibly();
                app.process().waitFor(10, TimeUnit.SECONDS);
            }
        } finally {
            Files.deleteIfExists(app.log());
        }
    }

    private static String appLog(SampleApp app) {
        try {
            return "\n--- application log ---\n" + Files.readString(app.log(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            return "\n--- application log unavailable: " + error + " ---";
        }
    }

    private static String javaExecutable() {
        String executableName =
                System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        return Path.of(System.getProperty("java.home"), "bin", executableName).toString();
    }
}
