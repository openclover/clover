package com.atlassian.clover.api.registry;

public interface HasMetrics {

    String getName();

    BlockMetrics getMetrics();

    BlockMetrics getRawMetrics();

    void setMetrics(BlockMetrics metrics);

}
