package org.openclover.idea.report.treemap;

import net.sf.jtreemap.swing.ValuePercent;
import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.registry.entities.FullClassInfo;

public class ClassInfoValue extends ValuePercent {
    private final ClassInfo classInfo;

    /**
     * Required for HSBTreeMapColorProvider.setValues(), which uses value.getClass().newInstance() to create a copy for
     * maxValue field. Obviously cloning() is a path to the Dark Side.<p>
     * How cute...
     */
    public ClassInfoValue() {
        classInfo = null;
    }

    public ClassInfoValue(ClassInfo classInfo) {
        super(classInfo.getMetrics().getPcCoveredElements() * 100f);

        this.classInfo = classInfo;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }
}
