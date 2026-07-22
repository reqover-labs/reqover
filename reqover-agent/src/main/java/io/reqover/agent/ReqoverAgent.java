package io.reqover.agent;

import java.lang.instrument.Instrumentation;

public final class ReqoverAgent {
    private ReqoverAgent() {
    }

    public static void premain(String agentArgs, Instrumentation instrumentation) {
        AgentOptions options = AgentOptions.parse(agentArgs);
        instrumentation.addTransformer(new ReqoverClassFileTransformer(options));
        System.out.println("[reqover] agent started; includes=" + options.includes() + ", excludes=" + options.excludes());
    }
}

