package org.openclover.idea;

import org.openclover.core.api.registry.HasMetrics;

public interface HasMetricsListener {
    void valueChanged(HasMetrics hasMetrics);
}
