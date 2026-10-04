package io.reqover.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates monotonically increasing, human-readable request ids such as
 * {@code req-1}. Thread-safe.
 *
 * <p>The sequence is shared by every generator in the JVM, so ids stay unique
 * when a test run holds several application contexts whose reports are
 * exported to one file.
 */
public final class RequestIdGenerator {
    private static final AtomicLong SEQUENCE = new AtomicLong();
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
        return prefix + "-" + SEQUENCE.incrementAndGet();
    }
}

