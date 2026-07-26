package io.reqover.instrumentation;

import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.ReqoverProbe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReqoverClassInstrumenterTest {
    @BeforeEach
    void setUp() {
        ReqoverProbe.resetGlobalStateForTests();
    }

    @AfterEach
    void tearDown() {
        ReqoverProbe.resetGlobalStateForTests();
    }

    @Test
    void instrumentsMethodEntryWithReqoverProbeHit() throws Exception {
        byte[] original = classBytes(TargetClass.class);
        InstrumentationResult result = new ReqoverClassInstrumenter().instrument(original);
        ProbeRegistry.registerAll(result.metadata());

        Class<?> instrumentedClass = new SingleClassLoader(TargetClass.class.getName(), result.bytecode())
                .loadClass(TargetClass.class.getName());
        Constructor<?> constructor = instrumentedClass.getDeclaredConstructor();
        Object instance = constructor.newInstance();
        Method method = instrumentedClass.getDeclaredMethod("greet", String.class);

        assertEquals("hello reqover", method.invoke(instance, "reqover"));

        List<ProbeMetadata> metadata = result.metadata();
        assertTrue(result.instrumented());
        assertEquals(1, metadata.size());
        ProbeMetadata probe = metadata.get(0);
        assertNotNull(probe.lineNumber());
        assertTrue(ReqoverProbe.globalSnapshot().hasHit(probe.classId(), probe.probeId()));
    }

    private static byte[] classBytes(Class<?> type) throws IOException {
        String resourceName = "/" + type.getName().replace('.', '/') + ".class";
        try (InputStream input = type.getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IOException("Missing class resource: " + resourceName);
            }
            return input.readAllBytes();
        }
    }

    private static final class SingleClassLoader extends ClassLoader {
        private final String targetName;
        private final byte[] targetBytes;

        private SingleClassLoader(String targetName, byte[] targetBytes) {
            super(ReqoverClassInstrumenterTest.class.getClassLoader());
            this.targetName = targetName;
            this.targetBytes = targetBytes.clone();
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals(targetName)) {
                Class<?> loaded = findLoadedClass(name);
                if (loaded == null) {
                    loaded = defineClass(name, targetBytes, 0, targetBytes.length);
                }
                if (resolve) {
                    resolveClass(loaded);
                }
                return loaded;
            }
            return super.loadClass(name, resolve);
        }
    }
}
