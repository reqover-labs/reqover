package io.reqover.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates monotonically increasing, human-readable request ids such as
 * {@code req-1}. Thread-safe; ids are unique within one generator instance.
 */
public final class RequestIdGenerator {
    private final AtomicLong sequence = new AtomicLong();
    private final String prefix;

    public RequestIdGenerator() {
        this("req");
    }

    public RequestIdGenerator(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        this.prefix = prefix;
    }

    public String nextId() {
        return prefix + "-" + sequence.incrementAndGet();
    }
}

