package io.reqover.instrumentation;

public final class StableClassId {
    private StableClassId() {
    }

    public static int of(String className) {
        int hash = 0x811c9dc5;
        for (int i = 0; i < className.length(); i++) {
            hash ^= className.charAt(i);
            hash *= 0x01000193;
        }
        return hash & 0x7fffffff;
    }
}

