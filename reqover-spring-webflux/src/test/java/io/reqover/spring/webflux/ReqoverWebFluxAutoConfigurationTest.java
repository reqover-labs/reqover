package io.reqover.spring.webflux;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;

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
}
