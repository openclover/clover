package com.atlassian.clover.reporters.html;

import com.atlassian.clover.api.registry.HasMetrics;

public class TestClassCoverageThresholdFilter extends TestClassFilter {
    @Override
    public boolean accept(HasMetrics hm) {
        return super.accept(hm) && hm.getMetrics().getPcCoveredElements() < 1;
    }
}
