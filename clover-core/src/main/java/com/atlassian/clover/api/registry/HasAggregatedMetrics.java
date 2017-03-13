package com.atlassian.clover.api.registry;

/**
 *
 */
public interface HasAggregatedMetrics {
    public int getAggregatedComplexity();

    public void setAggregatedComplexity(int aggregatedComplexity);

    public int getAggregatedStatementCount();

    public void setAggregatedStatementCount(int aggregatedStatements);
}