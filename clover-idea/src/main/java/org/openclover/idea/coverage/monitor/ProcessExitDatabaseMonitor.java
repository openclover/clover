package org.openclover.idea.coverage.monitor;

import org.openclover.idea.config.IdeaCloverConfig;
import org.openclover.idea.coverage.CoverageManager;
import org.openclover.idea.junit.JUnitOptimizingProgramRunnerBase;
import com.intellij.execution.ExecutionAdapter;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public class ProcessExitDatabaseMonitor extends ExecutionAdapter {
    private final CoverageManager coverageManager;
    private final IdeaCloverConfig config;

    public ProcessExitDatabaseMonitor(CoverageManager coverageManager, IdeaCloverConfig config) {
        this.coverageManager = coverageManager;
        this.config = config;
    }

    @Override
    public void processTerminated(@NotNull RunProfile runProfile, @NotNull ProcessHandler handler) {
        reloadCoverage(JUnitOptimizingProgramRunnerBase.wasExplicitCoverageLoadRequested(handler));
        super.processTerminated(runProfile, handler);
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
