package com.atlassian.clover.idea.coverage.monitor;

import com.atlassian.clover.idea.coverage.CoverageManager;

public abstract class AbstractCoverageMonitor
        implements CoverageMonitor {

    protected CoverageManager coverageManager;

    @Override
    public void setCoverageManager(CoverageManager subject) {
        coverageManager = subject;
    }

    public CoverageManager getCoverageManager() {
        return coverageManager;
    }
}
