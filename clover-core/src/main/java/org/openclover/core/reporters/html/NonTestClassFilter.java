package org.openclover.core.reporters.html;

import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.registry.entities.BaseClassInfo;
import org.openclover.core.registry.metrics.HasMetricsFilter;

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
