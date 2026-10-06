package io.reqover.instrumentation;

import io.reqover.core.ProbeMetadata;
import io.reqover.core.ProbeRegistry;
import io.reqover.core.ReqoverProbe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void skipsMethodsThatOnlyReadOrWriteOneOwnField() throws Exception {
        InstrumentationResult result = new ReqoverClassInstrumenter().instrument(classBytes(AccessorTarget.class));

        assertEquals(
                Set.of("getDisplayName", "getParentName", "getCountPlusOne", "setNameTrimmed", "withCountChecked"),
                instrumentedMethodNames(result)
        );
    }

    @Test
    void recordsInterfaceCallsAndEnumConstantsAgainstTheReferencedClass() throws Exception {
        String lookup = ReferenceTarget.Lookup.class.getName();
        String area = ReferenceTarget.Area.class.getName();
        InstrumentationResult result = new ReqoverClassInstrumenter(true, name -> name.startsWith("io.reqover.instrumentation."))
                .instrument(classBytes(ReferenceTarget.class));
        ProbeRegistry.registerAll(result.metadata());

        Class<?> instrumented = new SingleClassLoader(ReferenceTarget.class.getName(), result.bytecode())
                .loadClass(ReferenceTarget.class.getName());
        Object target = instrumented.getDeclaredConstructor().newInstance();
        ReferenceTarget.Lookup stub = key -> key;
        instrumented.getMethod("area", ReferenceTarget.Lookup.class).invoke(target, stub);

        ProbeMetadata find = reference(result, lookup, "find");
        ProbeMetadata parking = reference(result, area, "PARKING");
        assertEquals(StableClassId.of(lookup), find.classId());
        assertTrue(find.probeId() >= ReferenceProbes.FIRST_PROBE_ID);
        assertTrue(ReqoverProbe.globalSnapshot().hasHit(find.classId(), find.probeId()));
        assertTrue(ReqoverProbe.globalSnapshot().hasHit(parking.classId(), parking.probeId()));
        assertTrue(result.metadata().stream().noneMatch(m -> m.methodName().contains("$SwitchMap")),
                "compiler-made switch maps are not references");
        assertTrue(ProbeRegistry.find(find.classId(), find.probeId()).isPresent(),
                "registered when assigned, before any class using it is defined");
    }

    @Test
    void recordsTheEnumASwitchReadsAndMethodReferences() throws Exception {
        InstrumentationResult result = new ReqoverClassInstrumenter(true, name -> name.startsWith("io.reqover.instrumentation."))
                .instrument(classBytes(ReferenceTarget.class));
        Class<?> instrumented = new SingleClassLoader(ReferenceTarget.class.getName(), result.bytecode())
                .loadClass(ReferenceTarget.class.getName());
        Object target = instrumented.getDeclaredConstructor().newInstance();
        ReferenceTarget.Lookup stub = key -> key;

        instrumented.getMethod("areaOrdinal", ReferenceTarget.Area.class).invoke(target, ReferenceTarget.Area.PARKING);
        instrumented.getMethod("methodReference", ReferenceTarget.Lookup.class).invoke(target, stub);

        // A switch reads javac's $SwitchMap$ for an enum in another file (and, on
        // JDK 17, for one in the same file too); JDK 21 calls ordinal() directly
        // for a same-file enum. The holder class is package-private to this
        // loader, so switches are checked for their probe, not run.
        reference(result, ReferenceStatus.class.getName(), "<switch>");
        ProbeMetadata ordinal = reference(result, ReferenceTarget.Area.class.getName(), "ordinal");
        assertTrue(ReqoverProbe.globalSnapshot().hasHit(ordinal.classId(), ordinal.probeId()));
        ProbeMetadata find = reference(result, ReferenceTarget.Lookup.class.getName(), "find");
        assertTrue(ReqoverProbe.globalSnapshot().hasHit(find.classId(), find.probeId()));
    }

    @Test
    void recordsReferencesInInterfaceDefaultMethods() throws Exception {
        InstrumentationResult result = new ReqoverClassInstrumenter(true, name -> name.startsWith("io.reqover.instrumentation."))
                .instrument(classBytes(ReferenceTarget.Lookup.class));

        assertTrue(result.instrumented());
        reference(result, ReferenceTarget.Area.class.getName(), "WELFARE_CENTER");
    }

    @Test
    void namesTheEnumBehindASwitchMap() {
        assertEquals("kr/ac/knu/groove/domain/pub/entity/PubArea",
                ReferenceProbes.switchedEnum("$SwitchMap$kr$ac$knu$groove$domain$pub$entity$PubArea"));
        assertEquals("io/reqover/instrumentation/ReferenceTarget$Area",
                ReferenceProbes.switchedEnum("$SwitchMap$io$reqover$instrumentation$ReferenceTarget$Area"));
        assertEquals(null, ReferenceProbes.switchedEnum("ordinaryField"));
    }

    @Test
    void recordsReferencesInsideLambdas() throws Exception {
        InstrumentationResult result = new ReqoverClassInstrumenter(true, name -> name.startsWith("io.reqover.instrumentation."))
                .instrument(classBytes(ReferenceTarget.class));
        ProbeRegistry.registerAll(result.metadata());

        Class<?> instrumented = new SingleClassLoader(ReferenceTarget.class.getName(), result.bytecode())
                .loadClass(ReferenceTarget.class.getName());
        Object target = instrumented.getDeclaredConstructor().newInstance();
        ReferenceTarget.Lookup stub = key -> key;
        instrumented.getMethod("inLambda", ReferenceTarget.Lookup.class).invoke(target, stub);

        ProbeMetadata find = reference(result, ReferenceTarget.Lookup.class.getName(), "find");
        assertTrue(ReqoverProbe.globalSnapshot().hasHit(find.classId(), find.probeId()));
    }

    @Test
    void recordsNoReferencesUnlessAskedTo() throws Exception {
        InstrumentationResult result = new ReqoverClassInstrumenter().instrument(classBytes(ReferenceTarget.class));

        assertTrue(result.metadata().stream().allMatch(m -> m.className().equals(ReferenceTarget.class.getName())));
    }

    private static ProbeMetadata reference(InstrumentationResult result, String className, String member) {
        return result.metadata().stream()
                .filter(m -> m.className().equals(className) && m.methodName().equals(member))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no reference probe for " + className + "#" + member));
    }

    @Test
    void instrumentsAccessorsWhenAskedTo() throws Exception {
        InstrumentationResult result = new ReqoverClassInstrumenter(false).instrument(classBytes(RecordTarget.class));

        Set<String> names = instrumentedMethodNames(result);
        assertTrue(names.contains("code"));
        assertTrue(names.contains("status"));
    }

    @Test
    void skipsRecordComponentAccessorsButKeepsComputedMethods() throws Exception {
        InstrumentationResult result = new ReqoverClassInstrumenter().instrument(classBytes(RecordTarget.class));

        Set<String> names = instrumentedMethodNames(result);
        assertTrue(names.contains("label"));
        assertFalse(names.contains("code"));
        assertFalse(names.contains("status"));
    }

    @Test
    void keepsAccessorShapedMethodsThatHandleRequests() {
        byte[] mapped = accessorClass(null, "Lorg/springframework/web/bind/annotation/GetMapping;");
        byte[] restController = accessorClass("Lorg/springframework/web/bind/annotation/RestController;", null);
        byte[] plain = accessorClass(null, null);

        assertEquals(Set.of("status"), instrumentedMethodNames(new ReqoverClassInstrumenter().instrument(mapped)));
        assertEquals(Set.of("status"), instrumentedMethodNames(new ReqoverClassInstrumenter().instrument(restController)));
        assertEquals(Set.of(), instrumentedMethodNames(new ReqoverClassInstrumenter().instrument(plain)));
    }

    /** {@code String status() { return status; }}, optionally annotated on the class or the method. */
    private static byte[] accessorClass(String classAnnotation, String methodAnnotation) {
        String owner = "io/reqover/instrumentation/GeneratedHandler";
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, owner, null, "java/lang/Object", null);
        if (classAnnotation != null) {
            writer.visitAnnotation(classAnnotation, true).visitEnd();
        }
        writer.visitField(Opcodes.ACC_PRIVATE, "status", "Ljava/lang/String;", null, null).visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC, "status", "()Ljava/lang/String;", null, null);
        if (methodAnnotation != null) {
            method.visitAnnotation(methodAnnotation, true).visitEnd();
        }
        method.visitCode();
        method.visitVarInsn(Opcodes.ALOAD, 0);
        method.visitFieldInsn(Opcodes.GETFIELD, owner, "status", "Ljava/lang/String;");
        method.visitInsn(Opcodes.ARETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static Set<String> instrumentedMethodNames(InstrumentationResult result) {
        return result.metadata().stream()
                .map(ProbeMetadata::methodName)
                .collect(Collectors.toSet());
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
