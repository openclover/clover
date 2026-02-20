package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.openclover.runtime.CloverNames;
import org.openclover.runtime.recorder.pertest.SnifferType;
import org_openclover_runtime.TestNameSniffer;

import static groovyjarjarasm.asm.Opcodes.ACC_FINAL;
import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static groovyjarjarasm.asm.Opcodes.ACC_STATIC;
import static groovyjarjarasm.asm.Opcodes.ACC_SYNTHETIC;

class ClassNodeTestSnifferTransformer implements ClassNodeTransformer {
    @Override
    public void transform(ClassNode classNode, GroovyInstrumentationResult flags) {
        createTestNameSnifferField(classNode, flags);
    }

    /**
     * Add the field named {@link CloverNames#CLOVER_TEST_NAME_SNIFFER} to the class.
     *
     * @param clazz class to be extended
     * @param flags flags after first instrumentation pass
     */
    private void createTestNameSnifferField(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.isTestClass) {
            createTestNameSnifferField(clazz,
                    flags.isSpockSpecification ? SnifferType.SPOCK
                            : (flags.isParameterizedJUnit ? SnifferType.JUNIT : SnifferType.NULL));
        }
    }

    /**
     * Add the field named {@link CloverNames#CLOVER_TEST_NAME_SNIFFER} to the class.
     *
     * @param clazz       class to be extended
     * @param snifferType type of the sniffer to be embedded
     */
    private void createTestNameSnifferField(final ClassNode clazz, final SnifferType snifferType) {
        final Expression fieldInitializationExpr;
        switch (snifferType) {
            case JUNIT:
            case SPOCK:
                // new TestNameSniffer.Simple()
                fieldInitializationExpr = new ConstructorCallExpression(
                        createSimpleSnifferClassNode(), ArgumentListExpression.EMPTY_ARGUMENTS);
                break;
            case NULL:
            default:
                // TestNameSniffer.NULL_INSTANCE
                fieldInitializationExpr = new PropertyExpression(
                        new ClassExpression(ClassHelper.make(TestNameSniffer.class)), "NULL_INSTANCE");
                break;
        }

        // add field
        // public static final __CLRx_y_z_TEST_NAME_SNIFFER = ...
        clazz.addField( CloverNames.CLOVER_TEST_NAME_SNIFFER,
                ACC_STATIC | ACC_PUBLIC | ACC_SYNTHETIC | ACC_FINAL,
                ClassHelper.make(org_openclover_runtime.TestNameSniffer.class),
                fieldInitializationExpr);
    }

    /**
     * @return ClassNode representing TestNameSniffer.Simple class
     */
    private ClassNode createSimpleSnifferClassNode() {
        return ClassHelper.make(TestNameSniffer.Simple.class);
    }
}
