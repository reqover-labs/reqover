package io.reqover.spring.webflux;

import io.reqover.core.CoverageStore;
import io.reqover.core.InMemoryCoverageStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverWebFluxAutoConfigurationTest {
    private final ReactiveWebApplicationContextRunner contextRunner =
            new ReactiveWebApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ReqoverWebFluxAutoConfiguration.class));

    @Test
    void enablesWebFluxInstrumentationByDefault() {
        contextRunner.run(context -> {
            assertEquals(1, context.getBeansOfType(ReqoverWebFluxConfiguration.class).size());
            assertEquals(1, context.getBeansOfType(ReqoverWebFilter.class).size());
        });
    }

    @Test
    void disablesWebFluxInstrumentationWhenConfigured() {
        contextRunner
                .withPropertyValues("reqover.webflux.enabled=false")
                .run(context -> {
                    assertTrue(context.getBeansOfType(ReqoverWebFluxConfiguration.class).isEmpty());
                    assertTrue(context.getBeansOfType(ReqoverWebFilter.class).isEmpty());
                });
    }

    @Test
    void contributesTheCoverageStoreThroughTheSpi() {
        contextRunner.run(context -> assertEquals(1, context.getBeansOfType(CoverageStore.class).size()));
    }

    @Test
    void sizesTheDefaultStoreFromTheConfiguredBound() {
        contextRunner
                .withPropertyValues("reqover.webflux.max-snapshots=25")
                .run(context -> {
                    InMemoryCoverageStore store = (InMemoryCoverageStore) context.getBean(CoverageStore.class);
                    assertEquals(25, store.maxSnapshots());
                });
    }

    @Test
    void bindsTheConfiguredExcludedPrefixes() {
        contextRunner
                .withPropertyValues("reqover.webflux.exclude-path-prefixes=/internal,/actuator")
                .run(context -> assertEquals(
                        List.of("/internal", "/actuator"),
                        context.getBean(ReqoverWebFluxProperties.class).getExcludePathPrefixes()
                ));
    }

    @Test
    void excludesTheReportEndpointByDefault() {
        contextRunner.run(context -> assertEquals(
                List.of("/reqover"),
                context.getBean(ReqoverWebFluxProperties.class).getExcludePathPrefixes()
        ));
    }
}
