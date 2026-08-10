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
