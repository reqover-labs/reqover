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

package io.reqover.example.webflux.auto;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class AutoReactiveOrderService {
    public Mono<AutoReactiveOrderResponse> find(long id) {
        return Mono.just(id)
                .publishOn(Schedulers.boundedElastic())
                .map(this::validate)
                .publishOn(Schedulers.parallel())
                .map(this::toResponse);
    }

    private long validate(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        return id;
    }

    private AutoReactiveOrderResponse toResponse(long id) {
        return new AutoReactiveOrderResponse(id, "AUTO_REACTIVE_FOUND");
    }
}

