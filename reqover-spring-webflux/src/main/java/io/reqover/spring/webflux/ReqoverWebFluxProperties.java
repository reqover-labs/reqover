package io.reqover.spring.webflux;

import io.reqover.core.InMemoryCoverageStore;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings for the Spring WebFlux adapter, bound from {@code reqover.webflux.*}.
 */
@ConfigurationProperties(prefix = "reqover.webflux")
public class ReqoverWebFluxProperties {
    private boolean enabled = true;
    private List<String> excludePathPrefixes = new ArrayList<>(List.of("/reqover"));
    private int maxSnapshots = InMemoryCoverageStore.DEFAULT_MAX_SNAPSHOTS;

    /**
     * Whether the adapter is installed. Turning it off also skips enabling
     * Reactor's automatic context propagation, which is the one JVM-wide change
     * Reqover makes.
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Request paths excluded from attribution, matched as prefixes. Keep the
     * report endpoint excluded, or reading the report becomes a request that
     * appears in the next one.
     */
    public List<String> getExcludePathPrefixes() {
        return excludePathPrefixes;
    }

    public void setExcludePathPrefixes(List<String> excludePathPrefixes) {
        this.excludePathPrefixes = excludePathPrefixes;
    }

    /**
     * How many finished requests the default in-memory store retains before
     * evicting the oldest. Ignored when the application supplies its own
     * {@link io.reqover.core.CoverageStore} bean.
     */
    public int getMaxSnapshots() {
        return maxSnapshots;
    }

    public void setMaxSnapshots(int maxSnapshots) {
        this.maxSnapshots = maxSnapshots;
    }
}
