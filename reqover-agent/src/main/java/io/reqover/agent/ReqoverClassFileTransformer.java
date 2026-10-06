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

import io.reqover.core.ProbeRegistry;
import io.reqover.instrumentation.InstrumentationResult;
import io.reqover.instrumentation.ReqoverClassInstrumenter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public final class ReqoverClassFileTransformer implements ClassFileTransformer {
    private final AgentOptions options;
    private final ReqoverClassInstrumenter instrumenter;

    public ReqoverClassFileTransformer(AgentOptions options) {
        this.options = options;
        this.instrumenter = new ReqoverClassInstrumenter(
                !options.recordAccessors(),
                options.recordReferences() ? options::shouldInstrument : null
        );
    }

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer
    ) {
        if (className == null) {
            return null;
        }

        String dottedClassName = className.replace('/', '.');
        if (!options.shouldInstrument(dottedClassName)) {
            return null;
        }

        try {
            InstrumentationResult result = instrumenter.instrument(classfileBuffer);
            if (!result.instrumented()) {
                return null;
            }
            if (!ProbeRegistry.registerAll(result.metadata())) {
                System.err.println("[reqover] skipped instrumentation for " + dottedClassName
                        + " because its classId is already registered to another class");
                return null;
            }
            return result.bytecode();
        } catch (Throwable error) {
            System.err.println("[reqover] failed to instrument " + dottedClassName + ": " + error);
            return null;
        }
    }
}
