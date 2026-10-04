package io.reqover.instrumentation;

import io.reqover.core.ProbeMetadata;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ReqoverClassInstrumenter {
    private static final String PROBE_OWNER = "io/reqover/core/ReqoverProbe";
    private static final String PROBE_METHOD = "hit";
    private static final String PROBE_DESCRIPTOR = "(II)V";

    private final boolean skipTrivialAccessors;

    public ReqoverClassInstrumenter() {
        this(true);
    }

    /** {@code skipTrivialAccessors=false} instruments getters, setters and builder methods too. */
    public ReqoverClassInstrumenter(boolean skipTrivialAccessors) {
        this.skipTrivialAccessors = skipTrivialAccessors;
    }

    public InstrumentationResult instrument(byte[] originalBytecode) {
        ClassReader reader = new ClassReader(originalBytecode);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        List<ProbeMetadata> metadata = new ArrayList<>();
        Set<String> trivialAccessors = skipTrivialAccessors ? TrivialAccessorScanner.scan(reader) : Set.of();
        reader.accept(new ReqoverClassVisitor(writer, metadata, trivialAccessors), 0);

        if (metadata.isEmpty()) {
            return new InstrumentationResult(originalBytecode, List.of(), false);
        }
        return new InstrumentationResult(writer.toByteArray(), metadata, true);
    }

    private static final class ReqoverClassVisitor extends ClassVisitor {
        private final List<ProbeMetadata> metadata;
        private final Set<String> trivialAccessors;
        private String className;
        private int classId;
        private boolean instrumentableClass;
        private int nextProbeId;

        private ReqoverClassVisitor(
                ClassVisitor delegate,
                List<ProbeMetadata> metadata,
                Set<String> trivialAccessors
        ) {
            super(Opcodes.ASM9, delegate);
            this.metadata = metadata;
            this.trivialAccessors = trivialAccessors;
        }

        @Override
        public void visit(
                int version,
                int access,
                String name,
                String signature,
                String superName,
                String[] interfaces
        ) {
            this.className = name.replace('/', '.');
            this.classId = StableClassId.of(className);
            this.instrumentableClass = (access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION)) == 0;
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public MethodVisitor visitMethod(
                int access,
                String name,
                String descriptor,
                String signature,
                String[] exceptions
        ) {
            MethodVisitor methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions);
            if (!instrumentableClass
                    || !instrumentableMethod(access, name)
                    || trivialAccessors.contains(TrivialAccessorScanner.key(name, descriptor))) {
                return methodVisitor;
            }

            int probeId = nextProbeId++;
            return new ProbeMethodVisitor(methodVisitor, metadata, classId, probeId, className, name, descriptor);
        }

        private static boolean instrumentableMethod(int access, String name) {
            if ("<init>".equals(name) || "<clinit>".equals(name)) {
                return false;
            }
            return (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE | Opcodes.ACC_SYNTHETIC)) == 0;
        }
    }

    private static final class ProbeMethodVisitor extends MethodVisitor {
        private final List<ProbeMetadata> metadata;
        private final int classId;
        private final int probeId;
        private final String className;
        private final String methodName;
        private final String descriptor;
        private Integer firstLineNumber;

        private ProbeMethodVisitor(
                MethodVisitor delegate,
                List<ProbeMetadata> metadata,
                int classId,
                int probeId,
                String className,
                String methodName,
                String descriptor
        ) {
            super(Opcodes.ASM9, delegate);
            this.metadata = metadata;
            this.classId = classId;
            this.probeId = probeId;
            this.className = className;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            super.visitLdcInsn(classId);
            super.visitLdcInsn(probeId);
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    PROBE_OWNER,
                    PROBE_METHOD,
                    PROBE_DESCRIPTOR,
                    false
            );
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            if (firstLineNumber == null) {
                firstLineNumber = line;
            }
            super.visitLineNumber(line, start);
        }

        @Override
        public void visitEnd() {
            metadata.add(new ProbeMetadata(classId, probeId, className, methodName, descriptor, firstLineNumber));
            super.visitEnd();
        }
    }
}
