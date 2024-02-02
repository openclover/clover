package com.atlassian.clover.idea.coverage.monitor;

import com.atlassian.clover.idea.coverage.CoverageManager;

public interface CoverageMonitor {

    void start();

    void stop();

    void setCoverageManager(CoverageManager subject);
}
