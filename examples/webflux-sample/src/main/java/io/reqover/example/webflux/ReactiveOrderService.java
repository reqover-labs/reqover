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

import io.reqover.core.ReqoverProbe;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ReactiveOrderService {
    private final ReactiveOrderMapper mapper;
    private final ReactiveValidator validator;

    public ReactiveOrderService(ReactiveOrderMapper mapper, ReactiveValidator validator) {
        this.mapper = mapper;
        this.validator = validator;
    }

    public Mono<ReactiveOrderResponse> find(long id) {
        return Mono.just(id)
                .publishOn(Schedulers.boundedElastic())
                .map(value -> {
                    ReqoverProbe.hit(ProbeIds.REACTIVE_ORDER_SERVICE, 1);
                    validator.validate("order-" + value);
                    return value;
                })
                .publishOn(Schedulers.parallel())
                .map(mapper::toResponse);
    }
}

