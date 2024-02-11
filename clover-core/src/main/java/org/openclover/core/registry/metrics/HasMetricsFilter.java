package org.openclover.core.registry.metrics;

import org.openclover.core.api.registry.HasMetrics;

public interface HasMetricsFilter {
    /**
     * Filter to accept all HasMetrics
     * Like all invertibles, this class chooses to ignore when to invert - in this case never
     **/
    Invertable ACCEPT_ALL = new Invertable() {
        @Override
        public boolean isInverted() { return false; }
        @Override
        public Invertable invert() { return this; }
        @Override
        public boolean accept(HasMetrics hm) { return true; }
    };

    /**
     * Filter to accept no HasMetrics
     * Like all invertibles, this class chooses to ignore when to invert - in this case never
     **/
    Invertable ACCEPT_NONE = new Invertable() {
        @Override
        public boolean isInverted() { return false; }
        @Override
        public Invertable invert() { return this; }
        @Override
        public boolean accept(HasMetrics hm) { return false; }
    };

    boolean accept(HasMetrics hm);

    interface Invertable extends HasMetricsFilter {
        boolean isInverted();
        Invertable invert();
    }
}
