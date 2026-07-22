package io.reqover.agent;

import io.reqover.core.ProbeRegistry;
import io.reqover.instrumentation.InstrumentationResult;
import io.reqover.instrumentation.ReqoverClassInstrumenter;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;

public final class ReqoverClassFileTransformer implements ClassFileTransformer {
    private final AgentOptions options;
    private final ReqoverClassInstrumenter instrumenter = new ReqoverClassInstrumenter();

    public ReqoverClassFileTransformer(AgentOptions options) {
        this.options = options;
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
            ProbeRegistry.registerAll(result.metadata());
            return result.bytecode();
        } catch (Throwable error) {
            System.err.println("[reqover] failed to instrument " + dottedClassName + ": " + error);
            return null;
        }
    }
}

