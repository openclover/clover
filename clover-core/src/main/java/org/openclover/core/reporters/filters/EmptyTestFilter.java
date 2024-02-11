package org.openclover.core.reporters.filters;

import org.openclover.core.registry.BaseInvertableFilter;
import org.openclover.core.api.registry.HasMetrics;

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
