package io.reqover.instrumentation;

import io.reqover.core.ProbeMetadata;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of instrumenting one class: the (possibly rewritten) bytecode, the
 * probe metadata generated for it, and whether any probe was inserted.
 */
public record InstrumentationResult(
        byte[] bytecode,
        List<ProbeMetadata> metadata,
        boolean instrumented
) {
    public InstrumentationResult {
        bytecode = Objects.requireNonNull(bytecode, "bytecode").clone();
        metadata = List.copyOf(metadata);
    }

    /** Returns a defensive copy; the stored bytecode is never exposed directly. */
    @Override
    public byte[] bytecode() {
        return bytecode.clone();
    }
}
