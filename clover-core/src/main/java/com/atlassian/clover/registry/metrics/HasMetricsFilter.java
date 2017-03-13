package com.atlassian.clover.registry.metrics;

import com.atlassian.clover.api.registry.HasMetrics;

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

    public boolean accept(HasMetrics hm);

    public static interface Invertable extends HasMetricsFilter {
        public boolean isInverted();
        public Invertable invert();
    }
}
