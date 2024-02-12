package org.openclover.idea.junit;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openclover.core.optimization.OptimizationSession;
import org.openclover.idea.ProjectPluginViaReflection;
import org.openclover.idea.junit.config.OptimizedConfigurationSettings;
import org.openclover.idea.junit.config.OptimizedConfigurationSettingsEditor;

import java.io.File;

public class JUnitOptimizingProgramRunnerIdea13 extends DefaultJavaProgramRunner implements JUnitOptimizingProgramRunner, SavingsReporter {

    private static final String RUNNER_ID = "Clover Optimizing Runner IDEA 13+";

    // using composition instead of inheritance because JUnitOptimizingProgramRunnerIdea13 must extend
    // DefaultJavaProgramRunner from a specific IDEA version (due to method signatures)
    private final JUnitOptimizingProgramRunnerBase runnerBase = new JUnitOptimizingProgramRunnerBase();

    @Override
    public void patch(JavaParameters javaParameters, RunnerSettings runnerSettings, RunProfile runProfile, boolean b) throws ExecutionException {
        // fallback to default settings in case when runnerSettings does not have what we expect
        final OptimizedConfigurationSettings configurationSettings = runnerSettings instanceof OptimizedConfigurationSettings
                ? (OptimizedConfigurationSettings) runnerSettings : new OptimizedConfigurationSettings();
        runnerBase.patchImpl(javaParameters, runProfile, configurationSettings);
    }

    @Override
    public SettingsEditor getSettingsEditor(Executor executor, RunConfiguration configuration) {
        final String configurationId = configuration.getType().getId();
        return (runnerBase.getJUnitConfigurationId().equals(configurationId)
                || runnerBase.getAndroidJunitConfigurationId().equals(configurationId)) ?
                new OptimizedConfigurationSettingsEditor() : null;
    }

    @Nullable
    @Override
    public RunnerSettings createConfigurationData(ConfigurationInfoProvider settingsProvider) {
        return new OptimizedConfigurationSettings();
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (profile instanceof RunConfiguration) {
            final Project project = ((RunConfiguration) profile).getProject();
            return executorId.equals(CloverTestOptimizationExecutor.EXECUTOR_ID) && ProjectPluginViaReflection.getPlugin(project).getConfig().isEnabled();
        } else {
            return false;
        }
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
        if (runnerBase.checkExecutionEnvironment(executionEnvironment)) {
            super.execute(executionEnvironment);
        }
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment executionEnvironment,
                        @Nullable Callback callback) throws ExecutionException {
        if (runnerBase.checkExecutionEnvironment(executionEnvironment)) {
            super.execute(executionEnvironment, callback);
        }
    }

    @Override
    public void onProcessStarted(RunnerSettings runnerSettings, ExecutionResult executionResult) {
        executionResult.getProcessHandler().putUserData(runnerBase.getExplicitCoverageLoadRequestKey(), true);
    }

    /**
     * Retrieves the Idea-specific JUnit runner synchronization socket.
     *
     * @param javaParameters configured by Idea test runner guts for current test run
     * @return configured synchronization socket or -1 if not found (eg. Idea pre-9)
     */
    @Override
    public int retrieveJUnitSychSocket(@NotNull JavaParameters javaParameters) {
        return runnerBase.retrieveJUnitSychSocket(javaParameters);
    }

    @Override
    public void replaceJUnitSynchSocket(@NotNull JavaParameters javaParameters, int newSocket) {
        runnerBase.replaceJUnitSynchSocket(javaParameters, newSocket);
    }

    @Override
    @Nullable
    public File retrieveTmpFile(@NotNull JavaParameters javaParameters) {
        return runnerBase.retrieveTmpFile(javaParameters);
    }

    @Override
    public void reportSavings(@Nullable final Project project, @Nullable final OptimizationSession optimizationSession) {
        runnerBase.reportSavings(project, optimizationSession);
    }
}