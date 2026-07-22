package io.reqover.core;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ProbeRegistry {
    private static final ConcurrentMap<Key, ProbeMetadata> PROBES = new ConcurrentHashMap<>();

    private ProbeRegistry() {
    }

    public static void register(ProbeMetadata metadata) {
        PROBES.put(new Key(metadata.classId(), metadata.probeId()), metadata);
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

