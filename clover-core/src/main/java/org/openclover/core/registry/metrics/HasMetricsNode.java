package org.openclover.core.registry.metrics;

import org.openclover.core.api.registry.HasMetrics;
import org.openclover.core.api.registry.IsMetricsComparable;

import java.util.Comparator;

public interface HasMetricsNode extends HasMetrics, IsMetricsComparable {

    String getChildType();

    boolean isEmpty();

    int getNumChildren();

    HasMetricsNode getChild(int i);

    int getIndexOfChild(HasMetricsNode child);

    boolean isLeaf();

}
