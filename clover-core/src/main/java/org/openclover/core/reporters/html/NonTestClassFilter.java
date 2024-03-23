package org.openclover.core.reporters.html;

import org.openclover.core.api.registry.ClassInfo;
import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.HasMetricsFilter;

/**
 * Filters all non-test classes
 */
public class NonTestClassFilter implements HasMetricsFilter {
    @Override
    public boolean accept(HasMetrics hm) {
        if (hm instanceof ClassInfo) {
            final ClassInfo baseClassInfo = (ClassInfo) hm;
            return baseClassInfo.isTestClass();
        }
        return false;
    }
}
