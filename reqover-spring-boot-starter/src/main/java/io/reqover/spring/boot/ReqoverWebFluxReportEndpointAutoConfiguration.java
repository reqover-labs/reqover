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

package io.reqover.spring.boot;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;

/** Opt-in HTTP report endpoint for reactive applications. */
@AutoConfiguration(after = ReqoverReportAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(RouterFunction.class)
@ConditionalOnBean(ReqoverReportService.class)
@ConditionalOnProperty(prefix = "reqover.report.endpoint", name = "enabled", havingValue = "true")
public class ReqoverWebFluxReportEndpointAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(name = "reqoverReportRoutes")
    public RouterFunction<ServerResponse> reqoverReportRoutes(
            ReqoverReportService reportService,
            ReqoverReportProperties properties
    ) {
        String path = properties.getEndpoint().getPath();
        MediaType html = new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8);

        return RouterFunctions.route()
                .GET(path + ".html", request -> ServerResponse.ok()
                        .contentType(html)
                        .bodyValue(reportService.html()))
                .GET(path, request -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(reportService.json()))
                .build();
    }
}
