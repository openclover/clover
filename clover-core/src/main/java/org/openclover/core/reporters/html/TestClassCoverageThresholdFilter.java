package org.openclover.core.reporters.html;

import org.openclover.core.api.registry.HasMetrics;

public class TestClassCoverageThresholdFilter extends TestClassFilter {
    @Override
    public boolean accept(HasMetrics hm) {
        return super.accept(hm) && hm.getMetrics().getPcCoveredElements() < 1;
    }
}
