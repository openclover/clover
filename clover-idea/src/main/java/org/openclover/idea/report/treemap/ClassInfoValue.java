package org.openclover.idea.report.treemap;

import org.openclover.core.registry.entities.FullClassInfo;
import clover.net.sf.jtreemap.swing.ValuePercent;

public class ClassInfoValue extends ValuePercent {
    private final FullClassInfo classInfo;

    /**
     * Required for HSBTreeMapColorProvider.setValues(), which uses value.getClass().newInstance() to create a copy for
     * maxValue field. Obviously cloning() is a path to the Dark Side.<p>
     * How cute...
     */
    public ClassInfoValue() {
        classInfo = null;
    }

    public ClassInfoValue(FullClassInfo classInfo) {
        super(classInfo.getMetrics().getPcCoveredElements() * 100f);

        this.classInfo = classInfo;
    }

    public FullClassInfo getClassInfo() {
        return classInfo;
    }
}
