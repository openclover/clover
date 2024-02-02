package org.openclover.idea.coverage.monitor;

import org.openclover.idea.coverage.CoverageManager;

public interface CoverageMonitor {

    void start();

    void stop();

    void setCoverageManager(CoverageManager subject);
}
