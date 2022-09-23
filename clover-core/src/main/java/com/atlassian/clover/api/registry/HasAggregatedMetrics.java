package com.atlassian.clover.api.registry;

/**
 *
 */
public interface HasAggregatedMetrics {
    int getAggregatedComplexity();

    void setAggregatedComplexity(int aggregatedComplexity);

    int getAggregatedStatementCount();

    void setAggregatedStatementCount(int aggregatedStatements);
}