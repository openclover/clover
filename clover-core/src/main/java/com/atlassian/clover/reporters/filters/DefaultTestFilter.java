package com.atlassian.clover.reporters.filters;

import com.atlassian.clover.registry.BaseInvertableFilter;
import com.atlassian.clover.registry.entities.BaseClassInfo;
import com.atlassian.clover.api.registry.HasMetrics;

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
