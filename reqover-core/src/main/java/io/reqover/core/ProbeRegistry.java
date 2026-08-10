package io.reqover.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Process-wide registry mapping {@code (classId, probeId)} pairs to the code
 * location they were generated from.
 *
 * <p>Class ids are hashes of class names, so two distinct classes can collide.
 * Registration is atomic: a colliding class is rejected before any of its
 * metadata is stored, preserving the class that reserved the id first.
 */
public final class ProbeRegistry {
    private static final Object REGISTRATION_MONITOR = new Object();
    private static final ConcurrentMap<Integer, String> CLASS_NAMES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Key, ProbeMetadata> PROBES = new ConcurrentHashMap<>();

    private ProbeRegistry() {
    }

    public static void register(ProbeMetadata metadata) {
        tryRegister(metadata);
    }

    /**
     * Registers one probe unless another class has already reserved its class id.
     *
     * @return {@code true} when the metadata was registered, or {@code false}
     * when a class-id collision caused it to be rejected
     */
    public static boolean tryRegister(ProbeMetadata metadata) {
        return registerAll(List.of(metadata));
    }

    /**
     * Atomically registers a batch of probe metadata. If any class id in the
     * batch belongs to a different class, the whole batch is rejected without
     * changing the registry.
     */
    public static boolean registerAll(Collection<ProbeMetadata> metadata) {
        List<ProbeMetadata> batch = List.copyOf(metadata);
        if (batch.isEmpty()) {
            return true;
        }

        synchronized (REGISTRATION_MONITOR) {
            Map<Integer, String> batchClassNames = new HashMap<>();
            for (ProbeMetadata probe : batch) {
                String batchClassName = batchClassNames.putIfAbsent(probe.classId(), probe.className());
                if (batchClassName != null && !batchClassName.equals(probe.className())) {
                    reportCollision(probe.classId(), batchClassName, probe.className());
                    return false;
                }

                String registeredClassName = CLASS_NAMES.get(probe.classId());
                if (registeredClassName != null && !registeredClassName.equals(probe.className())) {
                    reportCollision(probe.classId(), registeredClassName, probe.className());
                    return false;
                }
            }

            batchClassNames.forEach(CLASS_NAMES::putIfAbsent);
            batch.forEach(probe -> PROBES.put(new Key(probe.classId(), probe.probeId()), probe));
            return true;
        }
    }

    public static Optional<ProbeMetadata> find(int classId, int probeId) {
        return Optional.ofNullable(PROBES.get(new Key(classId, probeId)));
    }

    public static List<ProbeMetadata> all() {
        return List.copyOf(PROBES.values());
    }

    public static void clear() {
        synchronized (REGISTRATION_MONITOR) {
            PROBES.clear();
            CLASS_NAMES.clear();
        }
    }

    private static void reportCollision(int classId, String registeredClassName, String rejectedClassName) {
        System.err.println("[reqover] classId collision: " + registeredClassName + " and "
                + rejectedClassName + " share classId " + classId
                + "; rejected metadata for " + rejectedClassName);
    }

    private record Key(int classId, int probeId) {
    }
}
