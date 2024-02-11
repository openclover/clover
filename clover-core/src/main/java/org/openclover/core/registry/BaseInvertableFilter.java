package org.openclover.core.registry;

import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.metrics.HasMetricsFilter;

public abstract class BaseInvertableFilter implements HasMetricsFilter.Invertable {
    private final boolean inverted;

    public BaseInvertableFilter() {
        this(false);
    }

    public BaseInvertableFilter(boolean inverted) {
        this.inverted = inverted;
    }

    @Override
    public final boolean isInverted() {
        return inverted;
    }

    @Override
    public abstract BaseInvertableFilter invert();

    @Override
    public abstract boolean accept(HasMetrics hm);
}
