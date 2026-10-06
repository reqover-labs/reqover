package io.reqover.instrumentation;

import io.reqover.core.ProbeMetadata;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Assigns probes to members that have no body of their own to instrument: a
 * method of an application interface (a Spring Data repository, a port) and a
 * static field such as an enum constant.
 *
 * <p>The probe sits at the call site in already-instrumented code, but it is
 * attributed to the referenced class, so a change to that class's file maps to
 * the endpoints that used it. Reference probes share the referenced class's id
 * and take probe ids from {@link #FIRST_PROBE_ID} up, clear of the ids its own
 * methods get.
 */
final class ReferenceProbes {
    static final int FIRST_PROBE_ID = 1 << 30;

    private final Predicate<String> isTarget;
    private final Map<String, Integer> probeIds = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> nextProbeIds = new ConcurrentHashMap<>();

    /** @param isTarget whether a dotted class name is application code whose references are recorded */
    ReferenceProbes(Predicate<String> isTarget) {
        this.isTarget = isTarget;
    }

    boolean isTarget(String internalName) {
        return isTarget.test(internalName.replace('/', '.'));
    }

    /**
     * The probe id for one member, assigning it on first use and adding its
     * metadata to {@code newMetadata} so the caller's class registers it.
     */
    int probeId(String owner, String name, String descriptor, List<ProbeMetadata> newMetadata) {
        String key = owner + '#' + name + descriptor;
        Integer existing = probeIds.get(key);
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            existing = probeIds.get(key);
            if (existing != null) {
                return existing;
            }
            int probeId = nextProbeIds.computeIfAbsent(owner, ignored -> new AtomicInteger(FIRST_PROBE_ID))
                    .getAndIncrement();
            String className = owner.replace('/', '.');
            newMetadata.add(new ProbeMetadata(StableClassId.of(className), probeId, className, name, descriptor, null));
            probeIds.put(key, probeId);
            return probeId;
        }
    }
}
