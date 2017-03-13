package com.atlassian.clover.api.registry;

/**
 *
 */
public interface ElementInfo extends SourceInfo {
    public int getHitCount();

    public ContextSet getContext();

    /**
     * Returns cyclomatic complexity of this code element
     * @return int complexity
     */
    int getComplexity();
}

