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

package io.reqover.example.mvc.payment;

import io.reqover.core.ReqoverProbe;
import io.reqover.example.mvc.ProbeIds;
import io.reqover.example.mvc.SharedValidator;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final SharedValidator validator;

    public PaymentService(SharedValidator validator) {
        this.validator = validator;
    }

    public PaymentResponse pay() {
        ReqoverProbe.hit(ProbeIds.PAYMENT_SERVICE, 1);
        validator.validate("payment");
        return new PaymentResponse("APPROVED");
    }
}

