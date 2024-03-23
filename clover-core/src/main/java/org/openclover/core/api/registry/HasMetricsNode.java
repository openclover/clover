package org.openclover.core.api.registry;

public interface HasMetricsNode extends HasMetrics, IsMetricsComparable {

    String getChildType();

    boolean isEmpty();

    int getNumChildren();

    HasMetricsNode getChild(int i);

    int getIndexOfChild(HasMetricsNode child);

    boolean isLeaf();

}
