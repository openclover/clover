package org.openclover.idea;

import com.atlassian.clover.api.registry.HasMetrics;

public interface HasMetricsListener {
    void valueChanged(HasMetrics hasMetrics);
}
