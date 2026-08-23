package io.reqover.core;

/**
 * How {@link InMemoryCoverageStore} behaves once {@code maxSnapshots} is reached.
 */
public enum SnapshotEvictionPolicy {
    /** Drop the oldest retained snapshot to make room for the new one (default). */
    OLDEST_FIRST,
    /** Leave the store unchanged and ignore the newly flushed bucket. */
    REJECT_WHEN_FULL;

    /**
     * Parses a configuration token such as {@code oldest-first} or
     * {@code reject-when-full}. Unknown values throw.
     */
    public static SnapshotEvictionPolicy fromProperty(String value) {
        if (value == null || value.isBlank()) {
            return OLDEST_FIRST;
        }
        String normalized = value.trim().toLowerCase().replace('_', '-');
        return switch (normalized) {
            case "oldest-first", "oldestfirst", "oldest" -> OLDEST_FIRST;
            case "reject-when-full", "rejectwhenfull", "reject" -> REJECT_WHEN_FULL;
            default -> throw new IllegalArgumentException(
                    "Unknown snapshot eviction policy: " + value
                            + " (expected oldest-first or reject-when-full)");
        };
    }
}
