package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;

class ClassNodeSafeEvalMethodsTransformer implements ClassNodeTransformer {
    @Override
    public void transform(ClassNode classNode, GroovyInstrumentationResult flags) {
        createSafeEvalMethods(classNode, flags);
    }

    private void createSafeEvalMethods(final ClassNode clazz, final GroovyInstrumentationResult flags) {
        if (flags.safeExprUsed) {
            for (final MethodNode methodNode : flags.safeEvalMethods.values()) {
                clazz.addMethod(methodNode);
            }
        }
    }
}
