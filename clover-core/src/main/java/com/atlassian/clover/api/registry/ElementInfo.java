package com.atlassian.clover.api.registry;

/**
 *
 */
public interface ElementInfo extends SourceInfo {
    int getHitCount();

    ContextSet getContext();

    /**
     * Returns cyclomatic complexity of this code element
     * @return int complexity
     */
    int getComplexity();
}

