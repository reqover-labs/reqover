package io.reqover.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProbeRegistryTest {
    @AfterEach
    void tearDown() {
        ProbeRegistry.clear();
    }

    @Test
    void registersProbeMetadata() {
        ProbeMetadata metadata = new ProbeMetadata(1, 2, "sample.OrderService", "find", "(J)V", 42);

        ProbeRegistry.register(metadata);

        assertTrue(ProbeRegistry.find(1, 2).isPresent());
        assertEquals("sample.OrderService#find(J)V", ProbeRegistry.find(1, 2).orElseThrow().codeLocationKey());
    }
}

