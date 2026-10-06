/*
 * Copyright 2026 Reqover contributors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

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
