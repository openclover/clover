package com.atlassian.clover.idea.junit;

import com.atlassian.clover.Logger;
import com.atlassian.clover.idea.CloverToolWindowId;
import com.atlassian.clover.idea.junit.config.OptimizedConfigurationSettings;
import com.atlassian.clover.idea.util.l10n.CloverIdeaPluginMessages;
import com.atlassian.clover.idea.util.ui.CloverIcons;
import com.atlassian.clover.idea.util.ui.MessageDialogs;
import com.atlassian.clover.optimization.OptimizationSession;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindowManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JUnitOptimizingProgramRunnerBase implements SavingsReporter {
    private static final String JUNIT_CONFIGURATION_ID = "JUnit";
    private static final String ANDROID_JUNIT_CONFIGURATION_ID = "AndroidJUnit";
    private static final Key<Boolean> EXPLICIT_COVERAGE_LOAD_REQUEST_KEY = new Key<>("com.cenqua.clover.explicit_coverage_load_request");

    public static boolean wasExplicitCoverageLoadRequested(ProcessHandler processHandler) {
        final Boolean requested = processHandler.getUserData(EXPLICIT_COVERAGE_LOAD_REQUEST_KEY);
        return requested != null && requested;
    }

    @NotNull
    String getJUnitConfigurationId() {
        return JUNIT_CONFIGURATION_ID;
    }

    @NotNull
    String getAndroidJunitConfigurationId() { return ANDROID_JUNIT_CONFIGURATION_ID; }

    Key<Boolean> getExplicitCoverageLoadRequestKey() {
        return EXPLICIT_COVERAGE_LOAD_REQUEST_KEY;
    }

    boolean checkExecutionEnvironment(@NotNull ExecutionEnvironment executionEnvironment) {
        if (executionEnvironment.getRunProfile() instanceof RunConfiguration) {
            final RunConfiguration runConfiguration = (RunConfiguration) executionEnvironment.getRunProfile();
            final String configuratonId = runConfiguration.getType().getId();
            if (JUNIT_CONFIGURATION_ID.equals(configuratonId) || ANDROID_JUNIT_CONFIGURATION_ID.equals(configuratonId)) {
                    return true;
            }
        }
        MessageDialogs.showInfoMessage(null, CloverIdeaPluginMessages.getString("launch.optimized.junitonly"), "Clover Test Optimization");
        return false;
    }

    void patchImpl(JavaParameters javaParameters, RunProfile runProfile,
                   OptimizedConfigurationSettings configurationSettings) throws ExecutionException {
        final Project currentProject;
        if (runProfile instanceof RunConfiguration) {
            currentProject = ((RunConfiguration) runProfile).getProject();
        } else {
            final Project[] projects = ProjectManager.getInstance().getOpenProjects();
            for (Project project : projects) {
                ToolWindowManager.getInstance(project).notifyByBalloon(CloverToolWindowId.TOOL_WINDOW_ID, MessageType.ERROR,
                        "Cannot retrieve project for current run configuration");
                reportSavings(project, "Cannot retrieve project for current run configuration. No Clover test optimization. ");
            }
            return;
        }

        final String configurationId = ((RunConfiguration) runProfile).getType().getId();
        if (!JUNIT_CONFIGURATION_ID.equals(configurationId) && !ANDROID_JUNIT_CONFIGURATION_ID.equals(configurationId)) {
            // 'Cannot happen' case
            Logger.getInstance().warn("Run profile is not a recognized JUnit or Android JUnit configuration");
            reportSavings(currentProject, "Run profile is not a recognized JUnit or Android JUnit configuration. No Clover test optimization.");
            return;
        }
        final File tmpFile = retrieveTmpFile(javaParameters);
        if (tmpFile != null) {
            final int jUnitSychSocket = retrieveJUnitSychSocket(javaParameters);
            if (jUnitSychSocket == -1) {
                final FileBasedJUnitClassListProcessor processor = new FileBasedJUnitClassListProcessor(
                        this, tmpFile, currentProject, configurationSettings);
                final File newFile = processor.processWhenFileNotEmpty();
                if (newFile != null) {
                    replaceTmpFile(javaParameters, newFile);
                }
            } else {
                final SocketBasedJUnitClassListProcessor processor = new SocketBasedJUnitClassListProcessor(
                        this, tmpFile, currentProject, configurationSettings);
                final int newSocket = processor.processWhenSocketReady(jUnitSychSocket);
                replaceJUnitSynchSocket(javaParameters, newSocket);
            }
        }
    }

    void replaceJUnitSynchSocket(@NotNull JavaParameters javaParameters, int newSocket) {
        final String socket = findSocketParamValue(javaParameters);
        javaParameters.getProgramParametersList().replaceOrAppend("-socket" + socket, "-socket" + newSocket);
    }

    @Override
    public void reportSavings(@Nullable final Project project, @Nullable final OptimizationSession optimizationSession) {
        if (project != null && optimizationSession != null) {
            reportSavings(project, optimizationSession.getPlainSummary());
        }
    }

    /**
     * Retrieves the temporary file containing info about which classes are about to be tested.<p>
     *
     * @param javaParameters configured by Idea test runner guts for current test run
     * @return File specified as @path program param or null if file is not defined or inaccessible
     */
    @Nullable
    File retrieveTmpFile(@NotNull JavaParameters javaParameters) {
        final String path = findTmpFileName(javaParameters);
        if (path != null) {
            final File tmpFile = new File(path);
            if (tmpFile.exists() && tmpFile.canRead() && tmpFile.canWrite()) {
                return tmpFile;
            } else {
                Logger.getInstance().warn("Cannot access JUnit temporary file at " + path);
                return null;
            }
        } else {
            return null; /* expected for single test class or single test method invocation */
        }
    }

    /**
     * Retrieves the Idea-specific JUnit runner synchronization socket.<p>
     *
     * @param javaParameters configured by Idea test runner guts for current test run
     * @return configured synchronization socket or -1 if not found (eg. Idea pre-9)
     */
    int retrieveJUnitSychSocket(@NotNull JavaParameters javaParameters) {
        final String param = findSocketParamValue(javaParameters);
        return param == null ? -1 : Integer.parseInt(param);
    }

    /**
     * Display tooltip with optimization savings summary. Runs in a dispatch thread.
     * @param project   current project
     * @param optimizationMessage message to be displayed
     */
    private void reportSavings(@NotNull final Project project, @NotNull final String optimizationMessage) {
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    reportSavings(project, optimizationMessage);
                }
            });
        } else {
            final String windowId = ExecutorRegistry.getInstance().getExecutorById(CloverTestOptimizationExecutor.EXECUTOR_ID).getToolWindowId();
            ToolWindowManager.getInstance(project).notifyByBalloon(windowId, MessageType.INFO, optimizationMessage, CloverIcons.CLOVER_BIG, null);
        }
    }

    @Nullable
    private String findSocketParamValue(@NotNull JavaParameters javaParameters) {
        Pattern pattern = Pattern.compile("^-socket(\\d+)$");
        for (String param : javaParameters.getProgramParametersList().getList()) {
            final Matcher matcher = pattern.matcher(param);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private static void replaceTmpFile(JavaParameters javaParameters, File newFile) {
        final String file = findTmpFileName(javaParameters);
        javaParameters.getProgramParametersList().replaceOrAppend("@" + file, "@" + newFile.getPath());
    }

    private static String findTmpFileName(@NotNull JavaParameters javaParameters) {
        for (String param : javaParameters.getProgramParametersList().getList()) {
            if (param.length() > 1 && param.charAt(0) == '@' && param.charAt(1) == '/') {
                return param.substring(1);
            }
        }
        return null;
    }
}
