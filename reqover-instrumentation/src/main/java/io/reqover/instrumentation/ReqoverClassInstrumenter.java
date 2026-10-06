package io.reqover.instrumentation;

import io.reqover.core.ProbeMetadata;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class ReqoverClassInstrumenter {
    private static final String PROBE_OWNER = "io/reqover/core/ReqoverProbe";
    private static final String PROBE_METHOD = "hit";
    private static final String PROBE_DESCRIPTOR = "(II)V";

    private final boolean skipTrivialAccessors;
    private final ReferenceProbes references;

    public ReqoverClassInstrumenter() {
        this(true);
    }

    /** {@code skipTrivialAccessors=false} instruments getters, setters and builder methods too. */
    public ReqoverClassInstrumenter(boolean skipTrivialAccessors) {
        this(skipTrivialAccessors, null);
    }

    /**
     * @param referenceTargets when not null, calls to interface methods and reads
     *                         of static fields of classes it accepts (dotted names)
     *                         are recorded against those classes; see {@link ReferenceProbes}
     */
    public ReqoverClassInstrumenter(boolean skipTrivialAccessors, Predicate<String> referenceTargets) {
        this.skipTrivialAccessors = skipTrivialAccessors;
        this.references = referenceTargets == null ? null : new ReferenceProbes(referenceTargets);
    }

    public InstrumentationResult instrument(byte[] originalBytecode) {
        ClassReader reader = new ClassReader(originalBytecode);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
        List<ProbeMetadata> metadata = new ArrayList<>();
        Set<String> trivialAccessors = skipTrivialAccessors ? TrivialAccessorScanner.scan(reader) : Set.of();
        ReqoverClassVisitor visitor = new ReqoverClassVisitor(writer, metadata, trivialAccessors, references);
        reader.accept(visitor, 0);

        if (metadata.isEmpty() && !visitor.referencesRecorded) {
            return new InstrumentationResult(originalBytecode, List.of(), false);
        }
        return new InstrumentationResult(writer.toByteArray(), metadata, true);
    }

    private static final class ReqoverClassVisitor extends ClassVisitor {
        private final List<ProbeMetadata> metadata;
        private final Set<String> trivialAccessors;
        private final ReferenceProbes references;
        private boolean referencesRecorded;
        private String className;
        private int classId;
        private boolean instrumentableClass;
        private boolean annotationType;
        private int nextProbeId;

        private ReqoverClassVisitor(
                ClassVisitor delegate,
                List<ProbeMetadata> metadata,
                Set<String> trivialAccessors,
                ReferenceProbes references
        ) {
            super(Opcodes.ASM9, delegate);
            this.metadata = metadata;
            this.trivialAccessors = trivialAccessors;
            this.references = references;
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
            this.annotationType = (access & Opcodes.ACC_ANNOTATION) != 0;
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
            if (references != null && !annotationType && !"<clinit>".equals(name)
                    && (access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                // Lambdas, constructors and interface default methods too: a
                // repository call inside a stream or an enum default in a field
                // initializer is still a use.
                methodVisitor = new ReferenceMethodVisitor(methodVisitor);
            }
            if (!instrumentableClass
                    || !instrumentableMethod(access, name)
                    || trivialAccessors.contains(TrivialAccessorScanner.key(name, descriptor))) {
                return methodVisitor;
            }

            int probeId = nextProbeId++;
            return new ProbeMethodVisitor(methodVisitor, metadata, classId, probeId, className, name, descriptor);
        }

        private final class ReferenceMethodVisitor extends MethodVisitor {
            private ReferenceMethodVisitor(MethodVisitor delegate) {
                super(Opcodes.ASM9, delegate);
            }

            @Override
            public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                if (opcode == Opcodes.INVOKEINTERFACE && references.isTarget(owner)) {
                    probe(owner, name, descriptor);
                } else if (opcode == Opcodes.INVOKEVIRTUAL && isEnumIdentity(name, descriptor)
                        && references.isTarget(owner)) {
                    // ordinal() and name() are final in java.lang.Enum, so nothing in
                    // the enum's own file runs; javac switches on ordinal() directly
                    // when the enum is declared in the same file.
                    probe(owner, name, descriptor);
                }
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            }

            @Override
            public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                if (opcode == Opcodes.GETSTATIC) {
                    String switchedEnum = ReferenceProbes.switchedEnum(name);
                    if (switchedEnum != null) {
                        // A switch on an enum reads javac's $SwitchMap$, not the enum.
                        if (references.isTarget(switchedEnum)) {
                            probe(switchedEnum, "<switch>", "");
                        }
                    } else if (name.indexOf('$') < 0
                            && !owner.equals(className.replace('.', '/')) && references.isTarget(owner)) {
                        probe(owner, name, descriptor);
                    }
                }
                super.visitFieldInsn(opcode, owner, name, descriptor);
            }

            @Override
            public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap, Object... arguments) {
                // A method reference such as repository::findById has no
                // INVOKEINTERFACE at the call site; record it where it is made.
                for (Object argument : arguments) {
                    if (argument instanceof Handle handle
                            && handle.getTag() == Opcodes.H_INVOKEINTERFACE
                            && references.isTarget(handle.getOwner())) {
                        probe(handle.getOwner(), handle.getName(), handle.getDesc());
                    }
                }
                super.visitInvokeDynamicInsn(name, descriptor, bootstrap, arguments);
            }

            private static boolean isEnumIdentity(String name, String descriptor) {
                return ("ordinal".equals(name) && "()I".equals(descriptor))
                        || ("name".equals(name) && "()Ljava/lang/String;".equals(descriptor));
            }

            private void probe(String owner, String name, String descriptor) {
                int probeId = references.probeId(owner, name, descriptor, metadata);
                referencesRecorded = true;
                super.visitLdcInsn(StableClassId.of(owner.replace('/', '.')));
                super.visitLdcInsn(probeId);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, PROBE_OWNER, PROBE_METHOD, PROBE_DESCRIPTOR, false);
            }
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
