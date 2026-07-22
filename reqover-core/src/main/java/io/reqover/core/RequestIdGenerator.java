package io.reqover.core;

import java.util.concurrent.atomic.AtomicLong;

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

