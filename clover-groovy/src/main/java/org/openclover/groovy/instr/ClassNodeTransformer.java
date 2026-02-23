package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassNode;

/**
 * Perform modification of the {@link ClassNode} to add instrumentation code.
 */
interface ClassNodeTransformer {
    void transform(ClassNode classNode, GroovyInstrumentationResult flags);
}
