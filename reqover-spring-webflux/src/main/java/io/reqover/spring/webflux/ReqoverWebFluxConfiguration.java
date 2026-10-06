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

import io.micrometer.context.ContextRegistry;
import io.reqover.core.CoverageStore;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.RequestIdGenerator;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Registers the Reqover WebFlux filter and its collaborators.
 *
 * <p>Initialization installs a JVM-wide {@code ThreadLocalAccessor} and
 * enables Reactor's automatic context propagation via
 * {@link Hooks#enableAutomaticContextPropagation()}. The hook changes Reactor
 * behavior for the whole application, which is what lets coverage buckets
 * follow a request across scheduler thread hops. It is installed once, even
 * if the application context is refreshed.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReqoverWebFluxProperties.class)
public class ReqoverWebFluxConfiguration implements InitializingBean {
    private static final AtomicBoolean CONTEXT_PROPAGATION_INSTALLED = new AtomicBoolean();

    @Override
    public void afterPropertiesSet() {
        if (CONTEXT_PROPAGATION_INSTALLED.compareAndSet(false, true)) {
            ContextRegistry.getInstance().registerThreadLocalAccessor(new ReqoverThreadLocalAccessor());
            Hooks.enableAutomaticContextPropagation();
        }
    }

    @Bean
    @ConditionalOnMissingBean(CoverageStore.class)
    public CoverageStore reqoverCoverageStore(ReqoverWebFluxProperties properties) {
        return new InMemoryCoverageStore(properties.getMaxSnapshots(), properties.getSnapshotEviction());
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestIdGenerator reqoverRequestIdGenerator() {
        return new RequestIdGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReqoverWebFilter reqoverWebFilter(
            CoverageStore coverageStore,
            RequestIdGenerator requestIdGenerator,
            ReqoverWebFluxProperties properties
    ) {
        return new ReqoverWebFilter(coverageStore, requestIdGenerator, properties.getExcludePathPrefixes());
    }
}
