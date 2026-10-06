package io.reqover.instrumentation;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the methods of a class whose whole body reads or writes one of its own
 * instance fields and nothing else: a Lombok {@code @Getter}/{@code @Setter},
 * a record accessor, a builder or fluent setter that stores its argument and
 * returns {@code this}, or a hand-written equivalent.
 *
 * <p>These run on every request that serializes the object, so recording them
 * makes a response envelope look like logic shared by every API. The match is
 * on the bytecode shape, not the name, so a getter that computes anything is
 * still recorded.
 *
 * <p>A request handler is never an accessor, whatever its body: a method
 * carrying a Spring web annotation such as {@code @GetMapping}, and every
 * method of a class annotated {@code @Controller} or {@code @RestController},
 * is kept.
 */
final class TrivialAccessorScanner {
    private static final String SPRING_WEB_ANNOTATION_PREFIX = "Lorg/springframework/web/bind/annotation/";
    private static final Set<String> CONTROLLER_ANNOTATIONS = Set.of(
            "Lorg/springframework/stereotype/Controller;",
            "Lorg/springframework/web/bind/annotation/RestController;"
    );

    private TrivialAccessorScanner() {
    }

    /** Returns {@code name + descriptor} of each trivial accessor. */
    static Set<String> scan(ClassReader reader) {
        Set<String> accessors = new HashSet<>();
        boolean[] controller = new boolean[1];
        reader.accept(new ClassVisitor(Opcodes.ASM9) {
            private String owner;

            @Override
            public void visit(
                    int version,
                    int access,
                    String name,
                    String signature,
                    String superName,
                    String[] interfaces
            ) {
                owner = name;
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                if (CONTROLLER_ANNOTATIONS.contains(descriptor)) {
                    controller[0] = true;
                }
                return null;
            }

            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions
            ) {
                if ((access & Opcodes.ACC_STATIC) != 0) {
                    return null;
                }
                return new ShapeRecorder(() -> accessors.add(key(name, descriptor)), owner, descriptor);
            }
        }, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        return controller[0] ? Set.of() : accessors;
    }

    static String key(String name, String descriptor) {
        return name + descriptor;
    }

    private static final class ShapeRecorder extends MethodVisitor {
        private final Runnable onTrivial;
        private final String owner;
        private final String descriptor;
        private final List<Integer> opcodes = new ArrayList<>();
        private final List<Integer> varIndexes = new ArrayList<>();
        private boolean complex;

        private ShapeRecorder(Runnable onTrivial, String owner, String descriptor) {
            super(Opcodes.ASM9);
            this.onTrivial = onTrivial;
            this.owner = owner;
            this.descriptor = descriptor;
        }

        @Override
        public AnnotationVisitor visitAnnotation(String annotationDescriptor, boolean visible) {
            if (annotationDescriptor.startsWith(SPRING_WEB_ANNOTATION_PREFIX)) {
                complex = true;
            }
            return null;
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            opcodes.add(opcode);
            varIndexes.add(varIndex);
        }

        @Override
        public void visitFieldInsn(int opcode, String fieldOwner, String name, String fieldDescriptor) {
            if ((opcode != Opcodes.GETFIELD && opcode != Opcodes.PUTFIELD) || !owner.equals(fieldOwner)) {
                complex = true;
                return;
            }
            opcodes.add(opcode);
            varIndexes.add(-1);
        }

        @Override
        public void visitInsn(int opcode) {
            opcodes.add(opcode);
            varIndexes.add(-1);
        }

        @Override
        public void visitIntInsn(int opcode, int operand) {
            complex = true;
        }

        @Override
        public void visitTypeInsn(int opcode, String type) {
            complex = true;
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            complex = true;
        }

        @Override
        public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap, Object... arguments) {
            complex = true;
        }

        @Override
        public void visitJumpInsn(int opcode, Label label) {
            complex = true;
        }

        @Override
        public void visitLdcInsn(Object value) {
            complex = true;
        }

        @Override
        public void visitIincInsn(int varIndex, int increment) {
            complex = true;
        }

        @Override
        public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            complex = true;
        }

        @Override
        public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            complex = true;
        }

        @Override
        public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            complex = true;
        }

        @Override
        public void visitEnd() {
            if (!complex && (isGetter() || isSetter() || isFluentSetter())) {
                onTrivial.run();
            }
        }

        /** {@code ()T}: {@code aload_0; getfield this.f; xreturn}. */
        private boolean isGetter() {
            Type returnType = Type.getReturnType(descriptor);
            return Type.getArgumentCount(descriptor) == 0
                    && returnType.getSort() != Type.VOID
                    && opcodes.size() == 3
                    && opcodes.get(0) == Opcodes.ALOAD && varIndexes.get(0) == 0
                    && opcodes.get(1) == Opcodes.GETFIELD
                    && opcodes.get(2) == returnType.getOpcode(Opcodes.IRETURN);
        }

        /** {@code (T)Self}: {@code aload_0; xload_1; putfield this.f; aload_0; areturn}. */
        private boolean isFluentSetter() {
            Type[] arguments = Type.getArgumentTypes(descriptor);
            Type returnType = Type.getReturnType(descriptor);
            return arguments.length == 1
                    && returnType.getSort() == Type.OBJECT
                    && returnType.getInternalName().equals(owner)
                    && opcodes.size() == 5
                    && opcodes.get(0) == Opcodes.ALOAD && varIndexes.get(0) == 0
                    && opcodes.get(1) == arguments[0].getOpcode(Opcodes.ILOAD) && varIndexes.get(1) == 1
                    && opcodes.get(2) == Opcodes.PUTFIELD
                    && opcodes.get(3) == Opcodes.ALOAD && varIndexes.get(3) == 0
                    && opcodes.get(4) == Opcodes.ARETURN;
        }

        /** {@code (T)V}: {@code aload_0; xload_1; putfield this.f; return}. */
        private boolean isSetter() {
            Type[] arguments = Type.getArgumentTypes(descriptor);
            return arguments.length == 1
                    && Type.getReturnType(descriptor).getSort() == Type.VOID
                    && opcodes.size() == 4
                    && opcodes.get(0) == Opcodes.ALOAD && varIndexes.get(0) == 0
                    && opcodes.get(1) == arguments[0].getOpcode(Opcodes.ILOAD) && varIndexes.get(1) == 1
                    && opcodes.get(2) == Opcodes.PUTFIELD
                    && opcodes.get(3) == Opcodes.RETURN;
        }
    }
}
