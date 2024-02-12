package org.openclover.groovy.instr.bytecode;

import org.codehaus.groovy.ast.ClassNode;

/**
 * This class MUST be compiled with Groovy 2.x and ASM 4.x in classpath.
 */
public class ByteCodeUtilsGroovy2 {

    public static String getClassInternalName(ClassNode t) {
        if (t.isPrimaryClassNode()) {
            return getClassInternalName(t.getName());
        }
        return getClassInternalName(t.getTypeClass());
    }

    public static String getClassInternalName(Class<?> t) {
        return org.objectweb.asm.Type.getInternalName(t);
    }

    public static String getClassInternalName(String name) {
        return name.replace('.', '/');
    }


    /**
     * pushConstant(groovyjarjarasm.asm.MethodVisitor as class, int)
     *
     * @param mv    visitor of the method to be modified
     * @param value value to be pushed on stack
     * @see #pushConstant(org.objectweb.asm.MethodVisitor, int)
     */
    public static void pushConstant(groovyjarjarasm.asm.MethodVisitor mv, int value) {
        switch (value) {
            case 0:
                mv.visitInsn(groovyjarjarasm.asm.Opcodes.ICONST_0);
                break;
            case 1:
                mv.visitInsn(groovyjarjarasm.asm.Opcodes.ICONST_1);
                break;
            case 2:
                mv.visitInsn(groovyjarjarasm.asm.Opcodes.ICONST_2);
                break;
            case 3:
                mv.visitInsn(groovyjarjarasm.asm.Opcodes.ICONST_3);
                break;
            case 4:
                mv.visitInsn(groovyjarjarasm.asm.Opcodes.ICONST_4);
                break;
            case 5:
                mv.visitInsn(groovyjarjarasm.asm.Opcodes.ICONST_5);
                break;
            default:
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    mv.visitIntInsn(groovyjarjarasm.asm.Opcodes.BIPUSH, value);
                } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    mv.visitIntInsn(groovyjarjarasm.asm.Opcodes.SIPUSH, value);
                } else {
                    mv.visitLdcInsn(value);
                }
        }
    }

    /**
     * pushConstant(org.objectweb.asm.MethodVisitor,int)
     *
     * @see #pushConstant(groovyjarjarasm.asm.MethodVisitor,int)
     * @param mv visitor of the method to be modified
     * @param value value to be pushed on stack
     */
    public static void pushConstant(org.objectweb.asm.MethodVisitor mv, int value) {
        switch (value) {
            case 0:
                mv.visitInsn(org.objectweb.asm.Opcodes.ICONST_0);
                break;
            case 1:
                mv.visitInsn(org.objectweb.asm.Opcodes.ICONST_1);
                break;
            case 2:
                mv.visitInsn(org.objectweb.asm.Opcodes.ICONST_2);
                break;
            case 3:
                mv.visitInsn(org.objectweb.asm.Opcodes.ICONST_3);
                break;
            case 4:
                mv.visitInsn(org.objectweb.asm.Opcodes.ICONST_4);
                break;
            case 5:
                mv.visitInsn(org.objectweb.asm.Opcodes.ICONST_5);
                break;
            default:
                if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
                    mv.visitIntInsn(org.objectweb.asm.Opcodes.BIPUSH, value);
                } else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
                    mv.visitIntInsn(org.objectweb.asm.Opcodes.SIPUSH, value);
                } else {
                    mv.visitLdcInsn(value);
                }
        }
    }
}
