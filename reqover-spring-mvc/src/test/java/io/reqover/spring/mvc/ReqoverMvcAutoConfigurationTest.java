package io.reqover.spring.mvc;

import io.reqover.core.CoverageBucket;
import io.reqover.core.CoverageBucketSnapshot;
import io.reqover.core.CoverageStore;
import io.reqover.core.InMemoryCoverageStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverMvcAutoConfigurationTest {
    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ReqoverMvcAutoConfiguration.class));

    @Test
    void enablesMvcInstrumentationByDefault() {
        contextRunner.run(context -> {
            assertEquals(1, context.getBeansOfType(ReqoverMvcInterceptor.class).size());
            assertEquals(1, context.getBeansOfType(CoverageStore.class).size());
        });
    }

    @Test
    void disablesMvcInstrumentationWhenConfigured() {
        contextRunner
                .withPropertyValues("reqover.mvc.enabled=false")
                .run(context -> {
                    assertTrue(context.getBeansOfType(ReqoverMvcConfiguration.class).isEmpty());
                    assertTrue(context.getBeansOfType(ReqoverMvcInterceptor.class).isEmpty());
                });
    }

    @Test
    void sizesTheDefaultStoreFromTheConfiguredBound() {
        contextRunner
                .withPropertyValues("reqover.mvc.max-snapshots=25")
                .run(context -> {
                    InMemoryCoverageStore store = (InMemoryCoverageStore) context.getBean(CoverageStore.class);
                    assertEquals(25, store.maxSnapshots());
                });
    }

    @Test
    void configuresSnapshotEvictionOnTheDefaultStore() {
        contextRunner
                .withPropertyValues(
                        "reqover.mvc.max-snapshots=3",
                        "reqover.mvc.snapshot-eviction=reject-when-full"
                )
                .run(context -> {
                    InMemoryCoverageStore store = (InMemoryCoverageStore) context.getBean(CoverageStore.class);
                    assertEquals(3, store.maxSnapshots());
                    assertEquals(
                            io.reqover.core.SnapshotEvictionPolicy.REJECT_WHEN_FULL,
                            store.evictionPolicy());
                });
    }

    @Test
    void backsOffWhenTheApplicationSuppliesItsOwnStore() {
        contextRunner
                .withUserConfiguration(CustomStoreConfiguration.class)
                .run(context -> {
                    assertEquals(1, context.getBeansOfType(CoverageStore.class).size());
                    assertSame(context.getBean(CountingStore.class), context.getBean(CoverageStore.class));
                });
    }

    @Test
    void bindsTheConfiguredPathPatterns() {
        contextRunner
                .withPropertyValues(
                        "reqover.mvc.include-path-patterns=/api/**",
                        "reqover.mvc.exclude-path-patterns=/api/internal/**"
                )
                .run(context -> {
                    ReqoverMvcProperties properties = context.getBean(ReqoverMvcProperties.class);
                    assertEquals(List.of("/api/**"), properties.getIncludePathPatterns());
                    assertEquals(List.of("/api/internal/**"), properties.getExcludePathPatterns());
                });
    }

    @Test
    void excludesTheReportEndpointByDefault() {
        contextRunner.run(context -> {
            ReqoverMvcProperties properties = context.getBean(ReqoverMvcProperties.class);
            assertTrue(properties.getExcludePathPatterns().contains("/reqover/**"), "reading the report "
                    + "must not appear as a request in the next report");
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class CustomStoreConfiguration {
        @Bean
        CountingStore coverageStore() {
            return new CountingStore();
        }
    }

    /** Stands in for the persistent store the SPI exists to allow. */
    static class CountingStore implements CoverageStore {
        private final List<CoverageBucketSnapshot> flushed = new ArrayList<>();

        @Override
        public void flush(CoverageBucket bucket) {
            flushed.add(bucket.snapshot());
        }

        @Override
        public List<CoverageBucketSnapshot> snapshots() {
            return List.copyOf(flushed);
        }

        @Override
        public void clear() {
            flushed.clear();
        }
    }
}
