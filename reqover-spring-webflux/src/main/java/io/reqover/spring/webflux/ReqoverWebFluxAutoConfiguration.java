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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.WebFilter;
import reactor.core.publisher.Hooks;

/**
 * Auto-configuration entry point for reactive applications using WebFlux.
 *
 * <p>The class conditions matter for the starter: a servlet application that
 * depends on {@code reqover-spring-boot-starter} has this adapter on the
 * classpath but neither Reactor nor WebFlux, and must not fail because of it.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass({WebFilter.class, Hooks.class, ContextRegistry.class})
@ConditionalOnProperty(prefix = "reqover.webflux", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(ReqoverWebFluxConfiguration.class)
public class ReqoverWebFluxAutoConfiguration {
}
