package io.reqover.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSpringE2ETest {
    private static final ObjectMapper JSON = new ObjectMapper();

    private record SampleApp(Process process, Path log, int port) {
    }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @Test
    void instrumentsMvcSampleThroughJavaAgent() throws Exception {
        SampleApp app = startSpringApp(
                System.getProperty("reqover.mvc.sample.jar"),
                "io.reqover.example.mvc.auto"
        );
        try {
            waitForApp(app);
            assertStatus(uri(app, "/auto/orders/42"), 200);

            String report = get(uri(app, "/reqover/report"));
            assertTrue(report.contains("GET /auto/orders/{id}"), report);
            assertTrue(report.contains("io.reqover.example.mvc.auto.AutoOrderService"), report);
            assertTrue(report.contains("io.reqover.example.mvc.auto.AutoOrderController"), report);
        } finally {
            stop(app);
        }
    }

    @Test
    void startsMvcSampleSafelyWhenAgentIncludeIsMissing() throws Exception {
        SampleApp app = startSpringApp(
                System.getProperty("reqover.mvc.sample.jar"),
                null
        );
        try {
            waitForApp(app);
            assertStatus(uri(app, "/auto/orders/42"), 200);

            String report = get(uri(app, "/reqover/report"));
            assertFalse(report.contains("io.reqover.example.mvc.auto.AutoOrderService"), report);
            assertTrue(appLog(app).contains("agent inactive"), appLog(app));
        } finally {
            stop(app);
        }
    }

    @Test
    void instrumentsWebFluxSampleThroughJavaAgentAcrossThreadHop() throws Exception {
        SampleApp app = startSpringApp(
                System.getProperty("reqover.webflux.sample.jar"),
                "io.reqover.example.webflux.auto"
        );
        try {
            waitForApp(app);
            exerciseConcurrentWebFluxRequests(app, 10);

            String report = get(uri(app, "/reqover/report"));
            JsonNode endpoint = endpoint(report, "GET /auto/reactive/orders/{id}");
            JsonNode manualEndpoint = endpoint(report, "GET /reactive/orders/{id}");
            assertEquals(10, endpoint.path("requestCount").asInt(), report);
            assertEquals(10, manualEndpoint.path("requestCount").asInt(), report);
            // Reactor Netty uses transport-specific suffixes such as nio on
            // macOS and epoll on Linux. Verify the event-loop family rather
            // than coupling the E2E contract to one operating system.
            assertTrue(hasThreadPrefix(endpoint, "reactor-http-"), report);
            assertTrue(hasThreadPrefix(endpoint, "boundedElastic-"), report);
            assertTrue(hasThreadPrefix(endpoint, "parallel-"), report);
            assertTrue(hasMethod(
                    endpoint,
                    "io.reqover.example.webflux.auto.AutoReactiveOrderService",
                    "validate",
                    "(J)J"
            ), report);
            assertTrue(hasClass(endpoint, "io.reqover.example.webflux.auto.AutoReactiveOrderController"), report);
            assertFalse(hasClass(endpoint, "ReactiveOrderService"), report);
            assertTrue(hasClass(manualEndpoint, "ReactiveOrderService"), report);
            assertFalse(hasClass(
                    manualEndpoint,
                    "io.reqover.example.webflux.auto.AutoReactiveOrderService"
            ), report);
        } finally {
            stop(app);
        }
    }

    private void exerciseConcurrentWebFluxRequests(SampleApp app, int iterations) throws Exception {
        List<CompletableFuture<HttpResponse<String>>> requests = new ArrayList<>();
        for (int i = 1; i <= iterations; i++) {
            requests.add(sendAsync(uri(app, "/auto/reactive/orders/" + i)));
            requests.add(sendAsync(uri(app, "/reactive/orders/" + i)));
        }
        CompletableFuture.allOf(requests.toArray(CompletableFuture[]::new))
                .get(30, TimeUnit.SECONDS);
        for (CompletableFuture<HttpResponse<String>> request : requests) {
            HttpResponse<String> response = request.get();
            assertEquals(200, response.statusCode(), response.body());
        }
    }

    private CompletableFuture<HttpResponse<String>> sendAsync(URI uri) {
        return http.sendAsync(
                HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(10)).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
    }

    private static JsonNode endpoint(String report, String endpointName) throws IOException {
        for (JsonNode endpoint : JSON.readTree(report).path("endpoints")) {
            if (endpointName.equals(endpoint.path("endpoint").asText())) {
                return endpoint;
            }
        }
        throw new AssertionError("Endpoint not found: " + endpointName + "\n" + report);
    }

    private static boolean hasThreadPrefix(JsonNode endpoint, String prefix) {
        for (JsonNode threadName : endpoint.path("threadNames")) {
            if (threadName.asText().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasClass(JsonNode endpoint, String className) {
        for (JsonNode coveredClass : endpoint.path("classes")) {
            if (className.equals(coveredClass.path("className").asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasMethod(
            JsonNode endpoint,
            String className,
            String methodName,
            String descriptor
    ) {
        for (JsonNode coveredClass : endpoint.path("classes")) {
            if (!className.equals(coveredClass.path("className").asText())) {
                continue;
            }
            for (JsonNode method : coveredClass.path("methods")) {
                if (methodName.equals(method.path("methodName").asText())
                        && descriptor.equals(method.path("descriptor").asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private SampleApp startSpringApp(String appJar, String includePackage) throws IOException {
        int port = freePort();
        Path log = Files.createTempFile("reqover-e2e-", ".log");
        List<String> command = new ArrayList<>();
        command.add(javaExecutable());
        String agentArgument = "-javaagent:" + System.getProperty("reqover.agent.jar");
        if (includePackage != null && !includePackage.isBlank()) {
            agentArgument += "=include=" + includePackage;
        }
        command.add(agentArgument);
        command.add("-jar");
        command.add(appJar);
        command.add("--server.address=127.0.0.1");
        command.add("--server.port=" + port);
        command.add("--spring.main.banner-mode=off");
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        return new SampleApp(process, log, port);
    }

    private void waitForApp(SampleApp app) throws Exception {
        URI uri = uri(app, "/reqover/report");
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
                    Thread.sleep(200);
                    if (!app.process().isAlive()) {
                        throw new AssertionError("Spring sample exited after readiness check" + appLog(app));
                    }
                    return;
                }
            } catch (Throwable error) {
                lastFailure = error;
            }
            Thread.sleep(500);
        }
        throw new AssertionError("Spring sample did not start on port " + app.port() + appLog(app), lastFailure);
    }

    private static URI uri(SampleApp app, String path) {
        return URI.create("http://127.0.0.1:" + app.port() + path);
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress())) {
            return socket.getLocalPort();
        }
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
            deleteLogWithRetry(app.log());
        }
    }

    private static void deleteLogWithRetry(Path log) throws Exception {
        java.nio.file.FileSystemException lastFailure = null;
        for (int attempt = 1; attempt <= 20; attempt++) {
            try {
                Files.deleteIfExists(log);
                return;
            } catch (java.nio.file.FileSystemException error) {
                lastFailure = error;
                if (attempt < 20) {
                    Thread.sleep(50);
                }
            }
        }
        throw lastFailure;
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
