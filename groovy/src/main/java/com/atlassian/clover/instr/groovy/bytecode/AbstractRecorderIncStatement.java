package com.atlassian.clover.instr.groovy.bytecode;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.classgen.BytecodeInstruction;

import java.lang.reflect.Constructor;

/**
 * Bytecode for calling R().inc(int) - TODO doesn't currently work due to JVM class file verification errors
 */
public abstract class AbstractRecorderIncStatement extends BytecodeInstruction {

    protected final ClassNode clazz;
    protected final String recorderFieldName;
    protected final int index;

    /**
     * Due to fact that in Groovy 1.x MethodVisitor is an interface while in Groovy 2.x is a class, we
     * get IncompatibleClassChangeError at runtime if code is compiled under one Groovy version and
     * runs under another one. Workaround: compile separately and use reflections to get an instance.
     * @return AbstractRecorderIncStatement or <code>null</code> if failed to instantiate
     */
    public static AbstractRecorderIncStatement newInstance(final ClassNode clazz, final String recorderFieldName, final int index) {
        String className = "<AbstractRecorderIncStatement>";
        try {
            final Class methodVisitor = Class.forName("groovyjarjarasm.asm.MethodVisitor");
            if (methodVisitor.isInterface()) {
                // Try Groovy1
                className = "com.atlassian.clover.instr.groovy.bytecode.RecorderIncStatementGroovy1";
            } else {
                // Try Groovy2
                className = "com.atlassian.clover.instr.groovy.bytecode.RecorderIncStatementGroovy2";
            }
            final Class<?> recorderIncStatement = Class.forName(className);
            final Constructor recorderConstructor = recorderIncStatement.getConstructor(ClassNode.class, String.class, int.class);
            return (AbstractRecorderIncStatement)recorderConstructor.newInstance(clazz, recorderFieldName, index);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to instantiate " + className + " through reflections", ex);
        }
    }

    protected AbstractRecorderIncStatement(final ClassNode clazz, final String recorderFieldName, final int index) {
        this.clazz = clazz;
        this.recorderFieldName = recorderFieldName;
        this.index = index;
    }

}
