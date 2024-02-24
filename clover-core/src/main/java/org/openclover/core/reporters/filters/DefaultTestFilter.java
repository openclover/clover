package org.openclover.core.reporters.filters;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.BaseInvertableFilter;
import org.openclover.core.registry.entities.BaseClassInfo;

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
        if (hasMetrics instanceof BaseClassInfo) {
            BaseClassInfo classInfo = (BaseClassInfo) hasMetrics;
            return isInverted() ^ (classInfo.isTestClass() || classInfo.getContainingFile().isTestFile());
        }

        return true;
    }
}
