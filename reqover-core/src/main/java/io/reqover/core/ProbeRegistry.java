package io.reqover.core;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Process-wide registry mapping {@code (classId, probeId)} pairs to the code
 * location they were generated from.
 *
 * <p>Class ids are hashes of class names, so two distinct classes can collide;
 * a collision is reported on standard error because the affected classes would
 * otherwise silently merge their coverage.
 */
public final class ProbeRegistry {
    private static final ConcurrentMap<Key, ProbeMetadata> PROBES = new ConcurrentHashMap<>();

    private ProbeRegistry() {
    }

    public static void register(ProbeMetadata metadata) {
        ProbeMetadata previous = PROBES.put(new Key(metadata.classId(), metadata.probeId()), metadata);
        if (previous != null && !previous.className().equals(metadata.className())) {
            System.err.println("[reqover] classId collision: " + previous.className() + " and "
                    + metadata.className() + " share classId " + metadata.classId()
                    + "; coverage for these classes may be merged");
        }
    }

    public static void registerAll(Collection<ProbeMetadata> metadata) {
        metadata.forEach(ProbeRegistry::register);
    }

    public static Optional<ProbeMetadata> find(int classId, int probeId) {
        return Optional.ofNullable(PROBES.get(new Key(classId, probeId)));
    }

    public static List<ProbeMetadata> all() {
        return List.copyOf(PROBES.values());
    }

    public static void clear() {
        PROBES.clear();
    }

    private record Key(int classId, int probeId) {
    }
}
