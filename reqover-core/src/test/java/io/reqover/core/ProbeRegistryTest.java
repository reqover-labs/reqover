package io.reqover.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void rejectsCollidingClassIdWithoutOverwritingRegisteredMetadata() {
        ProbeMetadata registered = new ProbeMetadata(7, 0, "sample.First", "first", "()V", 10);
        ProbeMetadata colliding = new ProbeMetadata(7, 0, "sample.Second", "second", "()V", 20);

        assertTrue(ProbeRegistry.tryRegister(registered));
        assertFalse(ProbeRegistry.tryRegister(colliding));

        assertEquals(registered, ProbeRegistry.find(7, 0).orElseThrow());
        assertEquals(List.of(registered), ProbeRegistry.all());
    }

    @Test
    void rejectsEntireBatchWhenOneClassIdCollides() {
        ProbeMetadata registered = new ProbeMetadata(11, 0, "sample.Registered", "work", "()V", null);
        ProbeMetadata newClass = new ProbeMetadata(12, 0, "sample.New", "work", "()V", null);
        ProbeMetadata colliding = new ProbeMetadata(11, 1, "sample.Collision", "work", "()V", null);
        ProbeRegistry.register(registered);

        assertFalse(ProbeRegistry.registerAll(List.of(newClass, colliding)));

        assertFalse(ProbeRegistry.find(12, 0).isPresent());
        assertFalse(ProbeRegistry.find(11, 1).isPresent());
        assertEquals(registered, ProbeRegistry.find(11, 0).orElseThrow());
    }

    @Test
    void concurrentCollidingRegistrationsHaveExactlyOneWinner() throws Exception {
        ProbeMetadata first = new ProbeMetadata(21, 0, "sample.ConcurrentFirst", "work", "()V", null);
        ProbeMetadata second = new ProbeMetadata(21, 0, "sample.ConcurrentSecond", "work", "()V", null);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> firstResult = executor.submit(() -> {
                start.await();
                return ProbeRegistry.tryRegister(first);
            });
            Future<Boolean> secondResult = executor.submit(() -> {
                start.await();
                return ProbeRegistry.tryRegister(second);
            });
            start.countDown();

            assertTrue(firstResult.get() ^ secondResult.get());
            assertEquals(1, ProbeRegistry.all().size());
        } finally {
            executor.shutdownNow();
        }
    }
}
