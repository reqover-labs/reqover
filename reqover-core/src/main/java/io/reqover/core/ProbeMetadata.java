package io.reqover.core;

import java.util.Objects;

public record ProbeMetadata(
        int classId,
        int probeId,
        String className,
        String methodName,
        String descriptor,
        Integer lineNumber
) {
    public ProbeMetadata {
        if (classId < 0) {
            throw new IllegalArgumentException("classId must be non-negative");
        }
        if (probeId < 0) {
            throw new IllegalArgumentException("probeId must be non-negative");
        }
        className = requireText(className, "className");
        methodName = requireText(methodName, "methodName");
        descriptor = Objects.requireNonNullElse(descriptor, "");
    }

    public String codeLocationKey() {
        return className + "#" + methodName + descriptor;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}

