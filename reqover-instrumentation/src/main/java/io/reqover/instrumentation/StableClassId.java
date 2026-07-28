package io.reqover.instrumentation;

/**
 * Derives a stable, non-negative class id from a class name using a 31-bit
 * FNV-1a hash.
 *
 * <p>The id is deterministic across JVM runs, but it is a hash: two distinct
 * class names can collide, in which case their probes share an id space and
 * {@code ProbeRegistry} reports the collision at registration time.
 */
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
