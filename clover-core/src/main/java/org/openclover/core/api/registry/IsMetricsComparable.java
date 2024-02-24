package org.openclover.core.api.registry;

import java.util.Comparator;

public interface IsMetricsComparable {
    public void setComparator(final Comparator<HasMetrics> cmp);
}
