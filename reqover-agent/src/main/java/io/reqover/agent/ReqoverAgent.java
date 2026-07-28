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
        instrumentation.addTransformer(new ReqoverClassFileTransformer(options));
        System.err.println("[reqover] agent started; includes=" + options.includes() + ", excludes=" + options.excludes());
    }
}

