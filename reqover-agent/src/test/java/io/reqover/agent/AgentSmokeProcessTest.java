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
