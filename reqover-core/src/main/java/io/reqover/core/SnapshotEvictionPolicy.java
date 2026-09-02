package io.reqover.core;

/**
 * How {@link InMemoryCoverageStore} behaves once {@code maxSnapshots} is reached.
 *
 * <p>The Spring adapters bind {@code reqover.mvc.snapshot-eviction} and
 * {@code reqover.webflux.snapshot-eviction} to these constants directly
 * ({@code oldest-first}, {@code reject-when-full}); there is no separate parser.
 */
public enum SnapshotEvictionPolicy {
    /** Drop the oldest retained snapshot to make room for the new one (default). */
    OLDEST_FIRST,
    /** Leave the store unchanged and ignore the newly flushed bucket. */
    REJECT_WHEN_FULL
}
