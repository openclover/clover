package com.atlassian.clover.reporters.html;

import com.atlassian.clover.registry.metrics.HasMetricsFilter;
import com.atlassian.clover.api.registry.HasMetrics;
import com.atlassian.clover.registry.entities.BaseClassInfo;

/**
 * Filters all non-test classes
 */
public class NonTestClassFilter implements HasMetricsFilter {
    @Override
    public boolean accept(HasMetrics hm) {
        if (hm instanceof BaseClassInfo) {
            final BaseClassInfo baseClassInfo = (BaseClassInfo) hm;
            return baseClassInfo.isTestClass();
        }
        return false;
    }
}
