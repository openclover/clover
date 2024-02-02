package org.openclover.idea.coverage.monitor;

import org.openclover.idea.coverage.CoverageManager;

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
