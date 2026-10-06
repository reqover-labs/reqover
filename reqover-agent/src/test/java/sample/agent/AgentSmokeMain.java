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

package sample.agent;

import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.ReqoverProbe;

public final class AgentSmokeMain {
    private AgentSmokeMain() {
    }

    public static void main(String[] args) {
        ReqoverProbe.resetGlobalStateForTests();

        String value = new AgentSmokeTarget().work("reqover");
        if (!"agent:reqover".equals(value)) {
            System.err.println("unexpected target result: " + value);
            System.exit(2);
        }

        boolean observed = ProbeRegistry.all().stream().anyMatch(AgentSmokeMain::hasGlobalHit);
        if (!observed) {
            System.err.println("agent did not record target probe hit");
            System.err.println("registered metadata=" + ProbeRegistry.all());
            System.err.println("global snapshot=" + ReqoverProbe.globalSnapshot());
            System.exit(3);
        }

        System.out.println("agent smoke ok");
    }

    private static boolean hasGlobalHit(ProbeMetadata metadata) {
        return metadata.className().equals("sample.agent.AgentSmokeTarget")
                && ReqoverProbe.globalSnapshot().hasHit(metadata.classId(), metadata.probeId());
    }
}

