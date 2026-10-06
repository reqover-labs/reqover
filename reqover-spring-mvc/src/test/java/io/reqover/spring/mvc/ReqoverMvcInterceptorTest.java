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

import io.reqover.core.CoverageBucketSnapshot;
import io.reqover.core.InMemoryCoverageStore;
import io.reqover.core.RequestIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverMvcInterceptorTest {
    private final InMemoryCoverageStore store = new InMemoryCoverageStore();
    private final ReqoverMvcInterceptor interceptor = new ReqoverMvcInterceptor(store, new RequestIdGenerator());

    @Test
    void skipsTheCatchAllResourceHandler() {
        handle("/no-such-endpoint", "/**", new ResourceHttpRequestHandler(), 404);

        assertTrue(store.snapshots().isEmpty(), store.snapshots().toString());
    }

    @Test
    void recordsAResourceHandlerMappedToItsOwnPattern() {
        handle("/downloads/report.csv", "/downloads/**", new ResourceHttpRequestHandler(), 200);

        List<CoverageBucketSnapshot> snapshots = store.snapshots();
        assertEquals(1, snapshots.size());
        assertEquals("GET /downloads/**", snapshots.get(0).unitInfo().name());
    }

    private void handle(String uri, String pattern, Object handler, int status) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, pattern);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(status);

        interceptor.preHandle(request, response, handler);
        interceptor.afterCompletion(request, response, handler, null);
    }
}
