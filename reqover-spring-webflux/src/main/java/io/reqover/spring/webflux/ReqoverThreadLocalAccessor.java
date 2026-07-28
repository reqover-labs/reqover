package io.reqover.spring.webflux;

import io.micrometer.context.ThreadLocalAccessor;
import io.reqover.core.CoverageBucket;
import io.reqover.core.CoverageContext;

/**
 * Bridges the coverage bucket between the Reactor {@code Context} and the
 * {@link CoverageContext} ThreadLocal so probe hits recorded on any scheduler
 * thread land in the bucket of the request being processed.
 */
public final class ReqoverThreadLocalAccessor implements ThreadLocalAccessor<CoverageBucket> {
    public static final String KEY = "io.reqover.coverageBucket";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public CoverageBucket getValue() {
        return CoverageContext.current();
    }

    @Override
    public void setValue(CoverageBucket value) {
        if (value == null) {
            CoverageContext.clear();
        } else {
            CoverageContext.set(value);
        }
    }

    @Override
    public void setValue() {
        CoverageContext.clear();
    }
}

