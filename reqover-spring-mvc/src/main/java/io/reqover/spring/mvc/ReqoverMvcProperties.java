package io.reqover.spring.mvc;

import io.reqover.core.InMemoryCoverageStore;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Settings for the Spring MVC adapter, bound from {@code reqover.mvc.*}.
 */
@ConfigurationProperties(prefix = "reqover.mvc")
public class ReqoverMvcProperties {
    private boolean enabled = true;
    private List<String> includePathPatterns = new ArrayList<>(List.of("/**"));
    private List<String> excludePathPatterns =
            new ArrayList<>(List.of("/reqover", "/reqover/**", "/error"));
    private int maxSnapshots = InMemoryCoverageStore.DEFAULT_MAX_SNAPSHOTS;

    /** Whether request attribution is installed at all. */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Request paths the interceptor attributes. Defaults to everything. */
    public List<String> getIncludePathPatterns() {
        return includePathPatterns;
    }

    public void setIncludePathPatterns(List<String> includePathPatterns) {
        this.includePathPatterns = includePathPatterns;
    }

    /**
     * Paths excluded from attribution. Keep the report endpoint excluded, or
     * reading the report becomes a request that appears in the next one.
     */
    public List<String> getExcludePathPatterns() {
        return excludePathPatterns;
    }

    public void setExcludePathPatterns(List<String> excludePathPatterns) {
        this.excludePathPatterns = excludePathPatterns;
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
