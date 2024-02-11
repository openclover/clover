package org.openclover.core.instr.java;

import org.openclover.core.instr.tests.TestDetector;
import org.openclover.core.registry.entities.Modifiers;

import java.util.List;
import java.util.Map;

public class JavaTypeContext implements TestDetector.TypeContext {
    private final Map<String, List<String>> tags;
    private final Modifiers mods;
    private final String pkgName;
    private final String classname;
    private final String superclass;

    public JavaTypeContext(Map<String, List<String>> tags, Modifiers mods, String pkgName, String classname, String superclass) {
        this.tags = tags;
        this.mods = mods;
        this.pkgName = pkgName;
        this.classname = classname;
        this.superclass = superclass;
    }

    @Override
    public Map<String, List<String>> getDocTags() {
        return tags;
    }

    @Override
    public Modifiers getModifiers() {
        return mods;
    }

    @Override
    public String getPackageName() {
        return pkgName;
    }

    @Override
    public String getTypeName() {
        return classname;
    }

    @Override
    public String getSuperTypeName() {
        return superclass;
    }
}
