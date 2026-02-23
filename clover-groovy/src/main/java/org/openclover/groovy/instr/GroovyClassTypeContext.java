package org.openclover.groovy.instr;

import org.codehaus.groovy.ast.ClassNode;
import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.registry.entities.Modifiers;

import java.util.Collections;
import java.util.List;
import java.util.Map;

class GroovyClassTypeContext implements TestDetector.TypeContext {
    private final ClassNode clazz;

    GroovyClassTypeContext(final ClassNode clazz) {
        this.clazz = clazz;
    }

    @Override
    public String getPackageName() {
        return clazz.getPackageName();
    }

    @Override
    public String getTypeName() {
        return clazz.getNameWithoutPackage();
    }

    @Override
    public String getSuperTypeName() {
        return clazz.getSuperClass().getName();
    }

    @Override
    public Map<String, List<String>> getDocTags() {
        return Collections.emptyMap();
    }

    @Override
    public Modifiers getModifiers() {
        return GroovyModelMiner.extractModifiers(clazz);
    }
}
