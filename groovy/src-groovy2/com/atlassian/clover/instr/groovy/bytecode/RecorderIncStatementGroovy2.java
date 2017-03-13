package com.atlassian.clover.instr.groovy.bytecode;

import com_atlassian_clover.CoverageRecorder;
import org.codehaus.groovy.ast.ClassNode;

/**
 * This class MUST be compiled with Groovy 2.x and ASM 4.x in classpath.
 *
 * @see AbstractRecorderIncStatement
 */
@SuppressWarnings("unused") // used via reflections
public class RecorderIncStatementGroovy2 extends AbstractRecorderIncStatement {

    protected static final String COVERAGE_RECORDER_DESC = org.objectweb.asm.Type.getType(CoverageRecorder.class)
            .getDescriptor();
    protected static final String COVERAGE_RECORDER_INTERNAL_NAME = org.objectweb.asm.Type.getType(CoverageRecorder.class)
            .getInternalName();
    protected static final String INC_METHOD_DESCRIPTOR = org.objectweb.asm.Type.getMethodDescriptor(
            org.objectweb.asm.Type.VOID_TYPE, new org.objectweb.asm.Type[]{org.objectweb.asm.Type.INT_TYPE});

    @SuppressWarnings("unused") // instantiated via reflections
    public RecorderIncStatementGroovy2(final ClassNode clazz, final String recorderFieldName, final int index) {
        super(clazz, recorderFieldName, index);
    }

    public void visit(org.objectweb.asm.MethodVisitor mv) {
        mv.visitFieldInsn(org.objectweb.asm.Opcodes.GETSTATIC,
                org.objectweb.asm.Type.getType("L" + ByteCodeUtilsGroovy2.getClassInternalName(clazz) + ";").getInternalName(),
                recorderFieldName,
                COVERAGE_RECORDER_DESC);
        ByteCodeUtilsGroovy2.pushConstant(mv, index);
        mv.visitMethodInsn(org.objectweb.asm.Opcodes.INVOKEVIRTUAL,
                COVERAGE_RECORDER_INTERNAL_NAME,
                "inc",
                INC_METHOD_DESCRIPTOR);
    }

    @Override
    public void visit(groovyjarjarasm.asm.MethodVisitor mv) {
        mv.visitFieldInsn(groovyjarjarasm.asm.Opcodes.GETSTATIC,
                groovyjarjarasm.asm.Type.getType("L" + ByteCodeUtilsGroovy2.getClassInternalName(clazz) + ";").getInternalName(),
                recorderFieldName,
                COVERAGE_RECORDER_DESC);
        ByteCodeUtilsGroovy2.pushConstant(mv, index);
        mv.visitMethodInsn(groovyjarjarasm.asm.Opcodes.INVOKEVIRTUAL,
                COVERAGE_RECORDER_INTERNAL_NAME,
                "inc",
                INC_METHOD_DESCRIPTOR);
    }
}
