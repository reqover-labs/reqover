package io.reqover.agent;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentSmokeProcessTest {
    @Test
    void javaAgentInstrumentsClassInSeparateJvm() throws Exception {
        String executableName = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", executableName).toString();
        String agentJar = System.getProperty("reqover.agent.jar");
        String classpath = System.getProperty("java.class.path");

        Process process = new ProcessBuilder(
                javaExecutable,
                "-javaagent:" + agentJar + "=include=sample.agent",
                "-cp",
                classpath,
                "sample.agent.AgentSmokeMain"
        ).redirectErrorStream(true).start();

        boolean finished = process.waitFor(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS);
        String output = readOutput(process);

        assertTrue(finished, "child JVM did not finish; output=" + output);
        assertEquals(0, process.exitValue(), output);
    }

    private static String readOutput(Process process) throws IOException {
        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
