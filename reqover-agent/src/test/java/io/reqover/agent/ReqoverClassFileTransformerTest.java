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

import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.instrumentation.StableClassId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sample.agent.AgentSmokeTarget;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverClassFileTransformerTest {
    @AfterEach
    void tearDown() {
        ProbeRegistry.clear();
    }

    @Test
    void leavesClassUnmodifiedWhenItsClassIdBelongsToAnotherClass() throws Exception {
        String targetClassName = AgentSmokeTarget.class.getName();
        int classId = StableClassId.of(targetClassName);
        ProbeMetadata registered = new ProbeMetadata(
                classId,
                0,
                "sample.collision.AlreadyRegistered",
                "work",
                "()V",
                null
        );
        assertTrue(ProbeRegistry.tryRegister(registered));

        ReqoverClassFileTransformer transformer = new ReqoverClassFileTransformer(
                AgentOptions.parse("include=sample.agent.")
        );
        byte[] transformed = transformer.transform(
                AgentSmokeTarget.class.getClassLoader(),
                targetClassName.replace('.', '/'),
                null,
                AgentSmokeTarget.class.getProtectionDomain(),
                classBytes(AgentSmokeTarget.class)
        );

        assertNull(transformed);
        assertEquals(registered, ProbeRegistry.find(classId, 0).orElseThrow());
        assertFalse(ProbeRegistry.all().stream()
                .anyMatch(metadata -> metadata.className().equals(targetClassName)));
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        String resourceName = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing class resource: " + resourceName);
            }
            return input.readAllBytes();
        }
    }
}
