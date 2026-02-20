package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassNode;
import org.jetbrains.annotations.Nullable;

interface ClassNodeTransformer {
    void transform(ClassNode classNode, @Nullable GroovyInstrumentationResult flags);
}
