package org.openclover.core.api.registry;

public interface HasMetrics {

    String getName();

    BlockMetrics getMetrics();

    BlockMetrics getRawMetrics();

    void setMetrics(BlockMetrics metrics);

}
