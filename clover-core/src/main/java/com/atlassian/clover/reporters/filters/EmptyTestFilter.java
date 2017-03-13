package com.atlassian.clover.reporters.filters;

import com.atlassian.clover.registry.BaseInvertableFilter;
import com.atlassian.clover.api.registry.HasMetrics;

public class EmptyTestFilter extends BaseInvertableFilter {
    public EmptyTestFilter() {
        this(false);
    }

    EmptyTestFilter(boolean inverted) {
        super(inverted);
    }

    @Override
    public EmptyTestFilter invert() {
        return new EmptyTestFilter(!isInverted());
    }

    @Override
    public boolean accept(HasMetrics hm) {
        return isInverted();
    }
}
