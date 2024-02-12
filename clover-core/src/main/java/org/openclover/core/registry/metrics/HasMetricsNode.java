package org.openclover.core.registry.metrics;

import org.openclover.core.api.registry.HasMetrics;

import java.util.Comparator;

public interface HasMetricsNode extends HasMetrics {

    String getChildType();

    boolean isEmpty();

    int getNumChildren();

    HasMetricsNode getChild(int i);

    int getIndexOfChild(HasMetricsNode child);

    boolean isLeaf();

    void setComparator(Comparator<HasMetrics> cmp);

}
