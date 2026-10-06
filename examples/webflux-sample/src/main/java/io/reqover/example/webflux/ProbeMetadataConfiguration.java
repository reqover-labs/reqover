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

package io.reqover.example.webflux;

import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProbeMetadataConfiguration {
    @PostConstruct
    void registerMetadata() {
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.REACTIVE_ORDER_CONTROLLER, 1, "ReactiveOrderController", "find", "(long)", null));
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.REACTIVE_ORDER_SERVICE, 1, "ReactiveOrderService", "find", "(long)", null));
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.REACTIVE_ORDER_MAPPER, 1, "ReactiveOrderMapper", "toResponse", "(long)", null));
        ProbeRegistry.register(new ProbeMetadata(ProbeIds.REACTIVE_VALIDATOR, 1, "ReactiveValidator", "validate", "(String)", null));
    }
}

