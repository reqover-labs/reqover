package io.reqover.agent;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSmokeProcessTest {
    @Test
    void javaAgentInstrumentsClassInSeparateJvm() throws Exception {
        String executableName =
                System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java";
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", executableName).toString();
        String agentJar = System.getProperty("reqover.agent.jar");
        String classpath = System.getProperty("java.class.path");

        Path log = Files.createTempFile("reqover-smoke-", ".log");
        Process process = new ProcessBuilder(
                javaExecutable,
                "-javaagent:" + agentJar + "=include=sample.agent",
                "-cp",
                classpath,
                "sample.agent.AgentSmokeMain"
        ).redirectErrorStream(true).redirectOutput(log.toFile()).start();

        try {
            boolean finished = process.waitFor(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS);
            String output = Files.readString(log, StandardCharsets.UTF_8);

            assertTrue(finished, "child JVM did not finish; output=" + output);
            assertEquals(0, process.exitValue(), output);
        } finally {
            process.destroyForcibly();
            process.waitFor(10, TimeUnit.SECONDS);
            Files.deleteIfExists(log);
        }
    }
}
