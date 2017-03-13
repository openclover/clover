package com.atlassian.clover.eclipse.core.projects.builder;

import org.eclipse.core.resources.IncrementalProjectBuilder;

public abstract class BaseCloverBuilder extends IncrementalProjectBuilder {
    protected String kindToString(int kind) {
        switch(kind) {
            case IncrementalProjectBuilder.AUTO_BUILD:
                return "AUTO_BUILD";
            case IncrementalProjectBuilder.CLEAN_BUILD:
                return "CLEAN_BUILD";
            case IncrementalProjectBuilder.FULL_BUILD:
                return "FULL_BUILD";
            case IncrementalProjectBuilder.INCREMENTAL_BUILD:
                return "INCREMENTAL_BUILD";
            default:
                return "UNKNOWN";
        }
    }
}
