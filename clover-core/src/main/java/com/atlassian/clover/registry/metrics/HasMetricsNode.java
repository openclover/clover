package com.atlassian.clover.registry.metrics;

import com.atlassian.clover.api.registry.HasMetrics;

import java.util.Comparator;

public interface HasMetricsNode extends HasMetrics {

    String getChildType();

    boolean isEmpty();

    int getNumChildren();

    HasMetricsNode getChild(int i);

    int getIndexOfChild(HasMetricsNode child);

    boolean isLeaf();

    void setComparator(Comparator cmp);

}
