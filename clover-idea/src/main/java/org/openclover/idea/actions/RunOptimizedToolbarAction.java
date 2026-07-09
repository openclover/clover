package org.openclover.idea.actions;

import com.intellij.execution.Executor;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.idea.junit.CloverTestOptimizationExecutor;
import org.openclover.idea.util.ui.CloverIcons;

/**
 * Top-level main-toolbar button that runs the currently selected run configuration with OpenClover's
 * "Run optimized" executor - the same thing the platform offers inside the run widget's "More" (⋮) menu,
 * promoted next to Run / Debug so optimized test runs are one click away.
 * <p>
 * The New UI run widget hard-codes its top-level buttons to Run and Debug, so this is registered as a standalone
 * action placed immediately after the run widget in {@code MainToolbarRight} rather than injected into it.
 * <p>
 * It is only enabled/visible when the selected configuration can actually be run optimized (i.e. a
 * {@link ProgramRunner} accepts it for our executor - in practice a JUnit configuration).
 */
public class RunOptimizedToolbarAction extends AnAction {

    public RunOptimizedToolbarAction() {
        super("Run Optimized", "Run the selected tests optimized with OpenClover", CloverIcons.CLOVERIZED_RUN);
    }

    @Nullable
    private static Executor optimizedExecutor() {
        return ExecutorRegistry.getInstance().getExecutorById(CloverTestOptimizationExecutor.EXECUTOR_ID);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final Executor executor = optimizedExecutor();
        if (project == null || executor == null) {
            return;
        }
        final RunnerAndConfigurationSettings settings = RunManager.getInstance(project).getSelectedConfiguration();
        if (settings != null) {
            ProgramRunnerUtil.executeConfiguration(settings, executor);
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(canRunOptimized(e.getProject()));
    }

    private static boolean canRunOptimized(@Nullable Project project) {
        final Executor executor = optimizedExecutor();
        if (project == null || executor == null) {
            return false;
        }
        final RunnerAndConfigurationSettings settings = RunManager.getInstance(project).getSelectedConfiguration();
        return settings != null
                && ProgramRunner.getRunner(executor.getId(), settings.getConfiguration()) != null;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // update() only queries RunManager / ProgramRunner (model state) - no Swing.
        return ActionUpdateThread.BGT;
    }
}
