package io.reqover.instrumentation;

import io.reqover.core.ProbeMetadata;

import java.util.List;

public record InstrumentationResult(
        byte[] bytecode,
        List<ProbeMetadata> metadata,
        boolean instrumented
) {
    public InstrumentationResult {
        bytecode = bytecode.clone();
        metadata = List.copyOf(metadata);
    }
}

