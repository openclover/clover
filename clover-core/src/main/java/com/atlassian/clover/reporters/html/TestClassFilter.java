package com.atlassian.clover.reporters.html;

import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.BaseClassInfo;

/**
 * Filters all test classes
 */
public class TestClassFilter implements HasMetricsFilter {
    @Override
    public boolean accept(HasMetrics hm) {
        if (hm instanceof BaseClassInfo) {
            final BaseClassInfo baseClassInfo = (BaseClassInfo) hm;
            return !baseClassInfo.isTestClass() && !baseClassInfo.isInterface();
        }
        return false;
    }
}
