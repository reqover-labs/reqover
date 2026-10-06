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

package io.reqover.agent;

import java.lang.instrument.Instrumentation;

/**
 * Java agent entry point. Attach with
 * {@code -javaagent:reqover-agent.jar=include=com.example.app}.
 */
public final class ReqoverAgent {
    private ReqoverAgent() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        AgentOptions options = AgentOptions.parse(agentArgs);
        if (options.includes().isEmpty()) {
            System.err.println("[reqover] agent inactive; configure an explicit include package to enable instrumentation");
            return;
        }
        instrumentation.addTransformer(new ReqoverClassFileTransformer(options));
        System.err.println("[reqover] agent started; includes=" + options.includes() + ", excludes=" + options.excludes()
                + ", accessors=" + (options.recordAccessors() ? "record" : "skip")
                + ", references=" + (options.recordReferences() ? "record" : "skip"));
    }
}
