package org.openclover.idea.coverage.monitor;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.junit.JUnitOptimizingProgramRunnerBase;

public class ProcessExitDatabaseMonitor implements ExecutionListener {
    private final CoverageManager coverageManager;
    private final IdeaCloverConfig config;

    public ProcessExitDatabaseMonitor(CoverageManager coverageManager, IdeaCloverConfig config) {
        this.coverageManager = coverageManager;
        this.config = config;
    }

    @Override
    public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env,
                                  @NotNull ProcessHandler handler, int exitCode) {
        reloadCoverage(JUnitOptimizingProgramRunnerBase.wasExplicitCoverageLoadRequested(handler));
    }

    private void reloadCoverage(final boolean forceCoverageLoad) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if ((forceCoverageLoad || config.isAutoRefresh())) {
                if (coverageManager.canLoadCoverageData()) {
                    coverageManager.loadCoverageData(forceCoverageLoad);
                }
            }
        });
    }
}
