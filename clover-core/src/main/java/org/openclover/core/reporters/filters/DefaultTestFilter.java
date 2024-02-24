package org.openclover.core.reporters.filters;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.BaseInvertableFilter;

public class DefaultTestFilter extends BaseInvertableFilter {
    public DefaultTestFilter() {
        this(false);
    }

    public DefaultTestFilter(boolean inverted) {
        super(inverted);
    }

    @Override
    public DefaultTestFilter invert() {
        return new DefaultTestFilter(!isInverted());
    }

    @Override
    public boolean accept(HasMetrics hasMetrics) {
        if (hasMetrics instanceof ClassInfo) {
            ClassInfo classInfo = (ClassInfo) hasMetrics;
            return isInverted() ^ (classInfo.isTestClass() || classInfo.getContainingFile().isTestFile());
        }

        return true;
    }
}
