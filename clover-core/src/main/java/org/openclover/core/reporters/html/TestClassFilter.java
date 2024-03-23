package org.openclover.core.reporters.html;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.HasMetricsFilter;

/**
 * Filters all test classes
 */
public class TestClassFilter implements HasMetricsFilter {
    @Override
    public boolean accept(HasMetrics hm) {
        if (hm instanceof ClassInfo) {
            final ClassInfo classInfo = (ClassInfo) hm;
            return !classInfo.isTestClass() && !classInfo.isInterface();
        }
        return false;
    }
}
