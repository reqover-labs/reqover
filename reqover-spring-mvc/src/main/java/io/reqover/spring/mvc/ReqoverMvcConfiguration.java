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

package io.reqover.spring.mvc;

import io.reqover.core.CoverageStore;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.RequestIdGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the Reqover MVC interceptor and its collaborators. Every bean
 * backs off when the application defines its own instance.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReqoverMvcProperties.class)
public class ReqoverMvcConfiguration {
    @Bean
    @ConditionalOnMissingBean(CoverageStore.class)
    public CoverageStore reqoverCoverageStore(ReqoverMvcProperties properties) {
        return new InMemoryCoverageStore(properties.getMaxSnapshots(), properties.getSnapshotEviction());
    }

    @Bean
    @ConditionalOnMissingBean
    public RequestIdGenerator reqoverRequestIdGenerator() {
        return new RequestIdGenerator();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReqoverMvcInterceptor reqoverMvcInterceptor(
            CoverageStore coverageStore,
            RequestIdGenerator requestIdGenerator
    ) {
        return new ReqoverMvcInterceptor(coverageStore, requestIdGenerator);
    }

    @Bean
    public WebMvcConfigurer reqoverWebMvcConfigurer(
            ReqoverMvcInterceptor interceptor,
            ReqoverMvcProperties properties
    ) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(interceptor)
                        .addPathPatterns(properties.getIncludePathPatterns())
                        .excludePathPatterns(properties.getExcludePathPatterns());
            }
        };
    }
}
