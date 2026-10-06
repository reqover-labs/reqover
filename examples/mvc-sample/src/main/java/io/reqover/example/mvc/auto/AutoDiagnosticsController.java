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

package io.reqover.example.mvc.auto;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Bounded, read-only demo endpoints for observing slow and failed requests. */
@RestController
public class AutoDiagnosticsController {
    @GetMapping("/auto/diagnostics/delay/{millis}")
    public Map<String, Long> delay(@PathVariable("millis") long millis) {
        if (millis < 0 || millis > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "delay must be between 0 and 2000 ms");
        }
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "demo request interrupted");
        }
        return Map.of("requestedDelayMillis", millis);
    }

    @GetMapping("/auto/diagnostics/failure")
    public void failure() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "intentional diagnostic demo failure");
    }
}
